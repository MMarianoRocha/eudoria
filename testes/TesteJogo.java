package testes;

import desafio1.jogo.Inimigo;
import desafio1.jogo.Jogo;
import desafio1.personagens.Guerreiro;
import desafio1.personagens.Ladino;
import desafio1.personagens.Mago;
import desafio1.personagens.Personagem;

public class TesteJogo {
    public static void main(String[] args) {
        testarAtbEAcoes();
        testarFraquezasEResistencias();
        testarPocoesEMana();
        testarRecompensaELoja();
        testarDerrota();
        testarEvolucaoEHabilidades();
        testarProgressao(new Guerreiro("Guerreiro"));
        testarProgressao(new Mago("Mago"));
        testarProgressao(new Ladino("Ladino"));
        testarSemSuprimentos(new Guerreiro("Guerreiro"));
        testarSemSuprimentos(new Mago("Mago"));
        testarSemSuprimentos(new Ladino("Ladino"));
        System.out.println("Todos os testes passaram.");
    }

    private static void testarFraquezasEResistencias() {
        desafio1.jogo.TesteFraquezas.executar();
        desafio1.jogo.TesteSistemasCombate.executar();
        Jogo jogo = new Jogo(new Mago("Mago"));
        jogo.proximaOnda();
        aguardar(jogo);
        verificar(jogo.atacar(0, false), "Ataque contra fraqueza deve funcionar");
        verificar(jogo.getMensagens().stream().anyMatch(m -> m.contains("Fraqueza explorada")),
                "O aviso curto deve confirmar quando o herói explora uma fraqueza");
    }

    private static void testarPocoesEMana() {
        Mago mago = new Mago("Teste");
        Jogo jogo = new Jogo(mago);
        verificar(jogo.comprar(Jogo.Melhoria.POCAO_VIDA), "Comprar poção de vida");
        verificar(jogo.comprar(Jogo.Melhoria.POCAO_MANA), "Comprar poção de mana");
        jogo.proximaOnda();
        mago.receberDano(60);
        verificar(mago.gastarMana(110), "Preparação de mana");
        aguardar(jogo);
        verificar(!jogo.atacar(0, true) && jogo.getEstado() == Jogo.Estado.ESCOLHA,
                "Habilidade sem mana não deve gastar a ação");
        int vida = mago.getHpAtual();
        verificar(jogo.usarPocao(true), "Poção de vida deve funcionar");
        verificar(mago.getHpAtual() == vida + 45 && jogo.getPocoesVida() == 0,
                "Poção de vida deve curar e reduzir estoque");
        aguardar(jogo);
        int mana = mago.getManaAtual();
        verificar(jogo.usarPocao(false), "Poção de mana deve funcionar");
        verificar(mago.getManaAtual() == mana + 30 && jogo.getPocoesMana() == 0,
                "Poção de mana deve restaurar e reduzir estoque");
    }

    private static void testarAtbEAcoes() {
        Jogo jogo = new Jogo(new Guerreiro("Teste"));
        jogo.proximaOnda();
        verificar(!jogo.atacar(0, false), "Não deve atacar antes da barra encher");
        aguardar(jogo);
        Inimigo inimigo = jogo.getInimigos().get(0);
        int atbInimigo = inimigo.getAtb();
        int vida = jogo.getHeroi().getHpAtual();
        for (int i = 0; i < 100; i++) jogo.avancarTempo();
        verificar(jogo.getHeroi().getHpAtual() == vida && inimigo.getAtb() == atbInimigo,
                "O tempo deve parar durante a escolha");
        int mana = jogo.getHeroi().getManaAtual();
        verificar(!jogo.atacar(-1, true), "Alvo inválido deve ser recusado");
        verificar(jogo.getHeroi().getManaAtual() == mana && jogo.getEstado() == Jogo.Estado.ESCOLHA,
                "Alvo inválido não deve consumir ação ou mana");
        verificar(!jogo.usarPocao(true) && jogo.getPocoesVida() == 0,
                "Não deve usar uma poção que não foi comprada");
        verificar(jogo.atacar(0, true), "Habilidade deve funcionar");
        verificar(jogo.getHeroi().getManaAtual() == mana - jogo.getHeroi().getCustoHabilidade(),
                "Habilidade deve gastar mana");
        verificar(jogo.getEstado() == Jogo.Estado.COMBATE, "Ação deve reiniciar a barra");
        while (inimigo.estaVivo()) {
            aguardar(jogo);
            jogo.atacar(0, false);
        }
        verificar(jogo.getEstado() == Jogo.Estado.INTERVALO, "Derrotar o último inimigo deve concluir a onda");
    }

    private static void testarRecompensaELoja() {
        Jogo jogo = new Jogo(new Guerreiro("Teste"));
        verificar(jogo.getPocoesVida() == 0 && jogo.getPocoesMana() == 0,
                "Partida deve começar sem poções");
        jogo.proximaOnda();
        concluirOnda(jogo);
        verificar(jogo.getOuro() == 97 && jogo.getExperiencia() == 25, "Recompensas da primeira onda");
        verificar(jogo.getPocoesVida() == 0 && jogo.getPocoesMana() == 0,
                "Vitórias não devem distribuir poções");
        int poder = jogo.getHeroi().getPoder();
        verificar(jogo.comprar(Jogo.Melhoria.ATAQUE), "Compra deve funcionar");
        verificar(jogo.getHeroi().getPoder() == poder + 4 && jogo.getOuro() == 62, "Compra deve alterar atributos e ouro");
        verificar(jogo.comprar(Jogo.Melhoria.POCAO_VIDA) && jogo.getPocoesVida() == 1,
                "Somente compra deve adicionar poções");
        verificar(!jogo.comprar(Jogo.Melhoria.ATAQUE), "Não deve comprar sem ouro");
        jogo.proximaOnda();
        verificar(jogo.getOnda() == 2 && jogo.getEstado() == Jogo.Estado.COMBATE, "Próxima onda deve iniciar");
        verificar(!jogo.comprar(Jogo.Melhoria.POCAO_VIDA), "Loja deve fechar durante combate");
        concluirOnda(jogo);
        verificar(jogo.getHeroi().getNivel() == 1, "Duas ondas não devem aumentar o nível");
    }

    private static void testarDerrota() {
        Jogo jogo = new Jogo(new Guerreiro("Teste"));
        jogo.proximaOnda();
        jogo.getHeroi().receberDano(jogo.getHeroi().getHpAtual() - 1);
        aguardar(jogo);
        jogo.atacar(0, false);
        for (int i = 0; i < 200 && jogo.getEstado() == Jogo.Estado.COMBATE; i++) jogo.avancarTempo();
        verificar(jogo.getEstado() == Jogo.Estado.DERROTA, "Inimigo deve poder derrotar o herói");
        verificar(!jogo.atacar(0, false), "Herói derrotado não pode agir");
    }

    private static void testarProgressao(Personagem personagem) {
        Jogo jogo = new Jogo(personagem);
        verificar(jogo.comprar(Jogo.Melhoria.POCAO_VIDA), "Suprimento inicial");
        verificar(jogo.comprar(Jogo.Melhoria.ATAQUE), "Melhoria inicial");
        jogo.proximaOnda();
        while (jogo.getOnda() <= 5) {
            if (jogo.getOnda() == 5) {
                verificar(jogo.getInimigos().get(0).getNome().contains("Guardião"), "Quinta onda deve ter chefe");
            }
            concluirOnda(jogo);
            if (jogo.getPocoesVida() < 2 && jogo.getOuro() >= jogo.preco(Jogo.Melhoria.POCAO_VIDA)) {
                jogo.comprar(Jogo.Melhoria.POCAO_VIDA);
            }
            if (jogo.getOuro() >= jogo.preco(Jogo.Melhoria.ATAQUE)) {
                jogo.comprar(Jogo.Melhoria.ATAQUE);
            }
            if (jogo.getOuro() >= jogo.preco(Jogo.Melhoria.VITALIDADE)) {
                jogo.comprar(Jogo.Melhoria.VITALIDADE);
            }
            if (jogo.getOnda() == 5) break;
            jogo.proximaOnda();
        }
        verificar(personagem.getNivel() == 2, personagem.getClasse() + " deve estar no nível 2 após cinco ondas");
    }

    private static void testarEvolucaoEHabilidades() {
        Guerreiro heroi = new Guerreiro("Teste");
        // Isola as regras de progressão da sobrevivência neste teste.
        for (int i = 0; i < 100; i++) {
            heroi.melhorarVitalidade();
            heroi.melhorarAtaque();
        }
        Jogo jogo = new Jogo(heroi);
        jogo.proximaOnda();
        Inimigo inicial = jogo.getInimigos().get(0);
        while (jogo.getOnda() < 11) {
            concluirOnda(jogo);
            if (jogo.getOnda() <= 4) verificar(heroi.getNivel() == 1, "Primeiro nível exige cinco ondas");
            if (jogo.getOnda() == 5) verificar(heroi.getNivel() == 2 && jogo.getExperiencia() == 25,
                    "Cinco ondas devem ultrapassar o primeiro limiar de XP");
            if (jogo.getOnda() == 10) verificar(heroi.getNivel() == 3,
                    "Dez ondas devem resultar em apenas dois aumentos de nível");
            jogo.proximaOnda();
        }
        Inimigo evoluido = jogo.getInimigos().get(0);
        verificar(evoluido.getNivel() > inicial.getNivel() && evoluido.getMelhorias() > inicial.getMelhorias()
                        && evoluido.getVidaMaxima() > inicial.getVidaMaxima()
                        && evoluido.getAtaque() > inicial.getAtaque() && evoluido.getDefesa() > inicial.getDefesa(),
                "Novas ondas devem melhorar nível e atributos dos inimigos");
        testarSkill(1, "Golpe perfurante");
        testarSkill(2, "Ataque duplo");
        testarSkill(3, "Rajada arcana");
        testarSkill(5, "Impacto devastador");
        testarSkill(4, "Abalo sísmico");
        testarSkill(5, "Contraestocada");
    }

    private static void testarSemSuprimentos(Personagem personagem) {
        Jogo jogo = new Jogo(personagem);
        jogo.proximaOnda();
        for (int i = 0; i < 10000 && jogo.getOnda() <= 10; i++) {
            if (jogo.getEstado() == Jogo.Estado.DERROTA) return;
            if (jogo.getEstado() == Jogo.Estado.INTERVALO) {
                jogo.proximaOnda();
            } else if (jogo.getEstado() == Jogo.Estado.COMBATE) {
                jogo.avancarTempo();
            } else {
                int alvo = 0;
                while (!jogo.getInimigos().get(alvo).estaVivo()) alvo++;
                jogo.atacar(alvo, personagem.getManaAtual() >= personagem.getCustoHabilidade());
            }
        }
        throw new AssertionError(personagem.getClasse() + " não deve vencer dez ondas sem compras");
    }

    private static void testarSkill(int onda, String habilidade) {
        Guerreiro heroi = new Guerreiro("Teste");
        for (int i = 0; i < 100; i++) {
            heroi.melhorarVitalidade();
            heroi.melhorarAtaque();
        }
        Jogo jogo = new Jogo(heroi);
        for (int i = 0; i < 3; i++) jogo.comprar(Jogo.Melhoria.POCAO_MANA);
        jogo.proximaOnda();
        while (jogo.getOnda() < onda) {
            concluirOnda(jogo);
            jogo.proximaOnda();
        }
        Inimigo inimigo = jogo.getInimigos().stream().filter(i -> i.getNomeHabilidade().equals(habilidade))
                .findFirst().orElseThrow(() -> new AssertionError("Habilidade ausente no encontro: " + habilidade));
        int vidaAntes = heroi.getHpAtual();
        boolean avisou = false;
        for (int i = 0; i < 500; i++) {
            jogo.avancarTempo();
            avisou |= inimigo.habilidadePronta();
            boolean usouSkill = jogo.getMensagens().stream()
                    .anyMatch(m -> m.startsWith(inimigo.getNome() + " (nível") && m.contains("usa " + habilidade));
            if (usouSkill) {
                verificar(avisou && !inimigo.habilidadePronta(), "Skill deve ser anunciada antes e entrar em recarga");
                verificar(heroi.getHpAtual() < vidaAntes, "Skill deve causar dano real");
                long acoes = jogo.getMensagens().stream().filter(m -> m.startsWith(inimigo.getNome() + " (nível")).count();
                verificar(acoes == 3, "Skill deve ocorrer na terceira ação do inimigo");
                return;
            }
            if (jogo.getEstado() == Jogo.Estado.ESCOLHA) {
                heroi.gastarMana(heroi.getManaAtual());
                verificar(jogo.usarPocao(false), "Ação de espera para testar skill");
            }
        }
        throw new AssertionError("Inimigo não executou " + habilidade);
    }

    private static void concluirOnda(Jogo jogo) {
        int passos = 0;
        while (jogo.getEstado() != Jogo.Estado.INTERVALO && passos++ < 5000) {
            if (jogo.getEstado() == Jogo.Estado.DERROTA) {
                throw new AssertionError(jogo.getHeroi().getClasse() + " morreu na onda " + jogo.getOnda());
            }
            if (jogo.getEstado() == Jogo.Estado.COMBATE) {
                jogo.avancarTempo();
            } else {
                Personagem p = jogo.getHeroi();
                if (p.getHpAtual() <= p.getHPMaximo() / 2 && jogo.getPocoesVida() > 0) {
                    jogo.usarPocao(true);
                } else {
                    int alvo = 0;
                    while (!jogo.getInimigos().get(alvo).estaVivo()) alvo++;
                    jogo.atacar(alvo, p.getManaAtual() >= p.getCustoHabilidade());
                }
            }
        }
        verificar(jogo.getEstado() == Jogo.Estado.INTERVALO, "Onda deve terminar");
    }

    private static void aguardar(Jogo jogo) {
        for (int i = 0; i < 200 && jogo.getEstado() == Jogo.Estado.COMBATE; i++) jogo.avancarTempo();
        verificar(jogo.getEstado() == Jogo.Estado.ESCOLHA, "ATB deve encher");
    }

    private static void verificar(boolean condicao, String mensagem) {
        if (!condicao) throw new AssertionError(mensagem);
    }
}
