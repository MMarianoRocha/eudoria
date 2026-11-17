package desafio1.visual;

import desafio1.jogo.Jogo;
import desafio1.personagens.Personagem;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/** Tela de LEVEL UP: o herói escolhe um bônus a cada nível alcançado. */
final class PainelNivel extends JPanel {
    private final JLabel titulo = texto("", 40, new Color(253, 224, 138));
    private final JLabel subtitulo = texto("", 17, new Color(203, 213, 225));
    private final Map<Jogo.BonusNivel, JButton> botoes = new EnumMap<>(Jogo.BonusNivel.class);

    PainelNivel(Consumer<Jogo.BonusNivel> escolher) {
        super(new BorderLayout(0, 24));
        Objects.requireNonNull(escolher);
        setBackground(new Color(12, 16, 29));
        setBorder(new EmptyBorder(48, 48, 48, 48));
        JPanel topo = new JPanel(new GridLayout(0, 1, 0, 8));
        topo.setOpaque(false);
        topo.add(titulo);
        topo.add(subtitulo);
        add(topo, BorderLayout.NORTH);

        JPanel cartas = new JPanel(new GridLayout(1, 0, 16, 0));
        cartas.setOpaque(false);
        for (Jogo.BonusNivel bonus : Jogo.BonusNivel.values()) {
            JButton botao = new JButton();
            botao.setIcon(new IconeBonus(bonus));
            botao.setVerticalTextPosition(SwingConstants.BOTTOM);
            botao.setHorizontalTextPosition(SwingConstants.CENTER);
            botao.setBackground(new Color(30, 38, 58));
            botao.setForeground(new Color(241, 245, 249));
            botao.setFocusPainted(false);
            botao.setPreferredSize(new Dimension(220, 250));
            botao.setBorder(new CompoundBorder(new LineBorder(cor(bonus), 2), new EmptyBorder(12, 8, 12, 8)));
            botao.addActionListener(e -> escolher.accept(bonus));
            botoes.put(bonus, botao);
            cartas.add(botao);
        }
        JPanel centro = new JPanel(new GridBagLayout());
        centro.setOpaque(false);
        centro.add(cartas);
        add(centro, BorderLayout.CENTER);
        JLabel dica = texto("Além do bônus escolhido, sua classe cresce automaticamente a cada nível.",
                14, new Color(148, 163, 184));
        add(dica, BorderLayout.SOUTH);
    }

    void mostrar(Jogo jogo) {
        Personagem heroi = jogo.getHeroi();
        titulo.setText("LEVEL UP!  Nível " + heroi.getNivel());
        subtitulo.setText(heroi.getNome() + " (" + heroi.getClasse() + ") • Escolha um bônus"
                + (jogo.getEscolhasNivel() > 1 ? " — " + jogo.getEscolhasNivel() + " escolhas restantes" : ""));
        for (Map.Entry<Jogo.BonusNivel, JButton> entrada : botoes.entrySet()) {
            Jogo.BonusNivel bonus = entrada.getKey();
            JButton botao = entrada.getValue();
            botao.setText("<html><center><font size='5'><b>" + bonus.getNome() + "</b></font><br>"
                    + bonus.getDescricao() + "<br><br><font color='#fde68a'>" + previa(bonus, heroi)
                    + "</font></center></html>");
            botao.getAccessibleContext().setAccessibleName(bonus.getNome() + ": " + bonus.getDescricao());
        }
    }

    JButton getBotao(Jogo.BonusNivel bonus) { return botoes.get(bonus); }
    String getTitulo() { return titulo.getText(); }

    private static String previa(Jogo.BonusNivel bonus, Personagem heroi) {
        switch (bonus) {
            case PODER: return "Poder " + heroi.getPoder() + " → " + (heroi.getPoder() + 3);
            case VIDA: return "Vida " + heroi.getHPMaximo() + " → " + (heroi.getHPMaximo() + 20);
            case DESTREZA: return "Destreza " + heroi.getDestreza() + " → " + (heroi.getDestreza() + 2);
            case MANA: return "Mana " + heroi.getManaMaxima() + " → " + (heroi.getManaMaxima() + 15);
            default: throw new IllegalArgumentException("Bônus desconhecido.");
        }
    }

    private static Color cor(Jogo.BonusNivel bonus) {
        switch (bonus) {
            case PODER: return new Color(251, 146, 60);
            case VIDA: return new Color(248, 92, 113);
            case DESTREZA: return new Color(74, 222, 128);
            case MANA: return new Color(78, 165, 250);
            default: throw new IllegalArgumentException("Bônus desconhecido.");
        }
    }

    private static JLabel texto(String texto, int tamanho, Color cor) {
        JLabel rotulo = new JLabel(texto, SwingConstants.CENTER);
        rotulo.setFont(new Font(Font.SERIF, Font.BOLD, tamanho));
        rotulo.setForeground(cor);
        return rotulo;
    }

    /** Sprites 16x16 em pixel art: espada, coração, bota alada e gota de mana. */
    private static final class IconeBonus implements Icon {
        private static final String[] ESPADA = {
                "..............LL",
                ".............LCL",
                "............LCL.",
                "...........LCL..",
                "..........LCL...",
                ".........LCL....",
                "........LCL.....",
                ".......LCL......",
                "..G...LCL.......",
                "...G.LCL........",
                "....GCL.........",
                "....GG..........",
                "...DGGG.........",
                "..DD..G.........",
                ".DD.............",
                "DD.............."
        };
        private static final String[] CORACAO = {
                "................",
                "..CCCC....CCCC..",
                ".CLLCCC..CCCCCC.",
                "CLLCCCCCCCCCCCCC",
                "CLCCCCCCCCCCCCCC",
                "CCCCCCCCCCCCCCCC",
                "CCCCCCCCCCCCCCCC",
                ".CCCCCCCCCCCCCC.",
                "..CCCCCCCCCCCC..",
                "...CCCCCCCCCC...",
                "....CCCCCCCC....",
                ".....CCCCCC.....",
                "......CCCC......",
                ".......CC.......",
                "................",
                "................"
        };
        private static final String[] BOTA = {
                "................",
                ".L.L............",
                "LLLL.CCCCC......",
                ".LLLLCCCCC......",
                "..LL.CCCCC......",
                ".....CCCCC......",
                ".....CCCCC......",
                ".....CCCCC......",
                ".....CCCCCC.....",
                ".....CCCCCCC....",
                ".....CCCCCCCCC..",
                ".....CCCCCCCCCC.",
                ".....DDDDDDDDDD.",
                "................",
                "..LL..LLL.......",
                "................"
        };
        private static final String[] GOTA = {
                ".......C........",
                "......CCC.......",
                "......CCC.......",
                ".....CCCCC......",
                "....CCCCCCC.....",
                "....CLCCCCC.....",
                "...CLCCCCCCC....",
                "...CLCCCCCCC....",
                "..CCLCCCCCCCC...",
                "..CCCCCCCCCCC...",
                "..CCCCCCCCCCC...",
                "..CCCCCCCCCCC...",
                "...CCCCCCCCC....",
                "....CCCCCCC.....",
                ".....CCCCC......",
                "................"
        };
        private final Jogo.BonusNivel bonus;

        IconeBonus(Jogo.BonusNivel bonus) { this.bonus = bonus; }

        @Override public int getIconWidth() { return 80; }
        @Override public int getIconHeight() { return 80; }

        @Override
        public void paintIcon(Component componente, Graphics g, int x, int y) {
            String[] pixels;
            switch (bonus) {
                case PODER: pixels = ESPADA; break;
                case VIDA: pixels = CORACAO; break;
                case DESTREZA: pixels = BOTA; break;
                default: pixels = GOTA; break;
            }
            Color principal = bonus == Jogo.BonusNivel.PODER ? new Color(203, 213, 225) : cor(bonus);
            for (int linha = 0; linha < pixels.length; linha++) {
                for (int coluna = 0; coluna < pixels[linha].length(); coluna++) {
                    char pixel = pixels[linha].charAt(coluna);
                    if (pixel == '.') continue;
                    g.setColor(pixel == 'L' ? new Color(248, 250, 252)
                            : pixel == 'G' ? new Color(234, 179, 8)
                            : pixel == 'D' ? new Color(120, 72, 40) : principal);
                    g.fillRect(x + coluna * 5, y + linha * 5, 5, 5);
                }
            }
        }
    }
}
