package mx.uam.sapcyti.planning.infrastructure.adapter.out;

import java.util.ArrayList;
import java.util.List;
import mx.uam.sapcyti.planning.domain.model.GraduateProgramMark;

public final class PcyotiExcelLayout {

    public static final String SECTION_TITLE = "PCyTI";
    public static final String CLAVE_HEADER = "Clave";
    public static final String NOMBRE_UEA_HEADER = "NOMBRE UEA";
    public static final String MODALIDAD_HEADER = "MODALIDAD";

    private PcyotiExcelLayout() {
    }

    public static List<String> buildHeaders(int year) {
        List<String> headers = new ArrayList<>();
        headers.add(CLAVE_HEADER);
        headers.add(NOMBRE_UEA_HEADER);
        String suffix = String.format("%02d", year % 100);
        headers.add("Gpos " + suffix + "-I");
        headers.add("Cupo " + suffix + "-I");
        headers.add("Gpos " + suffix + "-P");
        headers.add("Cupo " + suffix + "-P");
        headers.add("Gpos " + suffix + "-O");
        headers.add("Cupo " + suffix + "-O");
        for (GraduateProgramMark mark : GraduateProgramMark.ALL) {
            headers.add(mark.name());
        }
        headers.add(MODALIDAD_HEADER);
        return headers;
    }

    public static List<String> legendLines() {
        return List.of(
                "X = Obligatoria para este posgrado",
                "O = Optativa",
                "X/O = ambas",
                "* = se abrirá conforme demanda");
    }
}
