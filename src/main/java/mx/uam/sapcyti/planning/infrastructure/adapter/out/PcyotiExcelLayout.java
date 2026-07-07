package mx.uam.sapcyti.planning.infrastructure.adapter.out;

import java.util.ArrayList;
import java.util.List;
import mx.uam.sapcyti.planning.domain.model.GraduateProgramMark;

/** Column/row layout for the official PCyTI annual-plan Excel (HU-52). Text only, no styling colors. */
public final class PcyotiExcelLayout {

    /** Marker used by the format-check parser to locate the PCyTI section in an uploaded file. */
    public static final String SECTION_TITLE = "PCyTI";
    /** Section heading written on the exported Excel (the posgrado's full name). */
    public static final String EXPORT_SECTION_TITLE =
            "POSGRADO EN CIENCIAS Y TECNOLOGÍAS DE LA INFORMACIÓN";
    public static final String CLAVE_HEADER = "Clave";
    public static final String NOMBRE_UEA_HEADER = "NOMBRE UEA";

    private PcyotiExcelLayout() {
    }

    /** Top title, e.g. "PLANEACION ANUAL 2026 POSGRADO". */
    public static String title(int year) {
        return "PLANEACION ANUAL " + year + " POSGRADO";
    }

    /** Legend block as [symbol, description] rows, one per Excel row. */
    public static List<String[]> legendRows() {
        return List.of(
                new String[] {"Clave", "Siete dígitos"},
                new String[] {"X", "Obligatoria para este posgrado"},
                new String[] {"O", "Optativa"},
                new String[] {"X/O", "Funciona Obligatoria u Obligatoria para este posgrado"},
                new String[] {"*", "Se abrirá conforme demanda por alumnos"});
    }

    public static List<String> buildHeaders(int year) {
        String suffix = String.format("%02d", year % 100);
        List<String> headers = new ArrayList<>();
        headers.add(CLAVE_HEADER);
        headers.add(NOMBRE_UEA_HEADER);
        headers.add("No. de Gpos " + suffix + "-I");
        headers.add("Cupos " + suffix + "-I");
        headers.add("No. de Gpos " + suffix + "-P");
        headers.add("Cupos " + suffix + "-P");
        headers.add("No. de Gpos " + suffix + "-O");
        headers.add("Cupos " + suffix + "-O");
        for (GraduateProgramMark mark : GraduateProgramMark.ALL) {
            headers.add(headerFor(mark));
        }
        return headers;
    }

    private static String headerFor(GraduateProgramMark mark) {
        return mark == GraduateProgramMark.PCYTI ? "PCyTI" : mark.name();
    }
}
