package desafio1.visual;

import desafio1.jogo.Expedicao;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

final class IlustracaoEvento extends JPanel {
    private final Expedicao.Evento evento;
    private final boolean resultado;

    IlustracaoEvento(Expedicao.Evento evento, boolean resultado) {
        this.evento = evento;
        this.resultado = resultado;
        setOpaque(false);
        setPreferredSize(new Dimension(560, 250));
        getAccessibleContext().setAccessibleName("Ilustração da situação: " + evento.name());
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            g.setColor(new Color(23, 31, 51));
            g.fillRoundRect(5, 5, w - 10, h - 10, 24, 24);
            g.setColor(new Color(79, 91, 117));
            g.setStroke(new BasicStroke(2));
            g.drawRoundRect(5, 5, w - 10, h - 10, 24, 24);
            g.setColor(new Color(44, 51, 72));
            g.fillOval(w / 2 - 135, h - 85, 270, 42);
            switch (evento) {
                case BAU: bau(g, w, h); break;
                case ALTAR: altar(g, w, h); break;
                case ARMADILHA: armadilha(g, w, h); break;
                case FONTE: fonte(g, w, h); break;
                case FORJA: forja(g, w, h); break;
                default: throw new IllegalStateException("Evento sem ilustração.");
            }

            if (resultado) {
                g.setColor(new Color(8, 12, 24, 165));
                g.fillRoundRect(20, 20, w - 40, 42, 16, 16);
                g.setColor(new Color(255, 225, 150));
                g.drawString("CONSEQUÊNCIA DA ESCOLHA", 34, 47);
            }
        } finally {
            g.dispose();
        }
    }

    private void fonte(Graphics2D g, int w, int h) {
        int x = w / 2;
        g.setColor(new Color(98, 117, 142));
        g.fillOval(x - 100, h - 100, 200, 70);
        g.setColor(new Color(70, 185, 225));
        g.fillOval(x - 85, h - 95, 170, 45);
        g.setColor(new Color(175, 233, 255));
        g.setStroke(new BasicStroke(6));
        g.drawArc(x - 40, h - 170, 80, 120, 0, 180);
        g.drawLine(x, h - 165, x, h - 90);
        for (int i = -2; i <= 2; i++) g.fillOval(x + i * 30 - 3, h - 190 + Math.abs(i) * 15, 7, 7);
    }

    private void forja(Graphics2D g, int w, int h) {
        int x = w / 2;
        g.setColor(new Color(112, 78, 70));
        g.fillRect(x - 95, h - 140, 190, 105);
        g.setColor(new Color(34, 30, 40));
        g.fillRect(x - 65, h - 110, 130, 75);
        g.setColor(new Color(255, 138, 50));
        for (int i = -1; i <= 1; i++) {
            int centro = x + i * 35;
            g.fillPolygon(new int[]{centro - 22, centro, centro + 22},
                    new int[]{h - 40, h - 108 + Math.abs(i) * 20, h - 40}, 3);
        }
        g.setColor(new Color(163, 183, 199));
        g.fillRect(x - 110, h - 155, 220, 18);
        g.setColor(new Color(255, 223, 118));
        g.fillOval(x - 12, h - 186, 24, 24);
    }

    private void bau(Graphics2D g, int w, int h) {
        int x = w / 2 - 65;
        int y = h / 2 - 14;
        g.setColor(new Color(112, 67, 45));
        g.fillRoundRect(x, y, 130, 77, 12, 12);
        g.setColor(new Color(194, 139, 66));
        g.fillRoundRect(x + 5, y + 6, 120, 23, 8, 8);
        g.fillRect(x + 10, y + 37, 110, 9);
        g.setColor(new Color(234, 195, 106));
        g.fillRect(x + 59, y + 32, 13, 23);
        g.fillOval(x + 60, y + 47, 11, 11);
        g.setColor(new Color(29, 34, 48));
        g.fillOval(x + 65, y + 50, 3, 6);
        g.setColor(new Color(255, 198, 83));
        g.fillOval(x + 18, y - 25, 8, 8);
        g.fillOval(x + 98, y - 33, 7, 7);
        g.fillOval(x + 76, y - 21, 6, 6);
    }

    private void altar(Graphics2D g, int w, int h) {
        int x = w / 2;
        g.setColor(new Color(103, 99, 130));
        g.fillPolygon(new int[]{x - 96, x + 96, x + 72, x - 72},
                new int[]{h - 53, h - 53, h - 25, h - 25}, 4);
        g.fillRect(x - 60, h - 115, 120, 52);
        g.setColor(new Color(140, 123, 170));
        g.fillPolygon(new int[]{x - 77, x, x + 77}, new int[]{h - 112, h - 172, h - 112}, 3);
        g.setColor(new Color(149, 220, 244, 115));
        g.fillOval(x - 35, h - 184, 70, 70);
        g.setColor(new Color(206, 241, 252));
        g.fillOval(x - 12, h - 159, 24, 24);
        g.setColor(new Color(255, 222, 138));
        g.fillOval(x - 4, h - 151, 8, 8);
    }

    private void armadilha(Graphics2D g, int w, int h) {
        int x = w / 2;
        g.setColor(new Color(72, 79, 94));
        g.fillRect(x - 130, h - 95, 260, 18);
        g.setColor(new Color(124, 132, 145));
        g.fillRect(x - 150, h - 98, 300, 8);
        g.setColor(new Color(188, 198, 212));
        for (int i = -2; i <= 2; i++) {
            int px = x + i * 31;
            g.fillPolygon(new int[]{px - 12, px + 12, px}, new int[]{h - 83, h - 83, h - 32}, 3);
        }
        g.setColor(new Color(183, 97, 82));
        g.setStroke(new BasicStroke(5));
        g.drawLine(x - 130, h - 151, x + 130, h - 111);
        g.drawLine(x + 130, h - 151, x - 130, h - 111);
        g.setColor(new Color(238, 193, 103));
        g.fillOval(x - 7, h - 148, 14, 14);
    }
}
