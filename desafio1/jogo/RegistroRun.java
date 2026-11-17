package desafio1.jogo;

import desafio1.personagens.Personagem;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/** Resumo persistente de uma run; guarda o estado completo enquanto ela estiver em andamento. */
public final class RegistroRun implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Modo { ONDAS, EXPEDICAO }
    public enum Resultado { EM_ANDAMENTO, VITORIA, DERROTA }

    private final String id = UUID.randomUUID().toString();
    private final String heroi;
    private final String classe;
    private final Modo modo;
    private final long inicio;
    private long ultimaAtualizacao;
    private Resultado resultado = Resultado.EM_ANDAMENTO;
    private int nivel;
    private int progresso;
    private int capitulo;
    private int ciclo;
    private Jogo.Estatisticas estatisticas = new Jogo.Estatisticas();
    private Jogo jogo;
    private Expedicao expedicao;

    private RegistroRun(Personagem heroi, Modo modo, Jogo jogo, Expedicao expedicao) {
        this.heroi = heroi.getNome();
        this.classe = heroi.getClasse();
        this.modo = modo;
        this.jogo = jogo;
        this.expedicao = expedicao;
        inicio = System.currentTimeMillis();
        atualizar();
    }

    public static RegistroRun deOndas(Jogo jogo) {
        Objects.requireNonNull(jogo, "Jogo é obrigatório.");
        return new RegistroRun(jogo.getHeroi(), Modo.ONDAS, jogo, null);
    }

    public static RegistroRun deExpedicao(Expedicao expedicao) {
        Objects.requireNonNull(expedicao, "Expedição é obrigatória.");
        return new RegistroRun(expedicao.getJogo().getHeroi(), Modo.EXPEDICAO, expedicao.getJogo(), expedicao);
    }

    /** Copia os dados atuais da partida; descarta o estado salvo quando a run termina. */
    public void atualizar() {
        if (jogo == null) return;
        ultimaAtualizacao = System.currentTimeMillis();
        nivel = jogo.getHeroi().getNivel();
        estatisticas = jogo.getEstatisticas().copia();
        if (modo == Modo.EXPEDICAO) {
            progresso = expedicao.getSalaAtual().getAndar();
            capitulo = expedicao.getCapitulo();
            ciclo = expedicao.getCiclo();
            if (expedicao.getEstado() == Expedicao.Estado.VITORIA) resultado = Resultado.VITORIA;
            else if (expedicao.getEstado() == Expedicao.Estado.DERROTA) resultado = Resultado.DERROTA;
        } else {
            progresso = jogo.getOnda();
            if (jogo.getEstado() == Jogo.Estado.DERROTA) resultado = Resultado.DERROTA;
        }
        if (resultado != Resultado.EM_ANDAMENTO) {
            jogo = null;
            expedicao = null;
        }
    }

    public boolean podeContinuar() { return resultado == Resultado.EM_ANDAMENTO && jogo != null; }
    public String getId() { return id; }
    public String getHeroi() { return heroi; }
    public String getClasse() { return classe; }
    public Modo getModo() { return modo; }
    public long getInicio() { return inicio; }
    public long getUltimaAtualizacao() { return ultimaAtualizacao; }
    public Resultado getResultado() { return resultado; }
    public int getNivel() { return nivel; }
    public int getProgresso() { return progresso; }
    public Jogo.Estatisticas getEstatisticas() { return estatisticas; }
    public Jogo getJogo() { return jogo; }
    public Expedicao getExpedicao() { return expedicao; }

    public String descricaoProgresso() {
        if (modo == Modo.EXPEDICAO && capitulo > 0) {
            return "Cap. " + capitulo + "/" + Expedicao.TOTAL_CAPITULOS
                    + (ciclo > 0 ? " • Ascensão " + ciclo : "")
                    + (progresso == 0 ? " • Entrada" : " • Andar " + progresso + "/8");
        }
        if (progresso == 0) return modo == Modo.EXPEDICAO ? "Entrada da dungeon" : "Preparação";
        return modo == Modo.EXPEDICAO ? "Andar " + progresso + " de 8" : "Onda " + progresso;
    }

    public String descricaoResultado() {
        switch (resultado) {
            case VITORIA: return "Vitória";
            case DERROTA: return "Derrota";
            default: return "Em andamento";
        }
    }
}
