package desafio1.personagens;

import desafio1.interfaces.Restauravel;

public abstract class Personagem implements Restauravel, java.io.Serializable {
    private static final long serialVersionUID = 1L;
    public enum TipoAtaque { FISICO, MAGICO, PERFURANTE }

    protected final String nome;
    protected final String classe;
    protected int nivel;
    protected int forca;
    protected int destreza;
    protected int constituicao;
    protected int inteligencia;
    protected int hpAtual;
    protected int manaAtual;
    private int bonusAtaque;
    private int bonusManaMaxima;
    private int bonusVidaMaxima;

    public Personagem(String nome, String classe, int nivel, int forca, int destreza,
                      int constituicao, int inteligencia) {
        if (nome == null || nome.isBlank() || classe == null || classe.isBlank()) {
            throw new IllegalArgumentException("Nome e classe são obrigatórios.");
        }
        this.nome = nome.trim();
        this.classe = classe;
        this.nivel = nivel;
        this.forca = forca;
        this.destreza = destreza;
        this.constituicao = constituicao;
        this.inteligencia = inteligencia;
        this.hpAtual = getHPMaximo();
        this.manaAtual = getManaMaxima();
    }

    public String getNome() { return nome; }
    public String getClasse() { return classe; }
    public int getNivel() { return nivel; }
    public int getForca() { return forca; }
    public int getDestreza() { return destreza; }
    public int getConstituicao() { return constituicao; }
    public int getInteligencia() { return inteligencia; }
    public int getHPMaximo() { return 50 + constituicao * 10 + bonusVidaMaxima; }
    public int getHpAtual() { return hpAtual; }
    public int getManaMaxima() { return 20 + inteligencia * 10 + bonusManaMaxima; }
    public int getManaAtual() { return manaAtual; }
    public int getDefesa() { return constituicao + destreza / 2; }
    public int getPoder() { return poderPorAtributo() + bonusAtaque; }

    protected abstract int poderPorAtributo();
    protected abstract String descricaoAtaque();
    protected abstract void crescer();
    public abstract String getNomeHabilidade();
    public abstract String getNomeLimite();
    public abstract int getCustoHabilidade();
    public abstract TipoAtaque getTipoAtaque();

    public int receberDano(int dano) {
        if (dano < 0) {
            throw new IllegalArgumentException("Dano não pode ser negativo.");
        }
        int recebido = Math.min(hpAtual, dano);
        hpAtual -= recebido;
        return recebido;
    }

    public void ataqueNormal(Personagem alvo) {
        int dano = alvo.receberDano(Math.max(1, getPoder() - alvo.getDefesa()));
        System.out.println(nome + descricaoAtaque() + alvo.getNome() + ". Dano: " + dano);
    }

    public boolean gastarMana(int valor) {
        if (valor < 0) {
            throw new IllegalArgumentException("Custo não pode ser negativo.");
        }
        if (manaAtual < valor) {
            return false;
        }
        manaAtual -= valor;
        return true;
    }

    public boolean estaVivo() { return hpAtual > 0; }

    public void upar() {
        int hpAnterior = getHPMaximo();
        int manaAnterior = getManaMaxima();
        nivel++;
        crescer();
        hpAtual += getHPMaximo() - hpAnterior;
        manaAtual += getManaMaxima() - manaAnterior;
    }

    public void melhorarAtaque() { bonusAtaque += 4; }

    /** Bônus escolhidos ao subir de nível. */
    public void treinarPoder(int valor) { bonusAtaque += valor; }

    public void treinarVida(int valor) {
        bonusVidaMaxima += valor;
        hpAtual += valor;
    }

    public void treinarMana(int valor) {
        bonusManaMaxima += valor;
        manaAtual += valor;
    }

    public void treinarDestreza(int valor) {
        int hpAnterior = getHPMaximo();
        int manaAnterior = getManaMaxima();
        destreza += valor;
        hpAtual += getHPMaximo() - hpAnterior;
        manaAtual += getManaMaxima() - manaAnterior;
    }

    public void melhorarMana() {
        bonusManaMaxima += 20;
        manaAtual += 20;
    }

    public void melhorarVitalidade() {
        constituicao += 2;
        hpAtual += 20;
    }

    public void exibeStatus() {
        System.out.println(nome + " (" + classe + ") - Nível " + nivel
                + " | HP " + hpAtual + "/" + getHPMaximo()
                + " | Mana " + manaAtual + "/" + getManaMaxima());
    }

    @Override
    public void restaurarVida(int valor) {
        if (valor <= 0 || !estaVivo()) {
            throw new IllegalArgumentException("Restauração inválida.");
        }
        hpAtual = (int) Math.min((long) getHPMaximo(), (long) hpAtual + valor);
    }

    @Override
    public void restaurarMana(int valor) {
        if (valor <= 0 || !estaVivo()) {
            throw new IllegalArgumentException("Restauração inválida.");
        }
        manaAtual = (int) Math.min((long) getManaMaxima(), (long) manaAtual + valor);
    }
}
