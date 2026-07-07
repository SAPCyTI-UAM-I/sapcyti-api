package mx.uam.sapcyti.planning.domain.model;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public enum GraduateProgramMark {
    P_FIS,
    P_MAT,
    MCMAI,
    P_QUIM,
    P_IQUIM,
    P_IBIOM,
    PCYTI,
    PEMA,
    EFMC;

    public static final List<GraduateProgramMark> ALL = List.of(values());

    public static Optional<GraduateProgramMark> fromCode(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(mark -> mark.name().equalsIgnoreCase(code.trim()))
                .findFirst();
    }
}
