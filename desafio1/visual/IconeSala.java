package desafio1.visual;

import desafio1.jogo.Expedicao;

import javax.swing.Icon;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

final class IconeSala implements Icon {
    private static final int ESCALA = 5;
    private static final Color CONTORNO = new Color(25, 30, 45);
    private final Expedicao.TipoSala tipo;
    private final boolean disponivel;
    private final boolean atual;
    private final boolean visitada;
    private final boolean revelada;
    private final int tamanho;

    IconeSala(Expedicao.TipoSala tipo, boolean disponivel, boolean atual, boolean visitada) {
        this(tipo, disponivel, atual, visitada, 82);
    }

    IconeSala(Expedicao.TipoSala tipo, boolean disponivel, boolean atual, boolean visitada, int tamanho) {
        this(tipo, disponivel, atual, visitada, tamanho, true);
    }

    IconeSala(Expedicao.TipoSala tipo, boolean disponivel, boolean atual, boolean visitada,
              int tamanho, boolean revelada) {
        this.tamanho = tamanho;
        this.tipo = tipo;
        this.disponivel = disponivel;
        this.atual = atual;
        this.visitada = visitada;
        this.revelada = revelada;
    }

    @Override public int getIconWidth() { return tamanho; }
    @Override public int getIconHeight() { return tamanho; }

    @Override
    public void paintIcon(Component componente, Graphics graphics, int x, int y) {
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.translate(x, y);
            g.scale(tamanho / 82.0, tamanho / 82.0);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            g.setColor(new Color(8, 12, 24, 110));
            g.fillRoundRect(3, 5, 76, 76, 14, 14);
            Color pedra = atual ? new Color(135, 101, 42)
                    : disponivel ? new Color(69, 91, 73)
                    : visitada ? new Color(48, 77, 68) : new Color(47, 57, 78);
            g.setColor(pedra);
            g.fillRoundRect(2, 1, 76, 76, 14, 14);
            g.setColor(atual ? new Color(255, 215, 110)
                    : disponivel ? new Color(168, 210, 135)
                    : new Color(93, 110, 135));
            g.drawRoundRect(2, 1, 76, 76, 14, 14);
            g.translate(10, 9);
            if (revelada) desenharSala(g);
            else porta(g);
            g.translate(-10, -9);
            if (atual || disponivel) {
                g.setColor(atual ? new Color(255, 223, 120) : new Color(170, 223, 145));
                g.fillOval(34, 72, 12, 5);
            }
        } finally {
            g.dispose();
        }
    }

    private void desenharSala(Graphics2D g) {
        switch (tipo) {
            case COMBATE: espadas(g); break;
            case ELITE: monstro(g, true); break;
            case LOJA: tenda(g); break;
            case EVENTO: bandeira(g); break;
            case ACAMPAMENTO: fogueira(g); break;
            case CHEFE: monstro(g, false); cor(g, "#ffd46b"); pixel(g, 5, 0); pixel(g, 6, 0); pixel(g, 7, 0); break;
            case ENTRADA: portal(g); break;
            default: throw new IllegalArgumentException("Sala sem sprite.");
        }

    }

    private void porta(Graphics2D g) {
        cor(g, "#171923");
        bloco(g, 3, 3, 6, 8);
        cor(g, "#89909c");
        bloco(g, 2, 3, 1, 8); bloco(g, 9, 3, 1, 8);
        bloco(g, 3, 2, 1, 1); bloco(g, 8, 2, 1, 1);
        bloco(g, 4, 1, 4, 1);
        cor(g, "#53596b");
        bloco(g, 1, 5, 1, 1); bloco(g, 10, 5, 1, 1);
        bloco(g, 1, 8, 1, 1); bloco(g, 10, 8, 1, 1);
        cor(g, "#342f3d");
        bloco(g, 4, 4, 4, 7);
        cor(g, "#171923");
        bloco(g, 5, 4, 1, 7); bloco(g, 7, 4, 1, 7);
        cor(g, "#c4a66a");
        pixel(g, 6, 7);
        cor(g, "#e69049");
        pixel(g, 0, 4); pixel(g, 11, 4);
        cor(g, "#ffe4a0");
        pixel(g, 0, 3); pixel(g, 11, 3);
        cor(g, "#85848b");
        bloco(g, 2, 11, 8, 1);
    }

    private void espadas(Graphics2D g) {
        cor(g, "#e7edf5");
        for (int i = 0; i < 7; i++) pixel(g, 2 + i, 2 + i);
        for (int i = 0; i < 7; i++) pixel(g, 9 - i, 2 + i);
        cor(g, "#f0bd5a");
        bloco(g, 2, 8, 3, 1); bloco(g, 7, 8, 3, 1);
        cor(g, "#81563b");
        bloco(g, 0, 10, 2, 1); bloco(g, 10, 10, 2, 1);
        cor(g, CONTORNO);
        pixel(g, 2, 2); pixel(g, 9, 2);
    }

    private void monstro(Graphics2D g, boolean elite) {
        cor(g, elite ? "#93c47d" : "#c86b63");
        bloco(g, 2, 3, 8, 6); bloco(g, 3, 2, 6, 8);
        bloco(g, 1, 4, 2, 3); bloco(g, 9, 4, 2, 3);
        cor(g, elite ? "#d4ed9e" : "#f4cf67");
        bloco(g, 3, 0, 2, 3); bloco(g, 7, 0, 2, 3);
        cor(g, "#f6e8cd"); pixel(g, 4, 5); pixel(g, 7, 5);
        cor(g, "#9d3e48"); bloco(g, 5, 7, 2, 1);
        cor(g, CONTORNO); bloco(g, 2, 9, 3, 1); bloco(g, 7, 9, 3, 1);
    }

    private void tenda(Graphics2D g) {
        cor(g, "#dfc38d");
        bloco(g, 1, 6, 10, 1); bloco(g, 2, 5, 8, 1); bloco(g, 3, 4, 6, 1);
        bloco(g, 4, 3, 4, 1); bloco(g, 5, 2, 2, 1);
        cor(g, "#a75846");
        bloco(g, 2, 6, 4, 1); bloco(g, 3, 5, 3, 1); bloco(g, 4, 4, 2, 1);
        cor(g, "#70513c");
        bloco(g, 5, 6, 2, 4); bloco(g, 1, 10, 10, 1);
        cor(g, "#e9c25e"); bloco(g, 5, 3, 2, 1);
    }

    private void bandeira(Graphics2D g) {
        cor(g, "#d5dbea");
        bloco(g, 3, 1, 1, 10); bloco(g, 2, 10, 3, 1);
        cor(g, "#d05b65");
        bloco(g, 4, 2, 5, 1); bloco(g, 4, 3, 4, 1); bloco(g, 4, 4, 5, 1);
        bloco(g, 4, 5, 3, 1);
        cor(g, "#f0c66b"); pixel(g, 9, 2); pixel(g, 8, 3);
    }

    private void fogueira(Graphics2D g) {
        cor(g, "#8d6343");
        bloco(g, 2, 9, 8, 1); bloco(g, 3, 8, 6, 1);
        cor(g, "#f08a45");
        bloco(g, 4, 5, 4, 3); bloco(g, 5, 3, 2, 2);
        cor(g, "#ffd45c");
        bloco(g, 5, 5, 2, 3); pixel(g, 6, 4);
        cor(g, "#7ca66b");
        pixel(g, 2, 7); pixel(g, 9, 6); pixel(g, 1, 5); pixel(g, 10, 8);
    }

    private void portal(Graphics2D g) {
        cor(g, "#8794b5");
        bloco(g, 2, 2, 2, 9); bloco(g, 8, 2, 2, 9); bloco(g, 3, 1, 6, 2);
        bloco(g, 1, 10, 10, 1);
        cor(g, "#78d3e8");
        bloco(g, 4, 4, 4, 6); bloco(g, 5, 3, 2, 1);
        cor(g, "#c8f2f5"); pixel(g, 5, 5); pixel(g, 6, 7);
    }

    private void cor(Graphics2D g, Color color) { g.setColor(color); }
    private void cor(Graphics2D g, String color) { g.setColor(Color.decode(color)); }

    private void pixel(Graphics2D g, int x, int y) {
        g.fillRect(x * ESCALA, y * ESCALA, ESCALA, ESCALA);
    }

    private void bloco(Graphics2D g, int x, int y, int largura, int altura) {
        g.fillRect(x * ESCALA, y * ESCALA, largura * ESCALA, altura * ESCALA);
    }
}
