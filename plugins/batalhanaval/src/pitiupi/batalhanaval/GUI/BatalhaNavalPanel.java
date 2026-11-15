package pitiupi.batalhanaval.GUI;

import pitiupi.application.presence.UserPeerView;
import pitiupi.batalhanaval.net.BatalhaMessage;
import pitiupi.control.ConnectionManager;
import pitiupi.application.messaging.MessageDispatcher;
import pitiupi.presentation.shell.PeerIpv4;
import pitiupi.application.messaging.SendTarget;
import pitiupi.application.messaging.TransportMode;
import pitiupi.presentation.shell.LocalNetworkInfo;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.net.InetAddress;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

public class BatalhaNavalPanel extends JPanel {

    private static final int TAM = 10;
    private static final int[] TAMANHOS_NAVIOS = {5, 4, 3, 3, 2};
    private static final String[] NOMES_NAVIOS = {
            "Porta-Aviões (5)", "Encouraçado (4)", "Cruzador (3)", "Submarino (3)", "Destroyer (2)"
    };

    private final ConnectionManager connectionManager;
    private final JButton[][] botoesProprios = new JButton[TAM][TAM];
    private final JButton[][] botoesInimigos = new JButton[TAM][TAM];

    private final int[][] gradePropria = new int[TAM][TAM];
    private final int[] vidasNavios = new int[TAMANHOS_NAVIOS.length + 1];

    private final JLabel lblStatus = new JLabel("Posicione sua frota e desafie um peer na lista lateral.");
    private final JTextArea logRede = new JTextArea(5, 40);
    private final JButton btnSortear = new JButton("Sortear Frota");
    private final JButton btnDesistir = new JButton("Encerrar Partida");

    private InetAddress oponenteAddr;
    private String oponenteNome = "";
    private boolean emPartida = false;
    private boolean meuTurno = false;

    public BatalhaNavalPanel(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel painelTopo = new JPanel(new BorderLayout(10, 5));
        lblStatus.setFont(lblStatus.getFont().deriveFont(Font.BOLD, 14f));
        painelTopo.add(lblStatus, BorderLayout.CENTER);

        JPanel painelBotoes = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnSortear.addActionListener(e -> sortearFrota());
        btnDesistir.setEnabled(false);
        btnDesistir.addActionListener(e -> encerrarPartidaLocal(true));
        painelBotoes.add(btnSortear);
        painelBotoes.add(btnDesistir);
        painelTopo.add(painelBotoes, BorderLayout.EAST);
        add(painelTopo, BorderLayout.NORTH);

        JPanel painelTabuleiros = new JPanel(new GridLayout(1, 2, 15, 0));
        painelTabuleiros.add(criarPainelGrade("Sua Frota (Defesa)", botoesProprios, false));
        painelTabuleiros.add(criarPainelGrade("Radar Inimigo (Ataque)", botoesInimigos, true));
        add(painelTabuleiros, BorderLayout.CENTER);

        logRede.setEditable(false);
        logRede.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane scrollLog = new JScrollPane(logRede);
        scrollLog.setBorder(new TitledBorder("Log de Protocolo da Aplicação (Transporte: TCP)"));
        add(scrollLog, BorderLayout.SOUTH);

        sortearFrota();
    }

    private JPanel criarPainelGrade(String titulo, JButton[][] matriz, boolean clicavel) {
        JPanel container = new JPanel(new BorderLayout());
        container.setBorder(new TitledBorder(titulo));
        JPanel grid = new JPanel(new GridLayout(TAM + 1, TAM + 1, 2, 2));

        grid.add(new JLabel(""));
        for (int c = 0; c < TAM; c++) {
            JLabel lbl = new JLabel(String.valueOf((char) ('A' + c)), SwingConstants.CENTER);
            lbl.setFont(lbl.getFont().deriveFont(Font.BOLD));
            grid.add(lbl);
        }

        for (int l = 0; l < TAM; l++) {
            JLabel lblLinha = new JLabel(String.valueOf(l + 1), SwingConstants.CENTER);
            lblLinha.setFont(lblLinha.getFont().deriveFont(Font.BOLD));
            grid.add(lblLinha);

            for (int c = 0; c < TAM; c++) {
                JButton btn = new JButton();
                btn.setPreferredSize(new Dimension(32, 32));
                btn.setBackground(new Color(210, 230, 250));
                btn.setFocusPainted(false);
                if (clicavel) {
                    final int linha = l;
                    final int coluna = c;
                    btn.setEnabled(false);
                    btn.addActionListener(e -> dispararTiro(linha, coluna));
                }
                matriz[l][c] = btn;
                grid.add(btn);
            }
        }
        container.add(grid, BorderLayout.CENTER);
        return container;
    }

    public void sortearFrota() {
        if (emPartida) return;
        Random rand = new Random();
        for (int l = 0; l < TAM; l++) {
            for (int c = 0; c < TAM; c++) {
                gradePropria[l][c] = 0;
                botoesProprios[l][c].setBackground(new Color(210, 230, 250));
                botoesProprios[l][c].setText("");
            }
        }

        for (int i = 0; i < TAMANHOS_NAVIOS.length; i++) {
            int tamanho = TAMANHOS_NAVIOS[i];
            int idNavio = i + 1;
            vidasNavios[idNavio] = tamanho;
            boolean posicionado = false;

            while (!posicionado) {
                boolean horizontal = rand.nextBoolean();
                int l = rand.nextInt(horizontal ? TAM : TAM - tamanho + 1);
                int c = rand.nextInt(horizontal ? TAM - tamanho + 1 : TAM);

                if (podePosicionar(l, c, tamanho, horizontal)) {
                    for (int k = 0; k < tamanho; k++) {
                        int nl = horizontal ? l : l + k;
                        int nc = horizontal ? c + k : c;
                        gradePropria[nl][nc] = idNavio;
                        botoesProprios[nl][nc].setBackground(new Color(70, 90, 110));
                    }
                    posicionado = true;
                }
            }
        }
        registrarLog("Frota posicionada aleatoriamente (17 células ocupadas).");
    }

    private boolean podePosicionar(int l, int c, int tamanho, boolean horizontal) {
        for (int k = 0; k < tamanho; k++) {
            int nl = horizontal ? l : l + k;
            int nc = horizontal ? c + k : c;
            if (gradePropria[nl][nc] != 0) return false;
        }
        return true;
    }

    public void desafiarPeer(UserPeerView peer) {
        if (peer.isSelf() || peer.isBroadcastTarget()) return;
        if (emPartida) {
            JOptionPane.showMessageDialog(this, "Você já está em uma partida ativa.");
            return;
        }
        peer.getInetAddress().ifPresent(addr -> {
            this.oponenteAddr = addr;
            this.oponenteNome = peer.getDisplayName();
            lblStatus.setText("Aguardando " + oponenteNome + " aceitar o desafio...");
            enviarTcp(new BatalhaMessage(BatalhaMessage.Tipo.CONVITE, getMeuNome(), getMeuIp()));
        });
    }

    private void dispararTiro(int linha, int coluna) {
        if (!emPartida || !meuTurno || oponenteAddr == null) return;
        meuTurno = false;
        atualizarBotoesInimigos(false);
        botoesInimigos[linha][coluna].setText("...");
        lblStatus.setText("Tiro disparado em " + formatarCoord(linha, coluna) + ". Aguardando resposta...");
        enviarTcp(BatalhaMessage.tiro(getMeuNome(), getMeuIp(), linha, coluna));
    }

    public void processarMensagem(BatalhaMessage msg) {
        String ipOrigem = PeerIpv4.normalize(msg.getRemetenteIp());
        registrarLog("[RX TCP <- " + ipOrigem + "] " + msg.getTipo() +
                (msg.getTipo() == BatalhaMessage.Tipo.TIRO || msg.getTipo() == BatalhaMessage.Tipo.RESULTADO_TIRO
                        ? " (" + formatarCoord(msg.getLinha(), msg.getColuna()) + ")" : ""));

        switch (msg.getTipo()) {
            case CONVITE -> {
                if (emPartida) {
                    enviarParaIp(ipOrigem, new BatalhaMessage(BatalhaMessage.Tipo.RECUSA, getMeuNome(), getMeuIp()));
                    return;
                }
                int resp = JOptionPane.showConfirmDialog(
                        this,
                        msg.getRemetenteNome() + " (" + ipOrigem + ") desafiou você para Batalha Naval. Aceitar?",
                        "Desafio Recebido",
                        JOptionPane.YES_NO_OPTION
                );
                if (resp == JOptionPane.YES_OPTION) {
                    try {
                        this.oponenteAddr = InetAddress.getByName(ipOrigem);
                        this.oponenteNome = msg.getRemetenteNome();
                        iniciarPartida(false);
                        enviarTcp(new BatalhaMessage(BatalhaMessage.Tipo.ACEITE, getMeuNome(), getMeuIp()));
                    } catch (Exception ex) {
                        registrarLog("Erro ao resolver IP do desafiante: " + ex.getMessage());
                    }
                } else {
                    enviarParaIp(ipOrigem, new BatalhaMessage(BatalhaMessage.Tipo.RECUSA, getMeuNome(), getMeuIp()));
                }
            }
            case ACEITE -> {
                if (oponenteAddr != null) {
                    iniciarPartida(true);
                }
            }
            case RECUSA -> {
                lblStatus.setText(msg.getRemetenteNome() + " recusou o desafio.");
                oponenteAddr = null;
            }
            case TIRO -> {
                if (!emPartida) return;
                int l = msg.getLinha();
                int c = msg.getColuna();
                int celula = gradePropria[l][c];
                boolean acertou = celula > 0;
                boolean afundou = false;
                String nomeNavio = "";

                if (acertou) {
                    vidasNavios[celula]--;
                    gradePropria[l][c] = -2;
                    botoesProprios[l][c].setBackground(Color.RED);
                    botoesProprios[l][c].setText("X");
                    if (vidasNavios[celula] == 0) {
                        afundou = true;
                        nomeNavio = NOMES_NAVIOS[celula - 1];
                    }
                } else {
                    gradePropria[l][c] = -1;
                    botoesProprios[l][c].setBackground(new Color(150, 180, 210));
                    botoesProprios[l][c].setText("~");
                }

                boolean derrota = verificarDerrota();
                enviarTcp(BatalhaMessage.resultado(getMeuNome(), getMeuIp(), l, c, acertou, afundou, derrota, nomeNavio));

                if (derrota) {
                    lblStatus.setText("DERROTA! Toda a sua frota foi afundada por " + oponenteNome + ".");
                    encerrarPartidaLocal(false);
                } else {
                    meuTurno = true;
                    atualizarBotoesInimigos(true);
                    lblStatus.setText("SUA VEZ! " + oponenteNome + " atirou em " + formatarCoord(l, c) +
                            (acertou ? " (ACERTOU!)" : " (Água)."));
                }
            }
            case RESULTADO_TIRO -> {
                if (!emPartida) return;
                int l = msg.getLinha();
                int c = msg.getColuna();
                JButton alvo = botoesInimigos[l][c];

                if (msg.isAcertou()) {
                    alvo.setBackground(Color.RED);
                    alvo.setText("X");
                    String info = msg.isAfundouNavio() ? " AFUNDOU: " + msg.getDetalheNavio() + "!" : " ACERTOU!";
                    lblStatus.setText("Tiro em " + formatarCoord(l, c) + ":" + info + " Aguardando turno de " + oponenteNome + "...");
                } else {
                    alvo.setBackground(new Color(150, 180, 210));
                    alvo.setText("~");
                    lblStatus.setText("Água em " + formatarCoord(l, c) + ". Aguardando turno de " + oponenteNome + "...");
                }

                if (msg.isFimDeJogo()) {
                    lblStatus.setText("VITÓRIA! Você afundou toda a frota de " + oponenteNome + "!");
                    JOptionPane.showMessageDialog(this, "Parabéns! Você venceu " + oponenteNome + "!");
                    encerrarPartidaLocal(false);
                }
            }
            case ENCERRAR -> {
                if (emPartida) {
                    lblStatus.setText(oponenteNome + " encerrou a partida.");
                    encerrarPartidaLocal(false);
                }
            }
        }
    }

    private void iniciarPartida(boolean comecaAtirando) {
        emPartida = true;
        meuTurno = comecaAtirando;
        btnSortear.setEnabled(false);
        btnDesistir.setEnabled(true);

        for (int l = 0; l < TAM; l++) {
            for (int c = 0; c < TAM; c++) {
                botoesInimigos[l][c].setBackground(new Color(210, 230, 250));
                botoesInimigos[l][c].setText("");
            }
        }
        atualizarBotoesInimigos(meuTurno);
        lblStatus.setText(meuTurno
                ? "Partida iniciada contra " + oponenteNome + "! SUA VEZ de atirar."
                : "Partida iniciada contra " + oponenteNome + "! Aguardando tiro adversário...");
    }

    private void encerrarPartidaLocal(boolean notificarOponente) {
        if (notificarOponente && oponenteAddr != null) {
            enviarTcp(new BatalhaMessage(BatalhaMessage.Tipo.ENCERRAR, getMeuNome(), getMeuIp()));
        }
        emPartida = false;
        meuTurno = false;
        oponenteAddr = null;
        btnSortear.setEnabled(true);
        btnDesistir.setEnabled(false);
        atualizarBotoesInimigos(false);
    }

    public void encerrarPartidaPorFechamento() {
        if (emPartida) {
            encerrarPartidaLocal(true);
        }
    }

    private void atualizarBotoesInimigos(boolean habilitar) {
        for (int l = 0; l < TAM; l++) {
            for (int c = 0; c < TAM; c++) {
                boolean jaAtirado = !botoesInimigos[l][c].getText().isEmpty();
                botoesInimigos[l][c].setEnabled(habilitar && !jaAtirado);
            }
        }
    }

    private boolean verificarDerrota() {
        for (int i = 1; i < vidasNavios.length; i++) {
            if (vidasNavios[i] > 0) return false;
        }
        return true;
    }

    private void enviarTcp(BatalhaMessage msg) {
        if (oponenteAddr == null) return;
        MessageDispatcher.send(connectionManager, msg, SendTarget.singlePeer(oponenteAddr), TransportMode.TCP);
        registrarLog("[TX TCP -> " + PeerIpv4.normalize(oponenteAddr.getHostAddress()) + "] " + msg.getTipo());
    }

    private void enviarParaIp(String ip, BatalhaMessage msg) {
        try {
            InetAddress addr = InetAddress.getByName(ip);
            MessageDispatcher.send(connectionManager, msg, SendTarget.singlePeer(addr), TransportMode.TCP);
            registrarLog("[TX TCP -> " + ip + "] " + msg.getTipo());
        } catch (Exception e) {
            registrarLog("Falha ao enviar para " + ip + ": " + e.getMessage());
        }
    }

    private void registrarLog(String texto) {
        String hora = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        logRede.append("[" + hora + "] " + texto + "\n");
        logRede.setCaretPosition(logRede.getDocument().getLength());
    }

    private String formatarCoord(int l, int c) {
        return String.valueOf((char) ('A' + c)) + (l + 1);
    }

    private String getMeuNome() {
        return connectionManager.getUserName();
    }

    private String getMeuIp() {
        return LocalNetworkInfo.bestLocalIpv4();
    }
}
