package desafio1.jogo;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** Persiste o histórico de runs em um único arquivo binário. */
public final class Salvamento {
    private final Path arquivo;

    public Salvamento(Path arquivo) {
        this.arquivo = Objects.requireNonNull(arquivo, "Arquivo é obrigatório.");
    }

    public static Salvamento padrao() {
        return new Salvamento(Paths.get(System.getProperty("user.home"), ".eudoria", "runs.dat"));
    }

    /** Runs da mais recente para a mais antiga; arquivo ausente ou corrompido resulta em lista vazia. */
    public synchronized List<RegistroRun> listar() {
        if (!Files.isRegularFile(arquivo)) return new ArrayList<>();
        try (InputStream entrada = Files.newInputStream(arquivo);
             ObjectInputStream objetos = new ObjectInputStream(entrada)) {
            List<RegistroRun> runs = new ArrayList<>();
            for (Object item : (List<?>) objetos.readObject()) {
                if (item instanceof RegistroRun) runs.add((RegistroRun) item);
            }
            runs.sort(Comparator.comparingLong(RegistroRun::getUltimaAtualizacao).reversed());
            return runs;
        } catch (IOException | ClassNotFoundException | ClassCastException erro) {
            return new ArrayList<>();
        }
    }

    public synchronized void registrar(RegistroRun run) {
        Objects.requireNonNull(run, "Run é obrigatória.");
        run.atualizar();
        List<RegistroRun> runs = listar();
        runs.removeIf(existente -> existente.getId().equals(run.getId()));
        runs.add(0, run);
        try {
            Path pasta = arquivo.toAbsolutePath().getParent();
            if (pasta != null) Files.createDirectories(pasta);
            Path temporario = Files.createTempFile(pasta, "runs", ".tmp");
            try (OutputStream saida = Files.newOutputStream(temporario);
                 ObjectOutputStream objetos = new ObjectOutputStream(saida)) {
                objetos.writeObject(runs);
            }
            Files.move(temporario, arquivo, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException erro) {
            throw new UncheckedIOException("Não foi possível salvar o jogo.", erro);
        }
    }
}
