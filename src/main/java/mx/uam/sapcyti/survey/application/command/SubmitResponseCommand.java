package mx.uam.sapcyti.survey.application.command;

import java.util.List;
import mx.uam.sapcyti.survey.domain.model.AcademicTerm;
import mx.uam.sapcyti.survey.domain.model.SurveyResponseMode;

public record SubmitResponseCommand(
        AcademicTerm academicTerm,
        SurveyResponseMode mode,
        List<Long> ueaIds) {}
