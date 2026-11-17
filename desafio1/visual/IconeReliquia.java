package desafio1.visual;

import desafio1.jogo.Expedicao;

import javax.swing.Icon;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

final class IconeReliquia implements Icon {
    private static final int ESCALA = 5;
    private static final int BASE = 14 * ESCALA;
    private static final Color CONTORNO = new Color(25, 30, 45);
    private final Expedicao.Reliquia reliquia;
    private final boolean ativa;
    private final int tamanho;

    IconeReliquia(Expedicao.Reliquia reliquia, boolean ativa, int tamanho) {
        this.reliquia = reliquia;
        this.ativa = ativa;
        this.tamanho = tamanho;
    }

    Expedicao.Reliquia getReliquia() { return reliquia; }
    boolean isAtiva() { return ativa; }

    @Override public int getIconWidth() { return tamanho; }
    @Override public int getIconHeight() { return tamanho; }

    @Override
    public void paintIcon(Component componente, Graphics graphics, int x, int y) {
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.translate(x, y);
            g.scale(tamanho / (double) BASE, tamanho / (double) BASE);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            g.setColor(ativa ? new Color(74, 58, 28) : new Color(38, 46, 64));
            g.fillRoundRect(1, 1, BASE - 2, BASE - 2, 14, 14);
            g.setColor(ativa ? new Color(240, 196, 92) : new Color(100, 116, 139));
            g.drawRoundRect(1, 1, BASE - 3, BASE - 3, 14, 14);
            if (!ativa) g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.45f));
            g.translate(ESCALA, ESCALA);
            switch (reliquia) {
                case LAMINA: lamina(g); break;
                case AMULETO: amuleto(g); break;
                case FOCO: foco(g); break;
                default: throw new IllegalArgumentException("Relíquia sem sprite.");
            }
        } finally {
            g.dispose();
        }
    }

    private void lamina(Graphics2D g) {
        cor(g, "#eef3fb");
        for (int i = 0; i < 7; i++) bloco(g, 4 + i, 6 - i, 2, 1);
        cor(g, "#9fb2cf");
        for (int i = 0; i < 6; i++) pixel(g, 4 + i, 7 - i);
        cor(g, "#7ad7f0"); pixel(g, 10, 0); pixel(g, 11, 0);
        cor(g, "#f0bd5a");
        bloco(g, 1, 6, 5, 1); bloco(g, 3, 4, 1, 5);
        cor(g, "#7b4f35");
        pixel(g, 2, 8); pixel(g, 1, 9);
        cor(g, "#f0bd5a"); pixel(g, 0, 10);
        g.setColor(CONTORNO); pixel(g, 3, 7);
    }

    private void amuleto(Graphics2D g) {
        cor(g, "#d9b45a");
        pixel(g, 3, 0); pixel(g, 2, 1); pixel(g, 2, 2); pixel(g, 3, 3);
        pixel(g, 8, 0); pixel(g, 9, 1); pixel(g, 9, 2); pixel(g, 8, 3);
        bloco(g, 4, 0, 4, 1);
        cor(g, "#f2d27a");
        bloco(g, 4, 4, 4, 1); bloco(g, 3, 5, 6, 4); bloco(g, 4, 9, 4, 1); bloco(g, 5, 10, 2, 1);
        cor(g, "#d84a5e");
        bloco(g, 4, 5, 4, 3); bloco(g, 5, 8, 2, 1);
        cor(g, "#ff9aa8"); pixel(g, 4, 5); pixel(g, 5, 5);
        cor(g, "#8f2337"); pixel(g, 7, 7);
    }

    private void foco(Graphics2D g) {
        cor(g, "#4d7bd8");
        bloco(g, 4, 1, 4, 1); bloco(g, 3, 2, 6, 1); bloco(g, 2, 3, 8, 4);
        bloco(g, 3, 7, 6, 1); bloco(g, 4, 8, 4, 1);
        cor(g, "#8fd0ff");
        bloco(g, 4, 3, 3, 3);
        cor(g, "#e6f6ff"); pixel(g, 4, 3); pixel(g, 5, 2);
        cor(g, "#b98bf2"); pixel(g, 7, 6); pixel(g, 8, 4);
        cor(g, "#7b5a3c");
        bloco(g, 3, 9, 6, 1); bloco(g, 2, 10, 8, 2);
        cor(g, "#a77b52"); bloco(g, 3, 10, 6, 1);
    }

    private void cor(Graphics2D g, String color) { g.setColor(Color.decode(color)); }

    private void pixel(Graphics2D g, int x, int y) {
        g.fillRect(x * ESCALA, y * ESCALA, ESCALA, ESCALA);
    }

    private void bloco(Graphics2D g, int x, int y, int largura, int altura) {
        g.fillRect(x * ESCALA, y * ESCALA, largura * ESCALA, altura * ESCALA);
    }
}
