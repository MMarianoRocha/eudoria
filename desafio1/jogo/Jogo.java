package desafio1.jogo;

import desafio1.itens.consumiveis.PocaoCura;
import desafio1.itens.consumiveis.PocaoMana;
import desafio1.personagens.Personagem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class Jogo implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    public static final int LIMITE_MAXIMO = 100;
    public enum Estado { COMBATE, ESCOLHA, INTERVALO, DERROTA }
    public enum Melhoria { ATAQUE, VITALIDADE, POCAO_VIDA, POCAO_MANA }
    /** Bônus escolhido a cada nível, além do crescimento natural da classe. */
    public enum BonusNivel {
        PODER("Força", "+3 de poder"),
        VIDA("Vigor", "+20 de vida máxima"),
        DESTREZA("Agilidade", "+2 de destreza: ATB mais rápido e defesa"),
        MANA("Espírito", "+15 de mana máxima");

        private final String nome;
        private final String descricao;

        BonusNivel(String nome, String descricao) {
            this.nome = nome;
            this.descricao = descricao;
        }

        public String getNome() { return nome; }
        public String getDescricao() { return descricao; }
    }
    /** Carga do limite a cada ação ofensiva e a cada inimigo derrotado. */
    static final int LIMITE_POR_ACAO = 4;
    static final int LIMITE_POR_ABATE = 8;
    private static final Inimigo.Tipo[] TIPOS_ENCONTRO = {
            Inimigo.Tipo.SENTINELA, Inimigo.Tipo.ESPREITADOR, Inimigo.Tipo.ARAUTO,
            Inimigo.Tipo.GOLEM, Inimigo.Tipo.DUELISTA
    };

    private final Personagem heroi;
    private final List<Inimigo> inimigos = new ArrayList<>();
    private final List<String> mensagens = new ArrayList<>();
    private Estado estado;
    private int onda;
    private int experiencia;
    private int escolhasNivel;
    private int ouro = 60;
    private int pocoesVida;
    private int pocoesMana;
    private int melhoriasAtaque;
    private int melhoriasVida;
    private double atb;
    private boolean encontroExpedicao;
    private int profundidadeEncontro;
    private int multiplicadorRecompensa = 1;
    private boolean lojaDisponivel = true;
    private int limite;
    private boolean defendendo;
    private final Estatisticas estatisticas = new Estatisticas();

    public Jogo(Personagem heroi) {
        this.heroi = Objects.requireNonNull(heroi, "Herói é obrigatório.");
        if (!heroi.estaVivo()) {
            throw new IllegalArgumentException("O herói deve estar vivo.");
        }
        estado = Estado.INTERVALO;
        registrar("Prepare-se: 60 ouro, sem poções. Compre suprimentos e inicie a primeira onda.");
    }

    public Personagem getHeroi() { return heroi; }
    public Estado getEstado() { return estado; }
    public int getOnda() { return onda; }
    public int getExperiencia() { return experiencia; }
    public int getExperienciaNecessaria() {
        return 110 + heroi.getNivel() * heroi.getNivel() * 40;
    }
    /** Níveis já alcançados cujo bônus ainda não foi escolhido. */
    public int getEscolhasNivel() { return escolhasNivel; }
    public int getOuro() { return ouro; }
    public int getPocoesVida() { return pocoesVida; }
    public int getPocoesMana() { return pocoesMana; }
    public int getAtb() { return (int) Math.min(100, atb); }
    public List<Inimigo> getInimigos() { return Collections.unmodifiableList(inimigos); }
    public List<String> getMensagens() { return Collections.unmodifiableList(mensagens); }
    public int getLimite() { return limite; }
    public boolean limitePronto() { return limite >= LIMITE_MAXIMO; }
    public boolean estaDefendendo() { return defendendo; }
    public int getReducaoDefesa() { return Math.min(65, 50 + heroi.getConstituicao() / 5); }
    public Estatisticas getEstatisticas() { return estatisticas; }

    public int previsaoDano(Inimigo inimigo) {
        return aplicarDefesa(Objects.requireNonNull(inimigo, "Inimigo é obrigatório.")
                .previsaoDano(heroi.getDefesa()));
    }

    private int aplicarDefesa(int dano) {
        return defendendo ? Math.max(1, (int) (dano * (100L - getReducaoDefesa()) / 100)) : dano;
    }

    public int preco(Melhoria melhoria) {
        Objects.requireNonNull(melhoria, "Melhoria é obrigatória.");
        switch (melhoria) {
            case ATAQUE: return 35 + melhoriasAtaque * 20;
            case VITALIDADE: return 35 + melhoriasVida * 20;
            case POCAO_VIDA:
            case POCAO_MANA: return 20;
            default: throw new IllegalArgumentException("Melhoria desconhecida.");
        }
    }

    public void avancarTempo() {
        if (estado != Estado.COMBATE) return;
        atb += Math.min(3.0, 1.7 + heroi.getDestreza() * 0.05);
        for (Inimigo inimigo : inimigos) {
            if (!inimigo.estaVivo()) continue;
            int continuo = inimigo.sofrerEfeitosContinuos();
            if (continuo > 0) {
                estatisticas.danoCausado += continuo;
                registrar(inimigo.getNome() + " sofre " + continuo + " de dano de veneno/queimadura.");
                if (!inimigo.estaVivo()) {
                    registrarAbate(inimigo);
                    if (inimigos.stream().noneMatch(Inimigo::estaVivo)) {
                        concluirOnda();
                        return;
                    }
                    continue;
                }
            }
            inimigo.avancar();
            if (inimigo.pronto()) {
                String acao = inimigo.getProximaAcao();
                int dano = heroi.receberDano(aplicarDefesa(inimigo.agir(heroi.getDefesa())));
                estatisticas.danoRecebido += dano;
                carregarLimite(dano);
                registrar(inimigo.getNome() + " (nível " + inimigo.getNivel() + ") usa "
                        + acao + " e causa " + dano + " de dano." + (defendendo ? " Defesa ativa!" : ""));
                if (inimigo.habilidadePronta()) {
                    registrar("Atenção: " + inimigo.getNome() + " prepara " + inimigo.getNomeHabilidade() + "!");
                }
                if (!heroi.estaVivo()) {
                    defendendo = false;
                    estado = Estado.DERROTA;
                    estatisticas.mortes++;
                    registrar("Você foi derrotado na onda " + onda + ".");
                    return;
                }
            }
        }
        if (atb >= 100) {
            atb = 100;
            defendendo = false;
            estado = Estado.ESCOLHA;
            registrar("Sua barra ATB está cheia. Escolha uma ação.");
        }
    }

    public boolean defender() {
        if (!podeAgir()) return false;
        defendendo = true;
        registrar(heroi.getNome() + " está defendendo: dano reduzido em " + getReducaoDefesa()
                + "% até a próxima vez.");
        encerrarAcao();
        return true;
    }

    public boolean atacar(int indice, boolean habilidade) {
        if (!podeAgir()) return false;
        if (indice < 0 || indice >= inimigos.size() || !inimigos.get(indice).estaVivo()) {
            registrar("Selecione um inimigo vivo.");
            return false;
        }
        if (habilidade && !heroi.gastarMana(heroi.getCustoHabilidade())) {
            registrar("Mana insuficiente para " + heroi.getNomeHabilidade() + ".");
            return false;
        }
        Inimigo alvo = inimigos.get(indice);
        boolean rompeArmadura = habilidade && heroi.getTipoAtaque() == Personagem.TipoAtaque.FISICO;
        int poder = habilidade ? heroi.getPoder() * 2 : heroi.getPoder();
        golpear(alvo, poder, rompeArmadura, habilidade ? heroi.getNomeHabilidade() : "Ataque");
        if (habilidade) aplicarEfeitoClasse(alvo, heroi.getPoder());
        finalizarAcaoOfensiva();
        return true;
    }

    /** Limit Break: disponível com a barra cheia; zera a barra ao ser usado. */
    public boolean usarLimite(int indice) {
        if (!podeAgir()) return false;
        if (!limitePronto()) {
            registrar("O Limit Break ainda não está carregado.");
            return false;
        }
        if (indice < 0 || indice >= inimigos.size() || !inimigos.get(indice).estaVivo()) {
            registrar("Selecione um inimigo vivo.");
            return false;
        }
        limite = 0;
        estatisticas.limitesUsados++;
        registrar("LIMIT BREAK! " + heroi.getNome() + " desencadeia " + heroi.getNomeLimite() + "!");
        int poder = heroi.getPoder();
        Inimigo alvo = inimigos.get(indice);
        switch (heroi.getTipoAtaque()) {
            case FISICO:
                golpear(alvo, poder * 5, true, heroi.getNomeLimite());
                aplicarEfeitoClasse(alvo, poder);
                break;
            case MAGICO:
                for (Inimigo inimigo : inimigos) {
                    if (!inimigo.estaVivo()) continue;
                    golpear(inimigo, poder * 3, false, heroi.getNomeLimite());
                    aplicarEfeitoClasse(inimigo, poder);
                }
                break;
            default:
                for (int golpe = 0; golpe < 3 && alvo.estaVivo(); golpe++) {
                    golpear(alvo, poder * 5 / 4, false, heroi.getNomeLimite());
                }
                aplicarEfeitoClasse(alvo, poder);
                break;
        }
        concluirAcao();
        return true;
    }

    private void golpear(Inimigo alvo, int poder, boolean rompeArmadura, String nomeAcao) {
        int danoBase = Math.max(1, poder - alvo.getDefesa());
        Inimigo.ResultadoDano resultado = alvo.receberAtaque(danoBase, heroi.getTipoAtaque(), rompeArmadura);
        int dano = resultado.getDano();
        estatisticas.danoCausado += dano;
        registrar(heroi.getNome() + " usa " + nomeAcao + " em " + alvo.getNome() + ": " + dano + " de dano."
                + (rompeArmadura ? " A armadura foi ignorada!" : "")
                + (resultado.explorouFraqueza() ? " Fraqueza explorada! Dano aumentado."
                : resultado.foiResistido() ? " Resistido: dano reduzido." : ""));
        if (!alvo.estaVivo()) registrarAbate(alvo);
    }

    private void aplicarEfeitoClasse(Inimigo alvo, int poder) {
        if (!alvo.estaVivo()) return;
        switch (heroi.getTipoAtaque()) {
            case FISICO:
                registrar(alvo.atordoar() ? alvo.getNome() + " ficou atordoado: ATB congelado."
                        : alvo.getNome() + " resiste ao atordoamento até agir novamente.");
                break;
            case MAGICO:
                alvo.queimar(Math.max(2, poder / 8));
                registrar(alvo.getNome() + " está em chamas.");
                break;
            default:
                alvo.envenenar(Math.max(2, poder / 5));
                registrar(alvo.getNome() + " foi envenenado (x" + alvo.getAcumulosVeneno() + ").");
                break;
        }
    }

    private void finalizarAcaoOfensiva() {
        adicionarLimite(LIMITE_POR_ACAO);
        concluirAcao();
    }

    private void concluirAcao() {
        if (inimigos.stream().noneMatch(Inimigo::estaVivo)) {
            concluirOnda();
        } else {
            encerrarAcao();
        }
    }

    private void registrarAbate(Inimigo inimigo) {
        estatisticas.abates++;
        adicionarLimite(LIMITE_POR_ABATE);
        if (inimigo.isChefe()) estatisticas.chefesDerrotados++;
        registrar(inimigo.getNome() + " foi derrotado.");
    }

    private void carregarLimite(int dano) {
        if (dano <= 0) return;
        adicionarLimite(Math.max(2, dano * 100 / Math.max(1, heroi.getHPMaximo())));
    }

    void adicionarLimite(int valor) {
        if (valor <= 0 || limite >= LIMITE_MAXIMO) return;
        limite = Math.min(LIMITE_MAXIMO, limite + valor);
        if (limite == LIMITE_MAXIMO) registrar("LIMIT BREAK pronto: " + heroi.getNomeLimite() + "!");
    }

    public boolean usarPocao(boolean vida) {
        if (!podeAgir()) return false;
        if (vida && (pocoesVida == 0 || heroi.getHpAtual() == heroi.getHPMaximo())) {
            registrar(pocoesVida == 0 ? "Não há poções de vida." : "Sua vida já está cheia.");
            return false;
        }
        if (!vida && (pocoesMana == 0 || heroi.getManaAtual() == heroi.getManaMaxima())) {
            registrar(pocoesMana == 0 ? "Não há poções de mana." : "Sua mana já está cheia.");
            return false;
        }
        int anterior = vida ? heroi.getHpAtual() : heroi.getManaAtual();
        if (vida) {
            new PocaoCura().usar(heroi);
            pocoesVida--;
        } else {
            new PocaoMana().usar(heroi);
            pocoesMana--;
        }
        int recuperado = (vida ? heroi.getHpAtual() : heroi.getManaAtual()) - anterior;
        registrar("Poção usada: " + recuperado + (vida ? " de vida" : " de mana") + " recuperados.");
        encerrarAcao();
        return true;
    }

    public boolean comprar(Melhoria melhoria) {
        Objects.requireNonNull(melhoria, "Melhoria é obrigatória.");
        if (estado != Estado.INTERVALO || !lojaDisponivel) {
            registrar("A loja não está disponível neste momento.");
            return false;
        }
        int preco = preco(melhoria);
        if (ouro < preco) {
            registrar("Ouro insuficiente.");
            return false;
        }
        ouro -= preco;
        switch (melhoria) {
            case ATAQUE:
                melhoriasAtaque++;
                heroi.melhorarAtaque();
                registrar("Ataque melhorado em 4 pontos.");
                break;
            case VITALIDADE:
                melhoriasVida++;
                heroi.melhorarVitalidade();
                registrar("Vida máxima aumentada em 20.");
                break;
            case POCAO_VIDA:
                pocoesVida++;
                registrar("Poção de vida adquirida.");
                break;
            case POCAO_MANA:
                pocoesMana++;
                registrar("Poção de mana adquirida.");
                break;
            default: throw new IllegalArgumentException("Melhoria desconhecida.");
        }
        return true;
    }

    public void proximaOnda() {
        if (estado != null && estado != Estado.INTERVALO) {
            throw new IllegalStateException("A próxima onda só pode começar no intervalo.");
        }
        onda++;
        encontroExpedicao = false;
        defendendo = false;
        multiplicadorRecompensa = 1;
        atb = 0;
        inimigos.clear();
        boolean chefe = onda % 5 == 0;
        int nivelInimigos = 1 + (onda - 1) / 3;
        int melhorias = (onda - 1) / 5;
        if (chefe) {
            inimigos.add(new Inimigo(Inimigo.Tipo.GUARDIAO, nivelInimigos + 1, melhorias, 1));
        }
        int quantidade = chefe ? 1 : Math.min(3, 1 + (onda - 1) / 3);
        for (int i = 0; i < quantidade; i++) {
            inimigos.add(new Inimigo(tipoEncontro(onda, i),
                    nivelInimigos, melhorias, i + 1));
        }
        estado = Estado.COMBATE;
        registrar("Onda " + onda + (chefe ? " — um guardião apareceu!" : " começou!")
                + " Inimigos de nível " + nivelInimigos + ", reforço +" + melhorias + ".");
    }

    void iniciarEncontroExpedicao(int profundidade, boolean elite, boolean chefe) {
        iniciarEncontroExpedicao(profundidade, elite, chefe, Inimigo.Chefe.GUARDIAO);
    }

    void iniciarEncontroExpedicao(int profundidade, boolean elite, boolean chefe, Inimigo.Chefe tipoChefe) {
        if (estado != Estado.INTERVALO || profundidade < 1 || (elite && chefe)) {
            throw new IllegalStateException("Não é possível iniciar este encontro agora.");
        }
        onda++;
        encontroExpedicao = true;
        defendendo = false;
        profundidadeEncontro = profundidade;
        multiplicadorRecompensa = chefe ? 3 : elite ? 2 : 1;
        atb = 0;
        inimigos.clear();
        int nivel = 1 + (profundidade - 1) / 2;
        int melhorias = profundidade / 3;
        if (chefe) {
            inimigos.add(new Inimigo(Objects.requireNonNull(tipoChefe), nivel + 1, melhorias));
        } else if (elite) {
            inimigos.add(new Inimigo(Inimigo.Tipo.ARAUTO, nivel, melhorias + 1, 1));
        }
        int quantidade = chefe ? 0 : elite ? 1 : Math.min(2, 1 + (profundidade - 1) / 5);
        for (int i = 0; i < quantidade; i++) {
            inimigos.add(new Inimigo(tipoEncontro(profundidade, i),
                    nivel, melhorias, i + 1));
        }
        estado = Estado.COMBATE;
        registrar((chefe ? "O guardião da dungeon surgiu!" : elite ? "Um inimigo de elite apareceu!" : "Um encontro começou!")
                + " Profundidade " + profundidade + ".");
    }

    private static Inimigo.Tipo tipoEncontro(int etapa, int indice) {
        return TIPOS_ENCONTRO[(etapa - 1 + indice * 3) % TIPOS_ENCONTRO.length];
    }

    void ganharOuro(int valor) {
        if (valor < 0) throw new IllegalArgumentException("Ouro não pode ser negativo.");
        ouro += valor;
        estatisticas.ouroGanho += valor;
    }

    /** Aplica o bônus escolhido para um nível alcançado. */
    public boolean escolherBonusNivel(BonusNivel bonus) {
        Objects.requireNonNull(bonus, "Bônus é obrigatório.");
        if (escolhasNivel <= 0 || estado != Estado.INTERVALO) {
            registrar("Não há bônus de nível para escolher agora.");
            return false;
        }
        switch (bonus) {
            case PODER: heroi.treinarPoder(3); break;
            case VIDA: heroi.treinarVida(20); break;
            case DESTREZA: heroi.treinarDestreza(2); break;
            case MANA: heroi.treinarMana(15); break;
            default: throw new IllegalArgumentException("Bônus desconhecido.");
        }
        escolhasNivel--;
        registrar(bonus.getNome() + " escolhida: " + bonus.getDescricao() + ".");
        return true;
    }

    void definirLojaDisponivel(boolean disponivel) {
        lojaDisponivel = disponivel;
    }

    void registrarMorte() {
        estatisticas.mortes++;
    }

    private boolean podeAgir() {
        if (estado == Estado.ESCOLHA) return true;
        registrar("Aguarde a barra ATB para agir.");
        return false;
    }

    private void encerrarAcao() {
        atb = 0;
        estado = Estado.COMBATE;
    }

    private void concluirOnda() {
        defendendo = false;
        int ganhoXp = encontroExpedicao ? (25 + profundidadeEncontro * 10) * multiplicadorRecompensa : 20 + onda * 5;
        int ganhoOuro = encontroExpedicao ? (30 + profundidadeEncontro * 9) * multiplicadorRecompensa : 25 + onda * 12;
        receberExperiencia(ganhoXp);
        ouro += ganhoOuro;
        estatisticas.ouroGanho += ganhoOuro;
        estatisticas.encontrosVencidos++;
        registrar("Onda concluída: +" + ganhoXp + " XP e +" + ganhoOuro + " ouro.");
        if (!encontroExpedicao) {
            heroi.restaurarVida(Math.max(1, heroi.getHPMaximo() / 4));
            heroi.restaurarMana(Math.max(1, heroi.getManaMaxima() / 4));
        }
        atb = 0;
        estado = Estado.INTERVALO;
        if (!encontroExpedicao) {
            registrar("Descanso: 25% de vida e mana. Poções somente na loja; escolha entre suprimentos e melhorias.");
        }
    }

    private void receberExperiencia(int valor) {
        experiencia += valor;
        while (experiencia >= getExperienciaNecessaria()) {
            experiencia -= getExperienciaNecessaria();
            heroi.upar();
            escolhasNivel++;
            registrar("LEVEL UP! Nível " + heroi.getNivel() + " alcançado. Escolha um bônus.");
        }
    }

    private void registrar(String mensagem) {
        mensagens.add(mensagem);
        if (mensagens.size() > 150) mensagens.remove(0);
    }

    /** Contadores acumulados durante uma run; copiados para o registro de salvamento. */
    public static final class Estatisticas implements java.io.Serializable {
        private static final long serialVersionUID = 1L;
        private int abates;
        private int chefesDerrotados;
        private int mortes;
        private long danoCausado;
        private long danoRecebido;
        private int limitesUsados;
        private long ouroGanho;
        private int encontrosVencidos;

        public int getAbates() { return abates; }
        public int getChefesDerrotados() { return chefesDerrotados; }
        public int getMortes() { return mortes; }
        public long getDanoCausado() { return danoCausado; }
        public long getDanoRecebido() { return danoRecebido; }
        public int getLimitesUsados() { return limitesUsados; }
        public long getOuroGanho() { return ouroGanho; }
        public int getEncontrosVencidos() { return encontrosVencidos; }

        public Estatisticas copia() {
            Estatisticas copia = new Estatisticas();
            copia.abates = abates;
            copia.chefesDerrotados = chefesDerrotados;
            copia.mortes = mortes;
            copia.danoCausado = danoCausado;
            copia.danoRecebido = danoRecebido;
            copia.limitesUsados = limitesUsados;
            copia.ouroGanho = ouroGanho;
            copia.encontrosVencidos = encontrosVencidos;
            return copia;
        }
    }
}
