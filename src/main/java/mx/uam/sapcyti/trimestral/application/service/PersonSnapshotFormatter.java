package mx.uam.sapcyti.trimestral.application.service;

import mx.uam.sapcyti.academic.domain.model.PersonalData;

/**
 * Creates the stable display name stored in trimestral snapshots.
 */
final class PersonSnapshotFormatter {

    private PersonSnapshotFormatter() {
    }

    static String fullName(PersonalData personalData) {
        String secondLastName = personalData.getSecondLastName();
        if (secondLastName == null || secondLastName.isBlank()) {
            return personalData.getFirstName() + " " + personalData.getFirstLastName();
        }
        return personalData.getFirstName()
                + " "
                + personalData.getFirstLastName()
                + " "
                + secondLastName;
    }
}
