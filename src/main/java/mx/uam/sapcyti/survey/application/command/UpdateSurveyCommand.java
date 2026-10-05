package mx.uam.sapcyti.survey.application.command;

import java.time.Instant;

public record UpdateSurveyCommand(
        String term,
        Instant opensAt,
        Instant closesAt,
        String introMessage) {}
