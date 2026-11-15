# Pitiupi — Plugin Batalha Naval P2P

Implementação de um plugin de **Batalha Naval em rede ponto-a-ponto (P2P)** desenvolvido como Trabalho Prático da disciplina de **Redes de Computadores** (UFSJ), integrado à arquitetura extensível do **Pitiupi**.

* **Autor do Plugin:** Rafael Dayvid

* **Repositório:** [rafael-dayvid/Piutiupi---TP-Flavio](https://github.com/rafael-dayvid/Piutiupi---TP-Flavio)

* **Projeto Base:** [GabrielFSSantos/pitiupi](https://github.com/GabrielFSSantos/pitiupi)

## Sumário

1. [Visão Geral e Arquitetura de Redes](#visão-geral-e-arquitetura-de-redes)

2. [Pré-requisitos](#pré-requisitos)

3. [Como Compilar e Executar](#como-compilar-e-executar)

4. [Como Jogar](#como-jogar)

5. [Estrutura do Plugin](#estrutura-do-plugin)

## Visão Geral e Arquitetura de Redes

O **Pitiupi** possui um núcleo em Java responsável pela descoberta de nós na rede local (presença via UDP) e pelo roteamento de mensagens, enquanto as funcionalidades são carregadas dinamicamente como módulos independentes (`.jar`).

O plugin **Batalha Naval P2P** explora os seguintes conceitos de Redes de Computadores:

* **Carregamento Dinâmico via Java SPI (*Service Provider Interface*):** O núcleo descobre o módulo em tempo de execução por meio do descritor `META-INF/services/pitiupi.models.Plugin`, instanciando `BatalhaNavalPlugin` em um *classloader* isolado sem acoplamento direto no código principal.

* **Transporte Confiável (TCP):** Todas as mensagens do jogo (`BatalhaMessage`) são serializadas e despachadas pelo `MessageDispatcher` usando **`TransportMode.TCP`** e endereçamento direto (**`SendTarget.singlePeer(...)`**), garantindo entrega ordenada e sem perda de pacotes para cada jogada.

* **Protocolo da Camada de Aplicação:** A sincronização entre os dois jogadores ocorre por meio de uma máquina de estados definida em `BatalhaMessage`:

  * `CONVITE` / `ACEITE` / `RECUSA`: Handshake inicial da partida entre desafiante e desafiado.

  * `TIRO`: Envio das coordenadas `(linha, coluna)` do ataque realizado.

  * `RESULTADO_TIRO`: Resposta automática do tabuleiro adversário informando água (`~`), acerto (`X`), embarcação afundada ou vitória.

  * `ENCERRAR`: Notificação de desistência ou fechamento da aba do jogo.

* **Monitoramento em Tempo Real:** A interface conta com um **Log de Protocolo da Aplicação** na parte inferior que exibe todos os pacotes transmitidos (`[TX TCP -> IP]`) e recebidos (`[RX TCP <- IP]`) durante a partida.

## Pré-requisitos

### Linux (Ubuntu / Debian)

Instale o JDK, Apache Ant e Python 3 (usado pelos scripts de gerenciamento do catálogo):

```
sudo apt update
sudo apt install openjdk-21-jdk ant python3

```

### Windows

1. Instale o **JDK 17+** (`winget install Microsoft.OpenJDK.17`).

2. Baixe o **Apache Ant**, extraia e adicione a pasta `bin` às variáveis de ambiente (`PATH`).

## Como Compilar e Executar

Clone o repositório, entre na pasta do projeto e execute o script de build e inicialização:

```
git clone https://github.com/rafael-dayvid/Piutiupi---TP-Flavio.git
cd Piutiupi---TP-Flavio
chmod +x scripts/pitiupi.sh
./scripts/pitiupi.sh run --all

```

*(No Windows, utilize `scripts\pitiupi.cmd run --all`).*

O script compila automaticamente o núcleo (`pitiupi`), o plugin `batalhanaval` (registrado na categoria `Jogos` em `plugins.registry.json`) e os demais plugins padrão, criando os vínculos em `.pitiupi/plugins/`.

### Cenários de Teste

* **Dois computadores na mesma rede local (Wi-Fi ou Cabo):**

  * Execute `./scripts/pitiupi.sh run --all` em ambas as máquinas.

  * Na tela inicial, informe **nomes diferentes** e a **mesma porta** (ex.: `5000`).

* **Mesmo computador (dois terminais):**

  * Abra dois terminais separados e rode `./scripts/pitiupi.sh run --all` em cada um.

  * Informe **nomes diferentes** e **portas distintas** (ex.: `5000` na primeira janela e `5001` na segunda).

## Como Jogar

1. **Abrir o Plugin:** Na barra superior do Pitiupi, acesse a aba **Plugins** e selecione **Batalha Naval P2P** *(recomendado que ambos os jogadores abram a aba antes do início)*.

2. **Organizar a Frota:** Ao abrir, o jogo posiciona aleatoriamente 5 embarcações no tabuleiro **Sua Frota (Defesa)**, totalizando **17 células**:

   * Porta-Aviões (5 células)

   * Encouraçado (4 células)

   * Cruzador (3 células)

   * Submarino (3 células)

   * Destroyer (2 células)

   * *Clique em **Sortear Frota** caso queira gerar uma nova disposição antes da partida começar.*

3. **Desafiar um Adversário:** Na lista lateral esquerda de *peers* conectados, clique com o botão direito sobre o colega e escolha **Batalha Naval P2P → Desafiar para Batalha Naval**.

4. **Aceitar a Partida:** O jogador desafiado verá um diálogo de confirmação para aceitar ou recusar o duelo.

5. **Disparos por Turno:**

   * Quem enviou o desafio realiza o primeiro ataque clicando em qualquer célula do tabuleiro **Radar Inimigo (Ataque)**.

   * Os turnos alternam automaticamente após cada disparo.

   * **`X` (Vermelho):** Acerto em embarcação.

   * **`~` (Azul escuro):** Tiro na água.

6. **Fim de Jogo:** Vence quem afundar primeiro todas as 5 embarcações da frota adversária.

## Estrutura do Plugin

```
plugins/batalhanaval/
├── build.xml
├── nbproject/
│   └── project.properties
└── src/
    ├── META-INF/
    │   └── services/
    │       └── pitiupi.models.Plugin          # Registro SPI do plugin
    └── pitiupi/
        └── batalhanaval/
            ├── plugin/
            │   └── BatalhaNavalPlugin.java    # Ciclo de vida, peer actions e recepção de mensagens
            ├── net/
            │   └── BatalhaMessage.java        # Modelo serializável do protocolo do jogo
            └── GUI/
                └── BatalhaNavalPanel.java     # Interface Swing, tabuleiros 10x10 e log TCP

```