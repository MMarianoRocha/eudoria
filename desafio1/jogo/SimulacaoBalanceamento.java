package desafio1.jogo;

import desafio1.personagens.Guerreiro;
import desafio1.personagens.Ladino;
import desafio1.personagens.Mago;
import desafio1.personagens.Personagem;

import java.util.EnumMap;
import java.util.Map;

/**
 * Joga o primeiro capítulo com uma IA que explora fraquezas, compra suprimentos e descansa,
 * medindo vitórias e vida perdida por tipo de inimigo para cada classe.
 */
public final class SimulacaoBalanceamento {
    static final String[] CLASSES = {"Guerreiro", "Mago", "Ladino"};

    static final class Resultado {
        int vitorias;
        int partidas;
        int somaAndar;
        int somaLimites;
        int somaNivel;
        final Map<Inimigo.Tipo, int[]> porTipo = new EnumMap<>(Inimigo.Tipo.class);

        double taxaVitoria() { return 100.0 * vitorias / partidas; }
        double andarMedio() { return (double) somaAndar / partidas; }
        double limitesMedios() { return (double) somaLimites / partidas; }
        double nivelMedio() { return (double) somaNivel / partidas; }

        /** Vida perdida média (% da vida máxima) em encontros comuns contra o tipo. */
        double perdaMedia(Inimigo.Tipo tipo) {
            int[] dados = porTipo.get(tipo);
            return dados == null || dados[0] == 0 ? 0 : (double) dados[1] / dados[0];
        }

        int derrotas(Inimigo.Tipo tipo) {
            int[] dados = porTipo.get(tipo);
            return dados == null ? 0 : dados[2];
        }
    }

    private SimulacaoBalanceamento() { }

    public static void main(String[] args) {
        int sementes = args.length > 0 ? Integer.parseInt(args[0]) : 300;
        Resultado[] resultados = simular(sementes, args.length > 1 && "campanha".equals(args[1]));
        imprimir(resultados);
    }

    static Resultado[] simular(int sementes) {
        return simular(sementes, false);
    }

    static Resultado[] simular(int sementes, boolean campanhaCompleta) {
        Resultado[] resultados = new Resultado[3];
        for (int classe = 0; classe < 3; classe++) {
            resultados[classe] = new Resultado();
            for (int semente = 0; semente < sementes; semente++) {
                jogar(classe, semente, resultados[classe], campanhaCompleta);
            }
        }
        return resultados;
    }

    static void imprimir(Resultado[] resultados) {
        StringBuilder cabecalho = new StringBuilder(String.format("%-14s", "Classe"));
        cabecalho.append(String.format("%9s %7s", "Vitória", "Andar"));
        for (Inimigo.Tipo tipo : Inimigo.Tipo.values()) cabecalho.append(String.format(" %12s", tipo));
        System.out.println(cabecalho);
        for (int classe = 0; classe < 3; classe++) {
            Resultado r = resultados[classe];
            StringBuilder linha = new StringBuilder(String.format("%-14s %7.1f%% %7.2f", CLASSES[classe],
                    r.taxaVitoria(), r.andarMedio()));
            for (Inimigo.Tipo tipo : Inimigo.Tipo.values()) {
                linha.append(String.format(" %6.1f%%/%-4d", r.perdaMedia(tipo), r.derrotas(tipo)));
            }
            System.out.println(linha);
        }
        System.out.println("(colunas de inimigo: vida perdida média por encontro / derrotas causadas)");
        for (int classe = 0; classe < 3; classe++) {
            System.out.printf("%-10s LIMIT por run %.2f, nível final médio %.2f%n", CLASSES[classe],
                    resultados[classe].limitesMedios(), resultados[classe].nivelMedio());
        }
    }

    static Personagem criar(int classe) {
        return classe == 0 ? new Guerreiro("Sim") : classe == 1 ? new Mago("Sim") : new Ladino("Sim");
    }

    private static void jogar(int classe, long semente, Resultado resultado, boolean campanhaCompleta) {
        Personagem heroi = criar(classe);
        Expedicao expedicao = new Expedicao(heroi, semente);
        Jogo jogo = expedicao.getJogo();
        resultado.partidas++;
        int vidaInicioCombate = 0;
        boolean emCombate = false;
        for (int passos = 0; passos < 200000; passos++) {
            Expedicao.Estado estado = expedicao.getEstado();
            if (estado != Expedicao.Estado.COMBATE && emCombate) {
                registrarCombate(resultado, jogo, heroi, vidaInicioCombate, estado == Expedicao.Estado.DERROTA);
                emCombate = false;
            }
            switch (estado) {
                case CAPITULO_CONCLUIDO:
                    if (campanhaCompleta) {
                        escolherBonus(jogo, heroi);
                        expedicao.avancarCapitulo();
                        break;
                    }
                case CAMPANHA_CONCLUIDA:
                case VITORIA:
                    resultado.somaLimites += jogo.getEstatisticas().getLimitesUsados();
                    resultado.somaNivel += heroi.getNivel();
                    resultado.vitorias++;
                    resultado.somaAndar += expedicao.getEtapa() * 8 + expedicao.getSalaAtual().getAndar();
                    return;
                case DERROTA:
                    resultado.somaLimites += jogo.getEstatisticas().getLimitesUsados();
                    resultado.somaNivel += heroi.getNivel();
                    resultado.somaAndar += expedicao.getEtapa() * 8 + expedicao.getSalaAtual().getAndar();
                    return;
                case LOJA: comprar(expedicao, heroi); break;
                case MAPA:
                    escolherBonus(jogo, heroi);
                    escolherSala(expedicao);
                    break;
                case EVENTO:
                    if (!expedicao.resolverEvento(Expedicao.EscolhaEvento.CUIDADOSO)) {
                        expedicao.resolverEvento(Expedicao.EscolhaEvento.IGNORAR);
                    }
                    break;
                case RESULTADO: expedicao.continuarResultado(); break;
                case ACAMPAMENTO: descansar(expedicao, heroi); break;
                case RECOMPENSA: expedicao.escolherReliquia(reliquiaPreferida(heroi)); break;
                case COMBATE:
                    if (!emCombate) {
                        emCombate = true;
                        vidaInicioCombate = heroi.getHpAtual();
                    }
                    lutar(jogo, heroi);
                    expedicao.sincronizarCombate();
                    break;
                default: throw new AssertionError("Estado inesperado: " + estado);
            }
        }
        throw new AssertionError("Simulação não terminou");
    }

    private static void registrarCombate(Resultado resultado, Jogo jogo, Personagem heroi,
                                         int vidaInicio, boolean derrota) {
        int vidaFinal = derrota ? 0 : heroi.getHpAtual();
        int perda = Math.max(0, (vidaInicio - vidaFinal) * 100 / Math.max(1, heroi.getHPMaximo()));
        for (Inimigo inimigo : jogo.getInimigos()) {
            int[] dados = resultado.porTipo.computeIfAbsent(inimigo.getTipo(), t -> new int[3]);
            dados[0]++;
            dados[1] += perda;
            if (derrota) dados[2]++;
        }
    }

    private static Expedicao.Reliquia reliquiaPreferida(Personagem heroi) {
        return heroi.getHpAtual() * 2 < heroi.getHPMaximo() ? Expedicao.Reliquia.AMULETO : Expedicao.Reliquia.LAMINA;
    }

    private static void comprar(Expedicao expedicao, Personagem heroi) {
        Jogo jogo = expedicao.getJogo();
        int alvoMana = heroi.getManaMaxima() >= 80 ? 1 : 0;
        while (jogo.getPocoesVida() < 2 && jogo.getOuro() >= 20) jogo.comprar(Jogo.Melhoria.POCAO_VIDA);
        while (jogo.getPocoesMana() < alvoMana && jogo.getOuro() >= 20) jogo.comprar(Jogo.Melhoria.POCAO_MANA);
        boolean comprou = true;
        while (comprou) {
            comprou = false;
            Jogo.Melhoria primeira = jogo.preco(Jogo.Melhoria.ATAQUE) <= jogo.preco(Jogo.Melhoria.VITALIDADE)
                    ? Jogo.Melhoria.ATAQUE : Jogo.Melhoria.VITALIDADE;
            if (jogo.getOuro() >= jogo.preco(primeira)) {
                jogo.comprar(primeira);
                comprou = true;
            }
        }
        while (!expedicao.getReliquiasPendentes().isEmpty()) expedicao.ativarReliquia(0);
        expedicao.sairDaLoja();
    }

    /** Varia os bônus como um jogador faria: alterna poder e vida, Mago prioriza mana. */
    static void escolherBonus(Jogo jogo, Personagem heroi) {
        while (jogo.getEscolhasNivel() > 0) {
            Jogo.BonusNivel bonus = heroi.getNivel() % 2 == 0 ? Jogo.BonusNivel.PODER
                    : heroi.getTipoAtaque() == Personagem.TipoAtaque.MAGICO ? Jogo.BonusNivel.MANA : Jogo.BonusNivel.VIDA;
            if (!jogo.escolherBonusNivel(bonus)) return;
        }
    }

    private static void escolherSala(Expedicao expedicao) {
        Jogo jogo = expedicao.getJogo();
        Personagem heroi = jogo.getHeroi();
        Expedicao.Sala melhor = null;
        int melhorValor = Integer.MIN_VALUE;
        boolean ferido = heroi.getHpAtual() * 2 < heroi.getHPMaximo();
        for (Expedicao.Sala sala : expedicao.getSalaAtual().getSaidas()) {
            int valor;
            switch (sala.getTipo()) {
                case ACAMPAMENTO: valor = ferido ? 100 : 20; break;
                case LOJA: valor = jogo.getOuro() >= 50 ? 85 : 25; break;
                case EVENTO: valor = ferido ? 60 : 40; break;
                case COMBATE: valor = ferido ? 30 : 70; break;
                case ELITE: valor = heroi.getHpAtual() * 4 >= heroi.getHPMaximo() * 3 ? 90 : 5; break;
                case CHEFE: valor = 0; break;
                default: throw new AssertionError("Sala inválida");
            }
            if (valor > melhorValor) { melhor = sala; melhorValor = valor; }
        }
        expedicao.selecionar(melhor);
    }

    private static void descansar(Expedicao expedicao, Personagem heroi) {
        Expedicao.AcaoDescanso acao;
        if (heroi.getHpAtual() * 2 < heroi.getHPMaximo()) acao = Expedicao.AcaoDescanso.VIDA;
        else if (!expedicao.getReliquiasPendentes().isEmpty()) acao = Expedicao.AcaoDescanso.RELIQUIA;
        else if (heroi.getManaAtual() * 2 < heroi.getManaMaxima()) acao = Expedicao.AcaoDescanso.MANA;
        else if (heroi.getHpAtual() < heroi.getHPMaximo()) acao = Expedicao.AcaoDescanso.VIDA;
        else if (heroi.getManaAtual() < heroi.getManaMaxima()) acao = Expedicao.AcaoDescanso.MANA;
        else if (!expedicao.getJogo().limitePronto()) acao = Expedicao.AcaoDescanso.CONCENTRAR;
        else acao = Expedicao.AcaoDescanso.CONTINUAR;
        expedicao.descansar(acao);
    }

    static void lutar(Jogo jogo, Personagem heroi) {
        if (jogo.getEstado() == Jogo.Estado.COMBATE) {
            jogo.avancarTempo();
            return;
        }
        if (jogo.getEstado() != Jogo.Estado.ESCOLHA) return;
        if (heroi.getHpAtual() * 5 < heroi.getHPMaximo() * 2 && jogo.getPocoesVida() > 0) {
            jogo.usarPocao(true);
            return;
        }
        if (heroi.getManaAtual() < heroi.getCustoHabilidade() && jogo.getPocoesMana() > 0
                && heroi.getManaMaxima() >= 80) {
            jogo.usarPocao(false);
            return;
        }
        int alvo = -1;
        int melhor = Integer.MIN_VALUE;
        for (int i = 0; i < jogo.getInimigos().size(); i++) {
            Inimigo inimigo = jogo.getInimigos().get(i);
            if (!inimigo.estaVivo()) continue;
            int valor = (inimigo.getFraqueza() == heroi.getTipoAtaque() ? 2000
                    : inimigo.getResistencia() == heroi.getTipoAtaque() ? 0 : 1000) - inimigo.getVida();
            if (valor > melhor) { melhor = valor; alvo = i; }
        }
        Inimigo escolhido = jogo.getInimigos().get(alvo);
        if (jogo.limitePronto()) {
            jogo.usarLimite(alvo);
            return;
        }
        boolean habilidade = heroi.getManaAtual() >= heroi.getCustoHabilidade()
                && escolhido.getVida() > Math.max(1, heroi.getPoder() - escolhido.getDefesa());
        jogo.atacar(alvo, habilidade);
    }
}
