package mx.uam.sapcyti.exam.infrastructure.adapter.in.dto;

import java.util.List;

/**
 * Body of {@code POST /api/exam-requests/me/{examType}/documents} when the envelope was accepted.
 * {@code request} is null when the batch left no draft.
 */
public record SaveDocumentsResponse(
        ExamRequestViewResponse request, int version, List<DocumentSaveResultResponse> results) {
}
