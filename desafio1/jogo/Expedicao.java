package desafio1.jogo;

import desafio1.personagens.Personagem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;

public final class Expedicao implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    public enum Estado { MAPA, COMBATE, EVENTO, RESULTADO, LOJA, ACAMPAMENTO, RECOMPENSA,
        CAPITULO_CONCLUIDO, CAMPANHA_CONCLUIDA, VITORIA, DERROTA }
    public enum TipoSala { ENTRADA, COMBATE, ELITE, EVENTO, LOJA, ACAMPAMENTO, CHEFE }
    public enum EscolhaEvento { CUIDADOSO, ARRISCAR, IGNORAR }
    public enum Reliquia { LAMINA, AMULETO, FOCO }
    public enum Evento { BAU, ALTAR, ARMADILHA, FONTE, FORJA }
    public enum AcaoDescanso { VIDA, MANA, CONCENTRAR, RELIQUIA, CONTINUAR }
    /** Carga de LIMIT obtida ao se concentrar no acampamento. */
    public static final int LIMITE_CONCENTRACAO = 50;
    public static final int TOTAL_CAPITULOS = 3;

    public static final class Sala implements java.io.Serializable {
        private static final long serialVersionUID = 1L;
        private final int andar;
        private final int linha;
        private final TipoSala tipo;
        private final Evento evento;
        private final List<Sala> saidas = new ArrayList<>();
        private boolean visitada;

        private Sala(int andar, int linha, TipoSala tipo, Evento evento) {
            this.andar = andar;
            this.linha = linha;
            this.tipo = tipo;
            this.evento = evento;
        }

        public int getAndar() { return andar; }
        public int getLinha() { return linha; }
        public TipoSala getTipo() { return tipo; }
        public boolean foiVisitada() { return visitada; }
        public List<Sala> getSaidas() { return Collections.unmodifiableList(saidas); }
    }

    private final Jogo jogo;
    private final Random aleatorio;
    private final List<Sala> salas = new ArrayList<>();
    private Sala atual;
    private Estado estado = Estado.LOJA;
    private String mensagem = "Prepare-se na entrada: compre suprimentos antes de explorar.";
    private int ouroAntesCombate;
    private boolean resultadoFatal;
    private final List<Reliquia> reliquiasPendentes = new ArrayList<>();
    private final List<Reliquia> reliquiasAtivas = new ArrayList<>();
    private int capitulo = 1;
    private int ciclo;

    public Expedicao(Personagem heroi, long semente) {
        jogo = new Jogo(heroi);
        aleatorio = new Random(semente);
        gerarMapa();
    }

    public Jogo getJogo() { return jogo; }
    public Estado getEstado() { return estado; }
    public Sala getSalaAtual() { return atual; }
    public List<Sala> getSalas() { return Collections.unmodifiableList(salas); }
    public String getMensagem() { return mensagem; }
    // Campos ausentes nos saves anteriores recebem zero na desserializacao.
    public int getCapitulo() { return capitulo == 0 ? 1 : capitulo; }
    public int getCiclo() { return ciclo; }
    public int getEtapa() { return ciclo * TOTAL_CAPITULOS + getCapitulo() - 1; }
    public String getNomeCapitulo() {
        switch (getCapitulo()) {
            case 1: return "Ruínas do Castelo";
            case 2: return "Catacumbas Arcanas";
            case 3: return "Cidadela do Eclipse";
            default: throw new IllegalStateException("Capítulo desconhecido.");
        }
    }
    public String getDescricaoProgresso() {
        return "Capítulo " + getCapitulo() + "/" + TOTAL_CAPITULOS
                + (ciclo > 0 ? " • Ascensão " + ciclo : "")
                + " • Andar " + atual.andar + "/8";
    }
    public Inimigo.Chefe getChefe() {
        switch (getCapitulo()) {
            case 1: return Inimigo.Chefe.GUARDIAO;
            case 2: return Inimigo.Chefe.ARQUIMAGO;
            case 3: return Inimigo.Chefe.SOBERANO;
            default: throw new IllegalStateException("Capítulo desconhecido.");
        }
    }
    public Evento getEventoAtual() {
        if (atual.evento == null) throw new IllegalStateException("A sala atual não é um evento.");
        return atual.evento;
    }
    public List<Reliquia> getReliquiasPendentes() {
        return Collections.unmodifiableList(reliquiasPendentes);
    }
    public List<Reliquia> getReliquiasAtivas() {
        return Collections.unmodifiableList(reliquiasAtivas);
    }

    public boolean estaDisponivel(Sala sala) {
        return estado == Estado.MAPA && atual.saidas.contains(sala);
    }

    private void gerarMapa() {
        salas.clear();
        atual = new Sala(0, 1, TipoSala.ENTRADA, null);
        atual.visitada = true;
        salas.add(atual);
        List<Sala> anteriores = Collections.singletonList(atual);
        TipoSala[][] tipos = {
                {TipoSala.COMBATE, TipoSala.COMBATE, TipoSala.COMBATE},
                {TipoSala.COMBATE, TipoSala.ELITE, TipoSala.COMBATE},
                {TipoSala.ACAMPAMENTO, TipoSala.EVENTO, TipoSala.LOJA},
                {TipoSala.ELITE, TipoSala.COMBATE, TipoSala.COMBATE},
                {TipoSala.LOJA, TipoSala.ACAMPAMENTO, TipoSala.EVENTO},
                {TipoSala.COMBATE, TipoSala.ELITE, TipoSala.COMBATE},
                {TipoSala.ACAMPAMENTO, TipoSala.COMBATE, TipoSala.LOJA}
        };
        for (int andar = 1; andar <= tipos.length; andar++) {
            List<TipoSala> ordem = new ArrayList<>();
            Collections.addAll(ordem, tipos[andar - 1]);
            Collections.shuffle(ordem, aleatorio);
            List<Sala> proximas = new ArrayList<>();
            for (int linha = 0; linha < 3; linha++) {
                TipoSala tipo = ordem.get(linha);
                Evento evento = null;
                if (tipo == TipoSala.EVENTO) {
                    if (andar == 3 && getCapitulo() > 1) {
                        evento = getCapitulo() == 2 ? Evento.FONTE : Evento.FORJA;
                    } else {
                        int quantidade = getCapitulo() == 1 ? 3 : getCapitulo() == 2 ? 4 : 5;
                        evento = Evento.values()[aleatorio.nextInt(quantidade)];
                    }
                }
                Sala sala = new Sala(andar, linha, tipo, evento);
                proximas.add(sala);
                salas.add(sala);
            }
            if (andar == 1) {
                atual.saidas.addAll(proximas);
            } else {
                for (Sala anterior : anteriores) {
                    anterior.saidas.add(proximas.get(anterior.linha));
                    int outra = anterior.linha == 0 ? 1 : anterior.linha == 2 ? 1 : (aleatorio.nextBoolean() ? 0 : 2);
                    anterior.saidas.add(proximas.get(outra));
                }
            }
            anteriores = proximas;
        }
        Sala chefe = new Sala(8, 1, TipoSala.CHEFE, null);
        salas.add(chefe);
        for (Sala anterior : anteriores) anterior.saidas.add(chefe);
    }

    public void selecionar(Sala sala) {
        if (!estaDisponivel(sala)) throw new IllegalStateException("Sala não está acessível.");
        atual = sala;
        atual.visitada = true;
        switch (sala.tipo) {
            case COMBATE:
            case ELITE:
            case CHEFE:
                ouroAntesCombate = jogo.getOuro();
                jogo.iniciarEncontroExpedicao(sala.andar + getEtapa() * 4,
                        sala.tipo == TipoSala.ELITE, sala.tipo == TipoSala.CHEFE, getChefe());
                estado = Estado.COMBATE;
                break;
            case EVENTO: estado = Estado.EVENTO; mensagem = descricaoEvento(); break;
            case LOJA:
                jogo.definirLojaDisponivel(true);
                estado = Estado.LOJA;
                mensagem = "Mercador encontrado. Compre ou siga viagem.";
                break;
            case ACAMPAMENTO: estado = Estado.ACAMPAMENTO; mensagem = "Escolha recuperar vida ou mana."; break;
            default: throw new IllegalStateException("Tipo de sala inválido.");
        }
    }

    public void sincronizarCombate() {
        if (estado != Estado.COMBATE) return;
        if (jogo.getEstado() == Jogo.Estado.DERROTA) {
            estado = Estado.DERROTA;
            mensagem = "Você foi derrotado na expedição.";
        } else if (jogo.getEstado() == Jogo.Estado.INTERVALO) {
            mensagem = "Vitória! Ouro recebido: " + (jogo.getOuro() - ouroAntesCombate) + ".";
            if (atual.tipo == TipoSala.CHEFE) {
                estado = getCapitulo() == TOTAL_CAPITULOS ? Estado.CAMPANHA_CONCLUIDA : Estado.CAPITULO_CONCLUIDO;
                mensagem = getChefe().getNome() + " derrotado! "
                        + (estado == Estado.CAMPANHA_CONCLUIDA
                        ? "Encerre com vitória ou continue em uma Ascensão mais difícil."
                        : "O próximo capítulo espera. Seu herói e suas relíquias continuam com você.");
            } else if (atual.tipo == TipoSala.ELITE) estado = Estado.RECOMPENSA;
            else estado = Estado.MAPA;
        }
    }

    public void avancarCapitulo() {
        if (estado != Estado.CAPITULO_CONCLUIDO && estado != Estado.CAMPANHA_CONCLUIDA) {
            throw new IllegalStateException("Derrote o chefe antes de seguir para o próximo capítulo.");
        }
        if (jogo.getEscolhasNivel() > 0) {
            throw new IllegalStateException("Escolha os bônus de nível antes de continuar.");
        }
        if (getCapitulo() == TOTAL_CAPITULOS) {
            ciclo++;
            capitulo = 1;
        } else {
            capitulo = getCapitulo() + 1;
        }
        gerarMapa();
        jogo.definirLojaDisponivel(true);
        estado = Estado.LOJA;
        mensagem = getNomeCapitulo() + ": prepare seus suprimentos. Níveis, ouro e relíquias foram preservados.";
    }

    public void concluirExpedicao() {
        if (estado != Estado.CAMPANHA_CONCLUIDA
                && !(ciclo > 0 && estado == Estado.CAPITULO_CONCLUIDO)) {
            throw new IllegalStateException("Conclua os três capítulos antes de encerrar com vitória.");
        }
        estado = Estado.VITORIA;
        mensagem = "A campanha foi concluída. Sua vitória ficará registrada!";
    }

    public String descricaoEvento() {
        exigir(Estado.EVENTO);
        switch (atual.evento) {
            case BAU: return "Um baú suspeito pode conter ouro ou esconder uma armadilha.";
            case ALTAR: return "Um altar oferece poder em troca de vitalidade.";
            case ARMADILHA: return "Um corredor está protegido por uma armadilha.";
            case FONTE: return "Uma fonte arcana restaura o corpo ou transforma mana em LIMIT.";
            case FORJA: return "Uma forja esquecida cria relíquias em troca de mana ou vitalidade.";
            default: throw new IllegalStateException("Evento desconhecido.");
        }
    }

    public String textoEscolha(EscolhaEvento escolha) {
        exigir(Estado.EVENTO);
        if (escolha == EscolhaEvento.IGNORAR) return "Ignorar e seguir em segurança";
        switch (atual.evento) {
            case BAU:
                if (escolha == EscolhaEvento.CUIDADOSO) {
                    return "Examinar: +" + (jogo.getHeroi().getClasse().equals("Ladino") ? 55 : 30) + " ouro, sem dano";
                }
                return "Arriscar: 50% +80 ouro; 50% perde 25% da vida máxima";
            case ALTAR:
                return escolha == EscolhaEvento.CUIDADOSO ? "Meditar: recupera 25% da mana máxima"
                        : "Sacrificar 25% da vida máxima: +4 poder permanente";
            case ARMADILHA:
                if (escolha == EscolhaEvento.CUIDADOSO) {
                    return jogo.getHeroi().getClasse().equals("Ladino")
                            ? "Desarmar como Ladino: +35 ouro"
                            : "Desarmar: custa 10 mana, +35 ouro";
                }
                return "Atravessar: perde 25% da vida máxima, +60 ouro";
            case FONTE:
                return escolha == EscolhaEvento.CUIDADOSO ? "Beber: recupera 25% da vida máxima"
                        : "Canalizar: custa 20 mana, +50% de LIMIT";
            case FORJA:
                return escolha == EscolhaEvento.CUIDADOSO ? "Forjar: custa 10 mana, guarda um Amuleto"
                        : "Sacrificar 25% da vida máxima: guarda um Foco";
            default: throw new IllegalStateException("Evento desconhecido.");
        }
    }

    public boolean resolverEvento(EscolhaEvento escolha) {
        exigir(Estado.EVENTO);
        Objects.requireNonNull(escolha);
        Personagem heroi = jogo.getHeroi();
        if (escolha == EscolhaEvento.IGNORAR) {
            mensagem = "Você seguiu em segurança.";
        } else if (atual.evento == Evento.FONTE) {
            if (escolha == EscolhaEvento.CUIDADOSO) {
                int antes = heroi.getHpAtual();
                heroi.restaurarVida(Math.max(1, heroi.getHPMaximo() / 4));
                mensagem = "A fonte restaurou " + (heroi.getHpAtual() - antes) + " de vida.";
            } else {
                if (!heroi.gastarMana(20)) {
                    mensagem = "Mana insuficiente para canalizar. Escolha outra opção.";
                    return false;
                }
                jogo.adicionarLimite(LIMITE_CONCENTRACAO);
                mensagem = "Canalização: LIMIT " + jogo.getLimite() + "%.";
            }
        } else if (atual.evento == Evento.FORJA) {
            if (escolha == EscolhaEvento.CUIDADOSO) {
                if (!heroi.gastarMana(10)) {
                    mensagem = "Mana insuficiente para forjar. Escolha outra opção.";
                    return false;
                }
                reliquiasPendentes.add(Reliquia.AMULETO);
                mensagem = "Amuleto forjado. Ative-o na loja ou no acampamento.";
            } else {
                ferir("A forja cobrou seu preço");
                if (heroi.estaVivo()) {
                    reliquiasPendentes.add(Reliquia.FOCO);
                    mensagem += " Foco guardado. Ative-o na loja ou no acampamento.";
                }
            }
        } else if (atual.evento == Evento.BAU) {
            if (escolha == EscolhaEvento.CUIDADOSO) {
                int ouro = heroi.getClasse().equals("Ladino") ? 55 : 30;
                jogo.ganharOuro(ouro);
                mensagem = "Baú examinado: +" + ouro + " ouro.";
            } else if (aleatorio.nextBoolean()) {
                jogo.ganharOuro(80);
                mensagem = "O baú continha 80 ouro!";
            } else {
                ferir("Uma armadilha disparou");
            }
        } else if (atual.evento == Evento.ALTAR) {
            if (escolha == EscolhaEvento.CUIDADOSO) {
                int antes = heroi.getManaAtual();
                heroi.restaurarMana(Math.max(1, heroi.getManaMaxima() / 4));
                mensagem = "Meditação: +" + (heroi.getManaAtual() - antes) + " mana.";
            } else {
                ferir("O altar cobrou seu preço");
                if (heroi.estaVivo()) {
                    reliquiasPendentes.add(Reliquia.LAMINA);
                    mensagem += " Uma Lâmina foi guardada. Ative-a na loja ou no acampamento.";
                }
            }
        } else if (escolha == EscolhaEvento.CUIDADOSO) {
            if (!heroi.getClasse().equals("Ladino") && !heroi.gastarMana(10)) {
                mensagem = "Mana insuficiente para desarmar. Escolha outra opção.";
                return false;
            }
            jogo.ganharOuro(35);
            mensagem = "Armadilha desarmada: +35 ouro.";
        } else {
            ferir("A armadilha feriu você");
            if (heroi.estaVivo()) {
                jogo.ganharOuro(60);
                mensagem += " Encontrou 60 ouro.";
            }
        }
        resultadoFatal = !heroi.estaVivo();
        estado = Estado.RESULTADO;
        if (resultadoFatal) {
            mensagem += " Você foi derrotado.";
            jogo.registrarMorte();
        }
        return true;
    }

    private void ferir(String origem) {
        Personagem heroi = jogo.getHeroi();
        int dano = heroi.receberDano(Math.max(1, heroi.getHPMaximo() / 4));
        mensagem = origem + ": -" + dano + " de vida.";
    }

    public void descansar(AcaoDescanso acao) {
        exigir(Estado.ACAMPAMENTO);
        Objects.requireNonNull(acao);
        Personagem heroi = jogo.getHeroi();
        switch (acao) {
            case VIDA:
                int vidaAntes = heroi.getHpAtual();
                heroi.restaurarVida(Math.max(1, heroi.getHPMaximo() * 40 / 100));
                mensagem = "Descanso: +" + (heroi.getHpAtual() - vidaAntes) + " vida.";
                break;
            case MANA:
                int manaAntes = heroi.getManaAtual();
                heroi.restaurarMana(Math.max(1, heroi.getManaMaxima() * 40 / 100));
                mensagem = "Meditação: +" + (heroi.getManaAtual() - manaAntes) + " mana.";
                break;
            case CONCENTRAR:
                jogo.adicionarLimite(LIMITE_CONCENTRACAO);
                mensagem = "Concentração: LIMIT " + jogo.getLimite() + "%.";
                break;
            case RELIQUIA:
                ativarReliquia(0);
                break;
            case CONTINUAR: mensagem = "Você deixou o acampamento sem descansar."; break;
            default: throw new IllegalArgumentException("Ação de descanso desconhecida.");
        }
        estado = Estado.MAPA;
    }

    public void sairDaLoja() {
        exigir(Estado.LOJA);
        jogo.definirLojaDisponivel(false);
        estado = Estado.MAPA;
        mensagem = "Escolha uma sala conectada para continuar.";
    }

    public void escolherReliquia(Reliquia reliquia) {
        exigir(Estado.RECOMPENSA);
        Objects.requireNonNull(reliquia);
        reliquiasPendentes.add(reliquia);
        mensagem = nomeReliquia(reliquia) + " guardada. Ative-a na loja ou no acampamento.";
        estado = Estado.MAPA;
    }

    public void ativarReliquia(int indice) {
        if (estado != Estado.LOJA && estado != Estado.ACAMPAMENTO) {
            throw new IllegalStateException("Relíquias só podem ser ativadas na loja ou no acampamento.");
        }
        if (indice < 0 || indice >= reliquiasPendentes.size()) {
            throw new IllegalArgumentException("Relíquia pendente inválida.");
        }
        Reliquia reliquia = reliquiasPendentes.remove(indice);
        switch (reliquia) {
            case LAMINA: jogo.getHeroi().melhorarAtaque(); break;
            case AMULETO: jogo.getHeroi().melhorarVitalidade(); break;
            case FOCO: jogo.getHeroi().melhorarMana(); break;
            default: throw new IllegalArgumentException("Relíquia desconhecida.");
        }
        reliquiasAtivas.add(reliquia);
        mensagem = nomeReliquia(reliquia) + " ativada.";
    }

    private static String nomeReliquia(Reliquia reliquia) {
        switch (reliquia) {
            case LAMINA: return "Lâmina";
            case AMULETO: return "Amuleto";
            case FOCO: return "Foco";
            default: throw new IllegalArgumentException("Relíquia desconhecida.");
        }
    }

    public void continuarResultado() {
        exigir(Estado.RESULTADO);
        estado = resultadoFatal ? Estado.DERROTA : Estado.MAPA;
    }

    private void exigir(Estado esperado) {
        if (estado != esperado) throw new IllegalStateException("Ação indisponível neste momento.");
    }
}
