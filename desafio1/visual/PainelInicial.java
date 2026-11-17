package desafio1.visual;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.GridBagLayout;
import java.awt.RenderingHints;

final class PainelInicial extends JPanel {
    private final JButton carregar;

    PainelInicial(Runnable novoJogo, Runnable carregarJogo, Runnable estatisticas, Runnable sair) {
        super(new BorderLayout(12, 12));
        setBackground(new Color(12, 16, 29));
        setBorder(new EmptyBorder(32, 48, 32, 48));
        JPanel titulos = new JPanel(new GridLayout(0, 1, 0, 8));
        titulos.setOpaque(false);
        JLabel titulo = texto("EUDORIA", 42, new Color(239, 209, 146));
        titulos.add(titulo);
        titulos.add(texto("Além dos portões, cada escolha tem um preço.", 17, new Color(190, 197, 212)));
        add(titulos, BorderLayout.NORTH);
        JComponent castelo = new Castelo();
        castelo.getAccessibleContext().setAccessibleName("Castelo de Eudoria sob a lua, com uma entrada de dungeon iluminada.");
        add(castelo, BorderLayout.CENTER);

        JPanel comandos = new JPanel(new GridLayout(0, 1, 0, 8));
        comandos.setOpaque(false);
        comandos.add(texto("Explore salas desconhecidas. Prepare-se. Sobreviva.", 15, new Color(190, 197, 212)));
        JButton jogar = new JButton("Novo jogo");
        jogar.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        jogar.setBackground(new Color(89, 66, 42));
        jogar.setForeground(new Color(255, 232, 183));
        jogar.setFocusPainted(false);
        jogar.addActionListener(e -> novoJogo.run());
        comandos.add(jogar);
        JPanel secundarios = new JPanel(new GridLayout(1, 0, 8, 0));
        secundarios.setOpaque(false);
        carregar = secundario("Carregar", carregarJogo);
        carregar.setToolTipText("Continuar uma run salva em andamento");
        secundarios.add(carregar);
        secundarios.add(secundario("Estatísticas", estatisticas));
        secundarios.add(secundario("Sair", sair));
        comandos.add(secundarios);
        comandos.setPreferredSize(new Dimension(460, 130));
        JPanel rodape = new JPanel(new GridBagLayout());
        rodape.setOpaque(false);
        rodape.add(comandos);
        add(rodape, BorderLayout.SOUTH);
    }

    void definirCarregarDisponivel(boolean disponivel) {
        carregar.setEnabled(disponivel);
    }

    private static JButton secundario(String texto, Runnable acao) {
        JButton botao = new JButton(texto);
        botao.setBackground(new Color(32, 41, 58));
        botao.setForeground(new Color(221, 218, 204));
        botao.setFocusPainted(false);
        botao.addActionListener(e -> acao.run());
        return botao;
    }

    private static JLabel texto(String texto, int tamanho, Color cor) {
        JLabel rotulo = new JLabel(texto, SwingConstants.CENTER);
        rotulo.setFont(new Font(Font.SERIF, Font.BOLD, tamanho));
        rotulo.setForeground(cor);
        return rotulo;
    }

    private static final class Castelo extends JPanel {
        Castelo() {
            setOpaque(false);
            setPreferredSize(new Dimension(960, 420));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                double escala = Math.min(getWidth() / 320.0, getHeight() / 140.0);
                g.translate((getWidth() - 320 * escala) / 2, (getHeight() - 140 * escala) / 2);
                g.scale(escala, escala);
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
                bloco(g, "#121c31", 0, 0, 320, 140);
                for (int i = 0; i < 38; i++) {
                    bloco(g, "#8993aa", (i * 47 + 13) % 320, (i * 19 + 7) % 65, 1, 1);
                }
                bloco(g, "#c7cfcd", 246, 14, 20, 20);
                bloco(g, "#e1ddc6", 250, 10, 12, 28);
                bloco(g, "#121c31", 257, 10, 12, 19);
                g.setColor(new Color(29, 39, 57));
                g.fillPolygon(new int[]{0, 40, 80, 119, 170, 218, 267, 320, 320, 0},
                        new int[]{91, 48, 83, 39, 87, 52, 80, 42, 140, 140}, 10);
                bloco(g, "#252a39", 0, 113, 320, 27);
                bloco(g, "#404653", 81, 69, 158, 53);
                torre(g, 78, 42);
                torre(g, 218, 42);
                torre(g, 148, 25);
                bloco(g, "#555d6a", 119, 57, 82, 65);
                for (int x = 119; x <= 191; x += 12) bloco(g, "#656d76", x, 51, 10, 10);
                for (int y = 70; y < 120; y += 10) {
                    for (int x = 120 + (y % 20 == 0 ? 8 : 0); x < 198; x += 16) {
                        bloco(g, "#454c5a", x, y, 13, 1);
                    }
                }
                bloco(g, "#292838", 143, 88, 34, 34);
                bloco(g, "#292838", 149, 81, 22, 8);
                bloco(g, "#111421", 148, 92, 24, 30);
                bloco(g, "#111421", 153, 87, 14, 7);
                bloco(g, "#514236", 153, 109, 14, 13);
                for (int y = 124; y < 140; y += 4) {
                    int margem = (y - 120) / 2;
                    bloco(g, "#66616a", 143 - margem, y, 34 + margem * 2, 2);
                }
                for (int x : new int[]{134, 183}) {
                    bloco(g, "#9d663b", x, 100, 3, 10);
                    bloco(g, "#eb9246", x - 1, 96, 5, 6);
                    bloco(g, "#ffe4a0", x, 94, 3, 6);
                }
                for (int i = 0; i < 9; i++) {
                    bloco(g, "#384251", i * 41 - 20, 116 + i % 3 * 5, 33, 2);
                }
            } finally {
                g.dispose();
            }
        }

        private static void torre(Graphics2D g, int x, int y) {
            bloco(g, "#69717c", x, y, 24, 80);
            bloco(g, "#444c5b", x + 17, y, 7, 80);
            for (int i = 0; i < 3; i++) bloco(g, "#858990", x - 2 + i * 10, y - 6, 8, 10);
            bloco(g, "#1a2030", x + 8, y + 18, 6, 13);
            bloco(g, "#d2a65f", x + 10, y + 20, 2, 8);
            bloco(g, "#858990", x + 11, y - 21, 1, 17);
            bloco(g, "#954852", x + 12, y - 21, 13, 7);
        }

        private static void bloco(Graphics2D g, String cor, int x, int y, int largura, int altura) {
            g.setColor(Color.decode(cor));
            g.fillRect(x, y, largura, altura);
        }
    }
}
