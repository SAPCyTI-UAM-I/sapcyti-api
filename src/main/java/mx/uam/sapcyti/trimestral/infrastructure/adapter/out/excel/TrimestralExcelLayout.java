package mx.uam.sapcyti.trimestral.infrastructure.adapter.out.excel;

import java.util.List;

/** Column layout for the official trimestral PCyTI Excel sheet «Original CyTI» (HU-60). */
public final class TrimestralExcelLayout {

    public static final String SHEET_NAME = "Original CyTI";
    public static final String DIVISION = "CBI";

    /** Official Sistemas Escolares headers occupy A→Y (0..24). */
    public static final int COLUMN_OBS = 24;

    /**
     * Student listing lives past the official headers (no header cells): name in Z, matrícula in AA,
     * per-student obs in AB. Reference: {@code PCYTI 26O_con_nombres_y_matriculas.xlsx}.
     */
    public static final int COLUMN_STUDENT_NAME = 25;

    public static final int COLUMN_STUDENT_ENROLLMENT = 26;

    public static final int COLUMN_STUDENT_OBS = 27;

    private TrimestralExcelLayout() {}

    /** Verified A→Y header order; TIPO DE UEA is column G (index 6). Students are not in OBS. */
    public static List<String> headers() {
        return List.of(
                "DIV",
                "TRIM",
                "CVEUEA",
                "UEA",
                "GRUPO",
                "CUPO",
                "TIPO DE UEA",
                "NEMP",
                "PROF",
                "LINI",
                "LFIN",
                "SaLun",
                "MaINI",
                "MaFIN",
                "SaMar",
                "MieINI",
                "MieFIN",
                "SaMier",
                "JueINI",
                "JueFIN",
                "SaJue",
                "VieINI",
                "VieFIN",
                "SaVier",
                "OBS");
    }

    public static String filename(String term) {
        return "PCYTI " + term + ".xlsx";
    }
}
