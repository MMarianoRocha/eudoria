package desafio1.visual;

import desafio1.jogo.Expedicao;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Vitrine das relíquias: ativas em destaque e pendentes esmaecidas. */
final class PainelReliquias extends JPanel {
    private final int tamanho;
    private final Color texto;
    private List<Expedicao.Reliquia> ultimasAtivas;
    private List<Expedicao.Reliquia> ultimasPendentes;

    PainelReliquias(int tamanho, Color texto) {
        super(new FlowLayout(FlowLayout.LEFT, 6, 0));
        this.tamanho = tamanho;
        this.texto = texto;
        setOpaque(false);
    }

    void atualizar(List<Expedicao.Reliquia> ativas, List<Expedicao.Reliquia> pendentes) {
        if (ativas.equals(ultimasAtivas) && pendentes.equals(ultimasPendentes)) return;
        ultimasAtivas = new ArrayList<>(ativas);
        ultimasPendentes = new ArrayList<>(pendentes);
        removeAll();
        add(rotulo("Relíquias:"));
        if (ativas.isEmpty() && pendentes.isEmpty()) add(rotulo("nenhuma"));
        for (Expedicao.Reliquia reliquia : Expedicao.Reliquia.values()) {
            adicionar(reliquia, Collections.frequency(ativas, reliquia), true);
        }
        for (Expedicao.Reliquia reliquia : Expedicao.Reliquia.values()) {
            adicionar(reliquia, Collections.frequency(pendentes, reliquia), false);
        }
        revalidate();
        repaint();
    }

    private void adicionar(Expedicao.Reliquia reliquia, int quantidade, boolean ativa) {
        if (quantidade == 0) return;
        JLabel item = rotulo(quantidade > 1 ? "x" + quantidade : "");
        item.setIcon(new IconeReliquia(reliquia, ativa, tamanho));
        item.setIconTextGap(2);
        String descricao = nome(reliquia) + " (" + efeito(reliquia) + ")"
                + (ativa ? " — ativa" : " — pendente: ative na loja ou no descanso")
                + (quantidade > 1 ? " x" + quantidade : "");
        item.setToolTipText(descricao);
        item.getAccessibleContext().setAccessibleName(descricao);
        add(item);
    }

    private JLabel rotulo(String valor) {
        JLabel rotulo = new JLabel(valor);
        rotulo.setForeground(texto);
        rotulo.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        return rotulo;
    }

    static String nome(Expedicao.Reliquia reliquia) {
        switch (reliquia) {
            case LAMINA: return "Lâmina";
            case AMULETO: return "Amuleto";
            case FOCO: return "Foco";
            default: throw new IllegalArgumentException("Relíquia desconhecida.");
        }
    }

    static String efeito(Expedicao.Reliquia reliquia) {
        switch (reliquia) {
            case LAMINA: return "+4 poder";
            case AMULETO: return "+20 vida máxima";
            case FOCO: return "+20 mana máxima";
            default: throw new IllegalArgumentException("Relíquia desconhecida.");
        }
    }
}
