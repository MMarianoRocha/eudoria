package desafio1;

import desafio1.visual.JanelaJogo;

import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new JanelaJogo().iniciar());
    }
}
