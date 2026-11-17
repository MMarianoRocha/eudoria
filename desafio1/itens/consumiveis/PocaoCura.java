package desafio1.itens.consumiveis;

import desafio1.interfaces.Usavel;
import desafio1.itens.Item;
import desafio1.personagens.Personagem;

public class PocaoCura extends Item implements Usavel {
    private static final int CURA = 45;

    public PocaoCura() {
        super("Poção de Cura", 1.2);
    }

    @Override
    public void usar(Personagem alvo) {
        alvo.restaurarVida(CURA);
    }
}
