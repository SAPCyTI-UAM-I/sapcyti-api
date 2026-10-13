package mx.uam.sapcyti.exam.domain.model;

/**
 * Private object key for one PDF
 * (exam-requests.schema.json#/definitions/StoredDocumentReference).
 *
 * <p>The key is {@code {graduateProgramId}/{studentId}/exam-requests/{examRequestId}/{documentId}.pdf}.
 * It is never built from the received file name. The request id and the document id are reserved
 * before the object is written.
 */
public final class ExamObjectKey {

    private ExamObjectKey() {
    }

    public static String pdf(long graduateProgramId, long studentId, long examRequestId, long documentId) {
        if (graduateProgramId < 1 || studentId < 1 || examRequestId < 1 || documentId < 1) {
            throw new IllegalArgumentException("object key ids must be positive");
        }
        return graduateProgramId
                + "/"
                + studentId
                + "/exam-requests/"
                + examRequestId
                + "/"
                + documentId
                + ".pdf";
    }
}
