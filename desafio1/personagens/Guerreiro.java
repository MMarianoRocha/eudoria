package desafio1.personagens;

public class Guerreiro extends Personagem {
    private static final long serialVersionUID = 1L;

    private static final int FORCA_INICIAL = 10;
    private static final int DESTREZA_INICIAL = 5;
    private static final int CONSTITUICAO_INICIAL = 10;
    private static final int INTELIGENCIA_INICIAL = 2;

    public Guerreiro(String nome) {
        super(
                nome,
                "Guerreiro",
                1,
                FORCA_INICIAL,
                DESTREZA_INICIAL,
                CONSTITUICAO_INICIAL,
                INTELIGENCIA_INICIAL
        );
    }

    @Override
    protected int poderPorAtributo() {
        return getForca() * 2;
    }

    @Override
    protected String descricaoAtaque() {
        return " usando toda sua força, atacou: ";
    }

    @Override
    protected void crescer() {
        forca += 2;
        destreza++;
        constituicao += 2;
        inteligencia++;
    }

    @Override
    public String getNomeHabilidade() { return "Golpe devastador"; }

    @Override
    public String getNomeLimite() { return "Fúria titânica"; }

    @Override
    public int getCustoHabilidade() { return 10; }

    @Override
    public TipoAtaque getTipoAtaque() { return TipoAtaque.FISICO; }
}