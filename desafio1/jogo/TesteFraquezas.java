package desafio1.jogo;

import desafio1.personagens.Personagem;

public final class TesteFraquezas {
    private TesteFraquezas() { }

    public static void executar() {
        verificarTipo(Inimigo.Tipo.SENTINELA, Personagem.TipoAtaque.MAGICO,
                Personagem.TipoAtaque.FISICO, "Mago", 10);
        verificarTipo(Inimigo.Tipo.ESPREITADOR, Personagem.TipoAtaque.FISICO,
                Personagem.TipoAtaque.MAGICO, "Guerreiro", 2);
        verificarTipo(Inimigo.Tipo.ARAUTO, Personagem.TipoAtaque.PERFURANTE,
                Personagem.TipoAtaque.FISICO, "Ladino", 4);
        verificarTipo(Inimigo.Tipo.GUARDIAO, Personagem.TipoAtaque.FISICO,
                Personagem.TipoAtaque.PERFURANTE, "Guerreiro", 9);
        verificarTipo(Inimigo.Tipo.GOLEM, Personagem.TipoAtaque.PERFURANTE,
                Personagem.TipoAtaque.MAGICO, "Ladino", 12);
        verificarTipo(Inimigo.Tipo.DUELISTA, Personagem.TipoAtaque.MAGICO,
                Personagem.TipoAtaque.PERFURANTE, "Mago", 2);
        testarNovosInimigos();
        testarArmaduraEFraquezas();
        compararClasses();
    }

    private static void testarArmaduraEFraquezas() {
        for (int nivel : new int[]{1, 4, 10}) {
            Inimigo sentinela = new Inimigo(Inimigo.Tipo.SENTINELA, nivel, nivel / 3, 1);
            int dano = sentinela.receberAtaque(20, Personagem.TipoAtaque.FISICO).getDano();
            verificar(dano >= 10, "Armadura e resistência não devem anular o dano físico");
        }
        for (int profundidade : new int[]{1, 4}) {
            Jogo jogo = new Jogo(new desafio1.personagens.Mago("Teste"));
            jogo.iniciarEncontroExpedicao(profundidade, false, false);
            while (jogo.getEstado() == Jogo.Estado.COMBATE) jogo.avancarTempo();
            Inimigo alvo = jogo.getInimigos().get(0);
            verificar(jogo.atacar(0, true) && alvo.estaVivo(),
                    "Mago sem melhorias não deve eliminar Sentinela ou Golem com uma habilidade");
        }
        Inimigo golem = new Inimigo(Inimigo.Tipo.GOLEM, 2, 0, 1);
        Inimigo.ResultadoDano magia = golem.receberAtaque(30, Personagem.TipoAtaque.MAGICO);
        Inimigo.ResultadoDano perfurante = new Inimigo(Inimigo.Tipo.GOLEM, 2, 0, 1)
                .receberAtaque(30, Personagem.TipoAtaque.PERFURANTE);
        verificar(magia.foiResistido() && !magia.explorouFraqueza()
                        && perfurante.explorouFraqueza() && perfurante.getDano() > magia.getDano(),
                "Golem deve resistir à magia e favorecer golpes perfurantes");
    }

    private static void compararClasses() {
        SimulacaoBalanceamento.Resultado[] resultados = SimulacaoBalanceamento.simular(200);
        SimulacaoBalanceamento.imprimir(resultados);
        double menor = 100;
        double maior = 0;
        for (SimulacaoBalanceamento.Resultado resultado : resultados) {
            verificar(resultado.taxaVitoria() >= 15 && resultado.taxaVitoria() <= 70,
                    "Com Limit supremo, cada classe deve poder vencer sem trivializar a expedição");
            menor = Math.min(menor, resultado.taxaVitoria());
            maior = Math.max(maior, resultado.taxaVitoria());
        }
        verificar(maior - menor <= 15, "Taxas de vitória com Limit supremo devem ficar a até 15 pontos");
        verificar(resultados[1].perdaMedia(Inimigo.Tipo.SENTINELA) >= 15
                        && resultados[1].perdaMedia(Inimigo.Tipo.GOLEM) >= 15,
                "Mago não deve atravessar Sentinelas e Golems sem perder vida");
    }

    private static void testarNovosInimigos() {
        int fisicoComum = new Inimigo(Inimigo.Tipo.ESPREITADOR, 2, 0, 1)
                .receberAtaque(30, Personagem.TipoAtaque.FISICO).getDano();
        int perfuranteComum = new Inimigo(Inimigo.Tipo.SENTINELA, 2, 0, 1)
                .receberAtaque(30, Personagem.TipoAtaque.PERFURANTE).getDano();
        verificar(new Inimigo(Inimigo.Tipo.GOLEM, 2, 0, 1)
                        .receberAtaque(30, Personagem.TipoAtaque.FISICO).getDano() < fisicoComum,
                "Golem deve dificultar o dano físico do Guerreiro");
        verificar(new Inimigo(Inimigo.Tipo.DUELISTA, 2, 0, 1)
                        .receberAtaque(30, Personagem.TipoAtaque.PERFURANTE).getDano() < perfuranteComum,
                "Duelista deve resistir aos golpes do Ladino");
        for (Inimigo.Tipo tipo : new Inimigo.Tipo[]{Inimigo.Tipo.GOLEM, Inimigo.Tipo.DUELISTA}) {
            Inimigo inimigo = new Inimigo(tipo, 2, 0, 1);
            int normal = 0;
            for (int acao = 0; acao < 3; acao++) {
                while (!inimigo.pronto()) inimigo.avancar();
                if (acao == 2) verificar(inimigo.habilidadePronta(),
                        "Nova habilidade deve ser anunciada após duas ações");
                int dano = inimigo.agir(18);
                if (acao == 0) normal = dano;
                if (acao == 2) verificar(dano > normal && !inimigo.habilidadePronta(),
                        "Nova habilidade deve penetrar defesa e entrar em recarga");
            }
        }
        Jogo golem = new Jogo(new desafio1.personagens.Guerreiro("Teste"));
        golem.iniciarEncontroExpedicao(4, false, false);
        verificar(golem.getInimigos().stream().anyMatch(i -> i.getTipo() == Inimigo.Tipo.GOLEM),
                "Golem deve aparecer nos encontros da dungeon");
        verificar(golem.getInimigos().size() == 1, "Encontro comum até a quinta camada deve ter um inimigo");
        Jogo duelista = new Jogo(new desafio1.personagens.Ladino("Teste"));
        duelista.iniciarEncontroExpedicao(7, false, false);
        verificar(duelista.getInimigos().stream().anyMatch(i -> i.getTipo() == Inimigo.Tipo.DUELISTA),
                "Duelista deve aparecer nos encontros da dungeon");
        verificar(duelista.getInimigos().size() == 2, "Encontro comum tardio deve limitar o grupo a dois inimigos");
    }

    private static void verificarTipo(Inimigo.Tipo tipo, Personagem.TipoAtaque fraqueza,
                                      Personagem.TipoAtaque resistencia, String classe, int armaduraBase) {
        Inimigo inimigo = new Inimigo(tipo, 1, 0, 1);
        verificar(inimigo.getFraqueza() == fraqueza, tipo + " deve revelar sua fraqueza");
        verificar(inimigo.getResistencia() == resistencia, tipo + " deve revelar sua resistência");
        verificar(inimigo.getClasseRecomendada().equals(classe), tipo + " deve recomendar a classe adequada");
        verificar(inimigo.getArmadura() == armaduraBase + 1, tipo + " deve calcular armadura pelo nível");
        verificar(inimigo.getNivelArmadura() != null && !inimigo.getNivelArmadura().isBlank(),
                tipo + " deve descrever o nível de armadura");

        Personagem.TipoAtaque neutro = java.util.Arrays.stream(Personagem.TipoAtaque.values())
                .filter(ataque -> ataque != fraqueza && ataque != resistencia)
                .findFirst().orElseThrow(AssertionError::new);
        int danoNeutro = new Inimigo(tipo, 1, 0, 1).        receberAtaque(30, neutro, true).getDano();
                int danoFraqueza = new Inimigo(tipo, 1, 0, 1).receberAtaque(30, fraqueza, true).getDano();
        int danoResistido = new Inimigo(tipo, 1, 0, 1).receberAtaque(30, resistencia).getDano();
        verificar(danoFraqueza > danoNeutro, tipo + " deve receber dano extra pela fraqueza");
        verificar(danoResistido < 30, tipo + " deve reduzir dano da resistência");
        verificar(new Inimigo(tipo, 1, 0, 1).receberAtaque(30, fraqueza).explorouFraqueza(),
                "O combate deve informar fraqueza explorada");
        verificar(new Inimigo(tipo, 1, 0, 1).receberAtaque(30, resistencia).foiResistido(),
                "O combate deve informar resistência");
    }

    private static void verificar(boolean condicao, String mensagem) {
        if (!condicao) throw new AssertionError(mensagem);
    }
}
