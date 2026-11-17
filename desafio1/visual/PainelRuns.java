package desafio1.visual;

import desafio1.jogo.Jogo;
import desafio1.jogo.RegistroRun;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/** Tela de runs salvas: no modo carregar lista só as runs em andamento; no modo estatísticas, todas. */
final class PainelRuns extends JPanel {
    private static final Color FUNDO = new Color(12, 16, 29);
    private static final Color CAIXA = new Color(24, 31, 48);
    private static final Color TEXTO = new Color(226, 232, 240);
    private static final Color OURO = new Color(239, 209, 146);
    private final DefaultListModel<RegistroRun> modelo = new DefaultListModel<>();
    private final JList<RegistroRun> lista = new JList<>(modelo);
    private final JLabel titulo = new JLabel("", SwingConstants.LEFT);
    private final JLabel detalhes = new JLabel();
    private final JLabel totais = new JLabel();
    private final JButton continuar = new JButton("Continuar run");

    PainelRuns(Consumer<RegistroRun> continuarRun, Runnable voltar) {
        super(new BorderLayout(14, 14));
        Objects.requireNonNull(continuarRun);
        Objects.requireNonNull(voltar);
        setBackground(FUNDO);
        setBorder(new EmptyBorder(24, 32, 24, 32));

        JPanel cabecalho = new JPanel(new BorderLayout());
        cabecalho.setOpaque(false);
        titulo.setFont(new Font(Font.SERIF, Font.BOLD, 30));
        titulo.setForeground(OURO);
        cabecalho.add(titulo, BorderLayout.WEST);
        JButton botaoVoltar = new JButton("Voltar");
        botaoVoltar.setFocusPainted(false);
        botaoVoltar.addActionListener(e -> voltar.run());
        cabecalho.add(botaoVoltar, BorderLayout.EAST);
        add(cabecalho, BorderLayout.NORTH);

        lista.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lista.setBackground(CAIXA);
        lista.setForeground(TEXTO);
        lista.setFixedCellHeight(58);
        lista.setCellRenderer(new Renderizador());
        lista.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) atualizarDetalhes();
        });
        JScrollPane rolagem = new JScrollPane(lista);
        rolagem.setBorder(BorderFactory.createLineBorder(new Color(71, 85, 105)));
        rolagem.setPreferredSize(new Dimension(340, 300));
        add(rolagem, BorderLayout.WEST);

        JPanel painelDetalhes = new JPanel(new BorderLayout(0, 12));
        painelDetalhes.setBackground(CAIXA);
        painelDetalhes.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(120, 94, 58)), new EmptyBorder(16, 20, 16, 20)));
        detalhes.setVerticalAlignment(SwingConstants.TOP);
        detalhes.setForeground(TEXTO);
        detalhes.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 15));
        painelDetalhes.add(detalhes, BorderLayout.CENTER);
        continuar.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        continuar.setFocusPainted(false);
        continuar.addActionListener(e -> {
            RegistroRun run = lista.getSelectedValue();
            if (run != null && run.podeContinuar()) continuarRun.accept(run);
        });
        painelDetalhes.add(continuar, BorderLayout.SOUTH);
        add(painelDetalhes, BorderLayout.CENTER);

        totais.setForeground(new Color(190, 197, 212));
        totais.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        JPanel rodape = new JPanel(new GridLayout(1, 1));
        rodape.setOpaque(false);
        rodape.add(totais);
        add(rodape, BorderLayout.SOUTH);
    }

    void mostrar(List<RegistroRun> runs, boolean modoCarregar) {
        titulo.setText(modoCarregar ? "Carregar jogo" : "Estatísticas das runs");
        modelo.clear();
        int vitorias = 0;
        int mortes = 0;
        int abates = 0;
        int chefes = 0;
        for (RegistroRun run : runs) {
            if (!modoCarregar || run.podeContinuar()) modelo.addElement(run);
            if (run.getResultado() == RegistroRun.Resultado.VITORIA) vitorias++;
            mortes += run.getEstatisticas().getMortes();
            abates += run.getEstatisticas().getAbates();
            chefes += run.getEstatisticas().getChefesDerrotados();
        }
        totais.setText("Total geral: " + runs.size() + " runs  •  " + vitorias + " vitórias  •  " + mortes
                + " mortes  •  " + abates + " abates  •  " + chefes + " chefes finalizados");
        continuar.setVisible(true);
        if (!modelo.isEmpty()) lista.setSelectedIndex(0);
        atualizarDetalhes();
    }

    private void atualizarDetalhes() {
        RegistroRun run = lista.getSelectedValue();
        continuar.setEnabled(run != null && run.podeContinuar());
        if (run == null) {
            detalhes.setText(modelo.isEmpty() ? "Nenhuma run encontrada. Comece um novo jogo no menu inicial."
                    : "Selecione uma run para ver os detalhes.");
            return;
        }
        Jogo.Estatisticas e = run.getEstatisticas();
        SimpleDateFormat data = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        detalhes.setText("<html><font size='5' color='#efd192'><b>" + escapar(run.getHeroi()) + "</b></font> &nbsp; "
                + run.getClasse() + " • Nível " + run.getNivel() + "<br>"
                + (run.getModo() == RegistroRun.Modo.EXPEDICAO ? "Expedição" : "Ondas infinitas") + " • "
                + run.descricaoProgresso() + " • <b>" + run.descricaoResultado() + "</b><br>"
                + "Início: " + data.format(new Date(run.getInicio())) + " &nbsp; Último save: "
                + data.format(new Date(run.getUltimaAtualizacao())) + "<br><br>"
                + "<table cellpadding='3'>"
                + linha("Abates", e.getAbates()) + linha("Chefes finalizados", e.getChefesDerrotados())
                + linha("Mortes", e.getMortes()) + linha("Encontros vencidos", e.getEncontrosVencidos())
                + linha("Dano causado", e.getDanoCausado()) + linha("Dano recebido", e.getDanoRecebido())
                + linha("Limit Breaks usados", e.getLimitesUsados()) + linha("Ouro obtido", e.getOuroGanho())
                + "</table></html>");
    }

    String getTextoDetalhes() { return detalhes.getText(); }
    String getTextoTotais() { return totais.getText(); }
    int getQuantidadeRuns() { return modelo.size(); }
    boolean podeContinuarSelecionada() { return continuar.isEnabled(); }
    void continuarSelecionada() { continuar.doClick(); }

    private static String linha(String nome, long valor) {
        return "<tr><td>" + nome + "</td><td align='right'><b>" + valor + "</b></td></tr>";
    }

    private static String escapar(String texto) {
        return texto.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static final class Renderizador extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object valor, int index,
                                                      boolean selecionado, boolean foco) {
            RegistroRun run = (RegistroRun) valor;
            JLabel label = (JLabel) super.getListCellRendererComponent(list, valor, index, selecionado, foco);
            String cor = run.getResultado() == RegistroRun.Resultado.VITORIA ? "#86efac"
                    : run.getResultado() == RegistroRun.Resultado.DERROTA ? "#fca5a5" : "#fde68a";
            label.setText("<html><b>" + escapar(run.getHeroi()) + "</b> • " + run.getClasse() + " Nv." + run.getNivel()
                    + "<br>" + run.descricaoProgresso() + " • <font color='" + cor + "'>"
                    + run.descricaoResultado() + "</font></html>");
            label.setBorder(new EmptyBorder(6, 12, 6, 12));
            label.setBackground(selecionado ? new Color(51, 65, 85) : CAIXA);
            label.setForeground(TEXTO);
            return label;
        }
    }
}
