package co.edu.unipamplona.logicode.files.infrastructure;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.OptionalLong;
import java.util.regex.Pattern;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Cliente mínimo de la API S3 (MinIO u otro compatible): crear el bucket y poner, consultar, leer y borrar objetos,
 * firmando cada solicitud con AWS Signature V4 (encabezados firmados: host, x-amz-content-sha256, x-amz-date).
 * Sin dependencias: LogiCode solo usa estas cinco operaciones (T-85).
 */
class S3ObjectClient {

    static final String EMPTY_SHA256 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
    private static final Pattern SAFE_PATH = Pattern.compile("[A-Za-z0-9._/-]+");
    private static final DateTimeFormatter AMZ_DATE = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")
            .withZone(ZoneOffset.UTC);

    private final URI endpoint;
    private final String bucket;
    private final String accessKey;
    private final String secretKey;
    private final String region;
    private final Clock clock;
    private final HttpClient http = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(10)).build();

    S3ObjectClient(URI endpoint, String bucket, String accessKey, String secretKey, String region, Clock clock) {
        if (!SAFE_PATH.matcher(bucket).matches()) {
            throw new IllegalArgumentException("Nombre de bucket inválido");
        }
        this.endpoint = endpoint;
        this.bucket = bucket;
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.region = region;
        this.clock = clock;
    }

    /** Crea el bucket si no existe. */
    void ensureBucket() throws IOException {
        HttpResponse<String> response = send("PUT", "", EMPTY_SHA256, HttpRequest.BodyPublishers.noBody(), null,
                HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200 && !response.body().contains("BucketAlreadyOwnedByYou")) {
            throw failure("crear el bucket", response.statusCode(), response.body());
        }
    }

    /** Sube un archivo; el servidor rechaza la carga si el contenido no coincide con {@code sha256}. */
    void put(String key, Path file, String sha256) throws IOException {
        HttpResponse<String> response = send("PUT", key, sha256, HttpRequest.BodyPublishers.ofFile(file), null,
                HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw failure("guardar un archivo", response.statusCode(), response.body());
        }
    }

    /** Tamaño del objeto, o vacío si no existe. */
    OptionalLong size(String key) throws IOException {
        HttpResponse<Void> response = send("HEAD", key, EMPTY_SHA256, HttpRequest.BodyPublishers.noBody(), null,
                HttpResponse.BodyHandlers.discarding());
        if (response.statusCode() == 404) {
            return OptionalLong.empty();
        }
        if (response.statusCode() != 200) {
            throw failure("consultar un archivo", response.statusCode(), "");
        }
        return response.headers().firstValueAsLong("Content-Length");
    }

    /** Contenido del objeto desde el byte {@code offset} (para Range y videos). */
    InputStream get(String key, long offset) throws IOException {
        HttpResponse<InputStream> response = send("GET", key, EMPTY_SHA256, HttpRequest.BodyPublishers.noBody(),
                offset > 0 ? "bytes=" + offset + "-" : null, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() != 200 && response.statusCode() != 206) {
            String body;
            try (InputStream in = response.body()) {
                body = new String(in.readNBytes(2000), StandardCharsets.UTF_8);
            }
            throw failure("leer un archivo", response.statusCode(), body);
        }
        return response.body();
    }

    void delete(String key) throws IOException {
        HttpResponse<String> response = send("DELETE", key, EMPTY_SHA256, HttpRequest.BodyPublishers.noBody(), null,
                HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 204 && response.statusCode() != 200) {
            throw failure("borrar un archivo", response.statusCode(), response.body());
        }
    }

    private <T> HttpResponse<T> send(String method, String key, String payloadSha256,
            HttpRequest.BodyPublisher body, String range, HttpResponse.BodyHandler<T> handler) throws IOException {
        if (!key.isEmpty() && (!SAFE_PATH.matcher(key).matches() || key.contains(".."))) {
            throw new IllegalArgumentException("Clave de almacenamiento inválida");
        }
        // Las claves solo llevan caracteres que no se codifican en la URI, así que la ruta firmada es la enviada.
        String path = "/" + bucket + (key.isEmpty() ? "" : "/" + key);
        URI uri = endpoint.resolve(path);
        String amzDate = AMZ_DATE.format(clock.instant());
        HttpRequest.Builder request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofMinutes(30))
                .method(method, body)
                .header("x-amz-date", amzDate)
                .header("x-amz-content-sha256", payloadSha256)
                .header("Authorization", authorization(method, path, host(uri), amzDate, payloadSha256));
        if (range != null) {
            request.header("Range", range);
        }
        try {
            return http.send(request.build(), handler);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrumpido", e);
        }
    }

    String authorization(String method, String path, String host, String amzDate, String payloadSha256) {
        String date = amzDate.substring(0, 8);
        String scope = date + "/" + region + "/s3/aws4_request";
        String signedHeaders = "host;x-amz-content-sha256;x-amz-date";
        String canonicalRequest = method + "\n" + path + "\n\n"
                + "host:" + host + "\n"
                + "x-amz-content-sha256:" + payloadSha256 + "\n"
                + "x-amz-date:" + amzDate + "\n\n"
                + signedHeaders + "\n" + payloadSha256;
        String stringToSign = "AWS4-HMAC-SHA256\n" + amzDate + "\n" + scope + "\n" + sha256Hex(canonicalRequest);
        byte[] key = hmac(("AWS4" + secretKey).getBytes(StandardCharsets.UTF_8), date);
        key = hmac(key, region);
        key = hmac(key, "s3");
        key = hmac(key, "aws4_request");
        String signature = HexFormat.of().formatHex(hmac(key, stringToSign));
        return "AWS4-HMAC-SHA256 Credential=" + accessKey + "/" + scope + ", SignedHeaders=" + signedHeaders
                + ", Signature=" + signature;
    }

    /** El encabezado Host que pone HttpClient: el puerto solo va si la URI lo trae. */
    private static String host(URI uri) {
        return uri.getPort() == -1 ? uri.getHost() : uri.getHost() + ":" + uri.getPort();
    }

    private static IOException failure(String action, int status, String body) {
        String code = body.replaceAll("(?s).*<Code>([^<]*)</Code>.*", "$1");
        return new IOException("El almacenamiento de archivos no pudo " + action + " (HTTP " + status
                + (code.length() < 80 && !code.isBlank() ? ", " + code : "") + ")");
    }

    private static byte[] hmac(byte[] key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String sha256Hex(String text) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
