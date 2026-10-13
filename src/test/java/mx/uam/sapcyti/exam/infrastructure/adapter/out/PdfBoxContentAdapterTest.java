package mx.uam.sapcyti.exam.infrastructure.adapter.out;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import mx.uam.sapcyti.exam.domain.model.PdfContentInspection;
import mx.uam.sapcyti.exam.domain.port.out.PdfContentPort;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionJavaScript;
import org.junit.jupiter.api.Test;

class PdfBoxContentAdapterTest {

    private final PdfContentPort adapter = new PdfBoxContentAdapter();

    @Test
    void minimalPdfIsAccepted() throws IOException {
        PdfContentInspection inspection = adapter.inspect(minimalPdf(false, false));

        assertThat(inspection.accepted()).isTrue();
    }

    @Test
    void bytesDisguisedAsPdfAreRejected() {
        byte[] disguised = "%PDF-1.4\nthis is not a pdf".getBytes(java.nio.charset.StandardCharsets.UTF_8);

        assertThat(adapter.inspect(disguised).accepted()).isFalse();
    }

    @Test
    void truncatedPdfIsRejected() throws IOException {
        byte[] pdf = minimalPdf(false, false);
        byte[] truncated = java.util.Arrays.copyOf(pdf, pdf.length / 2);

        assertThat(adapter.inspect(truncated).accepted()).isFalse();
    }

    @Test
    void emptyBytesAreRejected() {
        assertThat(adapter.inspect(new byte[0]).accepted()).isFalse();
        assertThat(adapter.inspect(null).accepted()).isFalse();
    }

    @Test
    void unreadableEncryptionIsRejected() throws IOException {
        assertThat(adapter.inspect(minimalPdf(true, false)).accepted()).isFalse();
    }

    @Test
    void javascriptOpenActionIsRejected() throws IOException {
        assertThat(adapter.inspect(minimalPdf(false, true)).accepted()).isFalse();
    }

    private static byte[] minimalPdf(boolean encrypt, boolean javascript) throws IOException {
        try (PDDocument document = new PDDocument();
                ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            document.addPage(new PDPage(PDRectangle.A4));
            if (javascript) {
                document.getDocumentCatalog().setOpenAction(new PDActionJavaScript("app.alert(1)"));
            }
            if (encrypt) {
                StandardProtectionPolicy policy = new StandardProtectionPolicy(
                        "owner-secret", "user-secret", new AccessPermission());
                policy.setEncryptionKeyLength(128);
                document.protect(policy);
            }
            document.save(out);
            return out.toByteArray();
        }
    }
}
