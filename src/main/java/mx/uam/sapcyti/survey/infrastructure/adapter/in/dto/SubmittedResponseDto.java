package mx.uam.sapcyti.survey.infrastructure.adapter.in.dto;

import java.time.Instant;
import java.util.List;
import mx.uam.sapcyti.survey.domain.model.AcademicTerm;
import mx.uam.sapcyti.survey.domain.model.SurveyResponseMode;

public record SubmittedResponseDto(
        AcademicTerm academicTerm,
        SurveyResponseMode mode,
        List<Long> ueaIds,
        int totalUeas,
        Instant submittedAt) {}
