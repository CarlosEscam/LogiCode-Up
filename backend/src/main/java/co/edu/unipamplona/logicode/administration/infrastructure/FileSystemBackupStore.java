package co.edu.unipamplona.logicode.administration.infrastructure;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import co.edu.unipamplona.logicode.administration.application.BackupStore;
import org.springframework.stereotype.Component;

/**
 * Lee la carpeta que escribe {@code respaldos/respaldo.sh}: una subcarpeta {@code logicode-AAAAMMDD-HHMMSS} (hora de
 * Colombia) por respaldo, {@code estado.txt} como latido del servicio y {@code solicitudes/} para pedir uno ya.
 */
@Component
class FileSystemBackupStore implements BackupStore {

    private static final Pattern NAME = Pattern.compile("logicode-(\\d{8}-\\d{6})");
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    /** Colombia no tiene horario de verano: siempre UTC−5. */
    private static final ZoneOffset COLOMBIA = ZoneOffset.ofHours(-5);
    private static final Duration HEARTBEAT = Duration.ofMinutes(2);

    private final Path dir;

    FileSystemBackupStore(BackupProperties properties) {
        this.dir = properties.dir();
    }

    @Override
    public List<Backup> list() {
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        try (Stream<Path> entries = Files.list(dir)) {
            return entries.filter(Files::isDirectory).map(this::read).flatMap(Optional::stream)
                    .sorted(Comparator.comparing(Backup::createdAt).reversed()).toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public ServiceStatus status() {
        Path state = dir.resolve("estado.txt");
        if (!Files.isRegularFile(state)) {
            return new ServiceStatus(false, null, null);
        }
        try {
            boolean running = Files.getLastModifiedTime(state).toInstant().isAfter(Instant.now().minus(HEARTBEAT));
            Map<String, String> values = new HashMap<>();
            for (String line : Files.readAllLines(state, StandardCharsets.UTF_8)) {
                int eq = line.indexOf('=');
                if (eq > 0) {
                    values.put(line.substring(0, eq).strip(), line.substring(eq + 1).strip());
                }
            }
            return new ServiceStatus(running, integer(values.get("hora")), integer(values.get("retencion_dias")));
        } catch (IOException e) {
            return new ServiceStatus(false, null, null);
        }
    }

    @Override
    public boolean hasPendingRequest() {
        Path requests = dir.resolve("solicitudes");
        if (!Files.isDirectory(requests)) {
            return false;
        }
        try (Stream<Path> entries = Files.list(requests)) {
            return entries.findAny().isPresent();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void request(UUID requestedBy, Instant now) {
        try {
            Path requests = Files.createDirectories(dir.resolve("solicitudes"));
            Files.writeString(requests.resolve(now.toEpochMilli() + "-" + requestedBy),
                    "solicitado_por=" + requestedBy + "\nsolicitado_en=" + now + "\n", StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo registrar la solicitud de respaldo", e);
        }
    }

    private Optional<Backup> read(Path folder) {
        Matcher name = NAME.matcher(folder.getFileName().toString());
        Path database = folder.resolve("base.dump");
        Path files = folder.resolve("archivos.tar.gz");
        if (!name.matches() || !Files.isRegularFile(database) || !Files.isRegularFile(files)) {
            return Optional.empty();
        }
        try {
            Instant createdAt = LocalDateTime.parse(name.group(1), STAMP).toInstant(COLOMBIA);
            String test = null;
            String detail = null;
            Path testFile = folder.resolve("prueba-restauracion.txt");
            if (Files.isRegularFile(testFile)) {
                List<String> lines = Files.readAllLines(testFile, StandardCharsets.UTF_8);
                test = lines.isEmpty() ? null : lines.getFirst().strip();
                detail = lines.size() > 1 ? String.join(" ", lines.subList(1, lines.size())).strip() : null;
            }
            return Optional.of(new Backup(folder.getFileName().toString(), createdAt, Files.size(database),
                    Files.size(files), test, detail));
        } catch (IOException | DateTimeParseException e) {
            return Optional.empty();
        }
    }

    private static Integer integer(String value) {
        try {
            return value == null ? null : Integer.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
