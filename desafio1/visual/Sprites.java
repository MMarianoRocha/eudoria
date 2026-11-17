package desafio1.visual;

import desafio1.jogo.Inimigo;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.Map;

final class Sprites {
    private static final String[] HEROI = {
        "................",
        ".....OOOOOO.....",
        "....OHHHHHHO....",
        "....OHHHHHHO....",
        "....OSSSSSSO....",
        "....OSSOSSO.....",
        ".....OSSSO......",
        "....OOOOOOOO....",
        "...OCCCCCCCCO...",
        "..OSOCCLLCCOSO..",
        "..OSOCCCCCCOSO..",
        "..OOOCCCCCCOOO..",
        "....OCCCCCCO....",
        "....OOLLLLOO....",
        "....OBBOOBBO....",
        "....OBBOOBBO....",
        "...OOBBOOBBOO...",
        "...OOOOOOOOOO...",
        "................",
        "................"
    };
    private static final String[] MONSTRO = {
        "................",
        "...OO......OO...",
        "..OCCO....OCCO..",
        "..OCCOOOOOOCCO..",
        "...OCCCCCCCCO...",
        "..OCCCCCCCCCCO..",
        "..OCCLCCCCLCCO..",
        "..OCCOCCCCOCCO..",
        "...OCCCCCCCCO...",
        "....OOSSSSOO....",
        "...OCCCCCCCCO...",
        "..OCCCCCCCCCCO..",
        ".OCCOCCCCCCOCCO.",
        ".OCCOCCCCCCOCCO.",
        "..OOOCCCCCCOOO..",
        "....OCCCCCCO....",
        "....OCCOOCCO....",
        "...OCCO..OCCO...",
        "...OOO....OOO...",
        "................"
    };
    private static final String[] GOLEM = {
        "................",
        ".....OOOOOO.....",
        "....OCCCCCCO....",
        "....OCLCCLCO....",
        "....OCCCCCCO....",
        ".OOOOOOOOOOOOOO.",
        "OCCCCCCCCCCCCCCO",
        "OCCCCOCCCCOCCCCO",
        "OCCCCOCLLCOCCCCO",
        "OCCCCOCLLCOCCCCO",
        "OCCCCOCCCCOCCCCO",
        "OCCCCOCCCCOCCCCO",
        ".OOOOCCCCCCOOOO.",
        "....OCCCCCCO....",
        "....OOOOOOOO....",
        "...OCCCOOCCCO...",
        "...OCCCOOCCCO...",
        "..OCCCCOOCCCCO..",
        "..OOOOOOOOOOOO..",
        "................"
    };
    private static final String[] DUELISTA = {
        "................",
        ".....OOOO.......",
        "....OLLLLO......",
        "....OSSSSO......",
        "....OLOOLO......",
        ".....OSSO.......",
        "...OOOOOOOO.....",
        "..OCCCCCCCCO....",
        ".OCCOCCCCOCCO...",
        ".OCCOCCCCOCCO.O.",
        "..OOOCCCCCOO.LO.",
        "....OCCCCCO.LO..",
        "....OLLLLCOLO...",
        "....OOOOOOLO....",
        "....OCCOCCO.....",
        "....OCCOCCO.....",
        "...OCCO.OCCO....",
        "...OBBO.OBBO....",
        "...OOOO.OOOO....",
        "................"
    };
    private final BufferedImage guerreiro = humano(new Color(80, 150, 210), new Color(200, 215, 230), false, false);
    private final BufferedImage mago = humano(new Color(130, 90, 205), new Color(240, 190, 80), true, false);
    private final BufferedImage ladino = humano(new Color(65, 160, 115), new Color(65, 75, 90), false, true);
    private final Map<Inimigo.Tipo, BufferedImage> inimigos = new EnumMap<>(Inimigo.Tipo.class);

    Sprites() {
        inimigos.put(Inimigo.Tipo.SENTINELA, criar(MONSTRO, new Color(100, 145, 185), new Color(255, 170, 60)));
        inimigos.put(Inimigo.Tipo.ESPREITADOR, criar(MONSTRO, new Color(90, 170, 105), new Color(235, 245, 125)));
        inimigos.put(Inimigo.Tipo.ARAUTO, criar(MONSTRO, new Color(160, 95, 195), new Color(80, 235, 245)));
        inimigos.put(Inimigo.Tipo.GUARDIAO, criar(MONSTRO, new Color(195, 75, 65), new Color(255, 205, 70)));
        inimigos.put(Inimigo.Tipo.GOLEM, criar(GOLEM, new Color(120, 139, 158), new Color(255, 167, 69)));
        inimigos.put(Inimigo.Tipo.DUELISTA, criar(DUELISTA, new Color(173, 64, 88), new Color(220, 229, 244)));
    }

    BufferedImage heroi(String classe) {
        switch (classe) {
            case "Guerreiro": return guerreiro;
            case "Mago": return mago;
            case "Ladino": return ladino;
            default: throw new IllegalArgumentException("Classe sem sprite: " + classe);
        }
    }

    BufferedImage inimigo(Inimigo.Tipo tipo) { return inimigos.get(tipo); }

    private static BufferedImage humano(Color roupa, Color detalhe, boolean mago, boolean ladino) {
        BufferedImage imagem = criar(HEROI, roupa, detalhe);
        java.awt.Graphics2D g = imagem.createGraphics();
        try {
            if (mago) {
                g.setColor(roupa);
                g.fillRect(5, 0, 6, 3);
                g.fillRect(7, 0, 2, 1);
                g.fillRect(3, 3, 10, 1);
                g.setColor(new Color(160, 110, 60));
                g.fillRect(14, 6, 1, 12);
                g.setColor(new Color(90, 235, 255));
                g.fillRect(13, 5, 3, 3);
            } else {
                g.setColor(new Color(225, 240, 245));
                g.fillRect(14, ladino ? 9 : 5, 1, ladino ? 4 : 8);
                g.setColor(new Color(235, 185, 75));
                g.fillRect(13, 12, 3, 1);
                g.setColor(new Color(100, 65, 40));
                g.fillRect(14, 13, 1, 2);
            }
        } finally {
            g.dispose();
        }
        return imagem;
    }

    private static BufferedImage criar(String[] pixels, Color corpo, Color detalhe) {
        BufferedImage imagem = new BufferedImage(16, 20, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < pixels.length; y++) {
            for (int x = 0; x < pixels[y].length(); x++) {
                Color cor;
                switch (pixels[y].charAt(x)) {
                    case 'O': cor = new Color(25, 30, 45); break;
                    case 'C': cor = corpo; break;
                    case 'L': cor = detalhe; break;
                    case 'S': cor = new Color(245, 200, 155); break;
                    case 'H': cor = new Color(85, 65, 65); break;
                    case 'B': cor = new Color(65, 75, 95); break;
                    default: continue;
                }
                imagem.setRGB(x, y, cor.getRGB());
            }
        }
        return imagem;
    }
}
