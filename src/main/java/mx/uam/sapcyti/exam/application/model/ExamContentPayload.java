package mx.uam.sapcyti.exam.application.model;

import mx.uam.sapcyti.exam.domain.model.OpenedObject;

/**
 * PDF bytes already opened for the caller's current reference.
 * The HTTP body is {@link OpenedObject#body()}, not this record as JSON.
 */
public record ExamContentPayload(String disposition, String originalName, OpenedObject object) {
}
