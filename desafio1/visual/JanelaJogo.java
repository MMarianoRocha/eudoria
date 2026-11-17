package desafio1.visual;

import desafio1.jogo.Inimigo;
import desafio1.jogo.Jogo;
import desafio1.jogo.Expedicao;
import desafio1.jogo.RegistroRun;
import desafio1.jogo.Salvamento;
import desafio1.personagens.Guerreiro;
import desafio1.personagens.Ladino;
import desafio1.personagens.Mago;
import desafio1.personagens.Personagem;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

public final class JanelaJogo extends JFrame {
    private static final Color FUNDO = new Color(15, 23, 42);
    private static final Color PAINEL = new Color(30, 41, 59);
    private static final Color TEXTO = new Color(226, 232, 240);
    private static final Color DESTAQUE = new Color(56, 189, 248);
    private final JLabel titulo = rotulo("", 23, true);
    private final JLabel estado = rotulo("", 14, false);
    private final JLabel heroi = rotulo("", 20, true);
    private final JLabel atributos = rotulo("", 13, false);
    private final JLabel recursos = rotulo("", 13, false);
    private final PainelReliquias reliquias = new PainelReliquias(24, new Color(203, 213, 225));
    private final JProgressBar vida = barra(new Color(52, 211, 153));
    private final JProgressBar mana = barra(new Color(96, 165, 250));
    private final JProgressBar experiencia = barra(new Color(192, 132, 252));
    private final JProgressBar atb = barra(new Color(251, 191, 36));
    private final JProgressBar limite = barra(new Color(248, 113, 113));
    private final DefaultListModel<Inimigo> modelo = new DefaultListModel<>();
    private final JList<Inimigo> lista = new JList<>(modelo);
    private final Arena2D arena2D = new Arena2D(indice -> lista.setSelectedIndex(indice));
    private final JLabel fichaAlvo = rotulo("Selecione um inimigo para ver suas características.", 12, false);
    private final JLabel aviso = rotulo("", 13, false);
    private final JButton ataque = botao("Ataque");
    private final JButton defender = botao("Defender");
    private final JButton habilidade = botao("Habilidade");
    private final JButton pocaoVida = botao("Poção de vida");
    private final JButton pocaoMana = botao("Poção de mana");
    private final JButton limiteBreak = botao("Limit Break");
    private final JButton melhoriaAtaque = botao("");
    private final JButton melhoriaVida = botao("");
    private final JButton comprarVida = botao("");
    private final JButton comprarMana = botao("");
    private final JButton avancar = botao("Próxima onda");
    private final JButton reiniciar = botao("Menu inicial");
    // Na expedição a loja só existe nas salas de loja, então este painel fica oculto na batalha.
    private final JPanel loja = caixa("INTERVALO • LOJA");
    private final Timer timer;
    private final CardLayout layoutTelas = new CardLayout();
    private final PainelNivel painelNivel = new PainelNivel(this::escolherBonus);
    private boolean mostrandoNivel;
    private final JPanel telas = new JPanel(layoutTelas);
    private Jogo jogo;
    private Expedicao expedicao;
    private PainelExpedicao painelExpedicao;
    private boolean mostrandoExpedicao;
    private int ondaExibida;
    private final Salvamento salvamento = Salvamento.padrao();
    private final PainelInicial menuInicial = new PainelInicial(this::novoJogo, () -> mostrarRuns(true),
            () -> mostrarRuns(false), this::sair);
    private final PainelRuns painelRuns = new PainelRuns(this::continuarRun, this::mostrarMenu);
    private RegistroRun run;
    private String marcadorSalvo = "";

    public JanelaJogo() {
        super("Eudoria — Arena das Ondas");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent evento) {
                salvar();
            }
        });
        setMinimumSize(new Dimension(1050, 800));
        setSize(1280, 900);
        setLocationRelativeTo(null);
        getContentPane().setBackground(FUNDO);
        construirInterface();
        timer = new Timer(50, evento -> {
            if (jogo != null) {
                if (expedicao == null || expedicao.getEstado() == Expedicao.Estado.COMBATE) jogo.avancarTempo();
                arena2D.avancarAnimacao();
                atualizar();
            }
        });
    }

    public void iniciar() {
        mostrarMenu();
        setVisible(true);
    }

    private void novoJogo() {
        if (escolherHeroi()) timer.start();
    }

    private void sair() {
        salvar();
        dispose();
    }

    private void mostrarMenu() {
        timer.stop();
        salvar();
        menuInicial.definirCarregarDisponivel(salvamento.listar().stream().anyMatch(RegistroRun::podeContinuar));
        layoutTelas.show(telas, "inicio");
    }

    private void mostrarRuns(boolean modoCarregar) {
        painelRuns.mostrar(salvamento.listar(), modoCarregar);
        layoutTelas.show(telas, "runs");
    }

    private void continuarRun(RegistroRun salva) {
        prepararPartida(salva.getJogo(), salva.getExpedicao(), salva);
        timer.start();
    }

    private void prepararPartida(Jogo novoJogo, Expedicao novaExpedicao, RegistroRun registro) {
        if (painelExpedicao != null) {
            telas.remove(painelExpedicao);
            painelExpedicao = null;
        }
        jogo = novoJogo;
        expedicao = novaExpedicao;
        if (expedicao != null) {
            painelExpedicao = new PainelExpedicao(expedicao, this::atualizarTelaExpedicao, this::mostrarMenu);
            telas.add(painelExpedicao, "expedicao");
        }
        run = registro;
        marcadorSalvo = "";
        mostrandoExpedicao = false;
        mostrandoNivel = false;
        ondaExibida = -1;
        layoutTelas.show(telas, "combate");
        atualizar();
    }

    /** Grava a run atual; falhas de disco não interrompem a partida. */
    private void salvar() {
        if (run == null) return;
        try {
            salvamento.registrar(run);
        } catch (RuntimeException erro) {
            aviso.setText("Não foi possível salvar: " + erro.getMessage());
        }
    }

    private void salvarSeMudou() {
        if (run == null) return;
        String marcador = jogo.getEstado() + "|" + jogo.getOnda() + "|" + jogo.getHeroi().getNivel() + "|"
                + jogo.getEscolhasNivel() + "|" + jogo.getLimite() / 10 + "|"
                + jogo.getOuro() + "|" + (expedicao == null ? "" : expedicao.getEstado() + "|"
                + expedicao.getCapitulo() + "|" + expedicao.getCiclo() + "|"
                + expedicao.getSalaAtual().getAndar() + "|" + expedicao.getSalaAtual().getLinha());
        if (marcador.equals(marcadorSalvo)) return;
        marcadorSalvo = marcador;
        salvar();
    }

    private boolean escolherHeroi() {
        JTextField nome = new JTextField("Herói");
        JComboBox<String> classe = new JComboBox<>(new String[]{"Guerreiro", "Mago", "Ladino"});
        JComboBox<String> modo = new JComboBox<>(new String[]{"Expedição", "Ondas infinitas"});
        JPanel formulario = new JPanel(new GridLayout(0, 1, 5, 5));
        formulario.add(new JLabel("Nome do herói:"));
        formulario.add(nome);
        formulario.add(new JLabel("Classe:"));
        formulario.add(classe);
        formulario.add(new JLabel("Modo de jogo:"));
        formulario.add(modo);
        while (true) {
            int opcao = JOptionPane.showConfirmDialog(this, formulario, "Criar herói",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (opcao != JOptionPane.OK_OPTION) return false;
            if (nome.getText().isBlank()) {
                JOptionPane.showMessageDialog(this, "Informe um nome para o herói.");
                continue;
            }
            String escolha = (String) classe.getSelectedItem();
            Personagem personagem;
            if ("Mago".equals(escolha)) personagem = new Mago(nome.getText());
            else if ("Ladino".equals(escolha)) personagem = new Ladino(nome.getText());
            else personagem = new Guerreiro(nome.getText());
            if (modo.getSelectedIndex() == 0) {
                Expedicao nova = new Expedicao(personagem, new java.util.Random().nextLong());
                prepararPartida(nova.getJogo(), nova, RegistroRun.deExpedicao(nova));
            } else {
                Jogo novo = new Jogo(personagem);
                prepararPartida(novo, null, RegistroRun.deOndas(novo));
            }
            return true;
        }
    }

    private void construirInterface() {
        JPanel raiz = painel(new BorderLayout(12, 12));
        raiz.setBorder(new EmptyBorder(18, 20, 18, 20));
        telas.add(raiz, "combate");
        telas.add(menuInicial, "inicio");
        telas.add(painelRuns, "runs");
        telas.add(painelNivel, "nivel");
        layoutTelas.show(telas, "inicio");
        setContentPane(telas);

        JPanel cabecalho = painel(new BorderLayout());
        cabecalho.add(titulo, BorderLayout.WEST);
        cabecalho.add(estado, BorderLayout.EAST);
        raiz.add(cabecalho, BorderLayout.NORTH);

        JPanel esquerda = painel(new BorderLayout(0, 12));
        JPanel ficha = caixa("SEU HERÓI");
        ficha.setLayout(new GridLayout(0, 1, 0, 6));
        ficha.add(heroi);
        ficha.add(atributos);
        ficha.add(comIcone(vida, IconeRecurso.Tipo.VIDA));
        ficha.add(comIcone(mana, IconeRecurso.Tipo.MANA));
        ficha.add(comIcone(experiencia, IconeRecurso.Tipo.XP));
        ficha.add(atb);
        ficha.add(limite);
        ficha.add(recursos);
        ficha.add(reliquias);
        esquerda.add(ficha, BorderLayout.NORTH);

        JPanel acoes = caixa("COMANDOS");
        acoes.setLayout(new GridLayout(0, 2, 8, 8));
        acoes.add(ataque);
        acoes.add(habilidade);
        acoes.add(pocaoVida);
        acoes.add(pocaoMana);
        limiteBreak.setFont(limiteBreak.getFont().deriveFont(Font.BOLD));
        acoes.add(limiteBreak);
        acoes.add(defender);
        esquerda.add(acoes, BorderLayout.CENTER);

        loja.setLayout(new GridLayout(0, 2, 8, 8));
        loja.add(melhoriaAtaque);
        loja.add(melhoriaVida);
        loja.add(comprarVida);
        loja.add(comprarMana);
        loja.add(avancar);
        JPanel rodapeEsquerda = painel(new BorderLayout(0, 8));
        rodapeEsquerda.add(loja, BorderLayout.CENTER);
        rodapeEsquerda.add(reiniciar, BorderLayout.SOUTH);
        esquerda.add(rodapeEsquerda, BorderLayout.SOUTH);

        JPanel direita = painel(new BorderLayout(0, 12));
        JPanel arena = caixa("INIMIGOS • SELECIONE UM ALVO");
        arena.setLayout(new BorderLayout());
        lista.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lista.setBackground(PAINEL);
        lista.setForeground(TEXTO);
        lista.setCellRenderer(new RenderizadorInimigo());
        lista.setFixedCellHeight(66);
        JScrollPane rolagemInimigos = new JScrollPane(lista);
        rolagemInimigos.setBorder(null);
        fichaAlvo.setVerticalAlignment(SwingConstants.CENTER);
        fichaAlvo.setBorder(new EmptyBorder(6, 8, 8, 8));
        JPanel detalhes = painel(new BorderLayout());
        detalhes.add(fichaAlvo, BorderLayout.CENTER);
        arena.add(detalhes, BorderLayout.NORTH);
        arena.add(rolagemInimigos, BorderLayout.CENTER);
        JPanel campo = caixa("ARENA 2D • CLIQUE NO INIMIGO PARA MIRAR");
        campo.setLayout(new BorderLayout());
        campo.add(arena2D, BorderLayout.CENTER);
        JSplitPane batalha = new JSplitPane(JSplitPane.VERTICAL_SPLIT, campo, arena);
        batalha.setBorder(null);
        batalha.setDividerSize(8);
        batalha.setResizeWeight(0.65);
        arena.setMinimumSize(new Dimension(320, 120));
        direita.add(batalha, BorderLayout.CENTER);

        JScrollPane rolagemComandos = new JScrollPane(esquerda);
        rolagemComandos.setBorder(null);
        rolagemComandos.setMinimumSize(new Dimension(360, 0));
        rolagemComandos.getVerticalScrollBar().setUnitIncrement(16);
        JSplitPane divisao = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, rolagemComandos, direita);
        divisao.setBorder(null);
        divisao.setDividerSize(10);
        divisao.setResizeWeight(0.40);
        divisao.setOpaque(false);
        raiz.add(divisao, BorderLayout.CENTER);

        JLabel ajuda = rotulo("ATB: o tempo para quando sua barra enche. Escolha um alvo e uma ação.", 12, false);
        ajuda.setForeground(new Color(148, 163, 184));
        JPanel rodape = painel(new GridLayout(0, 1, 0, 4));
        aviso.putClientProperty("html.disable", Boolean.TRUE);
        rodape.add(aviso);
        rodape.add(ajuda);
        raiz.add(rodape, BorderLayout.SOUTH);

        lista.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && jogo != null) {
                arena2D.atualizar(jogo, lista.getSelectedIndex());
                atualizarFichaAlvo();
            }
        });
        ataque.addActionListener(e -> atacar(false));
        defender.addActionListener(e -> { jogo.defender(); atualizar(); });
        habilidade.addActionListener(e -> atacar(true));
        pocaoVida.addActionListener(e -> { jogo.usarPocao(true); atualizar(); });
        pocaoMana.addActionListener(e -> { jogo.usarPocao(false); atualizar(); });
        limiteBreak.addActionListener(e -> usarLimite());
        melhoriaAtaque.addActionListener(e -> comprar(Jogo.Melhoria.ATAQUE));
        melhoriaVida.addActionListener(e -> comprar(Jogo.Melhoria.VITALIDADE));
        comprarVida.addActionListener(e -> comprar(Jogo.Melhoria.POCAO_VIDA));
        comprarMana.addActionListener(e -> comprar(Jogo.Melhoria.POCAO_MANA));
        avancar.addActionListener(e -> { jogo.proximaOnda(); atualizar(); });
        reiniciar.addActionListener(e -> mostrarMenu());
    }

    private void usarLimite() {
        int alvo = lista.getSelectedIndex();
        boolean area = jogo.getHeroi().getTipoAtaque() == Personagem.TipoAtaque.MAGICO;
        List<Integer> alvos = new ArrayList<>();
        for (int i = 0; i < jogo.getInimigos().size(); i++) {
            if (jogo.getInimigos().get(i).estaVivo() && (area || i == alvo)) alvos.add(i);
        }
        if (jogo.usarLimite(alvo)) {
            for (int indice : alvos) arena2D.animarAtaque(indice, true);
        }
        atualizar();
    }

    private void atualizarTelaExpedicao() {
        if (expedicao.getEstado() != Expedicao.Estado.COMBATE) painelExpedicao.atualizar();
        atualizar();
    }

    private void comprar(Jogo.Melhoria melhoria) {
        jogo.comprar(melhoria);
        atualizar();
    }

    private void atacar(boolean habilidade) {
        int alvo = lista.getSelectedIndex();
        if (jogo.atacar(alvo, habilidade)) arena2D.animarAtaque(alvo, habilidade);
        atualizar();
    }

    private void escolherBonus(Jogo.BonusNivel bonus) {
        jogo.escolherBonusNivel(bonus);
        if (jogo.getEscolhasNivel() > 0) {
            painelNivel.mostrar(jogo);
        } else {
            mostrandoNivel = false;
            mostrandoExpedicao = false;
            layoutTelas.show(telas, "combate");
            if (expedicao != null && expedicao.getEstado() != Expedicao.Estado.COMBATE) painelExpedicao.atualizar();
        }
        atualizar();
    }

    private void atualizar() {
        if (expedicao != null) expedicao.sincronizarCombate();
        if (jogo.getEscolhasNivel() > 0 && jogo.getEstado() == Jogo.Estado.INTERVALO) {
            salvarSeMudou();
            if (!mostrandoNivel) {
                painelNivel.mostrar(jogo);
                layoutTelas.show(telas, "nivel");
                mostrandoNivel = true;
            }
            return;
        }
        if (expedicao != null) {
            salvarSeMudou();
            if (expedicao.getEstado() != Expedicao.Estado.COMBATE) {
                if (!mostrandoExpedicao) {
                    painelExpedicao.atualizar();
                    layoutTelas.show(telas, "expedicao");
                    mostrandoExpedicao = true;
                }
                return;
            }
        }
        if (mostrandoExpedicao || expedicao == null) {
            layoutTelas.show(telas, "combate");
            mostrandoExpedicao = false;
        }
        if (expedicao == null) salvarSeMudou();
        Personagem p = jogo.getHeroi();
        titulo.setText(expedicao != null ? "EUDORIA  /  CAP. " + expedicao.getCapitulo()
                + "  /  SALA " + expedicao.getSalaAtual().getAndar()
                + (expedicao.getCiclo() > 0 ? "  /  ASC. " + expedicao.getCiclo() : "")
                : jogo.getOnda() == 0 ? "EUDORIA  /  PREPARAÇÃO" : "EUDORIA  /  ONDA " + jogo.getOnda());
        switch (jogo.getEstado()) {
            case COMBATE: estado.setText(jogo.estaDefendendo()
                    ? "Defendendo — dano -" + jogo.getReducaoDefesa() + "%" : "ATB carregando"); break;
            case ESCOLHA: estado.setText("Sua vez — tempo pausado"); break;
            case INTERVALO: estado.setText("Onda concluída — intervalo"); break;
            case DERROTA: estado.setText("Derrota — volte ao menu inicial"); break;
            default: throw new IllegalStateException("Estado de jogo desconhecido.");
        }
        heroi.setText(p.getNome() + "  •  " + p.getClasse() + "  /  Nível " + p.getNivel());
        atributos.setText("Poder " + p.getPoder() + "    Defesa " + p.getDefesa()
                + "    CON " + p.getConstituicao());
        atualizarBarra(vida, p.getHpAtual(), p.getHPMaximo(), "VIDA");
        atualizarBarra(mana, p.getManaAtual(), p.getManaMaxima(), "MANA");
        atualizarBarra(experiencia, jogo.getExperiencia(), jogo.getExperienciaNecessaria(),
                "XP");
        atualizarBarra(atb, jogo.getAtb(), 100, "ATB");
        atualizarBarra(limite, jogo.getLimite(), Jogo.LIMITE_MAXIMO, jogo.limitePronto() ? "LIMIT PRONTO" : "LIMIT");
        recursos.setText("Ouro: " + jogo.getOuro() + "    Poções: " + jogo.getPocoesVida()
                + " vida / " + jogo.getPocoesMana() + " mana");
        reliquias.setVisible(expedicao != null);
        if (expedicao != null) reliquias.atualizar(expedicao.getReliquiasAtivas(), expedicao.getReliquiasPendentes());
        if (ondaExibida != jogo.getOnda()) {
            modelo.clear();
            for (Inimigo inimigo : jogo.getInimigos()) modelo.addElement(inimigo);
            lista.setSelectedIndex(0);
            ondaExibida = jogo.getOnda();
        }
        if (lista.getSelectedIndex() < 0 || !modelo.get(lista.getSelectedIndex()).estaVivo()) {
            for (int i = 0; i < modelo.size(); i++) {
                if (modelo.get(i).estaVivo()) {
                    lista.setSelectedIndex(i);
                    break;
                }
            }
        }
        lista.repaint();
        arena2D.atualizar(jogo, lista.getSelectedIndex());
        atualizarFichaAlvo();
        boolean escolha = jogo.getEstado() == Jogo.Estado.ESCOLHA;
        boolean intervalo = jogo.getEstado() == Jogo.Estado.INTERVALO;
        loja.setVisible(expedicao == null);
        ataque.setEnabled(escolha);
        defender.setEnabled(escolha);
        defender.setToolTipText("Consome sua vez e reduz em " + jogo.getReducaoDefesa()
                + "% o dano recebido até a próxima vez (CON " + p.getConstituicao() + ").");
        habilidade.setEnabled(escolha);
        habilidade.setText(p.getNomeHabilidade() + " (" + p.getCustoHabilidade() + " MP)");
        pocaoVida.setEnabled(escolha);
        pocaoMana.setEnabled(escolha);
        limiteBreak.setEnabled(escolha && jogo.limitePronto());
        limiteBreak.setText("LIMIT: " + p.getNomeLimite());
        melhoriaAtaque.setEnabled(intervalo);
        melhoriaVida.setEnabled(intervalo);
        comprarVida.setEnabled(intervalo);
        comprarMana.setEnabled(intervalo);
        avancar.setEnabled(intervalo);
        avancar.setText(jogo.getOnda() == 0 ? "Iniciar primeira onda" : "Próxima onda");
        melhoriaAtaque.setText("Poder +4  •  " + jogo.preco(Jogo.Melhoria.ATAQUE) + " ouro");
        melhoriaVida.setText("Vida +20  •  " + jogo.preco(Jogo.Melhoria.VITALIDADE) + " ouro");
        comprarVida.setText("Poção vida  •  20 ouro");
        comprarMana.setText("Poção mana  •  20 ouro");
        if (!jogo.getMensagens().isEmpty()) {
            String mensagem = jogo.getMensagens().get(jogo.getMensagens().size() - 1);
            aviso.setText(mensagem);
        }
    }

    private void atualizarFichaAlvo() {
        int indice = lista.getSelectedIndex();
        if (jogo == null || indice < 0 || indice >= jogo.getInimigos().size()) {
            fichaAlvo.setText("Selecione um inimigo para ver suas características.");
            return;
        }
        Inimigo inimigo = jogo.getInimigos().get(indice);
        fichaAlvo.setText("<html><b>" + escaparHtml(inimigo.getNome()) + " • Nível "
                + inimigo.getNivel() + "</b> &nbsp; Armadura: " + inimigo.getNivelArmadura()
                + " (" + inimigo.getArmadura() + "; -" + inimigo.getReducaoArmadura()
                + "% dano físico)<br><font color='#86efac'>Fraqueza:</font> "
                + Inimigo.nomeTipoAtaque(inimigo.getFraqueza()) + " — bônus de 25%<br>"
                + "<font color='#fca5a5'>Resistência:</font> "
                + Inimigo.nomeTipoAtaque(inimigo.getResistencia()) + " — redução de 20%<br>"
                + "Melhor combinação: <b>" + inimigo.getClasseRecomendada()
                + "</b> &nbsp; | &nbsp; Próxima ação: " + escaparHtml(inimigo.getProximaAcao())
                + " (" + jogo.previsaoDano(inimigo) + " de dano)"
                + (inimigo.getVeneno() > 0 ? " &nbsp; <font color='#a3e635'>Veneno x" + inimigo.getAcumulosVeneno() + " (" + inimigo.getDanoVeneno() + "/pulso)</font>" : "")
                + (inimigo.getQueimadura() > 0 ? " &nbsp; <font color='#fb923c'>Queimadura (" + inimigo.getDanoQueimadura() + "/pulso)</font>" : "")
                + (inimigo.isAtordoado() ? " &nbsp; <font color='#fde68a'>Atordoado: ATB congelado</font>" : "")
                + "</html>");
    }

    private static String escaparHtml(String texto) {
        return texto.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static void atualizarBarra(JProgressBar barra, int valor, int maximo, String nome) {
        barra.setMaximum(maximo);
        barra.setValue(valor);
        barra.setString(nome + "  " + valor + " / " + maximo);
    }

    private static JPanel painel(java.awt.LayoutManager layout) {
        JPanel painel = new JPanel(layout);
        painel.setBackground(FUNDO);
        return painel;
    }

    private static JPanel caixa(String titulo) {
        JPanel painel = new JPanel();
        painel.setBackground(PAINEL);
        painel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(71, 85, 105)),
                        titulo, 0, 0, new Font(Font.SANS_SERIF, Font.BOLD, 11), DESTAQUE),
                new EmptyBorder(8, 10, 10, 10)));
        return painel;
    }

    private static JLabel rotulo(String texto, int tamanho, boolean negrito) {
        JLabel label = new JLabel(texto);
        label.setForeground(TEXTO);
        label.setFont(new Font(Font.SANS_SERIF, negrito ? Font.BOLD : Font.PLAIN, tamanho));
        return label;
    }

    private static JButton botao(String texto) {
        JButton botao = new JButton(texto);
        botao.setFocusPainted(false);
        return botao;
    }

    private static JProgressBar barra(Color cor) {
        JProgressBar barra = new JProgressBar();
        barra.setStringPainted(true);
        barra.setForeground(cor);
        barra.setPreferredSize(new Dimension(200, 22));
        return barra;
    }

    private static JPanel comIcone(JProgressBar barra, IconeRecurso.Tipo tipo) {
        JPanel linha = new JPanel(new BorderLayout(6, 0));
        linha.setOpaque(false);
        JLabel icone = new JLabel(new IconeRecurso(tipo));
        icone.setToolTipText(tipo.name());
        icone.getAccessibleContext().setAccessibleName(tipo.name());
        linha.add(icone, BorderLayout.WEST);
        linha.add(barra, BorderLayout.CENTER);
        return linha;
    }

    private final class RenderizadorInimigo extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object valor, int index,
                                                       boolean selecionado, boolean foco) {
            Inimigo inimigo = (Inimigo) valor;
            JLabel label = (JLabel) super.getListCellRendererComponent(list, valor, index, selecionado, foco);
            label.setText("<html><b>" + escapar(inimigo.getNome()) + " • Nv." + inimigo.getNivel() + "</b> "
                    + (inimigo.estaVivo() ? "" : "(derrotado)") + "<br>Vida: "
                    + inimigo.getVida() + "/" + inimigo.getVidaMaxima() + " &nbsp; ATB: "
                    + inimigo.getAtb() + "% &nbsp; Próxima: " + escapar(inimigo.getProximaAcao())
                    + (inimigo.estaVivo() && jogo != null ? " (" + jogo.previsaoDano(inimigo) + ")" : "")
                    + "</html>");
            label.setBorder(new EmptyBorder(6, 12, 6, 12));
            label.setBackground(selecionado ? new Color(51, 65, 85) : PAINEL);
            label.setForeground(inimigo.estaVivo() ? TEXTO : new Color(148, 163, 184));
            label.setHorizontalAlignment(SwingConstants.LEFT);
            return label;
        }

        private String escapar(String texto) {
            return texto.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        }
    }
}
