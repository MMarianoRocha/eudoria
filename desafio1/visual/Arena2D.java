package desafio1.visual;

import desafio1.jogo.Inimigo;
import desafio1.jogo.Jogo;
import desafio1.personagens.Personagem;

import javax.swing.JPanel;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.IntConsumer;

public final class Arena2D extends JPanel {
    private static final int LARGURA = 800;
    private static final int ALTURA = 360;
    private static final int DURACAO = 16;
    private final Sprites sprites = new Sprites();
    private final IntConsumer selecionarAlvo;
    private final List<Efeito> efeitos = new ArrayList<>();
    private Jogo jogo;
    private int onda = -1;
    private int selecionado = -1;
    private int vidaHeroi;
    private int[] vidas = new int[0];
    private int[] barras = new int[0];
    private boolean[] skills = new boolean[0];
    private int quadro;

    public Arena2D(IntConsumer selecionarAlvo) {
        this.selecionarAlvo = Objects.requireNonNull(selecionarAlvo);
        setPreferredSize(new Dimension(640, 288));
        setMinimumSize(new Dimension(320, 144));
        setBackground(new Color(12, 18, 35));
        setToolTipText("Clique em um inimigo vivo para selecionar o alvo.");
        getAccessibleContext().setAccessibleName("Arena 2D de combate");
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evento) {
                if (jogo == null || evento.getButton() != MouseEvent.BUTTON1) return;
                Point ponto = paraArena(evento.getPoint());
                for (int i = 0; i < jogo.getInimigos().size(); i++) {
                    if (jogo.getInimigos().get(i).estaVivo() && areaInimigo(i).contains(ponto)) {
                        selecionarAlvo.accept(i);
                        return;
                    }
                }
            }
        });
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    public void atualizar(Jogo novoJogo, int alvo) {
        Objects.requireNonNull(novoJogo);
        if (jogo != novoJogo || onda != novoJogo.getOnda()) {
            jogo = novoJogo;
            onda = jogo.getOnda();
            efeitos.clear();
            vidaHeroi = jogo.getHeroi().getHpAtual();
            int quantidade = jogo.getInimigos().size();
            vidas = new int[quantidade];
            barras = new int[quantidade];
            skills = new boolean[quantidade];
            for (int i = 0; i < quantidade; i++) vidas[i] = jogo.getInimigos().get(i).getVida();
        }
        selecionado = alvo;
        int vidaAtual = jogo.getHeroi().getHpAtual();
        if (vidaAtual != vidaHeroi) efeitos.add(new Efeito(-1, vidaHeroi - vidaAtual, false, false, false));
        vidaHeroi = vidaAtual;
        for (int i = 0; i < vidas.length; i++) {
            Inimigo inimigo = jogo.getInimigos().get(i);
            if (inimigo.getVida() != vidas[i]) efeitos.add(new Efeito(i, vidas[i] - inimigo.getVida(), false, false, false));
            if (inimigo.getAtb() < barras[i]) efeitos.add(new Efeito(i, 0, true, skills[i], true));
            vidas[i] = inimigo.getVida();
            barras[i] = inimigo.getAtb();
            skills[i] = inimigo.habilidadePronta();
        }
        repaint();
    }

    public void animarAtaque(int alvo, boolean habilidade) {
        if (jogo == null || alvo < 0 || alvo >= jogo.getInimigos().size()) {
            throw new IllegalArgumentException("Alvo da animação inválido.");
        }
        efeitos.add(new Efeito(alvo, 0, true, habilidade, false));
    }

    public void avancarAnimacao() {
        quadro++;
        for (Efeito efeito : efeitos) efeito.tempo--;
        efeitos.removeIf(efeito -> efeito.tempo <= 0);
        repaint();
    }

    private Point posicaoInimigo(int indice) {
        int quantidade = jogo.getInimigos().size();
        if (quantidade == 1) return new Point(600, 240);
        if (quantidade == 2) return new Point(530 + indice * 150, 200 + indice * 70);
        return new Point(465 + indice * 140, indice == 1 ? 290 : 185);
    }

    private Rectangle areaInimigo(int indice) {
        Point p = posicaoInimigo(indice);
        return new Rectangle(p.x - 58, p.y - 130, 116, 145);
    }

    private double escala() {
        return Math.min(getWidth() / (double) LARGURA, getHeight() / (double) ALTURA);
    }

    private Point paraArena(Point ponto) {
        double escala = escala();
        if (escala <= 0) return new Point(-1, -1);
        return new Point((int) ((ponto.x - (getWidth() - LARGURA * escala) / 2) / escala),
                (int) ((ponto.y - (getHeight() - ALTURA * escala) / 2) / escala));
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            double escala = escala();
            g.translate((getWidth() - LARGURA * escala) / 2, (getHeight() - ALTURA * escala) / 2);
            g.scale(escala, escala);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            desenharCenario(g);
            if (jogo == null) return;
            Personagem heroi = jogo.getHeroi();
            desenharCombatente(g, sprites.heroi(heroi.getClasse()), new Point(170, 250),
                    -1, heroi.estaVivo(), false);
            g.setColor(Color.WHITE);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
            centralizar(g, heroi.getClasse() + " • Nv. " + heroi.getNivel(), 170, 120);
            desenharBarra(g, 112, 130, 116, heroi.getHpAtual(), heroi.getHPMaximo(), new Color(52, 211, 153));
            desenharBarra(g, 112, 139, 116, jogo.getAtb(), 100, new Color(251, 191, 36));
            desenharBarra(g, 112, 148, 116, jogo.getLimite(), Jogo.LIMITE_MAXIMO,
                    jogo.limitePronto() && quadro % 10 < 5 ? Color.WHITE : new Color(239, 68, 68));
            if (jogo.limitePronto()) {
                g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
                g.setColor(new Color(254, 202, 202));
                centralizar(g, "LIMIT!", 250, 155);
            }
            if (jogo.estaDefendendo()) {
                g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
                g.setColor(new Color(147, 197, 253));
                centralizar(g, "DEFESA -" + jogo.getReducaoDefesa() + "%", 170, 167);
                int[] x = {213, 237, 237, 225, 213};
                int[] y = {205, 205, 225, 237, 225};
                g.setColor(new Color(59, 130, 246, 190));
                g.fillPolygon(x, y, x.length);
                g.setColor(new Color(191, 219, 254));
                g.drawPolygon(x, y, x.length);
            }
            for (int i = 0; i < jogo.getInimigos().size(); i++) desenharInimigo(g, i);
            for (Efeito efeito : efeitos) desenharEfeito(g, efeito);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
            g.setColor(new Color(225, 235, 255));
            String legenda;
            switch (jogo.getEstado()) {
                case ESCOLHA: legenda = "SUA VEZ — selecione o alvo e escolha um comando"; break;
                case INTERVALO: legenda = onda == 0 ? "Prepare os suprimentos para entrar na arena" : "VITÓRIA — prepare-se para a próxima onda"; break;
                case DERROTA: legenda = "DERROTA — comece uma nova jornada"; break;
                default: legenda = "BATALHA — barras ATB carregando";
            }
            centralizar(g, legenda, LARGURA / 2, 338);
        } finally {
            g.dispose();
        }
    }

    private void desenharCenario(Graphics2D g) {
        g.setPaint(new GradientPaint(0, 0, new Color(22, 28, 65), 0, 270, new Color(70, 60, 95)));
        g.fillRect(0, 0, LARGURA, ALTURA);
        g.setColor(new Color(130, 155, 180));
        g.fillOval(660, 20, 40, 40);
        g.setColor(new Color(32, 40, 65));
        g.fillPolygon(new int[]{0, 80, 160, 270, 380, 480, 620, 720, 800, 800, 0},
                new int[]{160, 85, 145, 60, 160, 100, 160, 80, 150, 240, 240}, 11);
        g.setColor(new Color(50, 65, 70));
        g.fillRect(0, 210, LARGURA, 150);
        g.setColor(new Color(62, 78, 78));
        for (int y = 220; y < 330; y += 26) {
            for (int x = (y / 26 % 2) * -40; x < 800; x += 80) g.fillRect(x, y, 76, 22);
        }
        g.setColor(new Color(21, 29, 43));
        g.fillRect(0, 316, LARGURA, 44);
        for (int x : new int[]{30, 760}) {
            g.setColor(new Color(80, 90, 105));
            g.fillRect(x, 90, 14, 130);
            g.fillRect(x - 6, 85, 26, 12);
            g.fillRect(x - 6, 210, 26, 12);
            g.setColor(new Color(255, 170 + quadro % 3 * 20, 60));
            g.fillOval(x - 2, 67, 18, 24);
        }
    }

    private void desenharInimigo(Graphics2D g, int indice) {
        Inimigo inimigo = jogo.getInimigos().get(indice);
        Point p = posicaoInimigo(indice);
        if (selecionado == indice && inimigo.estaVivo()) {
            g.setColor(new Color(250, 210, 85));
            g.setStroke(new BasicStroke(2));
            g.drawOval(p.x - 40, p.y - 10, 80, 20);
            g.fillPolygon(new int[]{p.x - 7, p.x + 7, p.x}, new int[]{p.y - 143, p.y - 143, p.y - 135}, 3);
        }
        desenharCombatente(g, sprites.inimigo(inimigo.getTipo()), p, indice, inimigo.estaVivo(),
                inimigo.isChefe());
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        g.setColor(inimigo.estaVivo() ? Color.WHITE : Color.LIGHT_GRAY);
        centralizar(g, inimigo.getNome() + " • Nv." + inimigo.getNivel(), p.x, p.y - 120);
        desenharBarra(g, p.x - 52, p.y - 113, 104, inimigo.getVida(), inimigo.getVidaMaxima(), new Color(248, 113, 113));
        desenharBarra(g, p.x - 52, p.y - 104, 104, inimigo.getAtb(), 100,
                inimigo.isAtordoado() ? new Color(148, 163, 184) : new Color(251, 191, 36));
        if (inimigo.estaVivo()) {
            boolean habilidade = inimigo.habilidadePronta();
            String intencao = (habilidade ? inimigo.getNomeHabilidade() : "Ataque") + " • "
                    + jogo.previsaoDano(inimigo);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
            int largura = g.getFontMetrics().stringWidth(intencao) + 12;
            g.setColor(habilidade ? new Color(120, 40, 20, 220) : new Color(15, 23, 42, 200));
            g.fillRoundRect(p.x - largura / 2, p.y + 11, largura, 15, 6, 6);
            g.setColor(habilidade ? new Color(255, 195, 100) : new Color(203, 213, 225));
            g.drawRoundRect(p.x - largura / 2, p.y + 11, largura, 15, 6, 6);
            centralizar(g, intencao, p.x, p.y + 22);
            int x = p.x - 52;
            if (inimigo.getVeneno() > 0) {
                x = desenharSelo(g, x, p.y - 97, "VEN x" + inimigo.getAcumulosVeneno() + " " + inimigo.getDanoVeneno(),
                        new Color(101, 163, 13));
            }
            if (inimigo.getQueimadura() > 0) {
                x = desenharSelo(g, x, p.y - 97, "QUE " + inimigo.getDanoQueimadura(), new Color(234, 88, 12));
            }
            if (inimigo.isAtordoado()) desenharSelo(g, x, p.y - 97, "ATORD.", new Color(202, 138, 4));
            if (inimigo.isAtordoado()) {
                g.setColor(new Color(253, 224, 71));
                for (int estrela = 0; estrela < 3; estrela++) {
                    double angulo = quadro * 0.2 + estrela * Math.PI * 2 / 3;
                    int ex = p.x + (int) (Math.cos(angulo) * 22);
                    int ey = p.y - 88 + (int) (Math.sin(angulo) * 5);
                    g.fillRect(ex - 2, ey, 5, 1);
                    g.fillRect(ex, ey - 2, 1, 5);
                }
            }
        }
    }

    private static int desenharSelo(Graphics2D g, int x, int y, String texto, Color cor) {
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 9));
        int largura = g.getFontMetrics().stringWidth(texto) + 6;
        g.setColor(cor);
        g.fillRoundRect(x, y, largura, 11, 4, 4);
        g.setColor(Color.WHITE);
        g.drawString(texto, x + 3, y + 9);
        return x + largura + 3;
    }

    private void desenharCombatente(Graphics2D g, BufferedImage sprite, Point p, int indice, boolean vivo, boolean chefe) {
        int tamanho = chefe ? 5 : 4;
        int deslocamento = 0;
        boolean atingido = false;
        for (Efeito efeito : efeitos) {
            if (efeito.alvo == indice && !efeito.ataque && efeito.valor > 0) atingido = true;
            if (efeito.ataque && (indice == -1 && !efeito.inimigo || indice == efeito.alvo && efeito.inimigo)) {
                deslocamento = (int) (Math.sin((DURACAO - efeito.tempo) * Math.PI / DURACAO) * 24)
                        * (indice == -1 ? 1 : -1);
            }
        }
        g.setColor(new Color(0, 0, 0, 80));
        g.fillOval(p.x - 32, p.y - 6, 64, 15);
        if (!vivo) g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));
        int oscilacao = vivo ? (int) (Math.sin(quadro * 0.18 + indice) * 2) : 0;
        if (atingido) deslocamento += quadro % 2 == 0 ? 4 : -4;
        g.drawImage(sprite, p.x - 8 * tamanho + deslocamento, p.y - 20 * tamanho + oscilacao,
                16 * tamanho, 20 * tamanho, null);
        g.setComposite(AlphaComposite.SrcOver);
    }

    private void desenharEfeito(Graphics2D g, Efeito efeito) {
        Point alvo = efeito.alvo == -1 ? new Point(170, 250) : posicaoInimigo(efeito.alvo);
        float progresso = (DURACAO - efeito.tempo) / (float) DURACAO;
        if (efeito.ataque) {
            Point origem = efeito.inimigo ? alvo : new Point(170, 250);
            Point destino = efeito.inimigo ? new Point(170, 250) : alvo;
            int x = (int) (origem.x + (destino.x - origem.x) * progresso);
            int y = (int) (origem.y + (destino.y - origem.y) * progresso) - 40;
            g.setColor(efeito.habilidade ? new Color(150, 125, 255) : new Color(255, 220, 130));
            g.setStroke(new BasicStroke(4));
            if (efeito.habilidade) {
                g.fillOval(x - 10, y - 10, 20, 20);
                g.drawOval(x - 16, y - 16, 32, 32);
            } else {
                g.drawLine(x - 10, y + 10, x + 10, y - 10);
            }
        } else {
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 22));
            String texto = efeito.valor > 0 ? "-" + efeito.valor : "+" + -efeito.valor;
            int y = alvo.y - 75 - (int) (progresso * 40);
            g.setColor(new Color(15, 20, 30));
            centralizar(g, texto, alvo.x + 1, y + 1);
            g.setColor(efeito.valor > 0 ? new Color(255, 220, 130) : new Color(90, 255, 170));
            centralizar(g, texto, alvo.x, y);
        }
    }

    private static void desenharBarra(Graphics2D g, int x, int y, int largura, int valor, int maximo, Color cor) {
        g.setColor(new Color(15, 20, 30));
        g.fillRect(x, y, largura, 7);
        g.setColor(cor);
        g.fillRect(x + 1, y + 1, (int) ((largura - 2) * (valor / (double) maximo)), 5);
    }

    private static void centralizar(Graphics2D g, String texto, int x, int y) {
        g.drawString(texto, x - g.getFontMetrics().stringWidth(texto) / 2, y);
    }

    private static final class Efeito {
        private final int alvo;
        private final int valor;
        private final boolean ataque;
        private final boolean habilidade;
        private final boolean inimigo;
        private int tempo = DURACAO;

        Efeito(int alvo, int valor, boolean ataque, boolean habilidade, boolean inimigo) {
            this.alvo = alvo;
            this.valor = valor;
            this.ataque = ataque;
            this.habilidade = habilidade;
            this.inimigo = inimigo;
        }
    }
}
