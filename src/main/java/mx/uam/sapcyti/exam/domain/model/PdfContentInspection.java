package mx.uam.sapcyti.exam.domain.model;

/**
 * Result of opening PDF bytes with a parser
 * (exam-requests.schema.json#/definitions/PdfContentInspection).
 * A content type, a {@code .pdf} extension and a leading signature are not enough.
 * There is no maximum size: a file larger than 20 MB can still be accepted.
 */
public record PdfContentInspection(boolean accepted) {

    public static PdfContentInspection ok() {
        return new PdfContentInspection(true);
    }

    public static PdfContentInspection rejected() {
        return new PdfContentInspection(false);
    }
}
