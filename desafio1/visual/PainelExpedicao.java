package desafio1.visual;

import desafio1.jogo.Expedicao;
import desafio1.jogo.Jogo;
import desafio1.personagens.Personagem;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JComponent;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class PainelExpedicao extends JPanel {
    private static final Color FUNDO = new Color(15, 23, 42);
    private static final Color TEXTO = new Color(226, 232, 240);
    private final Expedicao expedicao;
    private final Runnable atualizarJanela;
    private final Runnable reiniciar;
    private final JLabel titulo = rotulo("", 23);
    private final JLabel recursos = rotulo("", 14);
    private final JLabel resumoHeroi = rotulo("", 14);
    private final JLabel vida = recurso(IconeRecurso.Tipo.VIDA);
    private final JLabel mana = recurso(IconeRecurso.Tipo.MANA);
    private final JLabel experiencia = recurso(IconeRecurso.Tipo.XP);
    private final JLabel mensagem = rotulo("", 14);
    private final PainelReliquias reliquias = new PainelReliquias(28, TEXTO);
    private final JPanel conteudo = new JPanel(new BorderLayout(12, 12));
    private String avisoLoja;

    PainelExpedicao(Expedicao expedicao, Runnable atualizarJanela, Runnable reiniciar) {
        this.expedicao = expedicao;
        this.atualizarJanela = atualizarJanela;
        this.reiniciar = reiniciar;
        setLayout(new BorderLayout(12, 12));
        setBorder(new EmptyBorder(20, 24, 20, 24));
        setBackground(FUNDO);
        JPanel topo = new JPanel(new GridLayout(0, 1, 0, 8));
        topo.setBackground(FUNDO);
        topo.add(titulo);
        topo.add(resumoHeroi);
        JPanel indicadores = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        indicadores.setOpaque(false);
        indicadores.add(vida);
        indicadores.add(mana);
        indicadores.add(experiencia);
        indicadores.add(recursos);
        topo.add(indicadores);
        topo.add(reliquias);
        JPanel cabecalho = new JPanel(new BorderLayout(12, 0));
        cabecalho.setBackground(FUNDO);
        cabecalho.add(topo, BorderLayout.CENTER);
        JButton novoJogo = new JButton("Menu inicial");
        novoJogo.addActionListener(e -> reiniciar.run());
        cabecalho.add(novoJogo, BorderLayout.EAST);
        add(cabecalho, BorderLayout.NORTH);
        conteudo.setBackground(FUNDO);
        add(conteudo, BorderLayout.CENTER);
        mensagem.putClientProperty("html.disable", Boolean.TRUE);
        add(mensagem, BorderLayout.SOUTH);
    }

    void atualizar() {
        Personagem heroi = expedicao.getJogo().getHeroi();
        titulo.setText("EUDORIA  /  " + expedicao.getNomeCapitulo() + "  •  " + nomeEstado());
        resumoHeroi.setText(heroi.getNome() + " (" + heroi.getClasse() + ", nível " + heroi.getNivel() + ")"
                + "  •  " + expedicao.getDescricaoProgresso());
        vida.setText("Vida " + heroi.getHpAtual() + "/" + heroi.getHPMaximo());
        mana.setText("Mana " + heroi.getManaAtual() + "/" + heroi.getManaMaxima());
        experiencia.setText("XP " + expedicao.getJogo().getExperiencia() + "/"
                + expedicao.getJogo().getExperienciaNecessaria()
                + " • LIMIT " + expedicao.getJogo().getLimite() + "%");
        recursos.setText("Ouro " + expedicao.getJogo().getOuro()
                + "     Poções " + expedicao.getJogo().getPocoesVida() + " vida / "
                + expedicao.getJogo().getPocoesMana() + " mana");
        reliquias.atualizar(expedicao.getReliquiasAtivas(), expedicao.getReliquiasPendentes());
        mensagem.setText(avisoLoja != null && expedicao.getEstado() == Expedicao.Estado.LOJA
                ? avisoLoja : expedicao.getMensagem());
        conteudo.removeAll();
        switch (expedicao.getEstado()) {
            case MAPA:
                JLabel instrucao = rotulo("Escolha uma porta iluminada. O conteúdo só é revelado ao entrar.", 15);
                JPanel cabecalhoMapa = new JPanel(new GridLayout(0, 1, 0, 4));
                cabecalhoMapa.setBackground(FUNDO);
                cabecalhoMapa.add(instrucao);
                JPanel legenda = criarLegenda();
                cabecalhoMapa.add(legenda);
                conteudo.add(cabecalhoMapa, BorderLayout.NORTH);
                conteudo.add(new Mapa(expedicao), BorderLayout.CENTER);
                break;
            case EVENTO:
                mostrarOpcoes(expedicao.descricaoEvento(), new IlustracaoEvento(expedicao.getEventoAtual(), false),
                        botao(expedicao.textoEscolha(Expedicao.EscolhaEvento.CUIDADOSO),
                                () -> expedicao.resolverEvento(Expedicao.EscolhaEvento.CUIDADOSO)),
                        botao(expedicao.textoEscolha(Expedicao.EscolhaEvento.ARRISCAR),
                                () -> expedicao.resolverEvento(Expedicao.EscolhaEvento.ARRISCAR)),
                        botao(expedicao.textoEscolha(Expedicao.EscolhaEvento.IGNORAR),
                                () -> expedicao.resolverEvento(Expedicao.EscolhaEvento.IGNORAR)));
                break;
            case RESULTADO:
                mostrarOpcoes(expedicao.getMensagem(),
                        new IlustracaoEvento(expedicao.getEventoAtual(), true),
                        botao("Continuar para o mapa", expedicao::continuarResultado));
                break;
            case ACAMPAMENTO:
                List<JButton> acoesDescanso = new java.util.ArrayList<>();
                Personagem descansando = expedicao.getJogo().getHeroi();
                if (descansando.getHpAtual() < descansando.getHPMaximo()) {
                    acoesDescanso.add(botao("Descansar: recuperar 40% da vida máxima",
                            () -> expedicao.descansar(Expedicao.AcaoDescanso.VIDA)));
                }
                if (descansando.getManaAtual() < descansando.getManaMaxima()) {
                    acoesDescanso.add(botao("Meditar: recuperar 40% da mana máxima",
                            () -> expedicao.descansar(Expedicao.AcaoDescanso.MANA)));
                }
                if (!expedicao.getJogo().limitePronto()) {
                    acoesDescanso.add(botao("Concentrar: +" + Expedicao.LIMITE_CONCENTRACAO + "% de LIMIT (atual "
                                    + expedicao.getJogo().getLimite() + "%)",
                            () -> expedicao.descansar(Expedicao.AcaoDescanso.CONCENTRAR)));
                }
                if (!expedicao.getReliquiasPendentes().isEmpty()) {
                    Expedicao.Reliquia proxima = expedicao.getReliquiasPendentes().get(0);
                    acoesDescanso.add(botaoReliquia(proxima, "Ativar relíquia: " + PainelReliquias.nome(proxima)
                                    + " (" + PainelReliquias.efeito(proxima) + ")",
                            () -> expedicao.descansar(Expedicao.AcaoDescanso.RELIQUIA)));
                }
                if (acoesDescanso.isEmpty()) {
                    acoesDescanso.add(botao("Deixar o acampamento", () -> expedicao.descansar(Expedicao.AcaoDescanso.CONTINUAR)));
                }
                mostrarOpcoes("Só é possível escolher uma ação neste acampamento:",
                        new JLabel(new IconeSala(Expedicao.TipoSala.ACAMPAMENTO, true, true, false)),
                        acoesDescanso.toArray(new JButton[0]));
                break;
            case LOJA:
                Jogo jogo = expedicao.getJogo();
                List<JButton> acoesLoja = new java.util.ArrayList<>();
                acoesLoja.add(botao("Poder +4 • " + jogo.preco(Jogo.Melhoria.ATAQUE) + " ouro", () -> comprar(Jogo.Melhoria.ATAQUE)));
                acoesLoja.add(botao("Vida máxima +20 • " + jogo.preco(Jogo.Melhoria.VITALIDADE) + " ouro", () -> comprar(Jogo.Melhoria.VITALIDADE)));
                acoesLoja.add(botao("Poção de vida • 20 ouro", () -> comprar(Jogo.Melhoria.POCAO_VIDA)));
                acoesLoja.add(botao("Poção de mana • 20 ouro", () -> comprar(Jogo.Melhoria.POCAO_MANA)));
                for (int i = 0; i < expedicao.getReliquiasPendentes().size(); i++) {
                    final int indice = i;
                    Expedicao.Reliquia pendente = expedicao.getReliquiasPendentes().get(i);
                    acoesLoja.add(botaoReliquia(pendente, "Ativar " + PainelReliquias.nome(pendente)
                                    + " (" + PainelReliquias.efeito(pendente) + ")",
                            () -> {
                                expedicao.ativarReliquia(indice);
                                avisoLoja = expedicao.getMensagem();
                            }));
                }
                acoesLoja.add(botao("Sair e abrir o mapa", () -> { avisoLoja = null; expedicao.sairDaLoja(); }));
                mostrarOpcoes("Mercador: compre suprimentos/melhorias ou ative relíquias pendentes.",
                        new JLabel(new IconeSala(Expedicao.TipoSala.LOJA, true, true, false)),
                        acoesLoja.toArray(new JButton[0]));
                break;
            case RECOMPENSA:
                mostrarOpcoes("Elite derrotado! Escolha uma relíquia para guardar. Ela só será ativada na loja ou no descanso.",
                        new JLabel(new IconeSala(Expedicao.TipoSala.ELITE, true, true, false)),
                        opcaoReliquia(Expedicao.Reliquia.LAMINA),
                        opcaoReliquia(Expedicao.Reliquia.AMULETO),
                        opcaoReliquia(Expedicao.Reliquia.FOCO));
                break;
            case CAPITULO_CONCLUIDO:
                if (expedicao.getCiclo() > 0) {
                    mostrarOpcoes("Chefe derrotado! Continue a Ascensão ou encerre com vitória.",
                            botao("Próximo capítulo", expedicao::avancarCapitulo),
                            botao("Encerrar com vitória", expedicao::concluirExpedicao));
                } else {
                    mostrarOpcoes("Capítulo concluído! Seu herói, recursos e relíquias serão preservados.",
                            botao("Próximo capítulo", expedicao::avancarCapitulo));
                }
                break;
            case CAMPANHA_CONCLUIDA:
                mostrarOpcoes(expedicao.getCiclo() == 0 ? "Os três chefes caíram. A campanha foi concluída!"
                                : "Ascensão concluída! Um desafio ainda maior espera.",
                        botao("Iniciar Ascensão " + (expedicao.getCiclo() + 1), expedicao::avancarCapitulo),
                        botao("Encerrar com vitória", expedicao::concluirExpedicao));
                break;
            case VITORIA:
            case DERROTA:
                mostrarOpcoes(expedicao.getEstado() == Expedicao.Estado.VITORIA
                                ? "Os três capítulos foram vencidos. Expedição concluída!"
                                : "Sua jornada terminou. Tente outra rota!",
                        new JLabel(new IconeSala(Expedicao.TipoSala.CHEFE, true, true, false)),
                        botao("Menu inicial", reiniciar));
                break;
            default: throw new IllegalStateException("Tela de expedição indisponível.");
        }
        revalidate();
        repaint();
    }

    private String nomeEstado() {
        switch (expedicao.getEstado()) {
            case MAPA: return "MAPA";
            case EVENTO: return "EVENTO";
            case RESULTADO: return "RESULTADO";
            case LOJA: return "LOJA";
            case ACAMPAMENTO: return "ACAMPAMENTO";
            case RECOMPENSA: return "RECOMPENSA";
            case CAPITULO_CONCLUIDO: return "CAPÍTULO CONCLUÍDO";
            case CAMPANHA_CONCLUIDA: return "CAMPANHA CONCLUÍDA";
            case VITORIA: return "VITÓRIA";
            case DERROTA: return "DERROTA";
            default: throw new IllegalStateException("Tela de expedição indisponível.");
        }
    }

    private void comprar(Jogo.Melhoria melhoria) {
        Jogo jogo = expedicao.getJogo();
        jogo.comprar(melhoria);
        avisoLoja = jogo.getMensagens().get(jogo.getMensagens().size() - 1);
    }

    private JButton botao(String texto, Runnable acao) {
        JButton botao = new JButton(texto);
        botao.addActionListener(e -> {
            acao.run();
            atualizarJanela.run();
        });
        return botao;
    }

    private JButton opcaoReliquia(Expedicao.Reliquia reliquia) {
        return botaoReliquia(reliquia, PainelReliquias.nome(reliquia) + ": " + PainelReliquias.efeito(reliquia),
                () -> expedicao.escolherReliquia(reliquia));
    }

    private JButton botaoReliquia(Expedicao.Reliquia reliquia, String texto, Runnable acao) {
        JButton botao = botao(texto, acao);
        botao.setIcon(new IconeReliquia(reliquia, true, 36));
        botao.setIconTextGap(10);
        return botao;
    }

    private void mostrarOpcoes(String descricao, JButton... botoes) {
        mostrarOpcoes(descricao, null, botoes);
    }

    private void mostrarOpcoes(String descricao, JComponent ilustracao, JButton... botoes) {
        JLabel texto = rotulo(descricao, 16);
        texto.setHorizontalAlignment(SwingConstants.CENTER);
        conteudo.add(texto, BorderLayout.NORTH);
        JPanel area = new JPanel(new BorderLayout(12, 12));
        area.setBackground(FUNDO);
        if (ilustracao != null) {
            JPanel imagem = new JPanel(new GridBagLayout());
            imagem.setBackground(FUNDO);
            imagem.add(ilustracao);
            area.add(imagem, BorderLayout.NORTH);
        }
        JPanel grade = new JPanel(new GridLayout(0, 1, 8, 10));
        grade.setBackground(FUNDO);
        grade.setBorder(new EmptyBorder(8, 100, 30, 100));
        for (JButton botao : botoes) grade.add(botao);
        area.add(grade, BorderLayout.CENTER);
        conteudo.add(area, BorderLayout.CENTER);
    }

    private JPanel criarLegenda() {
        JPanel legenda = new JPanel(new FlowLayout(FlowLayout.CENTER, 18, 0));
        legenda.setBackground(FUNDO);
        JLabel porta = new JLabel("Porta iluminada: caminho disponível",
                new IconeSala(Expedicao.TipoSala.ENTRADA, true, false, false, 32, false),
                SwingConstants.CENTER);
        porta.setForeground(TEXTO);
        legenda.add(porta);
        legenda.add(rotulo("Dourado: posição atual  •  Símbolos: salas visitadas", 12));
        return legenda;
    }

    private static JLabel rotulo(String texto, int tamanho) {
        JLabel rotulo = new JLabel(texto);
        rotulo.setForeground(TEXTO);
        rotulo.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, tamanho));
        return rotulo;
    }

    private static JLabel recurso(IconeRecurso.Tipo tipo) {
        JLabel rotulo = rotulo("", 14);
        rotulo.setIcon(new IconeRecurso(tipo));
        rotulo.setToolTipText(tipo.name());
        return rotulo;
    }

    private final class Mapa extends JPanel {
        private static final int TAMANHO_MAXIMO = 82;
        private static final int MARGEM = 16;
        private final Expedicao mapa;
        private final Map<JButton, Expedicao.Sala> botoes = new LinkedHashMap<>();
        private final int andares;
        private final int linhas;
        private int tamanho = TAMANHO_MAXIMO;
        private double passoX = 1;
        private double passoY = 1;
        private int origemX = MARGEM;
        private int origemY = MARGEM;

        Mapa(Expedicao mapa) {
            this.mapa = mapa;
            setLayout(null);
            setBackground(new Color(30, 41, 59));
            setPreferredSize(new Dimension(960, 300));
            setMinimumSize(new Dimension(480, 180));
            setBorder(BorderFactory.createLineBorder(new Color(71, 85, 105)));
            int maiorAndar = 0;
            int maiorLinha = 0;
            for (Expedicao.Sala sala : mapa.getSalas()) {
                maiorAndar = Math.max(maiorAndar, sala.getAndar());
                maiorLinha = Math.max(maiorLinha, sala.getLinha());
                boolean disponivel = mapa.estaDisponivel(sala);
                boolean atual = sala == mapa.getSalaAtual();
                JButton botao = new JButton(new IconeSala(sala.getTipo(), disponivel, atual,
                        sala.foiVisitada(), TAMANHO_MAXIMO, sala.foiVisitada()));
                botao.setEnabled(disponivel);
                botao.setBorderPainted(false);
                botao.setContentAreaFilled(false);
                botao.setFocusPainted(false);
                botao.setMargin(new Insets(0, 0, 0, 0));
                String nome = sala.foiVisitada() ? nomeSala(sala.getTipo()) : "Sala inexplorada";
                String descricao = sala.foiVisitada() ? descricaoSala(sala.getTipo())
                        : "O conteúdo será revelado ao entrar.";
                botao.setToolTipText(nome + " • andar " + sala.getAndar() + " — " + descricao);
                botao.getAccessibleContext().setAccessibleName(nome
                        + ", andar " + sala.getAndar() + ". " + descricao);
                botao.getAccessibleContext().setAccessibleDescription(disponivel
                        ? "Sala acessível. Clique para entrar." : atual ? "Sua posição atual."
                        : "Caminho indisponível.");
                botao.addActionListener(e -> { mapa.selecionar(sala); atualizarJanela.run(); });
                botoes.put(botao, sala);
                add(botao);
            }
            andares = maiorAndar + 1;
            linhas = maiorLinha + 1;
        }

        @Override
        public void doLayout() {
            int largura = Math.max(1, getWidth() - 2 * MARGEM);
            int altura = Math.max(1, getHeight() - 2 * MARGEM);
            // Passos mínimos: 1,6x o ícone na horizontal e 1,25x na vertical, limitado a 82px.
            double porLargura = largura / (1 + 1.6 * (andares - 1));
            double porAltura = altura / (1 + 1.25 * (linhas - 1));
            tamanho = (int) Math.min(TAMANHO_MAXIMO, Math.min(porLargura, porAltura));
            tamanho = Math.max(28, tamanho);
            passoX = andares > 1 ? (double) (largura - tamanho) / (andares - 1) : 0;
            passoY = linhas > 1 ? Math.min((double) (altura - tamanho) / (linhas - 1), tamanho * 1.6) : 0;
            int alturaUsada = (int) (tamanho + passoY * (linhas - 1));
            origemX = MARGEM;
            origemY = MARGEM + Math.max(0, (altura - alturaUsada) / 2);
            for (Map.Entry<JButton, Expedicao.Sala> item : botoes.entrySet()) {
                Expedicao.Sala sala = item.getValue();
                JButton botao = item.getKey();
                botao.setIcon(new IconeSala(sala.getTipo(), mapa.estaDisponivel(sala),
                        sala == mapa.getSalaAtual(), sala.foiVisitada(), tamanho, sala.foiVisitada()));
                botao.setBounds(x(sala), y(sala), tamanho, tamanho);
            }
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.setColor(new Color(38, 42, 52));
                for (int y = 0; y < getHeight(); y += 24) {
                    int deslocamento = (y / 24 % 2) * 32;
                    for (int x = -32; x < getWidth(); x += 64) {
                        g.drawRect(x + deslocamento, y, 64, 24);
                    }
                }
                for (Expedicao.Sala sala : mapa.getSalas()) {
                    for (Expedicao.Sala destino : sala.getSaidas()) {
                        g.setStroke(new BasicStroke(9));
                        g.setColor(new Color(12, 16, 25));
                        g.drawLine(x(sala) + tamanho, y(sala) + tamanho / 2,
                                x(destino), y(destino) + tamanho / 2);
                        g.setStroke(new BasicStroke(3));
                        g.setColor(sala == mapa.getSalaAtual() ? new Color(211, 170, 87)
                                : new Color(69, 73, 85));
                        g.drawLine(x(sala) + tamanho, y(sala) + tamanho / 2,
                                x(destino), y(destino) + tamanho / 2);
                    }
                }
            } finally {
                g.dispose();
            }
        }

        private int x(Expedicao.Sala sala) { return origemX + (int) Math.round(sala.getAndar() * passoX); }
        private int y(Expedicao.Sala sala) { return origemY + (int) Math.round(sala.getLinha() * passoY); }
    }
    private static String nomeSala(Expedicao.TipoSala tipo) {
        switch (tipo) {
            case ENTRADA: return "Entrada";
            case COMBATE: return "Combate";
            case ELITE: return "Elite";
            case EVENTO: return "Evento";
            case LOJA: return "Loja";
            case ACAMPAMENTO: return "Descanso";
            case CHEFE: return "Chefe";
            default: throw new IllegalArgumentException("Sala desconhecida.");
        }
    }

    private static String descricaoSala(Expedicao.TipoSala tipo) {
        switch (tipo) {
            case COMBATE: return "Inimigos e recompensa normal.";
            case ELITE: return "Combate difícil, recompensa dobrada e escolha de relíquia.";
            case EVENTO: return "Uma escolha com riscos ou benefícios.";
            case LOJA: return "Compre suprimentos e melhorias.";
            case ACAMPAMENTO: return "Escolha recuperar vida ou mana.";
            case CHEFE: return "Enfrente o guardião final.";
            case ENTRADA: return "Início da expedição.";
            default: throw new IllegalArgumentException("Sala desconhecida.");
        }
    }
}
