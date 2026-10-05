package mx.uam.sapcyti.survey.infrastructure.adapter.in.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import mx.uam.sapcyti.survey.domain.model.SurveyStatus;

public record EnrollmentSurveyResponse(
        @Schema(example = "1") Long id,
        @Schema(example = "26O") String term,
        SurveyStatus status,
        Instant opensAt,
        Instant closesAt,
        String introMessage,
        long responseCount,
        String suggestedTerm) {}
