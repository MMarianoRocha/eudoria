package desafio1.jogo;

import desafio1.personagens.Guerreiro;
import desafio1.personagens.Ladino;
import desafio1.personagens.Mago;
import desafio1.personagens.Personagem;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Status, intenção dos inimigos, Limit Break, estatísticas e salvamento. */
public final class TesteSistemasCombate {
    private TesteSistemasCombate() { }

    public static void executar() {
        testarPrevisao();
        testarDefesa();
        testarDefesaPorConstituicao();
        testarDefesaEmGrupoEEncerramento();
        testarStatus();
        testarLimiteEEstatisticas();
        testarPotenciaLimites();
        testarSalvamento();
    }

    private static void testarPrevisao() {
        for (Inimigo.Tipo tipo : Inimigo.Tipo.values()) {
            Inimigo inimigo = new Inimigo(tipo, 2, 1, 1);
            for (int acao = 0; acao < 3; acao++) {
                int previsto = inimigo.previsaoDano(9);
                verificar(previsto == inimigo.agir(9), "Intenção deve prever o dano real de " + tipo);
            }
        }
    }

    private static void testarDefesa() {
        boolean testouImpar = false;
        boolean testouMinimo = false;
        for (Inimigo.Tipo tipo : Inimigo.Tipo.values()) {
            for (int destreza : new int[]{0, 4, 100}) {
                Personagem heroi = new Guerreiro("Defesa");
                heroi.treinarVida(2000);
                heroi.treinarDestreza(destreza);
                Jogo jogo = new Jogo(heroi);
                verificar(!jogo.defender() && !jogo.estaDefendendo(), "Não pode defender no intervalo");
                jogo.iniciarEncontroExpedicao(tipo.ordinal() + 1, false, tipo == Inimigo.Tipo.GUARDIAO);
                verificar(!jogo.defender() && jogo.getAtb() == 0, "Defender exige a vez do herói");
                Inimigo inimigo = jogo.getInimigos().get(0);
                aguardarEscolha(jogo);
                for (int acao = 0; acao < 3; acao++) {
                    if (acao == 2) verificar(inimigo.habilidadePronta(), "Defesa deve cobrir a habilidade de " + tipo);
                    int bruto = inimigo.previsaoDano(heroi.getDefesa());
                    int esperado = Math.max(1, bruto * 48 / 100);
                    testouImpar |= bruto > 1 && bruto % 2 == 1;
                    testouMinimo |= bruto == 1;
                    int mana = heroi.getManaAtual();
                    int limite = jogo.getLimite();
                    verificar(jogo.defender() && jogo.estaDefendendo() && jogo.getAtb() == 0
                                    && jogo.getEstado() == Jogo.Estado.COMBATE
                                    && heroi.getManaAtual() == mana && jogo.getLimite() == limite,
                            "Defender consome o ATB, sem mana nem LIMIT gratuito");
                    verificar(!jogo.defender() && jogo.estaDefendendo(), "Defesa não pode acumular fora da vez");
                    verificar(jogo.previsaoDano(inimigo) == esperado, "Intenção deve considerar a defesa ativa");
                    prepararAtaque(inimigo);
                    int vida = heroi.getHpAtual();
                    long recebido = jogo.getEstatisticas().getDanoRecebido();
                    jogo.avancarTempo();
                    verificar(vida - heroi.getHpAtual() == esperado
                                    && jogo.getEstatisticas().getDanoRecebido() - recebido == esperado,
                            "Defesa deve reduzir ataques e habilidades após a defesa normal: " + tipo);
                    verificar(jogo.getLimite() == Math.min(100,
                                    limite + Math.max(2, esperado * 100 / heroi.getHPMaximo())),
                            "LIMIT por dano deve usar apenas a vida realmente perdida");
                    verificar(jogo.estaDefendendo(), "Primeiro ataque não deve consumir a proteção");
                    aguardarEscolha(jogo);
                    verificar(!jogo.estaDefendendo()
                                    && jogo.previsaoDano(inimigo) == inimigo.previsaoDano(heroi.getDefesa()),
                            "Defesa deve terminar no início da próxima vez");
                }
            }
        }
        verificar(testouImpar && testouMinimo, "Defesa deve cobrir arredondamento e dano mínimo");
    }

    private static void testarDefesaPorConstituicao() {
        int[][] casos = {{4, 50}, {5, 51}, {9, 51}, {10, 52}, {14, 52}, {15, 53},
                {30, 56}, {74, 64}, {75, 65}, {80, 65}, {100, 65}};
        for (int[] caso : casos) {
            Personagem heroi = new GuerreiroConstituicao(caso[0]);
            heroi.treinarVida(10_000);
            Jogo jogo = new Jogo(heroi);
            jogo.iniciarEncontroExpedicao(120, false, true);
            aguardarEscolha(jogo);
            Inimigo inimigo = jogo.getInimigos().get(0);
            int bruto = inimigo.previsaoDano(heroi.getDefesa());
            verificar(jogo.previsaoDano(inimigo) == bruto,
                    "Constituição não deve aplicar a postura defensiva sem Defender");
            verificar(jogo.defender(), "Defender deve funcionar em todos os patamares de Constituição");
            int esperado = bruto * (100 - caso[1]) / 100;
            verificar(jogo.previsaoDano(inimigo) == esperado,
                    "Previsão deve respeitar patamares de 5 e teto de 65%: CON " + caso[0]);
            prepararAtaque(inimigo);
            int vida = heroi.getHpAtual();
            jogo.avancarTempo();
            verificar(vida - heroi.getHpAtual() == esperado,
                    "Dano real deve usar " + caso[1] + "% de redução para CON " + caso[0]);
        }

        Personagem heroi = new GuerreiroConstituicao(14);
        Jogo jogo = new Jogo(heroi);
        heroi.melhorarVitalidade();
        jogo.iniciarEncontroExpedicao(30, false, true);
        aguardarEscolha(jogo);
        Inimigo inimigo = jogo.getInimigos().get(0);
        int bruto = inimigo.previsaoDano(heroi.getDefesa());
        jogo.defender();
        verificar(jogo.previsaoDano(inimigo) == bruto * 47 / 100,
                "Melhorias de Constituição devem atualizar a proteção sem recriar a partida");
    }

    private static void testarDefesaEmGrupoEEncerramento() {
        Jogo grupo = new Jogo(new Guerreiro("Grupo"));
        grupo.iniciarEncontroExpedicao(6, false, false);
        aguardarEscolha(grupo);
        grupo.defender();
        int esperado = 0;
        int limiteEsperado = 0;
        for (Inimigo inimigo : grupo.getInimigos()) {
            int dano = grupo.previsaoDano(inimigo);
            esperado += dano;
            limiteEsperado += Math.max(2, dano * 100 / grupo.getHeroi().getHPMaximo());
            prepararAtaque(inimigo);
        }
        verificar(grupo.getInimigos().size() == 2, "Cenário deve incluir múltiplos atacantes");
        int vida = grupo.getHeroi().getHpAtual();
        grupo.avancarTempo();
        verificar(vida - grupo.getHeroi().getHpAtual() == esperado && grupo.estaDefendendo(),
                "Defesa deve proteger de todos os inimigos, inclusive no mesmo instante");
        verificar(grupo.getLimite() == limiteEsperado,
                "Defesa deve reduzir também a carga proporcional de LIMIT por dano recebido");
        Inimigo alvo = grupo.getInimigos().get(0);
        prepararAtaque(alvo);
        esperado = grupo.previsaoDano(alvo);
        vida = grupo.getHeroi().getHpAtual();
        grupo.avancarTempo();
        verificar(vida - grupo.getHeroi().getHpAtual() == esperado,
                "Defesa também deve proteger de ataques posteriores na mesma janela");
        while (grupo.getEstado() == Jogo.Estado.COMBATE && grupo.getAtb() < 98) grupo.avancarTempo();
        verificar(grupo.getEstado() == Jogo.Estado.COMBATE && grupo.estaDefendendo(),
                "Defesa deve durar até o último instante antes da próxima escolha");
        prepararAtaque(alvo);
        esperado = grupo.previsaoDano(alvo);
        vida = grupo.getHeroi().getHpAtual();
        grupo.avancarTempo();
        verificar(vida - grupo.getHeroi().getHpAtual() == esperado
                        && grupo.getEstado() == Jogo.Estado.ESCOLHA && !grupo.estaDefendendo(),
                "Ataque no instante de completar o ATB ainda deve ser protegido, antes de expirar");

        Jogo vitoria = new Jogo(new Guerreiro("Status"));
        vitoria.iniciarEncontroExpedicao(1, false, false);
        aguardarEscolha(vitoria);
        vitoria.defender();
        Inimigo ultimo = vitoria.getInimigos().get(0);
        ultimo.queimar(ultimo.getVidaMaxima());
        for (int tick = 1; tick < Inimigo.INTERVALO_PULSO; tick++) ultimo.sofrerEfeitosContinuos();
        vitoria.avancarTempo();
        verificar(vitoria.getEstado() == Jogo.Estado.INTERVALO && !vitoria.estaDefendendo(),
                "Vitória por efeito contínuo deve limpar a defesa");
        vitoria.proximaOnda();
        verificar(!vitoria.estaDefendendo(), "Proteção não pode passar para o próximo encontro");

        Jogo derrota = new Jogo(new Guerreiro("Derrota"));
        derrota.proximaOnda();
        aguardarEscolha(derrota);
        derrota.getHeroi().receberDano(derrota.getHeroi().getHpAtual() - 1);
        derrota.defender();
        prepararAtaque(derrota.getInimigos().get(0));
        derrota.avancarTempo();
        verificar(derrota.getEstado() == Jogo.Estado.DERROTA && !derrota.estaDefendendo(),
                "Defender não torna invencível e deve ser limpo na derrota");
    }

    private static void prepararAtaque(Inimigo inimigo) {
        for (int tick = 0; tick < 500 && !inimigo.pronto(); tick++) inimigo.avancar();
        verificar(inimigo.pronto(), "Inimigo de teste deve estar pronto para atacar");
    }

    private static void testarStatus() {
        Inimigo inimigo = new Inimigo(Inimigo.Tipo.GOLEM, 1, 0, 1);
        int vida = inimigo.getVida();
        inimigo.envenenar(3);
        inimigo.envenenar(3);
        inimigo.queimar(4);
        verificar(inimigo.getAcumulosVeneno() == 2 && inimigo.getDanoVeneno() == 6, "Veneno acumula");
        int total = 0;
        for (int tick = 1; tick < Inimigo.INTERVALO_PULSO; tick++) total += inimigo.sofrerEfeitosContinuos();
        verificar(total == 0, "Efeitos pulsam apenas no intervalo");
        verificar(inimigo.sofrerEfeitosContinuos() == 10, "Pulso soma veneno acumulado e queimadura");
        for (int tick = 0; tick < Inimigo.INTERVALO_PULSO * Inimigo.PULSOS_VENENO; tick++) {
            total += inimigo.sofrerEfeitosContinuos();
        }
        verificar(inimigo.getQueimadura() == 0 && inimigo.getVeneno() == 0
                        && inimigo.getVida() == vida - 6 * Inimigo.PULSOS_VENENO - 4 * Inimigo.PULSOS_QUEIMADURA,
                "Veneno dura 5 pulsos, queimadura 3, e ambos ignoram armadura");
        for (int i = 0; i < 4; i++) inimigo.envenenar(2);
        verificar(inimigo.getAcumulosVeneno() == Inimigo.MAX_ACUMULOS_VENENO, "Veneno acumula até o máximo");

        int atb = inimigo.getAtb();
        verificar(inimigo.atordoar() && inimigo.isAtordoado(), "Atordoamento é aplicado");
        for (int i = 0; i < Inimigo.ATORDOAMENTO; i++) inimigo.avancar();
        verificar(inimigo.getAtb() == atb && !inimigo.isAtordoado(), "Atordoamento congela o ATB");
        inimigo.avancar();
        verificar(inimigo.getAtb() >= atb, "ATB volta a carregar após o atordoamento");
        verificar(!inimigo.atordoar(), "Inimigo fica imune ao atordoamento até agir");
        inimigo.agir(0);
        verificar(!inimigo.isImuneAtordoamento(), "Agir remove a imunidade ao atordoamento");
    }

    private static void testarLimiteEEstatisticas() {
        Personagem heroi = new Guerreiro("Limite");
        Jogo jogo = new Jogo(heroi);
        jogo.ganharOuro(20 * 400);
        for (int i = 0; i < 400; i++) jogo.comprar(Jogo.Melhoria.POCAO_VIDA);
        jogo.iniciarEncontroExpedicao(3, false, true);
        boolean tentouSemCarga = false;
        for (int passo = 0; passo < 500_000 && !jogo.limitePronto(); passo++) {
            heroi.restaurarVida(heroi.getHPMaximo());
            if (jogo.getEstado() == Jogo.Estado.ESCOLHA) {
                if (!tentouSemCarga) {
                    verificar(!jogo.usarLimite(0), "Limit Break exige a barra cheia");
                    tentouSemCarga = true;
                }
                heroi.receberDano(1);
                verificar(jogo.usarPocao(true), "Poção usada para passar o turno durante a carga");
            }
            jogo.avancarTempo();
        }
        verificar(jogo.limitePronto() && jogo.getEstatisticas().getDanoRecebido() > 0,
                "Dano recebido deve carregar o Limit Break");
        while (jogo.getEstado() != Jogo.Estado.ESCOLHA) {
            heroi.restaurarVida(heroi.getHPMaximo());
            jogo.avancarTempo();
        }
        Inimigo chefe = jogo.getInimigos().get(0);
        int vida = chefe.getVida();
        verificar(jogo.usarLimite(0) && jogo.getLimite() == 0 && chefe.getVida() < vida
                        && jogo.getEstatisticas().getLimitesUsados() == 1,
                "Limit Break causa dano e esvazia a barra");
        verificar(chefe.isImuneAtordoamento(), "Fúria titânica atordoa o alvo");
        for (int passo = 0; passo < 2_000_000 && jogo.getEstado() != Jogo.Estado.INTERVALO; passo++) {
            heroi.restaurarVida(heroi.getHPMaximo());
            if (jogo.getEstado() == Jogo.Estado.ESCOLHA) jogo.atacar(0, false);
            jogo.avancarTempo();
        }
        Jogo.Estatisticas estatisticas = jogo.getEstatisticas();
        verificar(jogo.getEstado() == Jogo.Estado.INTERVALO && estatisticas.getAbates() == 1
                        && estatisticas.getChefesDerrotados() == 1 && estatisticas.getEncontrosVencidos() == 1,
                "Abates e chefes finalizados devem ser contabilizados");
        verificar(jogo.getLimite() >= Jogo.LIMITE_POR_ACAO + Jogo.LIMITE_POR_ABATE,
                "Ações ofensivas e abates carregam o Limit Break");
        verificar(heroi.getNivel() > 1 && jogo.getEscolhasNivel() > 0, "Vitória concede nível imediato");
        int escolhas = jogo.getEscolhasNivel();
        int poder = heroi.getPoder();
        verificar(jogo.escolherBonusNivel(Jogo.BonusNivel.PODER) && heroi.getPoder() == poder + 3
                        && jogo.getEscolhasNivel() == escolhas - 1,
                "Bônus de nível escolhido é aplicado");
    }

    private static void testarPotenciaLimites() {
        for (int bonus : new int[]{0, 20}) {
            Personagem[][] classes = {
                    {new Guerreiro("Habilidade"), new Guerreiro("Limit")},
                    {new Mago("Habilidade"), new Mago("Limit")},
                    {new Ladino("Habilidade"), new Ladino("Limit")}
            };
            for (Personagem[] classe : classes) {
                Jogo[] jogos = {new Jogo(classe[0]), new Jogo(classe[1])};
                for (Jogo jogo : jogos) {
                    jogo.getHeroi().treinarPoder(bonus);
                    jogo.iniciarEncontroExpedicao(bonus == 0 ? 8 : 18, false, true);
                    aguardarEscolha(jogo);
                }
                Jogo comum = jogos[0];
                Jogo supremo = jogos[1];
                verificar(comum.atacar(0, true), "Habilidade de comparação deve funcionar");
                supremo.adicionarLimite(Jogo.LIMITE_MAXIMO);
                int mana = supremo.getHeroi().getManaAtual();
                verificar(!supremo.usarLimite(-1) && supremo.limitePronto()
                                && supremo.getEstado() == Jogo.Estado.ESCOLHA,
                        "Alvo inválido não deve consumir Limit nem turno");
                verificar(supremo.usarLimite(0), "Limit deve funcionar para " + classe[1].getClasse());
                verificar(supremo.getEstatisticas().getDanoCausado()
                                >= comum.getEstatisticas().getDanoCausado() * 3 / 2,
                        "Limit deve superar a habilidade em pelo menos 50%, inclusive com melhorias: "
                                + classe[1].getClasse());
                verificar(supremo.getLimite() == 0 && supremo.getHeroi().getManaAtual() == mana
                                && supremo.getEstado() == Jogo.Estado.COMBATE,
                        "Limit deve consumir a carga e o turno, mas não mana");
                Inimigo alvo = supremo.getInimigos().get(0);
                verificar(alvo.estaVivo(), "Chefe de teste deve sobreviver para verificar os efeitos");
                switch (classe[1].getTipoAtaque()) {
                    case FISICO: verificar(alvo.isAtordoado(), "Fúria deve manter atordoamento"); break;
                    case MAGICO: verificar(alvo.getQueimadura() > 0, "Meteoro deve manter queimadura"); break;
                    default: verificar(alvo.getAcumulosVeneno() == 1, "Mil cortes deve manter veneno"); break;
                }
            }
        }

        Jogo area = new Jogo(new Mago("Área"));
        area.iniciarEncontroExpedicao(12, false, false);
        aguardarEscolha(area);
        area.adicionarLimite(Jogo.LIMITE_MAXIMO);
        verificar(area.usarLimite(1), "Meteoro aceita qualquer alvo vivo");
        verificar(area.getInimigos().size() == 2, "Encontro deve ter múltiplos alvos");
        for (Inimigo inimigo : area.getInimigos()) {
            verificar(inimigo.getVida() < inimigo.getVidaMaxima() && inimigo.getQueimadura() > 0,
                    "Meteoro deve causar dano e queimadura em todos os inimigos vivos");
        }

        Jogo letal = new Jogo(new Guerreiro("Abate"));
        letal.getHeroi().treinarPoder(100);
        letal.proximaOnda();
        aguardarEscolha(letal);
        letal.adicionarLimite(Jogo.LIMITE_MAXIMO);
        verificar(letal.usarLimite(0) && letal.getEstado() == Jogo.Estado.INTERVALO
                        && letal.getEstatisticas().getAbates() == 1
                        && letal.getLimite() == Jogo.LIMITE_POR_ABATE,
                "Limit letal deve concluir encontro, contar um abate e carregar apenas a recompensa do abate");
    }

    private static void aguardarEscolha(Jogo jogo) {
        for (int tick = 0; tick < 200 && jogo.getEstado() == Jogo.Estado.COMBATE; tick++) {
            jogo.avancarTempo();
        }
        verificar(jogo.getEstado() == Jogo.Estado.ESCOLHA, "ATB deve oferecer uma escolha");
    }

    private static void testarSalvamento() {
        Path arquivo = null;
        try {
            arquivo = Files.createTempFile("eudoria-save", ".dat");
            Salvamento salvamento = new Salvamento(arquivo);
            Expedicao expedicao = new Expedicao(new Guerreiro("Salvo"), 11);
            expedicao.sairDaLoja();
            RegistroRun run = RegistroRun.deExpedicao(expedicao);
            salvamento.registrar(run);
            salvamento.registrar(run);
            List<RegistroRun> runs = salvamento.listar();
            verificar(runs.size() == 1 && runs.get(0).getId().equals(run.getId()),
                    "Salvar a mesma run deve atualizar o registro existente");
            RegistroRun carregada = runs.get(0);
            Expedicao continuada = carregada.getExpedicao();
            verificar(carregada.podeContinuar() && continuada.getJogo() == carregada.getJogo()
                            && continuada.getEstado() == Expedicao.Estado.MAPA
                            && "Salvo".equals(continuada.getJogo().getHeroi().getNome()),
                    "Carregar deve restaurar a expedição com o mesmo estado");
            Expedicao.Sala sala = continuada.getSalaAtual().getSaidas().get(0);
            continuada.selecionar(sala);
            verificar(continuada.getSalaAtual() == sala, "A run carregada deve continuar jogável");
            aguardarEscolha(carregada.getJogo());
            carregada.getJogo().defender();
            salvamento.registrar(carregada);
            Jogo protegido = salvamento.listar().get(0).getJogo();
            verificar(protegido.estaDefendendo() && protegido.getEstado() == Jogo.Estado.COMBATE
                            && protegido.getAtb() == 0,
                    "Carregar deve preservar a proteção durante o combate");
            Inimigo inimigo = protegido.getInimigos().get(0);
            prepararAtaque(inimigo);
            int previsto = protegido.previsaoDano(inimigo);
            int vida = protegido.getHeroi().getHpAtual();
            protegido.avancarTempo();
            verificar(vida - protegido.getHeroi().getHpAtual() == previsto,
                    "Defesa carregada do save deve continuar reduzindo o dano");

            Files.write(arquivo, "corrompido".getBytes(StandardCharsets.UTF_8));
            verificar(salvamento.listar().isEmpty(), "Arquivo corrompido não deve derrubar o jogo");
        } catch (IOException erro) {
            throw new AssertionError(erro);
        } finally {
            try { if (arquivo != null) Files.deleteIfExists(arquivo); } catch (IOException ignorado) { }
        }
    }

    private static final class GuerreiroConstituicao extends Guerreiro {
        GuerreiroConstituicao(int valor) {
            super("Constituição");
            constituicao = valor;
            hpAtual = getHPMaximo();
        }
    }

    private static void verificar(boolean condicao, String mensagem) {
        if (!condicao) throw new AssertionError(mensagem);
    }
}
