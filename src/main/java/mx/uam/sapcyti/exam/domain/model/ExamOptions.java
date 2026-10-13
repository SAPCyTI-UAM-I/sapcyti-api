package mx.uam.sapcyti.exam.domain.model;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/**
 * Options read model (exam-requests.schema.json#/definitions/ExamOptions).
 * Specialization and master's yield {@code [GRADO]}. Doctorate yields {@code [GRADO, PREDOCTORAL]}.
 */
public record ExamOptions(
        Degree degree,
        ProgramStatus programStatus,
        boolean eligible,
        EligibilityReason eligibilityReason,
        List<ExamType> examTypes) {

    public ExamOptions {
        Objects.requireNonNull(degree, "degree");
        Objects.requireNonNull(programStatus, "programStatus");
        Objects.requireNonNull(examTypes, "examTypes");
        if (examTypes.isEmpty() || new HashSet<>(examTypes).size() != examTypes.size()) {
            throw new IllegalArgumentException("examTypes must be unique and non-empty");
        }
        examTypes = List.copyOf(examTypes);
        if (eligible == (eligibilityReason != null)) {
            throw new IllegalArgumentException("eligibilityReason is present only when not eligible");
        }
    }
}
