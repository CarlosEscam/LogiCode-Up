package co.edu.unipamplona.logicode.files.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.time.Clock;

import org.junit.jupiter.api.Test;

/** Firma V4 comparada con la que produce botocore (la biblioteca oficial de AWS) para las mismas solicitudes. */
class S3ObjectClientTest {

    private final S3ObjectClient client = new S3ObjectClient(URI.create("http://minio:9000"), "logicode",
            "AKIAIOSFODNN7EXAMPLE", "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY", "us-east-1", Clock.systemUTC());

    @Test
    void firmaUnaCargaConElSha256DelContenido() {
        assertThat(client.authorization("PUT", "/logicode/ab/cd/abcd1234-0000-0000-0000-000000000000", "minio:9000",
                "20261101T203000Z", "9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08"))
                .isEqualTo("AWS4-HMAC-SHA256 Credential=AKIAIOSFODNN7EXAMPLE/20261101/us-east-1/s3/aws4_request, "
                        + "SignedHeaders=host;x-amz-content-sha256;x-amz-date, "
                        + "Signature=4064b32570e2160dcfdb261b21f3ee43fa640995c11627aec991ee48158d9b2a");
    }

    @Test
    void firmaUnaSolicitudSinCuerpo() {
        assertThat(client.authorization("GET", "/logicode", "localhost:9000", "20261101T203000Z",
                S3ObjectClient.EMPTY_SHA256))
                .endsWith("Signature=3dbc127c23ee067befd9102e580e896b50ec875f53ca92d0af66b70f63e37a54");
    }
}
