package desafio1.visual;

import javax.swing.Icon;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.util.Objects;

final class IconeRecurso implements Icon {
    enum Tipo { VIDA, MANA, XP }

    private static final String[] CORACAO = {
            "............",
            "..RRR.RRR...",
            ".RLLRRRRRR..",
            ".RLRRRRRRR..",
            ".RRRRRRRRR..",
            "..RRRRRRR...",
            "...RRRRR....",
            "....RRR.....",
            ".....R......",
            "............",
            "............",
            "............"
    };
    private static final String[] GOTA = {
            "............",
            ".....M......",
            "....MMM.....",
            "...MMMMM....",
            "..MLMMMMM...",
            "..MLMMMMM...",
            ".MMLMMMMMM..",
            ".MMMMMMMMM..",
            "..MMMMMMM...",
            "...MMMMM....",
            "............",
            "............"
    };
    private static final String[] ESTRELA = {
            "............",
            ".....X......",
            "....XXX.....",
            "....XLX.....",
            ".XXXXLXXXX..",
            "..XXLLXXX...",
            "...XXXXX....",
            "...XXXXX....",
            "..XXX.XXX...",
            "..XX...XX...",
            "............",
            "............"
    };
    private final Tipo tipo;

    IconeRecurso(Tipo tipo) { this.tipo = Objects.requireNonNull(tipo, "Tipo de recurso obrigatório."); }

    @Override public int getIconWidth() { return 24; }
    @Override public int getIconHeight() { return 24; }

    @Override
    public void paintIcon(Component componente, Graphics g, int x, int y) {
        String[] pixels = tipo == Tipo.VIDA ? CORACAO : tipo == Tipo.MANA ? GOTA : ESTRELA;
        Color cor = tipo == Tipo.VIDA ? new Color(248, 92, 113)
                : tipo == Tipo.MANA ? new Color(78, 165, 250) : new Color(202, 151, 255);
        Graphics desenho = g.create();
        try {
            for (int linha = 0; linha < pixels.length; linha++) {
                for (int coluna = 0; coluna < pixels[linha].length(); coluna++) {
                    char pixel = pixels[linha].charAt(coluna);
                    if (pixel == '.') continue;
                    desenho.setColor(pixel == 'L' ? new Color(238, 239, 255) : cor);
                    desenho.fillRect(x + coluna * 2, y + linha * 2, 2, 2);
                }
            }
        } finally {
            desenho.dispose();
        }
    }
}
