package testes;

import desafio1.jogo.Jogo;
import desafio1.personagens.Guerreiro;
import desafio1.personagens.Ladino;
import desafio1.personagens.Mago;
import desafio1.personagens.Personagem;
import desafio1.visual.Arena2D;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

public class TesteArena {
    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> testar(args));
        System.out.println("Testes da arena 2D passaram.");
    }

    private static void testar(String[] args) {
        AtomicInteger alvo = new AtomicInteger(-1);
        Arena2D arena = new Arena2D(alvo::set);
        for (Personagem personagem : new Personagem[]{new Guerreiro("Teste"), new Mago("Teste"), new Ladino("Teste")}) {
            Jogo jogo = new Jogo(personagem);
            arena.atualizar(jogo, -1);
            desenhar(arena, 800, 360);
            jogo.proximaOnda();
            arena.atualizar(jogo, 0);
            BufferedImage antes = desenhar(arena, 800, 360);
            clicar(arena, 600, 200);
            verificar(alvo.get() == 0, "Clique no sprite deve selecionar o inimigo");
            while (jogo.getEstado() == Jogo.Estado.COMBATE) jogo.avancarTempo();
            verificar(jogo.atacar(0, true), "Ataque de preparação");
            arena.animarAtaque(0, true);
            arena.atualizar(jogo, 0);
            BufferedImage depois = desenhar(arena, 800, 360);
            verificar(diferentes(antes, depois), "Dano deve mudar a cena");
            int hp = personagem.getHpAtual();
            int atb = jogo.getAtb();
            int inimigoHp = jogo.getInimigos().get(0).getVida();
            for (int i = 0; i < 4; i++) arena.avancarAnimacao();
            BufferedImage projetil = desenhar(arena, 800, 360);
            verificar(projetil.getRGB(277, 207) == new java.awt.Color(150, 125, 255).getRGB(),
                    "Projétil de habilidade deve avançar do herói até o inimigo");
            for (int i = 0; i < 20; i++) {
                arena.avancarAnimacao();
                desenhar(arena, 640, 288);
            }
            verificar(personagem.getHpAtual() == hp && jogo.getAtb() == atb
                            && jogo.getInimigos().get(0).getVida() == inimigoHp,
                    "Animação não deve modificar regras ou atributos do jogo");
        }

        Guerreiro heroi = new Guerreiro("Teste");
        for (int i = 0; i < 100; i++) {
            heroi.melhorarAtaque();
            heroi.melhorarVitalidade();
        }
        Jogo jogo = new Jogo(heroi);
        jogo.proximaOnda();
        while (jogo.getOnda() < 7) {
            arena.atualizar(jogo, 0);
            desenhar(arena, 800, 360);
            concluirOnda(jogo);
            arena.atualizar(jogo, 0);
            alvo.set(-1);
            clicar(arena, 600, 200);
            verificar(alvo.get() == -1, "Não deve selecionar inimigo derrotado");
            jogo.proximaOnda();
        }
        arena.atualizar(jogo, 0);
        desenhar(arena, 1000, 600);
        // A cena 800x360 ocupa 1000x450, com margem vertical de 75 pixels.
        clicar(arena, 745 * 5 / 4, 160 * 5 / 4 + 75);
        verificar(alvo.get() == 2, "Seleção deve respeitar escala e margens");
        alvo.set(-1);
        clicar(arena, 10, 10);
        verificar(alvo.get() == -1, "Clique fora da cena não deve mudar alvo");
        arena.atualizar(jogo, 2);
        BufferedImage cena = desenhar(arena, 1000, 450);
        if (args.length == 1) {
            try {
                ImageIO.write(cena, "png", new File(args[0]));
            } catch (IOException erro) {
                throw new AssertionError("Não foi possível salvar a prévia da arena", erro);
            }
        }
    }

    private static void concluirOnda(Jogo jogo) {
        for (int i = 0; i < 2000 && jogo.getEstado() != Jogo.Estado.INTERVALO; i++) {
            if (jogo.getEstado() == Jogo.Estado.COMBATE) jogo.avancarTempo();
            else {
                int indice = 0;
                while (!jogo.getInimigos().get(indice).estaVivo()) indice++;
                jogo.atacar(indice, false);
            }
        }
        verificar(jogo.getEstado() == Jogo.Estado.INTERVALO, "Preparação da próxima cena");
    }

    private static BufferedImage desenhar(Arena2D arena, int largura, int altura) {
        arena.setSize(largura, altura);
        BufferedImage imagem = new BufferedImage(largura, altura, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = imagem.createGraphics();
        try {
            arena.paint(g);
        } finally {
            g.dispose();
        }
        return imagem;
    }

    private static void clicar(Arena2D arena, int x, int y) {
        arena.dispatchEvent(new MouseEvent(arena, MouseEvent.MOUSE_CLICKED, 0, 0, x, y,
                1, false, MouseEvent.BUTTON1));
    }

    private static boolean diferentes(BufferedImage a, BufferedImage b) {
        for (int y = 0; y < a.getHeight(); y++) {
            for (int x = 0; x < a.getWidth(); x++) {
                if (a.getRGB(x, y) != b.getRGB(x, y)) return true;
            }
        }
        return false;
    }

    private static void verificar(boolean condicao, String mensagem) {
        if (!condicao) throw new AssertionError(mensagem);
    }
}
