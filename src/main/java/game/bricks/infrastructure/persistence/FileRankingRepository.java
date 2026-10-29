package game.bricks.infrastructure.persistence;

import game.bricks.domain.game.Difficulty;
import game.bricks.domain.game.MatchResult;
import game.bricks.domain.ranking.RankingRepository;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public final class FileRankingRepository implements RankingRepository {

    private static final String HEADER = "LAN_BRICKS_V1";
    private final Path directory;

    public FileRankingRepository(Path directory) {
        this.directory = directory;
    }

    @Override
    public synchronized List<MatchResult> load() {
        Path file = directory.resolve("results.tsv");

        if (!Files.exists(file))
            return List.of();
        try {
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);

            if (lines.isEmpty() || !HEADER.equals(lines.getFirst()))
                throw new IOException("Unknown records format.");
            List<MatchResult> results = new ArrayList<>();

            for (String line : lines.subList(1, lines.size()))
                results.add(decode(line));
            return List.copyOf(results);
        } catch (IOException | IllegalArgumentException e) {
            throw new UncheckedIOException(
                "Could not read the records. The existing file has been preserved.",
                new IOException(e)
            );
        }
    }

    @Override
    public synchronized void save(MatchResult result) {
        try {
            Files.createDirectories(directory);

            try (FileChannel channel = FileChannel.open(
                directory.resolve("records.lock"),
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE
            );
                 var lock = channel.lock()) {
                List<MatchResult> results = new ArrayList<>(load());

                if (results.stream().anyMatch(record -> record.id().equals(result.id())))
                    return;
                results.add(result);
                List<String> lines = new ArrayList<>();
                lines.add(HEADER);

                for (MatchResult record : results)
                    lines.add(encode(record));
                Path temporary = Files.createTempFile(directory, "results-", ".tmp");

                try {
                    Files.write(temporary, lines, StandardCharsets.UTF_8);

                    try {
                        Files.move(
                            temporary,
                            directory.resolve("results.tsv"),
                            StandardCopyOption.REPLACE_EXISTING,
                            StandardCopyOption.ATOMIC_MOVE
                        );
                    } catch (AtomicMoveNotSupportedException e) {
                        Files.move(temporary, directory.resolve("results.tsv"), StandardCopyOption.REPLACE_EXISTING);
                    }
                } finally {
                    Files.deleteIfExists(temporary);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Could not save the result.", e);
        }
    }

    private String encode(MatchResult result) {
        List<String> fields = new ArrayList<>(List.of(
            result.id(),
            Boolean.toString(result.versus()),
            result.difficulty().name(),
            Double.toString(result.elapsed()),
            Integer.toString(result.winner())
        ));

        for (int i = 0; i < 2; i++) {
            if (i < result.players().size()) {
                var p = result.players().get(i);
                fields.addAll(List.of(
                    p.id(),
                    Integer.toString(p.score()),
                    Integer.toString(p.lives()),
                    Integer.toString(p.level())
                ));
            } else
                fields.addAll(List.of("", "0", "0", "1"));
        }

        return String.join("\t", fields);
    }

    private MatchResult decode(String line) throws IOException {
        String[] values = line.split("\t", -1);

        if (values.length != 13 || !(values[1].equals("true") || values[1].equals("false")))
            throw new IOException("The match record is damaged.");
        boolean versus = Boolean.parseBoolean(values[1]);
        List<MatchResult.Player> players = new ArrayList<>();

        for (int i = 0; i < (versus ? 2 : 1); i++) {
            int offset = 5 + i * 4;
            players.add(new MatchResult.Player(
                values[offset],
                Integer.parseInt(values[offset + 1]),
                Integer.parseInt(values[offset + 2]),
                Integer.parseInt(values[offset + 3])
            ));
        }

        return new MatchResult(
            values[0],
            versus,
            Difficulty.valueOf(values[2]),
            Double.parseDouble(values[3]),
            Integer.parseInt(values[4]),
            players
        );
    }
}
