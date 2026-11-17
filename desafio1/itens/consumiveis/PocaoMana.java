package desafio1.itens.consumiveis;

import desafio1.interfaces.Usavel;
import desafio1.itens.Item;
import desafio1.personagens.Personagem;

public class PocaoMana extends Item implements Usavel {
    private static final int MANA = 30;

    public PocaoMana() {
        super("Poção de Mana", 1.2);
    }

    @Override
    public void usar(Personagem alvo) {
        alvo.restaurarMana(MANA);
    }
}
