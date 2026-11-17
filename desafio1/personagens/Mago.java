package desafio1.personagens;

public class Mago extends Personagem {
    private static final long serialVersionUID = 1L;

    private static final int FORCA_INICIAL = 2;
    private static final int DESTREZA_INICIAL = 4;
    private static final int CONSTITUICAO_INICIAL = 4;
    private static final int INTELIGENCIA_INICIAL = 10;

    public Mago(String nome) {
        super(
                nome,
                "Mago",
                1,
                FORCA_INICIAL,
                DESTREZA_INICIAL,
                CONSTITUICAO_INICIAL,
                INTELIGENCIA_INICIAL
        );
    }

    @Override
    protected int poderPorAtributo() {
        return getInteligencia() * 2;
    }

    @Override
    protected String descricaoAtaque() {
        return " usando disparo arcano, atacou: ";
    }

    @Override
    protected void crescer() {
        forca++;
        destreza++;
        constituicao += 2;
        inteligencia += 2;
    }

    @Override
    public String getNomeHabilidade() { return "Explosão arcana"; }

    @Override
    public String getNomeLimite() { return "Meteoro"; }

    @Override
    public int getCustoHabilidade() { return 22; }

    @Override
    public TipoAtaque getTipoAtaque() { return TipoAtaque.MAGICO; }
}