package testes;

import desafio1.jogo.Expedicao;
import desafio1.jogo.Jogo;
import desafio1.jogo.Inimigo;
import desafio1.jogo.RegistroRun;
import desafio1.jogo.Salvamento;
import desafio1.personagens.Guerreiro;
import desafio1.personagens.Ladino;
import desafio1.personagens.Mago;
import desafio1.personagens.Personagem;

import java.util.ArrayList;
import java.util.List;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class TesteExpedicao {
    public static void main(String[] args) {
        testarMapas();
        testarEconomiaETransicoes();
        testarEventos();
        testarRiscosERecursos();
        testarMelhoriasRestritas();
        testarAtivacaoNoDescanso();
        testarJornada(new Guerreiro("Guerreiro"));
        testarJornada(new Mago("Mago"));
        testarJornada(new Ladino("Ladino"));
        for (int classe = 0; classe < 3; classe++) testarRotaReal(classe);
        testarCampanha(new Guerreiro("Campanha"));
        testarCampanha(new Mago("Ascensão"));
        testarCampanha(new Ladino("Ascensão"));
        System.out.println("Testes da expedição passaram.");
    }

    private static void testarCampanha(Personagem heroi) {
        Expedicao expedicao = new Expedicao(heroiPreparado(heroi), 91);
        exigirTransicaoBloqueada(expedicao::avancarCapitulo);
        exigirTransicaoBloqueada(expedicao::concluirExpedicao);
        expedicao.getJogo().comprar(Jogo.Melhoria.POCAO_VIDA);
        expedicao.getJogo().comprar(Jogo.Melhoria.POCAO_MANA);
        expedicao.sairDaLoja();
        Expedicao.Sala elite = encontrarSalaPorAndar(expedicao.getSalas(), 4, Expedicao.TipoSala.ELITE);
        for (Expedicao.Sala sala : caminhoParaSala(expedicao.getSalaAtual(), elite)) {
            expedicao.selecionar(sala);
            resolverSala(expedicao);
        }
        Expedicao.Sala descanso = encontrarSalaPorAndar(expedicao.getSalas(), 5, Expedicao.TipoSala.ACAMPAMENTO);
        for (Expedicao.Sala sala : caminhoParaSala(expedicao.getSalaAtual(), descanso)) {
            expedicao.selecionar(sala);
            if (sala != descanso) resolverSala(expedicao);
        }
        expedicao.descansar(Expedicao.AcaoDescanso.RELIQUIA);
        verificar(!expedicao.getReliquiasAtivas().isEmpty(), "Campanha deve carregar relíquia ativa");

        int nivelChefeAnterior = 0;
        int capitulos = heroi instanceof Guerreiro ? 3 : 7;
        for (int etapa = 0; etapa < capitulos; etapa++) {
            vencerCapitulo(expedicao);
            Jogo jogo = expedicao.getJogo();
            Inimigo chefe = jogo.getInimigos().get(0);
            verificar(chefe.isChefe() && chefe.getNome().equals(expedicao.getChefe().getNome()),
                    "Cada capítulo deve usar seu próprio chefe");
            verificar(chefe.getNivel() > nivelChefeAnterior,
                    "A dificuldade deve crescer entre capítulos e Ascensões");
            nivelChefeAnterior = chefe.getNivel();
            verificar(jogo.getEstatisticas().getChefesDerrotados() == etapa + 1,
                    "Todos os tipos de chefe devem ser contados uma única vez");
            verificar(expedicao.getEstado() == (etapa % 3 == 2 ? Expedicao.Estado.CAMPANHA_CONCLUIDA
                            : Expedicao.Estado.CAPITULO_CONCLUIDO),
                    "Somente o terceiro chefe conclui a campanha ou Ascensão");

            expedicao = recarregar(expedicao);
            jogo = expedicao.getJogo();
            if (jogo.getEscolhasNivel() > 0) exigirTransicaoBloqueada(expedicao::avancarCapitulo);
            while (jogo.getEscolhasNivel() > 0) jogo.escolherBonusNivel(Jogo.BonusNivel.PODER);
            if (etapa == capitulos - 1) {
                expedicao.concluirExpedicao();
                RegistroRun registro = RegistroRun.deExpedicao(expedicao);
                verificar(registro.getResultado() == RegistroRun.Resultado.VITORIA && !registro.podeContinuar(),
                        "Encerrar com vitória deve guardar resumo e descartar estado apenas após a escolha");
                verificar(registro.descricaoProgresso().contains(expedicao.getCapitulo() + "/3"),
                        "Histórico deve preservar o capítulo final");
                break;
            }
            if (etapa < 2) exigirTransicaoBloqueada(expedicao::concluirExpedicao);
            int ouro = jogo.getOuro();
            int xp = jogo.getExperiencia();
            int nivel = jogo.getHeroi().getNivel();
            int vida = jogo.getHeroi().getHpAtual();
            int mana = jogo.getHeroi().getManaAtual();
            int poder = jogo.getHeroi().getPoder();
            int limite = jogo.getLimite();
            int pocoesVida = jogo.getPocoesVida();
            int pocoesMana = jogo.getPocoesMana();
            List<Expedicao.Reliquia> ativas = new ArrayList<>(expedicao.getReliquiasAtivas());
            List<Expedicao.Reliquia> pendentes = new ArrayList<>(expedicao.getReliquiasPendentes());
            Expedicao.Sala mapaAnterior = expedicao.getSalaAtual();
            expedicao.avancarCapitulo();
            verificar(expedicao.getCapitulo() == (etapa + 1) % 3 + 1
                            && expedicao.getCiclo() == (etapa + 1) / 3,
                    "Capítulo e Ascensão devem avançar sem reiniciar o herói");
            verificar(expedicao.getJogo() == jogo && jogo.getOuro() == ouro && jogo.getExperiencia() == xp
                            && jogo.getHeroi().getNivel() == nivel && jogo.getHeroi().getHpAtual() == vida
                            && jogo.getHeroi().getManaAtual() == mana && jogo.getHeroi().getPoder() == poder
                            && jogo.getLimite() == limite && jogo.getPocoesVida() == pocoesVida
                            && jogo.getPocoesMana() == pocoesMana
                            && expedicao.getReliquiasAtivas().equals(ativas)
                            && expedicao.getReliquiasPendentes().equals(pendentes),
                    "Transição deve preservar todos os atributos, recursos e relíquias sem reaplicá-las");
            verificar(expedicao.getEstado() == Expedicao.Estado.LOJA
                            && expedicao.getSalaAtual().getAndar() == 0
                            && expedicao.getSalas().size() == 23
                            && !expedicao.getSalas().contains(mapaAnterior)
                            && expedicao.getSalas().stream().filter(Expedicao.Sala::foiVisitada).count() == 1,
                    "Novo capítulo deve gerar somente seu próprio mapa, com conteúdo oculto e loja aberta");
            verificar(jogo.comprar(Jogo.Melhoria.POCAO_VIDA), "Loja deve aceitar compras na nova entrada");
            expedicao = recarregar(expedicao);
            if (expedicao.getCapitulo() > 1) testarEventoDoCapitulo(expedicao);
        }
    }

    private static void testarEventoDoCapitulo(Expedicao expedicao) {
        expedicao.sairDaLoja();
        Expedicao.Sala evento = encontrarSalaPorAndar(expedicao.getSalas(), 3, Expedicao.TipoSala.EVENTO);
        for (Expedicao.Sala sala : caminhoParaSala(expedicao.getSalaAtual(), evento)) {
            expedicao.selecionar(sala);
            if (sala != evento) resolverSala(expedicao);
        }
        Personagem heroi = expedicao.getJogo().getHeroi();
        verificar(expedicao.getEventoAtual() == (expedicao.getCapitulo() == 2
                        ? Expedicao.Evento.FONTE : Expedicao.Evento.FORJA),
                "Cada novo capítulo deve introduzir seu evento");
        Expedicao alternativa = recarregar(expedicao);
        Personagem outroHeroi = alternativa.getJogo().getHeroi();
        int pendentesAlternativa = alternativa.getReliquiasPendentes().size();
        if (expedicao.getCapitulo() == 2) {
            outroHeroi.receberDano(40);
            int antes = outroHeroi.getHpAtual();
            alternativa.resolverEvento(Expedicao.EscolhaEvento.CUIDADOSO);
            verificar(outroHeroi.getHpAtual() == Math.min(outroHeroi.getHPMaximo(),
                            antes + outroHeroi.getHPMaximo() / 4),
                    "Fonte deve restaurar vida sem ultrapassar o máximo");
        } else {
            int antes = outroHeroi.getHpAtual();
            alternativa.resolverEvento(Expedicao.EscolhaEvento.ARRISCAR);
            verificar(outroHeroi.getHpAtual() == antes - outroHeroi.getHPMaximo() / 4
                            && alternativa.getReliquiasPendentes().get(pendentesAlternativa) == Expedicao.Reliquia.FOCO,
                    "Forja arriscada deve cobrar vida e guardar Foco");
            Expedicao fatal = recarregar(expedicao);
            Personagem ferido = fatal.getJogo().getHeroi();
            ferido.receberDano(ferido.getHpAtual() - 1);
            fatal.resolverEvento(Expedicao.EscolhaEvento.ARRISCAR);
            fatal.continuarResultado();
            verificar(fatal.getEstado() == Expedicao.Estado.DERROTA
                            && fatal.getReliquiasPendentes().size() == pendentesAlternativa
                            && fatal.getJogo().getEstatisticas().getMortes() == 1,
                    "Forja fatal deve registrar derrota sem conceder relíquia");
        }
        heroi.gastarMana(heroi.getManaAtual());
        Expedicao.EscolhaEvento escolha = expedicao.getCapitulo() == 2
                ? Expedicao.EscolhaEvento.ARRISCAR : Expedicao.EscolhaEvento.CUIDADOSO;
        int pendentes = expedicao.getReliquiasPendentes().size();
        verificar(!expedicao.resolverEvento(escolha) && expedicao.getEstado() == Expedicao.Estado.EVENTO
                        && expedicao.getReliquiasPendentes().size() == pendentes,
                "Novo evento deve recusar mana insuficiente sem consumir sala ou conceder recompensa");
        heroi.restaurarMana(heroi.getManaMaxima());
        int mana = heroi.getManaAtual();
        int limite = expedicao.getJogo().getLimite();
        verificar(expedicao.resolverEvento(escolha), "Novo evento deve aceitar o custo correto");
        if (expedicao.getCapitulo() == 2) {
            verificar(heroi.getManaAtual() == mana - 20
                            && expedicao.getJogo().getLimite() == Math.min(100, limite + 50),
                    "Fonte deve converter exatamente 20 mana em 50 LIMIT");
        } else {
            verificar(heroi.getManaAtual() == mana - 10
                            && expedicao.getReliquiasPendentes().size() == pendentes + 1
                            && expedicao.getReliquiasPendentes().get(pendentes) == Expedicao.Reliquia.AMULETO,
                    "Forja deve guardar Amuleto sem ativá-lo");
        }
        expedicao.continuarResultado();
    }

    private static void vencerCapitulo(Expedicao expedicao) {
        for (int passo = 0; passo < 20; passo++) {
            if (expedicao.getEstado() == Expedicao.Estado.CAPITULO_CONCLUIDO
                    || expedicao.getEstado() == Expedicao.Estado.CAMPANHA_CONCLUIDA) return;
            if (expedicao.getEstado() == Expedicao.Estado.LOJA) expedicao.sairDaLoja();
            else {
                verificar(expedicao.getEstado() == Expedicao.Estado.MAPA, "Capítulo deve permitir explorar");
                expedicao.selecionar(expedicao.getSalaAtual().getSaidas().get(0));
                resolverSala(expedicao);
            }
        }
        throw new AssertionError("Capítulo não terminou");
    }

    private static Expedicao recarregar(Expedicao expedicao) {
        Path arquivo = null;
        try {
            arquivo = Files.createTempFile("eudoria-campanha", ".dat");
            Salvamento salvamento = new Salvamento(arquivo);
            RegistroRun registro = RegistroRun.deExpedicao(expedicao);
            salvamento.registrar(registro);
            RegistroRun carregada = salvamento.listar().get(0);
            verificar(carregada.podeContinuar() && carregada.getJogo() == carregada.getExpedicao().getJogo()
                            && carregada.getExpedicao().getEstado() == expedicao.getEstado()
                            && carregada.getExpedicao().getCapitulo() == expedicao.getCapitulo()
                            && carregada.getExpedicao().getCiclo() == expedicao.getCiclo()
                            && carregada.descricaoProgresso().contains("Cap. " + expedicao.getCapitulo()),
                    "Checkpoint deve continuar carregável, inclusive após vencer um chefe");
            return carregada.getExpedicao();
        } catch (IOException erro) {
            throw new AssertionError(erro);
        } finally {
            if (arquivo != null) {
                try { Files.deleteIfExists(arquivo); } catch (IOException erro) { throw new AssertionError(erro); }
            }
        }
    }

    private static void exigirTransicaoBloqueada(Runnable acao) {
        try {
            acao.run();
            throw new AssertionError("Transição fora de hora deve ser recusada");
        } catch (IllegalStateException esperado) {
            // A recusa preserva o estado da campanha.
        }
    }

    private static void testarMelhoriasRestritas() {
        Personagem heroi = heroiPreparado(new Guerreiro("Teste"));
        Expedicao expedicao = new Expedicao(heroi, 91);
        Jogo jogo = expedicao.getJogo();
        int poderInicial = heroi.getPoder();
        Expedicao.Sala elite = encontrarSalaPorAndar(expedicao.getSalas(), 4, Expedicao.TipoSala.ELITE);
        List<Expedicao.Sala> caminhoElite = caminhoParaSala(expedicao.getSalaAtual(), elite);
        verificar(caminhoElite != null, "Elite da quarta camada deve ser acessível");
        expedicao.sairDaLoja();
        for (Expedicao.Sala sala : caminhoElite) {
            expedicao.selecionar(sala);
            resolverSala(expedicao);
        }
        verificar(expedicao.getEstado() == Expedicao.Estado.MAPA
                        && !expedicao.getReliquiasPendentes().isEmpty(),
                "Elite deve guardar uma relíquia sem aplicá-la");
        int reliquiasGuardadas = expedicao.getReliquiasPendentes().size();
        verificar(heroi.getNivel() >= 2 && jogo.getEscolhasNivel() == heroi.getNivel() - 1,
                "Níveis são aplicados ao fim do combate e geram escolhas de bônus");
        int poderAntesBonus = heroi.getPoder();
        verificar(jogo.escolherBonusNivel(Jogo.BonusNivel.PODER) && heroi.getPoder() == poderAntesBonus + 3,
                "Bônus de nível escolhido deve ser aplicado");
        while (jogo.getEscolhasNivel() > 0) jogo.escolherBonusNivel(Jogo.BonusNivel.VIDA);
        verificar(!jogo.escolherBonusNivel(Jogo.BonusNivel.PODER), "Sem nível novo não há bônus");
        Expedicao.Sala acampamento = encontrarSalaPorAndar(expedicao.getSalas(), 5, Expedicao.TipoSala.ACAMPAMENTO);
        List<Expedicao.Sala> caminhoDescanso = caminhoParaSala(expedicao.getSalaAtual(), acampamento);
        verificar(caminhoDescanso != null, "Acampamento após elite deve ser alcançável");
        for (Expedicao.Sala sala : caminhoDescanso) {
            expedicao.selecionar(sala);
            if (expedicao.getEstado() == Expedicao.Estado.COMBATE) resolverSala(expedicao);
            else if (expedicao.getEstado() == Expedicao.Estado.EVENTO) {
                expedicao.resolverEvento(Expedicao.EscolhaEvento.IGNORAR);
                expedicao.continuarResultado();
            } else if (expedicao.getEstado() == Expedicao.Estado.LOJA) expedicao.sairDaLoja();
        }
        verificar(expedicao.getEstado() == Expedicao.Estado.ACAMPAMENTO, "Rota deve chegar ao acampamento");
        int limiteAntes = jogo.getLimite();
        int poderAntesConcentrar = heroi.getPoder();
        expedicao.descansar(Expedicao.AcaoDescanso.CONCENTRAR);
        verificar(jogo.getLimite() == Math.min(Jogo.LIMITE_MAXIMO, limiteAntes + Expedicao.LIMITE_CONCENTRACAO),
                "Concentrar no acampamento deve carregar o LIMIT");
        verificar(expedicao.getReliquiasPendentes().size() == reliquiasGuardadas
                        && heroi.getPoder() == poderAntesConcentrar,
                "Concentrar não deve consumir nem ativar a relíquia");
        Expedicao.Sala loja = encontrarSalaPorAndar(expedicao.getSalas(), 7, Expedicao.TipoSala.LOJA);
        List<Expedicao.Sala> caminhoLoja = caminhoParaSala(expedicao.getSalaAtual(), loja);
        verificar(caminhoLoja != null, "Loja antes do chefe deve ser alcançável");
        for (Expedicao.Sala sala : caminhoLoja) {
            expedicao.selecionar(sala);
            if (expedicao.getEstado() == Expedicao.Estado.COMBATE) resolverSala(expedicao);
            else if (expedicao.getEstado() == Expedicao.Estado.EVENTO) {
                expedicao.resolverEvento(Expedicao.EscolhaEvento.IGNORAR);
                expedicao.continuarResultado();
            } else if (expedicao.getEstado() == Expedicao.Estado.ACAMPAMENTO) descansarAutomatico(expedicao);
        }
        verificar(expedicao.getEstado() == Expedicao.Estado.LOJA, "Rota deve chegar à loja");
        int poderAntesReliquias = heroi.getPoder();
        int vidaAntesReliquias = heroi.getHPMaximo();
        int totalReliquias = expedicao.getReliquiasPendentes().size();
        while (!expedicao.getReliquiasPendentes().isEmpty()) expedicao.ativarReliquia(0);
        verificar(heroi.getPoder() == poderAntesReliquias + 4 * totalReliquias
                        && expedicao.getReliquiasPendentes().isEmpty(),
                "Relíquia deve ser aplicada e consumida na loja");
        verificar(expedicao.getReliquiasAtivas().size() == totalReliquias
                        && expedicao.getReliquiasAtivas().stream().allMatch(r -> r == Expedicao.Reliquia.LAMINA),
                "Relíquias ativadas devem ficar registradas para exibição");
        verificar(vidaAntesReliquias == heroi.getHPMaximo(), "Relíquia de poder não deve alterar vida máxima");
        try {
            expedicao.ativarReliquia(0);
            throw new AssertionError("Relíquia já consumida não pode ser ativada novamente");
        } catch (IllegalArgumentException esperado) {
            verificar(expedicao.getEstado() == Expedicao.Estado.LOJA, "Tentativa duplicada não deve encerrar a loja");
        }
    }

    private static void testarAtivacaoNoDescanso() {
        Personagem heroi = heroiPreparado(new Guerreiro("Teste"));
        Expedicao expedicao = new Expedicao(heroi, 91);
        expedicao.sairDaLoja();
        Expedicao.Sala elite = encontrarSalaPorAndar(expedicao.getSalas(), 4, Expedicao.TipoSala.ELITE);
        for (Expedicao.Sala sala : caminhoParaSala(expedicao.getSalaAtual(), elite)) {
            expedicao.selecionar(sala);
            resolverSala(expedicao);
        }
        Expedicao.Sala acampamento = encontrarSalaPorAndar(expedicao.getSalas(), 5,
                Expedicao.TipoSala.ACAMPAMENTO);
        for (Expedicao.Sala sala : caminhoParaSala(expedicao.getSalaAtual(), acampamento)) {
            expedicao.selecionar(sala);
            if (expedicao.getEstado() == Expedicao.Estado.COMBATE) resolverSala(expedicao);
            else if (expedicao.getEstado() == Expedicao.Estado.EVENTO) {
                expedicao.resolverEvento(Expedicao.EscolhaEvento.IGNORAR);
                expedicao.continuarResultado();
            } else if (expedicao.getEstado() == Expedicao.Estado.LOJA) expedicao.sairDaLoja();
        }
        int poderAntes = heroi.getPoder();
        int reliquiasAntes = expedicao.getReliquiasPendentes().size();
        int limiteAntes = expedicao.getJogo().getLimite();
        expedicao.descansar(Expedicao.AcaoDescanso.RELIQUIA);
        verificar(heroi.getPoder() == poderAntes + 4
                        && expedicao.getReliquiasPendentes().size() == reliquiasAntes - 1
                        && expedicao.getJogo().getLimite() == limiteAntes
                        && expedicao.getEstado() == Expedicao.Estado.MAPA,
                "Ativar relíquia no acampamento deve consumir só a ação escolhida");
    }

    private static Expedicao.Sala encontrarSalaPorAndar(List<Expedicao.Sala> salas, int andar,
                                                        Expedicao.TipoSala tipo) {
        return salas.stream().filter(s -> s.getAndar() == andar && s.getTipo() == tipo)
                .findFirst().orElseThrow(AssertionError::new);
    }

    private static List<Expedicao.Sala> caminhoParaSala(Expedicao.Sala atual, Expedicao.Sala destino) {
        List<List<Expedicao.Sala>> caminhos = new ArrayList<>();
        caminhos.add(new ArrayList<>());
        for (int i = 0; i < caminhos.size(); i++) {
            List<Expedicao.Sala> caminho = caminhos.get(i);
            Expedicao.Sala origem = caminho.isEmpty() ? atual : caminho.get(caminho.size() - 1);
            for (Expedicao.Sala proxima : origem.getSaidas()) {
                List<Expedicao.Sala> novo = new ArrayList<>(caminho);
                novo.add(proxima);
                if (proxima == destino) return novo;
                caminhos.add(novo);
            }
        }
        return null;
    }

    private static void testarRotaReal(int classe) {
        int maiorAndar = 0;
        String ultimo = "";
        for (int semente = 0; semente < 40; semente++) {
            Personagem heroi = classe == 0 ? new Guerreiro("Teste")
                    : classe == 1 ? new Mago("Teste") : new Ladino("Teste");
            Expedicao expedicao = new Expedicao(heroi, semente);
            Jogo jogo = expedicao.getJogo();
            jogo.comprar(classe == 1 ? Jogo.Melhoria.VITALIDADE : Jogo.Melhoria.ATAQUE);
            jogo.comprar(Jogo.Melhoria.POCAO_VIDA);
            expedicao.sairDaLoja();
            for (int passos = 0; passos < 20000; passos++) {
                if (expedicao.getEstado() == Expedicao.Estado.CAPITULO_CONCLUIDO) return;
                if (expedicao.getEstado() == Expedicao.Estado.DERROTA) {
                    if (expedicao.getSalaAtual().getAndar() >= maiorAndar) {
                        maiorAndar = expedicao.getSalaAtual().getAndar();
                        ultimo = "semente=" + semente + " mana=" + heroi.getManaAtual()
                                + " vida=" + heroi.getHpAtual() + "/" + heroi.getHPMaximo()
                                + " xp=" + jogo.getExperiencia()
                                + " ouro=" + jogo.getOuro() + " poder=" + heroi.getPoder()
                                + " nível=" + heroi.getNivel()
                                + " inimigos=" + jogo.getInimigos().stream().mapToInt(i -> i.getVida()).sum();
                    }
                    break;
                }
                switch (expedicao.getEstado()) {
                    case MAPA:
                        while (jogo.getEscolhasNivel() > 0) {
                            jogo.escolherBonusNivel(heroi instanceof Mago ? Jogo.BonusNivel.MANA : Jogo.BonusNivel.PODER);
                        }
                        List<Expedicao.Sala> rotaLoja = jogo.getOuro() >= jogo.preco(Jogo.Melhoria.ATAQUE) + 20
                                || jogo.getPocoesVida() == 0 && jogo.getOuro() >= 40
                                ? caminhoPara(expedicao.getSalaAtual(), Expedicao.TipoSala.LOJA) : null;
                        if (rotaLoja != null) {
                            expedicao.selecionar(rotaLoja.get(0));
                            break;
                        }

                        Expedicao.Sala melhor = null;
                        int melhorValor = Integer.MIN_VALUE;
                        for (Expedicao.Sala sala : expedicao.getSalaAtual().getSaidas()) {
                            int valor = avaliarSala(sala, jogo);
                            if (valor > melhorValor) { melhor = sala; melhorValor = valor; }
                        }
                        expedicao.selecionar(melhor);
                        break;
                    case EVENTO:
                        if (!expedicao.resolverEvento(Expedicao.EscolhaEvento.CUIDADOSO)) {
                            expedicao.resolverEvento(Expedicao.EscolhaEvento.IGNORAR);
                        }
                        break;
                    case RESULTADO: expedicao.continuarResultado(); break;
                    case LOJA:
                        while (jogo.getPocoesVida() < 2 && jogo.getOuro() >= jogo.preco(Jogo.Melhoria.POCAO_VIDA)) {
                            jogo.comprar(Jogo.Melhoria.POCAO_VIDA);
                        }
                        if (heroi instanceof Mago && jogo.getPocoesMana() < 1 && jogo.getOuro() >= 20) {
                            jogo.comprar(Jogo.Melhoria.POCAO_MANA);
                        }
                        while (jogo.getOuro() >= jogo.preco(Jogo.Melhoria.ATAQUE)) {
                            jogo.comprar(Jogo.Melhoria.ATAQUE);
                        }
                        if (jogo.getOuro() >= jogo.preco(Jogo.Melhoria.VITALIDADE)) {
                            jogo.comprar(Jogo.Melhoria.VITALIDADE);
                        }
                        while (!expedicao.getReliquiasPendentes().isEmpty()) expedicao.ativarReliquia(0);
                        expedicao.sairDaLoja();
                        break;
                    case ACAMPAMENTO:
                        descansarAutomatico(expedicao);
                        break;
                    case RECOMPENSA: expedicao.escolherReliquia(Expedicao.Reliquia.LAMINA); break;
                    case COMBATE:
                        if (jogo.getEstado() == Jogo.Estado.COMBATE) jogo.avancarTempo();
                        else if (jogo.getEstado() == Jogo.Estado.ESCOLHA) {
                            if (heroi.getHpAtual() <= heroi.getHPMaximo() - 40 && jogo.getPocoesVida() > 0) {
                                jogo.usarPocao(true);
                            } else if (heroi.getManaAtual() < heroi.getCustoHabilidade()
                                    && jogo.getPocoesMana() > 0) {
                                jogo.usarPocao(false);
                            } else {
                                int alvo = -1;
                                for (int i = 0; i < jogo.getInimigos().size(); i++) {
                                    if (jogo.getInimigos().get(i).estaVivo()
                                            && (alvo < 0 || jogo.getInimigos().get(i).getVida()
                                            < jogo.getInimigos().get(alvo).getVida())) alvo = i;
                                }
                                boolean usarHabilidade = heroi.getManaAtual() >= heroi.getCustoHabilidade()
                                        && jogo.getInimigos().get(alvo).getVida() > heroi.getPoder();
                                jogo.atacar(alvo, usarHabilidade);
                            }
                        }
                        expedicao.sincronizarCombate();
                        break;
                    default: throw new AssertionError("Estado inesperado");
                }
            }
        }
        throw new AssertionError("Nenhuma rota testada foi vencida pela classe " + classe
                + "; maior andar " + maiorAndar + " " + ultimo);
    }

    private static int avaliarSala(Expedicao.Sala sala, Jogo jogo) {
        Personagem heroi = jogo.getHeroi();
        switch (sala.getTipo()) {
            case ACAMPAMENTO:
                return heroi.getHpAtual() * 4 < heroi.getHPMaximo() * 3
                        || heroi.getManaAtual() * 3 < heroi.getManaMaxima() ? 100 : 15;
            case LOJA: return jogo.getOuro() >= 20 && jogo.getPocoesVida() < 2 ? 90 : 10;
            case EVENTO: return heroi instanceof Mago ? 85 : 30;
            case COMBATE: return 80;
            case ELITE: return heroi.getHpAtual() * 2 > heroi.getHPMaximo() ? 95 : 5;
            case CHEFE: return 0;
            default: throw new AssertionError("Sala inválida");
        }
    }

    private static void descansarAutomatico(Expedicao expedicao) {
        Personagem heroi = expedicao.getJogo().getHeroi();
        Expedicao.AcaoDescanso acao;
        if (heroi.getHpAtual() * 3 < heroi.getHPMaximo()) {
            acao = Expedicao.AcaoDescanso.VIDA;
        } else if (!expedicao.getReliquiasPendentes().isEmpty()) {
            acao = Expedicao.AcaoDescanso.RELIQUIA;
        } else if (heroi.getManaAtual() * 2 < heroi.getManaMaxima()) {
            acao = Expedicao.AcaoDescanso.MANA;
        } else if (heroi.getHpAtual() * 3 < heroi.getHPMaximo() * 2) {
            acao = Expedicao.AcaoDescanso.VIDA;
        } else if (heroi.getHpAtual() < heroi.getHPMaximo()) {
            acao = Expedicao.AcaoDescanso.VIDA;
        } else if (heroi.getManaAtual() < heroi.getManaMaxima()) {
            acao = Expedicao.AcaoDescanso.MANA;
        } else {
            acao = Expedicao.AcaoDescanso.CONTINUAR;
        }
        expedicao.descansar(acao);
    }

    private static void testarRiscosERecursos() {
        boolean armadilhaTestada = false;
        boolean bauPremio = false;
        boolean bauDano = false;
        boolean altar = false;
        for (int semente = 0; semente < 200; semente++) {
            Personagem heroi = heroiPreparado(new Guerreiro("Teste"));
            Expedicao expedicao = new Expedicao(heroi, semente);
            expedicao.sairDaLoja();
            entrarNoEvento(expedicao);
            String descricao = expedicao.descricaoEvento();
            if (descricao.contains("corredor") && !armadilhaTestada) {
                heroi.gastarMana(heroi.getManaAtual());
                verificar(!expedicao.resolverEvento(Expedicao.EscolhaEvento.CUIDADOSO)
                                && expedicao.getEstado() == Expedicao.Estado.EVENTO,
                        "Mana insuficiente não deve consumir evento");
                heroi.receberDano(heroi.getHpAtual() - 1);
                expedicao.resolverEvento(Expedicao.EscolhaEvento.ARRISCAR);
                verificar(expedicao.getEstado() == Expedicao.Estado.RESULTADO && !heroi.estaVivo(),
                        "Armadilha fatal deve mostrar resultado");
                expedicao.continuarResultado();
                verificar(expedicao.getEstado() == Expedicao.Estado.DERROTA,
                        "Resultado fatal deve encerrar a expedição");
                armadilhaTestada = true;
            } else if (descricao.contains("baú")) {
                int ouro = expedicao.getJogo().getOuro();
                int vida = heroi.getHpAtual();
                expedicao.resolverEvento(Expedicao.EscolhaEvento.ARRISCAR);
                bauPremio |= expedicao.getJogo().getOuro() == ouro + 80 && heroi.getHpAtual() == vida;
                bauDano |= expedicao.getJogo().getOuro() == ouro && heroi.getHpAtual() == vida - heroi.getHPMaximo() / 4;
            } else if (descricao.contains("altar") && !altar) {
                int poder = heroi.getPoder();
                int vida = heroi.getHpAtual();
                expedicao.resolverEvento(Expedicao.EscolhaEvento.ARRISCAR);
                verificar(heroi.getPoder() == poder && heroi.getHpAtual() == vida - heroi.getHPMaximo() / 4
                                && expedicao.getReliquiasPendentes().contains(Expedicao.Reliquia.LAMINA),
                        "Altar deve cobrar vida e guardar uma relíquia sem aplicar atributos");
                expedicao.continuarResultado();
                altar = true;
            }
        }
        verificar(armadilhaTestada && bauPremio && bauDano && altar, "Eventos devem cobrir seus resultados");
    }

    private static void testarMapas() {
        for (int semente = 0; semente < 50; semente++) {
            Expedicao expedicao = new Expedicao(new Guerreiro("Teste"), semente);
            verificar(expedicao.getSalas().size() == 23, "Mapa deve conter entrada, 21 salas e chefe");
            verificar(expedicao.getEstado() == Expedicao.Estado.LOJA, "Expedição deve iniciar na loja");
            expedicao.sairDaLoja();
            verificar(expedicao.getSalaAtual().getSaidas().size() == 3, "Entrada deve oferecer três caminhos");
            long combates = expedicao.getSalas().stream().filter(s ->
                    s.getTipo() == Expedicao.TipoSala.COMBATE || s.getTipo() == Expedicao.TipoSala.ELITE).count();
            verificar(combates * 2 > expedicao.getSalas().size() - 2,
                    "Maioria das salas intermediárias deve ser de luta");
            verificar(minimoCombates(expedicao.getSalaAtual()) >= 4,
                    "Toda rota deve ter ao menos quatro lutas antes do chefe");
            for (Expedicao.TipoSala tipo : new Expedicao.TipoSala[]{
                    Expedicao.TipoSala.LOJA, Expedicao.TipoSala.ACAMPAMENTO, Expedicao.TipoSala.EVENTO}) {
                verificar(caminhoPara(expedicao.getSalaAtual(), tipo) != null,
                        "A nova distribuição deve preservar " + tipo);
            }
            for (Expedicao.Sala sala : expedicao.getSalas()) {
                if (sala.getTipo() == Expedicao.TipoSala.CHEFE) {
                    verificar(sala.getSaidas().isEmpty() && sala.getAndar() == 8, "Chefe deve ser o final");
                } else {
                    verificar(!sala.getSaidas().isEmpty(), "Não deve existir caminho sem saída");
                    for (Expedicao.Sala proxima : sala.getSaidas()) {
                        verificar(proxima.getAndar() == sala.getAndar() + 1, "Conexões devem avançar um andar");
                    }
                    verificar(alcançaChefe(sala), "Toda sala deve levar ao chefe");
                }
            }
            Expedicao.Sala inacessivel = expedicao.getSalas().get(expedicao.getSalas().size() - 1);
            verificar(!expedicao.estaDisponivel(inacessivel), "Chefe não deve estar disponível no início");
            try {
                expedicao.selecionar(inacessivel);
                throw new AssertionError("Caminho não conectado deve ser recusado");
            } catch (IllegalStateException esperado) {
                verificar(expedicao.getSalaAtual().getAndar() == 0, "Caminho inválido não deve alterar posição");
            }
        }
    }

    private static boolean alcançaChefe(Expedicao.Sala sala) {
        if (sala.getTipo() == Expedicao.TipoSala.CHEFE) return true;
        for (Expedicao.Sala proxima : sala.getSaidas()) {
            if (alcançaChefe(proxima)) return true;
        }
        return false;
    }

    private static int minimoCombates(Expedicao.Sala sala) {
        int aqui = sala.getTipo() == Expedicao.TipoSala.COMBATE
                || sala.getTipo() == Expedicao.TipoSala.ELITE ? 1 : 0;
        if (sala.getSaidas().isEmpty()) return aqui;
        int minimo = Integer.MAX_VALUE;
        for (Expedicao.Sala proxima : sala.getSaidas()) minimo = Math.min(minimo, minimoCombates(proxima));
        return aqui + minimo;
    }

    private static Expedicao.Sala entrarNoEvento(Expedicao expedicao) {
        List<Expedicao.Sala> caminho = caminhoPara(expedicao.getSalaAtual(), Expedicao.TipoSala.EVENTO);
        verificar(caminho != null, "Deve haver evento acessível na dungeon");
        for (Expedicao.Sala sala : caminho) {
            expedicao.selecionar(sala);
            if (sala.getTipo() != Expedicao.TipoSala.EVENTO) resolverSala(expedicao);
        }
        return expedicao.getSalaAtual();
    }

    private static void testarEconomiaETransicoes() {
        Personagem heroi = heroiPreparado(new Guerreiro("Teste"));
        Expedicao expedicao = new Expedicao(heroi, 10);
        Jogo jogo = expedicao.getJogo();
        verificar(jogo.comprar(Jogo.Melhoria.POCAO_VIDA), "Poção deve poder ser comprada na entrada");
        verificar(jogo.getPocoesVida() == 1 && jogo.getOuro() == 40, "Compra deve cobrar corretamente");
        expedicao.sairDaLoja();
        verificar(!jogo.comprar(Jogo.Melhoria.POCAO_VIDA) && jogo.getPocoesVida() == 1,
                "Compras devem ser recusadas fora da loja");
        heroi.receberDano(20);
        int vida = heroi.getHpAtual();
        Expedicao.Sala combate = expedicao.getSalaAtual().getSaidas().stream()
                .filter(s -> s.getTipo() == Expedicao.TipoSala.COMBATE).findFirst().orElseThrow(AssertionError::new);
        expedicao.selecionar(combate);
        verificar(expedicao.getEstado() == Expedicao.Estado.COMBATE, "Sala deve iniciar combate");
        verificar(!jogo.comprar(Jogo.Melhoria.POCAO_VIDA), "Loja deve fechar durante combate");
        vencer(expedicao);
        verificar(expedicao.getEstado() == Expedicao.Estado.MAPA, "Vitória normal deve retornar ao mapa");
        verificar(heroi.getHpAtual() == vida, "Combate na dungeon não deve restaurar vida gratuitamente");
        verificar(jogo.getPocoesVida() == 1, "Combate não deve dar poção");
        verificar(expedicao.getSalaAtual() == combate && combate.foiVisitada(), "Sala vencida deve permanecer visitada");
    }

    private static void testarEventos() {
        for (int semente = 0; semente < 40; semente++) {
            Expedicao expedicao = new Expedicao(heroiPreparado(new Ladino("Teste")), semente);
            expedicao.sairDaLoja();
            Expedicao.Sala evento = entrarNoEvento(expedicao);
            verificar(expedicao.getEstado() == Expedicao.Estado.EVENTO, "Sala de evento deve exigir uma escolha");
            int antesVida = expedicao.getJogo().getHeroi().getHpAtual();
            verificar(expedicao.resolverEvento(Expedicao.EscolhaEvento.IGNORAR), "Ignorar deve funcionar");
            verificar(expedicao.getEstado() == Expedicao.Estado.RESULTADO
                    && expedicao.getJogo().getHeroi().getHpAtual() == antesVida,
                    "Evento deve apresentar o resultado sem dano");
            expedicao.continuarResultado();
            verificar(expedicao.getEstado() == Expedicao.Estado.MAPA, "Resultado deve retornar ao mapa");
            try {
                expedicao.resolverEvento(Expedicao.EscolhaEvento.IGNORAR);
                throw new AssertionError("Evento não pode ser resolvido duas vezes");
            } catch (IllegalStateException esperado) {
                verificar(expedicao.getSalaAtual() == evento, "Resolução duplicada não deve alterar progresso");
            }
        }
    }

    private static void testarJornada(Personagem heroi) {
        heroiPreparado(heroi);
        Expedicao expedicao = new Expedicao(heroi, 17);
        expedicao.sairDaLoja();
        List<Expedicao.TipoSala> objetivos = new ArrayList<>();
        objetivos.add(Expedicao.TipoSala.ELITE);
        objetivos.add(Expedicao.TipoSala.ACAMPAMENTO);
        objetivos.add(Expedicao.TipoSala.LOJA);
        for (Expedicao.TipoSala objetivo : objetivos) {
            List<Expedicao.Sala> caminho = caminhoPara(expedicao.getSalaAtual(), objetivo);
            verificar(caminho != null, "Deve haver uma rota para " + objetivo);
            for (Expedicao.Sala sala : caminho) {
                expedicao.selecionar(sala);
                resolverSala(expedicao);
            }
        }
        while (expedicao.getEstado() != Expedicao.Estado.CAPITULO_CONCLUIDO) {
            verificar(expedicao.getEstado() == Expedicao.Estado.MAPA, "A jornada deve retornar ao mapa");
            expedicao.selecionar(expedicao.getSalaAtual().getSaidas().get(0));
            resolverSala(expedicao);
        }
        verificar(expedicao.getSalaAtual().getTipo() == Expedicao.TipoSala.CHEFE,
                heroi.getClasse() + " deve vencer o chefe para concluir o primeiro capítulo");
    }

    private static Personagem heroiPreparado(Personagem heroi) {
        for (int i = 0; i < 50; i++) {
            heroi.melhorarAtaque();
            heroi.melhorarVitalidade();
        }
        return heroi;
    }

    private static List<Expedicao.Sala> caminhoPara(Expedicao.Sala atual, Expedicao.TipoSala tipo) {
        List<List<Expedicao.Sala>> caminhos = new ArrayList<>();
        caminhos.add(new ArrayList<>());
        for (int i = 0; i < caminhos.size(); i++) {
            List<Expedicao.Sala> caminho = caminhos.get(i);
            Expedicao.Sala origem = caminho.isEmpty() ? atual : caminho.get(caminho.size() - 1);
            for (Expedicao.Sala proxima : origem.getSaidas()) {
                List<Expedicao.Sala> novo = new ArrayList<>(caminho);
                novo.add(proxima);
                if (proxima.getTipo() == tipo) return novo;
                caminhos.add(novo);
            }
        } 
        return null;
    }

    private static void resolverSala(Expedicao expedicao) {
        switch (expedicao.getEstado()) {
            case COMBATE: vencer(expedicao); break;
            case EVENTO:
                expedicao.resolverEvento(Expedicao.EscolhaEvento.CUIDADOSO);
                expedicao.continuarResultado();
                break;
            case RESULTADO: expedicao.continuarResultado(); break;
            case LOJA: expedicao.sairDaLoja(); break;
            case ACAMPAMENTO: descansarAutomatico(expedicao); break;
            default: throw new AssertionError("Sala inesperada: " + expedicao.getEstado());
        }
        if (expedicao.getEstado() == Expedicao.Estado.RECOMPENSA) {
            Personagem heroi = expedicao.getJogo().getHeroi();
            int poder = heroi.getPoder();
            int vida = heroi.getHPMaximo();
            int mana = heroi.getManaMaxima();
            if (heroi instanceof Guerreiro) {
                expedicao.escolherReliquia(Expedicao.Reliquia.LAMINA);
                verificar(heroi.getPoder() == poder, "Relíquia não pode melhorar atributos fora da loja ou descanso");
            } else if (heroi instanceof Mago) {
                expedicao.escolherReliquia(Expedicao.Reliquia.FOCO);
                verificar(heroi.getManaMaxima() == mana, "Relíquia não pode aumentar mana fora da loja/descanso");
            } else {
                expedicao.escolherReliquia(Expedicao.Reliquia.AMULETO);
                verificar(heroi.getHPMaximo() == vida, "Relíquia não pode aumentar vida fora da loja/descanso");
            }
            verificar(!expedicao.getReliquiasPendentes().isEmpty(), "Elite deve guardar a escolha como pendente");
            try {
                expedicao.escolherReliquia(Expedicao.Reliquia.LAMINA);
                throw new AssertionError("Recompensa não pode ser escolhida duas vezes");
            } catch (IllegalStateException esperado) {
                verificar(expedicao.getEstado() == Expedicao.Estado.MAPA, "Escolha duplicada não deve alterar estado");
            }
        }
    }

    private static void vencer(Expedicao expedicao) {
        Jogo jogo = expedicao.getJogo();
        for (int i = 0; i < 10000 && expedicao.getEstado() == Expedicao.Estado.COMBATE; i++) {
            if (jogo.getEstado() == Jogo.Estado.COMBATE) jogo.avancarTempo();
            else if (jogo.getEstado() == Jogo.Estado.ESCOLHA) {
                int alvo = 0;
                while (!jogo.getInimigos().get(alvo).estaVivo()) alvo++;
                jogo.atacar(alvo, false);
            }
            expedicao.sincronizarCombate();
        }
        verificar(expedicao.getEstado() != Expedicao.Estado.COMBATE, "Combate deve terminar");
        verificar(expedicao.getEstado() != Expedicao.Estado.DERROTA, "Herói de teste não deve morrer");
    }

    private static void verificar(boolean condicao, String mensagem) {
        if (!condicao) throw new AssertionError(mensagem);
    }
}
