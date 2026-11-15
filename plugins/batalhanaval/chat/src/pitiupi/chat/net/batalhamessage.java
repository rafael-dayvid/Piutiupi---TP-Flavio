package pitiupi.batalhanaval.net;

import pitiupi.models.Messages.Message;

public class BatalhaMessage extends Message {

    public enum Tipo {
        CONVITE,
        ACEITE,
        RECUSA,
        TIRO,
        RESULTADO_TIRO,
        ENCERRAR
    }

    private Tipo tipo;
    private String remetenteNome;
    private String remetenteIp;
    private int linha;
    private int coluna;
    private boolean acertou;
    private boolean afundouNavio;
    private boolean fimDeJogo;
    private String detalheNavio;

    public BatalhaMessage() {
    }

    public BatalhaMessage(Tipo tipo, String remetenteNome, String remetenteIp) {
        this.tipo = tipo;
        this.remetenteNome = remetenteNome;
        this.remetenteIp = remetenteIp;
    }

    public static BatalhaMessage tiro(String nome, String ip, int linha, int coluna) {
        BatalhaMessage m = new BatalhaMessage(Tipo.TIRO, nome, ip);
        m.linha = linha;
        m.coluna = coluna;
        return m;
    }

    public static BatalhaMessage resultado(String nome, String ip, int linha, int coluna,
                                           boolean acertou, boolean afundou, boolean fimDeJogo, String navio) {
        BatalhaMessage m = new BatalhaMessage(Tipo.RESULTADO_TIRO, nome, ip);
        m.linha = linha;
        m.coluna = coluna;
        m.acertou = acertou;
        m.afundouNavio = afundou;
        m.fimDeJogo = fimDeJogo;
        m.detalheNavio = navio;
        return m;
    }

    public Tipo getTipo() { return tipo; }
    public String getRemetenteNome() { return remetenteNome; }
    public String getRemetenteIp() { return remetenteIp; }
    public int getLinha() { return linha; }
    public int getColuna() { return coluna; }
    public boolean isAcertou() { return acertou; }
    public boolean isAfundouNavio() { return afundouNavio; }
    public boolean isFimDeJogo() { return fimDeJogo; }
    public String getDetalheNavio() { return detalheNavio; }
}