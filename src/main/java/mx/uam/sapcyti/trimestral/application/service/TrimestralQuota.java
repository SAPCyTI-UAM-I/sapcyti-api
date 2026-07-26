package mx.uam.sapcyti.trimestral.application.service;

import mx.uam.sapcyti.trimestral.domain.model.QuotaLimit;

record TrimestralQuota(String maxGroups, String capacity) {

    TrimestralQuota {
        maxGroups = normalize(maxGroups);
        capacity = normalize(capacity);
    }

    boolean isOffered() {
        return maxGroups != null && capacity != null;
    }

    Integer finiteMaximumGroups() {
        return QuotaLimit.finiteValue(maxGroups);
    }

    Integer finiteCapacity() {
        return QuotaLimit.finiteValue(capacity);
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
