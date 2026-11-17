package desafio1.personagens;

public class Ladino extends Personagem {
    private static final long serialVersionUID = 1L;

    private static final int FORCA_INICIAL = 7;
    private static final int DESTREZA_INICIAL = 10;
    private static final int CONSTITUICAO_INICIAL = 6;
    private static final int INTELIGENCIA_INICIAL = 4;

    public Ladino(String nome) {
        super(
                nome,
                "Ladino",
                1,
                FORCA_INICIAL,
                DESTREZA_INICIAL,
                CONSTITUICAO_INICIAL,
                INTELIGENCIA_INICIAL
        );
    }

    @Override
    protected int poderPorAtributo() {
        return getDestreza() * 2;
    }

    @Override
    protected String descricaoAtaque() {
        return " usando agilidade, atacou: ";
    }

    @Override
    protected void crescer() {
        forca++;
        destreza += 2;
        constituicao += 2;
        inteligencia++;
    }

    @Override
    public String getNomeHabilidade() { return "Lâmina veloz"; }

    @Override
    public String getNomeLimite() { return "Mil cortes"; }

    @Override
    public int getCustoHabilidade() { return 15; }

    @Override
    public TipoAtaque getTipoAtaque() { return TipoAtaque.PERFURANTE; }
}