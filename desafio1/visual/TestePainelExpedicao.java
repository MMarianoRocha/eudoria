package desafio1.visual;

import desafio1.jogo.Expedicao;
import desafio1.jogo.Jogo;
import desafio1.jogo.Inimigo;
import desafio1.jogo.RegistroRun;
import desafio1.jogo.Salvamento;
import desafio1.personagens.Guerreiro;
import desafio1.personagens.Mago;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class TestePainelExpedicao {
    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> testar(args));
        System.out.println("Testes da interface da expedição passaram.");
    }

    private static void testar(String[] args) {
        testarSprites();
        testarTelaInicial(args);
        testarPainelRuns(args);
        testarPainelNivel(args);
        testarTransicoesCampanha();
        Guerreiro heroi = new Guerreiro("Teste");
        for (int i = 0; i < 50; i++) {
            heroi.melhorarAtaque();
            heroi.melhorarVitalidade();
        }
        Expedicao expedicao = new Expedicao(heroi, 7);
        PainelExpedicao[] painel = new PainelExpedicao[1];
        boolean[] abriuCombate = {false};
        painel[0] = new PainelExpedicao(expedicao, () -> {
            if (expedicao.getEstado() == Expedicao.Estado.COMBATE) abriuCombate[0] = true;
            else painel[0].atualizar();
        }, () -> { });
        painel[0].atualizar();
        verificar(contarIndicadores(painel[0]) == 3, "HUD deve mostrar sprites e valores de vida, mana e XP");
        encontrar(painel[0], "Sair e abrir o mapa").doClick();
        verificar(expedicao.getEstado() == Expedicao.Estado.MAPA, "Loja deve abrir mapa");
        List<JButton> botoes = botoes(painel[0]);
        int disponiveis = 0;
        for (JButton botao : botoes) {
            if (botao.getIcon() instanceof IconeSala) {
                verificar(botao.getText().isEmpty(), "Nós do mapa devem ser ícones, não retângulos com texto");
                verificar(botao.getAccessibleContext().getAccessibleName() != null
                                && !botao.getAccessibleContext().getAccessibleName().isBlank(),
                        "Cada ícone deve manter identificação acessível");
                if (botao.isEnabled()) disponiveis++;
            }
        }
        verificar(disponiveis == 3, "Mapa deve permitir somente as três salas conectadas");
        verificarSalasOcultas(painel[0], expedicao);
        for (int[] tamanho : new int[][]{{1050, 800}, {1280, 720}}) {
            painel[0].setSize(tamanho[0], tamanho[1]);
            organizar(painel[0]);
            for (JButton botao : botoes(painel[0])) {
                if (botao.getIcon() instanceof IconeSala) {
                    verificar(botao.getX() >= 0 && botao.getY() >= 0
                                    && botao.getX() + botao.getWidth() <= botao.getParent().getWidth()
                                    && botao.getY() + botao.getHeight() <= botao.getParent().getHeight(),
                            "Portas devem caber no mapa sem rolagem");
                }
            }
            verificarSalasOcultas(painel[0], expedicao);
        }

        painel[0].setSize(1280, 720);
        organizar(painel[0]);
        BufferedImage imagem = new BufferedImage(1280, 720, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = imagem.createGraphics();
        try {
            painel[0].paint(g);
        } finally {
            g.dispose();
        }
        if (args.length >= 1) {
            try {
                ImageIO.write(imagem, "png", new File(args[0]));
            } catch (IOException erro) {
                throw new AssertionError("Falha ao salvar prévia do mapa", erro);
            }
        }
        Expedicao.Sala salaEvento = prepararEvento(expedicao);
        painel[0].atualizar();
        JButton evento = encontrarSala(painel[0], expedicao, salaEvento);
        verificar(evento.getToolTipText().startsWith("Sala inexplorada"),
                "Evento acessível não deve ser revelado antes do clique");
        verificar(evento.getIcon() instanceof IconeSala, "Sala de evento deve usar sprite");
        evento.doClick();
        verificar(expedicao.getEstado() == Expedicao.Estado.EVENTO, "Clique deve abrir evento");
        verificar(contem(painel[0], IlustracaoEvento.class), "Evento deve exibir uma ilustração da situação");
        encontrar(painel[0], "Ignorar e seguir em segurança").doClick();
        verificar(expedicao.getEstado() == Expedicao.Estado.RESULTADO
                        && contem(painel[0], IlustracaoEvento.class),
                "Escolha deve mostrar a cena do resultado");
        encontrar(painel[0], "Continuar para o mapa").doClick();
        verificar(expedicao.getEstado() == Expedicao.Estado.MAPA
                        && expedicao.getSalaAtual().getAndar() == salaEvento.getAndar(),
                "Resultado deve voltar ao mapa");
        verificarSalasOcultas(painel[0], expedicao);
        verificar(encontrarSala(painel[0], expedicao, salaEvento).getToolTipText().startsWith("Evento"),
                "Sala visitada deve revelar sua identidade ao voltar ao mapa");
        Expedicao.Sala combate = salaEvento.getSaidas().stream()
                .filter(s -> s.getTipo() == Expedicao.TipoSala.COMBATE).findFirst().orElseThrow(AssertionError::new);
        JButton proximoCombate = encontrarSala(painel[0], expedicao, combate);
        verificar(proximoCombate != null, "Deve haver um combate acessível após o primeiro evento");
        proximoCombate.doClick();
        verificar(abriuCombate[0] && expedicao.getJogo().getEstado() == Jogo.Estado.COMBATE,
                "Selecionar batalha deve iniciar combate e solicitar troca de tela");
    }

    private static void testarTransicoesCampanha() {
        Guerreiro heroi = new Guerreiro("Campanha");
        for (int i = 0; i < 50; i++) {
            heroi.melhorarAtaque();
            heroi.melhorarVitalidade();
        }
        Expedicao expedicao = new Expedicao(heroi, 11);
        PainelExpedicao[] painel = new PainelExpedicao[1];
        painel[0] = new PainelExpedicao(expedicao, () -> painel[0].atualizar(), () -> { });
        for (int capitulo = 1; capitulo <= 3; capitulo++) {
            concluirCapitulo(expedicao);
            painel[0].atualizar();
            painel[0].setSize(1050, 800);
            organizar(painel[0]);
            for (JButton botao : botoes(painel[0])) {
                verificar(botao.getWidth() > 0 && botao.getHeight() >= botao.getPreferredSize().height,
                        "Ações da transição devem caber na janela mínima");
            }
            if (capitulo < 3) {
                encontrar(painel[0], "Próximo capítulo").doClick();
                verificar(expedicao.getCapitulo() == capitulo + 1
                                && expedicao.getEstado() == Expedicao.Estado.LOJA,
                        "Botão de continuação deve abrir o próximo capítulo");
                encontrar(painel[0], "Sair e abrir o mapa").doClick();
                verificarSalasOcultas(painel[0], expedicao);
            }
        }
        verificar(RegistroRun.deExpedicao(expedicao).podeContinuar(),
                "Tela de campanha concluída deve permanecer carregável enquanto há escolha");
        encontrar(painel[0], "Iniciar Ascensão 1").doClick();
        verificar(expedicao.getCiclo() == 1 && expedicao.getCapitulo() == 1,
                "Botão de Ascensão deve reiniciar capítulos com dificuldade crescente");
        concluirCapitulo(expedicao);
        painel[0].atualizar();
        encontrar(painel[0], "Encerrar com vitória").doClick();
        verificar(expedicao.getEstado() == Expedicao.Estado.VITORIA,
                "Pós-jogo deve permitir encerrar com vitória após outro chefe");
        encontrar(painel[0], "Menu inicial");

        BufferedImage anterior = null;
        for (Expedicao.Evento evento : new Expedicao.Evento[]{Expedicao.Evento.FONTE, Expedicao.Evento.FORJA}) {
            IlustracaoEvento ilustracao = new IlustracaoEvento(evento, false);
            ilustracao.setSize(560, 250);
            BufferedImage imagem = new BufferedImage(560, 250, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = imagem.createGraphics();
            try { ilustracao.paint(g); } finally { g.dispose(); }
            if (anterior != null) verificar(diferentes(anterior, imagem), "Novos eventos devem ter arte própria");
            anterior = imagem;
        }
    }

    private static void concluirCapitulo(Expedicao expedicao) {
        Jogo jogo = expedicao.getJogo();
        for (int passo = 0; passo < 50_000; passo++) {
            if (jogo.getEstado() == Jogo.Estado.INTERVALO) {
                while (jogo.getEscolhasNivel() > 0) jogo.escolherBonusNivel(Jogo.BonusNivel.PODER);
            }
            switch (expedicao.getEstado()) {
                case CAPITULO_CONCLUIDO:
                case CAMPANHA_CONCLUIDA: return;
                case LOJA: expedicao.sairDaLoja(); break;
                case MAPA: expedicao.selecionar(expedicao.getSalaAtual().getSaidas().get(0)); break;
                case EVENTO: expedicao.resolverEvento(Expedicao.EscolhaEvento.IGNORAR); break;
                case RESULTADO: expedicao.continuarResultado(); break;
                case ACAMPAMENTO: expedicao.descansar(Expedicao.AcaoDescanso.CONTINUAR); break;
                case RECOMPENSA: expedicao.escolherReliquia(Expedicao.Reliquia.AMULETO); break;
                case COMBATE: vencerCombate(expedicao); break;
                default: throw new AssertionError("Estado inesperado na campanha: " + expedicao.getEstado());
            }
        }
        throw new AssertionError("Capítulo não terminou");
    }

    private static void testarTelaInicial(String[] args) {
        int[] acoes = {0, 0, 0, 0};
        PainelInicial inicio = new PainelInicial(() -> acoes[0]++, () -> acoes[1]++, () -> acoes[2]++,
                () -> acoes[3]++);
        verificar(acoes[0] + acoes[1] + acoes[2] + acoes[3] == 0, "Menu deve aguardar uma escolha do jogador");
        BufferedImage anterior = null;
        for (int[] tamanho : new int[][]{{1050, 800}, {1280, 900}}) {
            inicio.setSize(tamanho[0], tamanho[1]);
            organizar(inicio);
            BufferedImage imagem = new BufferedImage(tamanho[0], tamanho[1], BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = imagem.createGraphics();
            try { inicio.paint(g); }
            finally { g.dispose(); }
            verificar(imagem.getRGB(tamanho[0] / 2, tamanho[1] / 2) != imagem.getRGB(0, 0),
                    "Tela inicial deve desenhar o castelo na área central");
            for (JButton botao : botoes(inicio)) {
                verificar(botao.isVisible(), "Ações do menu devem estar visíveis");
                verificar(botao.getHeight() > 0 && botao.getWidth() > 0, "Ações devem caber na tela inicial");
                verificar(botao.getY() + botao.getHeight() <= botao.getParent().getHeight(),
                        "Ações não devem ultrapassar a área do menu");
            }
            anterior = imagem;
        }
        if (args.length >= 2) {
            try {
                ImageIO.write(anterior, "png", new File(args[1]));
            } catch (IOException erro) {
                throw new AssertionError("Falha ao salvar prévia da tela inicial", erro);
            }
        }
        encontrar(inicio, "Novo jogo").doClick();
        verificar(acoes[0] == 1, "Novo jogo deve abrir a criação de herói");
        inicio.definirCarregarDisponivel(false);
        verificar(botoes(inicio).stream().anyMatch(b -> b.getText().equals("Carregar") && !b.isEnabled()),
                "Carregar fica desabilitado sem runs em andamento");
        inicio.definirCarregarDisponivel(true);
        encontrar(inicio, "Carregar").doClick();
        encontrar(inicio, "Estatísticas").doClick();
        encontrar(inicio, "Sair").doClick();
        verificar(acoes[0] == 1 && acoes[1] == 1 && acoes[2] == 1 && acoes[3] == 1,
                "Cada opção do menu deve acionar somente sua ação");
    }

    private static void testarPainelRuns(String[] args) {
        Path arquivo;
        try {
            arquivo = Files.createTempFile("eudoria-runs", ".dat");
            Files.delete(arquivo);
        } catch (IOException erro) {
            throw new AssertionError(erro);
        }
        try {
            Salvamento salvamento = new Salvamento(arquivo);
            Expedicao expedicao = new Expedicao(new Guerreiro("Aria"), 3);
            RegistroRun emAndamento = RegistroRun.deExpedicao(expedicao);
            salvamento.registrar(emAndamento);
            Jogo derrotado = new Jogo(new Mago("Zed"));
            derrotado.proximaOnda();
            derrotado.getHeroi().receberDano(derrotado.getHeroi().getHpAtual() - 1);
            for (int passo = 0; passo < 10_000 && derrotado.getEstado() != Jogo.Estado.DERROTA; passo++) {
                if (derrotado.getEstado() == Jogo.Estado.ESCOLHA) derrotado.atacar(0, false);
                derrotado.avancarTempo();
            }
            verificar(derrotado.getEstado() == Jogo.Estado.DERROTA, "Herói com 1 de vida deve cair na onda 1");
            salvamento.registrar(RegistroRun.deOndas(derrotado));

            RegistroRun[] continuada = {null};
            int[] voltou = {0};
            PainelRuns painel = new PainelRuns(run -> continuada[0] = run, () -> voltou[0]++);
            painel.mostrar(salvamento.listar(), false);
            verificar(painel.getQuantidadeRuns() == 2, "Estatísticas devem listar todas as runs");
            verificar(painel.getTextoTotais().contains("2 runs") && painel.getTextoTotais().contains("1 mortes"),
                    "Totais devem somar runs e mortes");
            verificar(painel.getTextoDetalhes().contains("Zed") && painel.getTextoDetalhes().contains("Derrota"),
                    "Run mais recente deve ser exibida em detalhe");
            verificar(!painel.podeContinuarSelecionada(), "Run encerrada não pode ser continuada");
            painel.mostrar(salvamento.listar(), true);
            verificar(painel.getQuantidadeRuns() == 1, "Carregar deve listar somente runs em andamento");
            verificar(painel.getTextoDetalhes().contains("Aria") && painel.getTextoDetalhes().contains("Abates"),
                    "Detalhes devem mostrar herói e estatísticas");
            painel.setSize(1050, 800);
            organizar(painel);
            if (args.length >= 3) {
                BufferedImage imagem = new BufferedImage(1050, 800, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = imagem.createGraphics();
                try { painel.paint(g); }
                finally { g.dispose(); }
                ImageIO.write(imagem, "png", new File(args[2]));
            }
            painel.continuarSelecionada();
            verificar(continuada[0] != null && continuada[0].getExpedicao() != null
                            && continuada[0].getExpedicao().getJogo() == continuada[0].getJogo(),
                    "Continuar deve entregar a expedição salva com o mesmo estado de jogo");
            encontrar(painel, "Voltar").doClick();
            verificar(voltou[0] == 1, "Voltar deve retornar ao menu");
        } catch (IOException erro) {
            throw new AssertionError(erro);
        } finally {
            try { Files.deleteIfExists(arquivo); } catch (IOException ignorado) { }
        }
    }

    private static void testarPainelNivel(String[] args) {
        Jogo.BonusNivel[] escolhido = new Jogo.BonusNivel[1];
        PainelNivel painel = new PainelNivel(bonus -> escolhido[0] = bonus);
        painel.mostrar(new Jogo(new Mago("Lumen")));
        verificar(painel.getTitulo().contains("Nível 1"), "Tela de nível deve mostrar o nível do herói");
        verificar(painel.getBotao(Jogo.BonusNivel.PODER).getText().contains("→"),
                "Cartas devem mostrar a prévia do bônus");
        painel.getBotao(Jogo.BonusNivel.MANA).doClick();
        verificar(escolhido[0] == Jogo.BonusNivel.MANA, "Clique deve escolher o bônus da carta");
        if (args.length >= 4) {
            painel.setSize(1050, 640);
            organizar(painel);
            BufferedImage imagem = new BufferedImage(1050, 640, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = imagem.createGraphics();
            try { painel.paint(g); }
            finally { g.dispose(); }
            try { ImageIO.write(imagem, "png", new File(args[3])); }
            catch (IOException erro) { throw new AssertionError(erro); }
        }
    }

    private static void verificarSalasOcultas(Container container, Expedicao expedicao) {
        for (Expedicao.Sala sala : expedicao.getSalas()) {
            JButton botao = encontrarSala(container, expedicao, sala);
            if (!sala.foiVisitada()) {
                String esperado = "Sala inexplorada, andar " + sala.getAndar()
                        + ". O conteúdo será revelado ao entrar.";
                verificar(esperado.equals(botao.getAccessibleContext().getAccessibleName()),
                        "Acessibilidade não deve revelar tipos de salas inexploradas");
                verificar(botao.getToolTipText().equals("Sala inexplorada • andar " + sala.getAndar()
                                + " — O conteúdo será revelado ao entrar."),
                        "Tooltip não deve revelar encontros, lojas, eventos, descanso ou chefe");
            }
        }
    }

    private static void testarSprites() {
        BufferedImage porta = desenhar(new IconeSala(Expedicao.TipoSala.ENTRADA, true, false, false, 82, false));
        for (Expedicao.TipoSala tipo : Expedicao.TipoSala.values()) {
            verificar(!diferentes(porta, desenhar(new IconeSala(tipo, true, false, false, 82, false))),
                    "Portas inexploradas devem ser idênticas independentemente do conteúdo");
            verificar(diferentes(porta, desenhar(new IconeSala(tipo, true, false, false, 82, true))),
                    "Sala revelada deve ter aparência distinta da porta fechada");
        }
        List<BufferedImage> imagens = new ArrayList<>();
        for (IconeRecurso.Tipo tipo : IconeRecurso.Tipo.values()) {
            IconeRecurso icone = new IconeRecurso(tipo);
            BufferedImage imagem = new BufferedImage(24, 24, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = imagem.createGraphics();
            try { icone.paintIcon(null, g, 0, 0); }
            finally { g.dispose(); }
            boolean desenhou = false;
            for (int y = 0; y < 24; y++) {
                for (int x = 0; x < 24; x++) desenhou |= imagem.getRGB(x, y) >>> 24 != 0;
            }
            verificar(desenhou, "Ícone de recurso deve renderizar pixels visíveis: " + tipo);
            for (BufferedImage anterior : imagens) verificar(diferentes(imagem, anterior),
                    "Cada recurso deve ter sprite próprio");
            imagens.add(imagem);
        }
        Sprites sprites = new Sprites();
        for (Inimigo.Tipo tipo : Inimigo.Tipo.values()) {
            verificar(sprites.inimigo(tipo) != null, "Todo inimigo deve ter sprite: " + tipo);
        }
        verificar(diferentes(sprites.inimigo(Inimigo.Tipo.GOLEM), sprites.inimigo(Inimigo.Tipo.SENTINELA))
                        && diferentes(sprites.inimigo(Inimigo.Tipo.DUELISTA), sprites.inimigo(Inimigo.Tipo.GOLEM)),
                "Novos inimigos devem ter aparência própria");
        List<BufferedImage> reliquias = new ArrayList<>();
        for (Expedicao.Reliquia reliquia : Expedicao.Reliquia.values()) {
            BufferedImage ativa = desenhar(new IconeReliquia(reliquia, true, 28));
            verificar(diferentes(ativa, desenhar(new IconeReliquia(reliquia, false, 28))),
                    "Relíquia pendente deve aparecer diferente da ativa: " + reliquia);
            for (BufferedImage anterior : reliquias) verificar(diferentes(ativa, anterior),
                    "Cada relíquia deve ter sprite próprio");
            reliquias.add(ativa);
        }
        PainelReliquias vitrine = new PainelReliquias(28, java.awt.Color.WHITE);
        vitrine.atualizar(List.of(Expedicao.Reliquia.LAMINA, Expedicao.Reliquia.LAMINA),
                List.of(Expedicao.Reliquia.FOCO));
        List<IconeReliquia> exibidas = new ArrayList<>();
        String quantidadeLamina = null;
        for (Component componente : vitrine.getComponents()) {
            if (componente instanceof JLabel && ((JLabel) componente).getIcon() instanceof IconeReliquia) {
                IconeReliquia icone = (IconeReliquia) ((JLabel) componente).getIcon();
                exibidas.add(icone);
                if (icone.getReliquia() == Expedicao.Reliquia.LAMINA) quantidadeLamina = ((JLabel) componente).getText();
            }
        }
        verificar(exibidas.size() == 2 && exibidas.get(0).isAtiva() && !exibidas.get(1).isAtiva()
                        && "x2".equals(quantidadeLamina),
                "Vitrine deve agrupar relíquias ativas e mostrar pendentes esmaecidas");
    }

    private static BufferedImage desenhar(javax.swing.Icon icone) {
        BufferedImage imagem = new BufferedImage(icone.getIconWidth(), icone.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = imagem.createGraphics();
        try { icone.paintIcon(null, g, 0, 0); }
        finally { g.dispose(); }
        return imagem;
    }

    private static boolean diferentes(BufferedImage a, BufferedImage b) {
        for (int y = 0; y < a.getHeight(); y++) {
            for (int x = 0; x < a.getWidth(); x++) if (a.getRGB(x, y) != b.getRGB(x, y)) return true;
        }
        return false;
    }

    private static int contarIndicadores(Container container) {
        int total = 0;
        for (Component componente : container.getComponents()) {
            if (componente instanceof JLabel && ((JLabel) componente).getIcon() instanceof IconeRecurso) {
                verificar(!((JLabel) componente).getText().isBlank(), "Sprites devem preservar valores legíveis");
                total++;
            }
            if (componente instanceof Container) total += contarIndicadores((Container) componente);
        }
        return total;
    }

    private static Expedicao.Sala prepararEvento(Expedicao expedicao) {
        List<List<Expedicao.Sala>> caminhos = new ArrayList<>();
        caminhos.add(new ArrayList<>());
        for (int i = 0; i < caminhos.size(); i++) {
            List<Expedicao.Sala> caminho = caminhos.get(i);
            Expedicao.Sala origem = caminho.isEmpty() ? expedicao.getSalaAtual() : caminho.get(caminho.size() - 1);
            for (Expedicao.Sala sala : origem.getSaidas()) {
                if (sala.getTipo() == Expedicao.TipoSala.EVENTO) {
                    for (Expedicao.Sala etapa : caminho) {
                        expedicao.selecionar(etapa);
                        vencerCombate(expedicao);
                        if (expedicao.getEstado() == Expedicao.Estado.RECOMPENSA) {
                            expedicao.escolherReliquia(Expedicao.Reliquia.LAMINA);
                        }
                        verificar(expedicao.getEstado() == Expedicao.Estado.MAPA,
                                "Combates anteriores ao evento devem ser concluídos");
                    }
                    return sala;
                }
                List<Expedicao.Sala> proximo = new ArrayList<>(caminho);
                proximo.add(sala);
                caminhos.add(proximo);
            }
        }
        throw new AssertionError("Evento não encontrado no mapa");
    }

    private static void vencerCombate(Expedicao expedicao) {
        Jogo jogo = expedicao.getJogo();
        for (int passo = 0; passo < 5000 && expedicao.getEstado() == Expedicao.Estado.COMBATE; passo++) {
            if (jogo.getEstado() == Jogo.Estado.COMBATE) jogo.avancarTempo();
            else if (jogo.getEstado() == Jogo.Estado.ESCOLHA) {
                int alvo = 0;
                while (!jogo.getInimigos().get(alvo).estaVivo()) alvo++;
                jogo.atacar(alvo, false);
            }
            expedicao.sincronizarCombate();
        }
        verificar(expedicao.getEstado() != Expedicao.Estado.COMBATE, "Combate deve terminar");
    }

    private static JButton encontrarSala(Container container, Expedicao expedicao, Expedicao.Sala sala) {
        int indice = 0;
        for (JButton botao : botoes(container)) {
            if (botao.getIcon() instanceof IconeSala) {
                if (indice == expedicao.getSalas().indexOf(sala)) return botao;
                indice++;
            }
        }
        throw new AssertionError("Sala não encontrada no mapa");
    }

    private static boolean contem(Container container, Class<?> tipo) {
        if (tipo.isInstance(container)) return true;
        for (Component componente : container.getComponents()) {
            if (componente instanceof Container && contem((Container) componente, tipo)) return true;
        }
        return false;
    }

    private static JButton encontrar(Container container, String texto) {
        for (JButton botao : botoes(container)) {
            if (botao.isEnabled() && botao.getText().equals(texto)) return botao;
        }
        throw new AssertionError("Botão não encontrado: " + texto);
    }

    private static List<JButton> botoes(Container container) {
        List<JButton> resultado = new ArrayList<>();
        for (Component componente : container.getComponents()) {
            if (componente instanceof JButton) resultado.add((JButton) componente);
            if (componente instanceof Container) resultado.addAll(botoes((Container) componente));
        }
        return resultado;
    }

    private static void organizar(Container container) {
        container.doLayout();
        for (Component componente : container.getComponents()) {
            if (componente instanceof Container) organizar((Container) componente);
        }
    }

    private static void verificar(boolean condicao, String mensagem) {
        if (!condicao) throw new AssertionError(mensagem);
    }
}
