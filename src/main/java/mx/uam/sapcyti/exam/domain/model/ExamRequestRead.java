package mx.uam.sapcyti.exam.domain.model;

import java.util.List;
import java.util.Objects;

/**
 * One existing dossier for {@code (studentId, examType)}, as returned by {@code ExamRequestReadPort}.
 * Empty means the student has no request. This read model has no storage location.
 */
public record ExamRequestRead(ExamRequestBinding binding, List<CurrentExamDocument> documents) {

    public ExamRequestRead {
        Objects.requireNonNull(binding, "binding");
        documents = documents == null ? List.of() : List.copyOf(documents);
    }
}
