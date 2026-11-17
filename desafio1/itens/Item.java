package desafio1.itens;

public abstract class Item {
    final String nome;
    protected double peso;
    
    public Item(String nome, double peso) {
        this.nome = nome;
        this.peso = peso;
    }
}