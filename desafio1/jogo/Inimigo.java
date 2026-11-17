package desafio1.jogo;

import desafio1.personagens.Personagem;

public final class Inimigo implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    /** Instantes (ticks) em que o ATB do inimigo fica congelado pelo atordoamento. */
    static final int ATORDOAMENTO = 18;
    /** Instantes entre dois pulsos de veneno ou queimadura. */
    static final int INTERVALO_PULSO = 20;
    static final int PULSOS_VENENO = 5;
    static final int PULSOS_QUEIMADURA = 3;
    static final int MAX_ACUMULOS_VENENO = 3;
    public enum Tipo {
        SENTINELA("Sentinela", "Golpe perfurante", 10,
                Personagem.TipoAtaque.MAGICO, Personagem.TipoAtaque.FISICO, "Mago"),
        ESPREITADOR("Espreitador", "Ataque duplo", 2,
                Personagem.TipoAtaque.FISICO, Personagem.TipoAtaque.MAGICO, "Guerreiro"),
        ARAUTO("Arauto", "Rajada arcana", 4,
                Personagem.TipoAtaque.PERFURANTE, Personagem.TipoAtaque.FISICO, "Ladino"),
        GOLEM("Golem de ferro", "Abalo sísmico", 12,
                Personagem.TipoAtaque.PERFURANTE, Personagem.TipoAtaque.MAGICO, "Ladino"),
        DUELISTA("Duelista", "Contraestocada", 2,
                Personagem.TipoAtaque.MAGICO, Personagem.TipoAtaque.PERFURANTE, "Mago"),
        GUARDIAO("Guardião", "Impacto devastador", 9,
                Personagem.TipoAtaque.FISICO, Personagem.TipoAtaque.PERFURANTE, "Guerreiro");

        private final String nome;
        private final String habilidade;
        private final int armaduraBase;
        private final Personagem.TipoAtaque fraqueza;
        private final Personagem.TipoAtaque resistencia;
        private final String classeRecomendada;

        Tipo(String nome, String habilidade, int armaduraBase,
             Personagem.TipoAtaque fraqueza, Personagem.TipoAtaque resistencia,
             String classeRecomendada) {
            this.nome = nome;
            this.habilidade = habilidade;
            this.armaduraBase = armaduraBase;
            this.fraqueza = fraqueza;
            this.resistencia = resistencia;
            this.classeRecomendada = classeRecomendada;
        }

    }

    public enum Chefe {
        GUARDIAO("Guardião das Ruínas", "Impacto devastador", Tipo.GUARDIAO),
        ARQUIMAGO("Arquimago das Catacumbas", "Tempestade arcana", Tipo.ARAUTO),
        SOBERANO("Soberano do Eclipse", "Lâmina do eclipse", Tipo.DUELISTA);

        private final String nome;
        private final String habilidade;
        private final Tipo tipo;

        Chefe(String nome, String habilidade, Tipo tipo) {
            this.nome = nome;
            this.habilidade = habilidade;
            this.tipo = tipo;
        }

        public String getNome() { return nome; }
    }

    private final String nome;
    private final Tipo tipo;
    private final int nivel;
    private final int melhorias;
    private final int vidaMaxima;
    private final int ataque;
    private final int defesa;
    private final double velocidade;
    private int vida;
    private int acoes;
    private double atb;
    private int pulsosVeneno;
    private int acumulosVeneno;
    private int danoVeneno;
    private int relogioVeneno;
    private int pulsosQueimadura;
    private int danoQueimadura;
    private int relogioQueimadura;
    private int atordoado;
    // Após ser atordoado, o inimigo só pode ser atordoado de novo depois de agir.
    private boolean imuneAtordoamento;
    private final Chefe chefe;

    Inimigo(Tipo tipo, int nivel, int melhorias, int numero) {
        this(tipo, nivel, melhorias, numero, null);
    }

    Inimigo(Chefe chefe, int nivel, int melhorias) {
        this(chefe.tipo, nivel, melhorias, 1, chefe);
    }

    private Inimigo(Tipo tipo, int nivel, int melhorias, int numero, Chefe chefe) {
        this.chefe = chefe;
        this.tipo = tipo;
        this.nome = chefe == null ? tipo.nome + " " + numero : chefe.nome;
        this.nivel = nivel;
        this.melhorias = melhorias;
        this.vidaMaxima = (int) ((isChefe() ? 75 + nivel * 12 : 38 + nivel * 10)
                * (1 + melhorias * 0.10));
        this.vida = vidaMaxima;
        this.ataque = (isChefe() ? 16 : 12) + nivel * 2 + melhorias * 2;
        this.defesa = 2 + nivel + melhorias;
        this.velocidade = Math.min(2.4, 1.5 + nivel * 0.05 + melhorias * 0.05
                + (tipo == Tipo.ESPREITADOR || tipo == Tipo.DUELISTA ? 0.15 : 0));
    }

    public String getNome() { return nome; }
    public Tipo getTipo() { return tipo; }
    public boolean isChefe() { return chefe != null || tipo == Tipo.GUARDIAO; }
    public int getNivel() { return nivel; }
    public int getMelhorias() { return melhorias; }
    public int getArmadura() { return tipo.armaduraBase + nivel + melhorias; }
    public int getReducaoArmadura() { return Math.min(30, getArmadura() * 2); }
    public Personagem.TipoAtaque getFraqueza() { return tipo.fraqueza; }
    public Personagem.TipoAtaque getResistencia() { return tipo.resistencia; }
    public String getClasseRecomendada() { return tipo.classeRecomendada; }
    public String getNivelArmadura() {
        int armadura = getArmadura();
        return armadura >= 12 ? "Alta" : armadura >= 7 ? "Média" : "Baixa";
    }
    public static String nomeTipoAtaque(Personagem.TipoAtaque tipo) {
        switch (tipo) {
            case FISICO: return "ataques físicos";
            case MAGICO: return "magia";
            case PERFURANTE: return "golpes perfurantes";
            default: throw new IllegalArgumentException("Tipo de ataque desconhecido.");
        }
    }
    public String getNomeHabilidade() { return chefe == null ? tipo.habilidade : chefe.habilidade; }
    public boolean habilidadePronta() { return (acoes + 1) % 3 == 0; }
    public String getProximaAcao() { return habilidadePronta() ? getNomeHabilidade() : "Ataque"; }
    public int getVida() { return vida; }
    public int getVidaMaxima() { return vidaMaxima; }
    public int getAtb() { return (int) Math.min(100, atb); }
    public boolean estaVivo() { return vida > 0; }
    public int getAtaque() { return ataque; }
    public int getDefesa() { return defesa; }
    void avancar() {
        if (atordoado > 0) {
            atordoado--;
            return;
        }
        atb += velocidade;
    }
    boolean pronto() { return atb >= 100; }
    int agir(int defesaHeroi) {
        int dano = calcularDano(habilidadePronta(), defesaHeroi);
        acoes++;
        atb -= 100;
        imuneAtordoamento = false;
        return dano;
    }

    /** Dano que a próxima ação causará, sem alterar o estado do inimigo. */
    public int previsaoDano(int defesaHeroi) {
        return calcularDano(habilidadePronta(), defesaHeroi);
    }

    private int calcularDano(boolean habilidade, int defesaHeroi) {
        if (!habilidade) return Math.max(1, ataque - defesaHeroi / 2);
        switch (tipo) {
            case SENTINELA: return Math.max(1, ataque - defesaHeroi / 4);
            case ESPREITADOR: return 2 * Math.max(1, ataque * 3 / 4 - defesaHeroi / 2);
            case ARAUTO: return Math.max(1, ataque * 3 / 2 - defesaHeroi / 4);
            case GOLEM: return Math.max(1, ataque * 7 / 5 - defesaHeroi / 4);
            case DUELISTA: return Math.max(1, ataque * 5 / 4 - defesaHeroi / 4);
            case GUARDIAO: return Math.max(1, ataque * 8 / 5 - defesaHeroi / 2);
            default: throw new IllegalStateException("Tipo de inimigo desconhecido.");
        }
    }

    public int getVeneno() { return acumulosVeneno > 0 ? pulsosVeneno : 0; }
    public int getAcumulosVeneno() { return pulsosVeneno > 0 ? acumulosVeneno : 0; }
    public int getDanoVeneno() { return pulsosVeneno > 0 ? danoVeneno * acumulosVeneno : 0; }
    public int getQueimadura() { return pulsosQueimadura; }
    public int getDanoQueimadura() { return pulsosQueimadura > 0 ? danoQueimadura : 0; }
    public boolean isAtordoado() { return atordoado > 0; }
    public int getAtordoamentoRestante() { return atordoado; }
    public boolean isImuneAtordoamento() { return imuneAtordoamento; }

    /** Veneno acumula até 3 vezes e renova a duração a cada aplicação. */
    void envenenar(int dano) {
        if (pulsosVeneno == 0) acumulosVeneno = 0;
        acumulosVeneno = Math.min(MAX_ACUMULOS_VENENO, acumulosVeneno + 1);
        danoVeneno = Math.max(danoVeneno, Math.max(1, dano));
        pulsosVeneno = PULSOS_VENENO;
        relogioVeneno = 0;
    }

    void queimar(int dano) {
        danoQueimadura = Math.max(pulsosQueimadura > 0 ? danoQueimadura : 0, Math.max(1, dano));
        pulsosQueimadura = PULSOS_QUEIMADURA;
        relogioQueimadura = 0;
    }

    /** Congela o ATB por alguns instantes; o alvo só pode ser atordoado de novo depois de agir. */
    boolean atordoar() {
        if (!estaVivo() || imuneAtordoamento) return false;
        atordoado = ATORDOAMENTO;
        imuneAtordoamento = true;
        return true;
    }

    /** Pulsos de veneno e queimadura no tempo; ignoram armadura e resistência. */
    int sofrerEfeitosContinuos() {
        int total = 0;
        if (pulsosVeneno > 0 && ++relogioVeneno >= INTERVALO_PULSO) {
            relogioVeneno = 0;
            total += danoVeneno * acumulosVeneno;
            if (--pulsosVeneno == 0) {
                danoVeneno = 0;
                acumulosVeneno = 0;
            }
        }
        if (pulsosQueimadura > 0 && ++relogioQueimadura >= INTERVALO_PULSO) {
            relogioQueimadura = 0;
            total += danoQueimadura;
            if (--pulsosQueimadura == 0) danoQueimadura = 0;
        }
        int recebido = Math.min(vida, total);
        vida -= recebido;
        return recebido;
    }
    public ResultadoDano receberAtaque(int danoBase, Personagem.TipoAtaque tipoAtaque) {
        return receberAtaque(danoBase, tipoAtaque, false);
    }

    /** {@code ignorarArmadura} representa golpes que rompem a armadura, como o Golpe devastador. */
    public ResultadoDano receberAtaque(int danoBase, Personagem.TipoAtaque tipoAtaque, boolean ignorarArmadura) {
        if (danoBase < 0 || tipoAtaque == null) {
            throw new IllegalArgumentException("Ataque inválido.");
        }
        int dano = danoBase;
        if (tipoAtaque == Personagem.TipoAtaque.FISICO && !ignorarArmadura) {
            dano = Math.max(1, dano * (100 - getReducaoArmadura()) / 100);
        } else if (tipoAtaque == Personagem.TipoAtaque.PERFURANTE) {
            dano = Math.max(1, dano * (100 - getReducaoArmadura() / 2) / 100);
        }
        boolean explorouFraqueza = tipoAtaque == tipo.fraqueza;
        boolean resistido = tipoAtaque == tipo.resistencia;
        if (explorouFraqueza) {
            dano = Math.max(1, dano * 5 / 4);
        } else if (resistido) {
            dano = Math.max(1, dano * 4 / 5);
        }
        int recebido = Math.min(vida, dano);
        vida -= recebido;
        return new ResultadoDano(recebido, explorouFraqueza, resistido);
    }

    public static final class ResultadoDano implements java.io.Serializable {
        private static final long serialVersionUID = 1L;
        private final int dano;
        private final boolean explorouFraqueza;
        private final boolean resistido;

        private ResultadoDano(int dano, boolean explorouFraqueza, boolean resistido) {
            this.dano = dano;
            this.explorouFraqueza = explorouFraqueza;
            this.resistido = resistido;
        }

        public int getDano() { return dano; }
        public boolean explorouFraqueza() { return explorouFraqueza; }
        public boolean foiResistido() { return resistido; }
    }
}
