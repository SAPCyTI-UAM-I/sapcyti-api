package mx.uam.sapcyti.exam.domain.model;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/**
 * GRADO requirements read model (exam-requests.schema.json#/definitions/ExamRequirements).
 * {@code request} is null when the student has no binding. A read does not create a draft.
 */
public record ExamRequirements(
        ExamType examType,
        Degree degree,
        ProgramStatus programStatus,
        boolean eligible,
        EligibilityReason eligibilityReason,
        ExamRequestSummary request,
        List<ExamRequirement> requirements) {

    public ExamRequirements {
        Objects.requireNonNull(examType, "examType");
        Objects.requireNonNull(degree, "degree");
        Objects.requireNonNull(programStatus, "programStatus");
        Objects.requireNonNull(requirements, "requirements");
        if (requirements.size() < 5 || requirements.size() > 6) {
            throw new IllegalArgumentException("GRADO publishes five or six requirements");
        }
        long distinct = requirements.stream().map(ExamRequirement::documentType).distinct().count();
        if (distinct != requirements.size()) {
            throw new IllegalArgumentException("each document type appears once");
        }
        requirements = List.copyOf(requirements);
        if (eligible == (eligibilityReason != null)) {
            throw new IllegalArgumentException("eligibilityReason is present only when not eligible");
        }
    }
}
