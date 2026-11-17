package desafio1.visual;

import desafio1.jogo.Jogo;
import desafio1.jogo.RegistroRun;
import desafio1.jogo.Expedicao;
import desafio1.personagens.Guerreiro;
import desafio1.personagens.Ladino;
import desafio1.personagens.Mago;
import desafio1.personagens.Personagem;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.Point;
import java.awt.Rectangle;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/** Executar com ambiente grafico; nao abre janela nem grava partidas. */
public final class TesteLayoutCombate {
    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                testar();
            } catch (ReflectiveOperationException erro) {
                throw new AssertionError(erro);
            }
        });
        System.out.println("Testes do layout de combate passaram.");
    }

    private static void testar() throws ReflectiveOperationException {
        JanelaJogo janela = new JanelaJogo();
        try {
            Method preparar = JanelaJogo.class.getDeclaredMethod("prepararPartida",
                    Jogo.class, Expedicao.class, RegistroRun.class);
            preparar.setAccessible(true);
            for (Personagem heroi : new Personagem[]{
                    new Guerreiro("Teste"), new Mago("Teste"), new Ladino("Teste")}) {
                Jogo jogo = new Jogo(heroi);
                // Sem registro, o teste nao escreve no salvamento do jogador.
                preparar.invoke(janela, jogo, null, null);
                for (int[] tamanho : new int[][]{{1050, 800}, {1280, 900}}) {
                    janela.setSize(tamanho[0], tamanho[1]);
                    organizar(janela);
                    verificarBotoes(janela);
                }
                jogo.proximaOnda();
                while (jogo.getEstado() == Jogo.Estado.COMBATE) jogo.avancarTempo();
                preparar.invoke(janela, jogo, null, null);
                organizar(janela);
                verificarBotoes(janela);
                verificarDefesa(janela, jogo);
            }
            Expedicao expedicao = new Expedicao(new Guerreiro("Defesa"), 11);
            expedicao.sairDaLoja();
            expedicao.selecionar(expedicao.getSalaAtual().getSaidas().get(0));
            Jogo jogo = expedicao.getJogo();
            while (jogo.getEstado() == Jogo.Estado.COMBATE) jogo.avancarTempo();
            preparar.invoke(janela, jogo, expedicao, null);
            verificarDefesa(janela, jogo);
        } finally {
            janela.dispose();
        }
    }

    private static void verificarDefesa(Container raiz, Jogo jogo) {
        List<JButton> botoes = new ArrayList<>();
        coletarBotoes(raiz, botoes);
        JButton defender = botoes.stream().filter(b -> "Defender".equals(b.getText()))
                .findFirst().orElseThrow(AssertionError::new);
        verificar(defender.isEnabled(), "Defender deve estar disponivel na vez do heroi");
        verificar(defender.getToolTipText().contains(jogo.getReducaoDefesa() + "%")
                        && defender.getToolTipText().contains("CON " + jogo.getHeroi().getConstituicao()),
                "Dica de Defender deve mostrar a porcentagem e a constituicao atuais");
        defender.doClick();
        verificar(jogo.estaDefendendo() && jogo.getEstado() == Jogo.Estado.COMBATE && !defender.isEnabled(),
                "Botao Defender deve consumir a vez e ficar desabilitado durante o ATB");
        verificar(contemDefesaAtiva(raiz, jogo.getReducaoDefesa()),
                "HUD deve indicar a porcentagem real da protecao ativa");
    }

    private static boolean contemDefesaAtiva(Container raiz, int reducao) {
        for (Component filho : raiz.getComponents()) {
            if (!filho.isVisible()) continue;
            if (filho instanceof JLabel && ((JLabel) filho).getText() != null
                    && ((JLabel) filho).getText().contains("Defendendo")
                    && ((JLabel) filho).getText().contains("-" + reducao + "%")) return true;
            if (filho instanceof Container && contemDefesaAtiva((Container) filho, reducao)) return true;
        }
        return false;
    }

    private static void verificarBotoes(Container raiz) {
        List<JButton> botoes = new ArrayList<>();
        coletarBotoes(raiz, botoes);
        JButton ataque = botoes.stream().filter(b -> "Ataque".equals(b.getText()))
                .findFirst().orElseThrow(AssertionError::new);
        JScrollPane rolagem = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, ataque);
        verificar(rolagem != null, "Coluna de comandos precisa de rolagem quando nao cabe");
        Container coluna = (Container) rolagem.getViewport().getView();
        List<Rectangle> areas = new ArrayList<>();
        List<JButton> comandos = new ArrayList<>();
        coletarBotoes(coluna, comandos);
        verificar(comandos.size() == 12, "Todos os comandos e compras devem permanecer acessiveis");
        for (JButton botao : comandos) {
            Rectangle area = SwingUtilities.convertRectangle(botao.getParent(), botao.getBounds(), coluna);
            verificar(area.width >= botao.getPreferredSize().width
                            && area.height >= botao.getPreferredSize().height,
                    "Botao nao pode ser comprimido: " + botao.getText());
            for (Rectangle outra : areas) {
                verificar(!area.intersects(outra), "Botoes nao podem se sobrepor: " + botao.getText());
            }
            areas.add(area);
            botao.scrollRectToVisible(new Rectangle(botao.getSize()));
            Point ponto = SwingUtilities.convertPoint(botao, botao.getWidth() / 2,
                    botao.getHeight() / 2, rolagem.getViewport());
            verificar(rolagem.getViewport().contains(ponto), "Rolagem deve permitir acessar " + botao.getText());
        }
    }

    private static void coletarBotoes(Container pai, List<JButton> botoes) {
        for (Component filho : pai.getComponents()) {
            if (!filho.isVisible()) continue;
            if (filho instanceof JButton) botoes.add((JButton) filho);
            else if (filho instanceof Container) coletarBotoes((Container) filho, botoes);
        }
    }

    private static void organizar(Container pai) {
        pai.doLayout();
        for (Component filho : pai.getComponents()) {
            if (filho.isVisible() && filho instanceof Container) organizar((Container) filho);
        }
    }

    private static void verificar(boolean condicao, String mensagem) {
        if (!condicao) throw new AssertionError(mensagem);
    }
}
