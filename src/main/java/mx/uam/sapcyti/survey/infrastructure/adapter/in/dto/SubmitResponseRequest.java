package mx.uam.sapcyti.survey.infrastructure.adapter.in.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import mx.uam.sapcyti.survey.domain.model.AcademicTerm;
import mx.uam.sapcyti.survey.domain.model.SurveyResponseMode;

public record SubmitResponseRequest(
        @NotNull AcademicTerm academicTerm,
        @NotNull SurveyResponseMode mode,
        @NotNull List<Long> ueaIds) {}
