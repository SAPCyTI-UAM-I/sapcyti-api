package mx.uam.sapcyti.trimestral.infrastructure.adapter.out.excel;

import java.util.List;

/** Column layout for the official trimestral PCyTI Excel sheet «Original CyTI» (HU-60). */
public final class TrimestralExcelLayout {

    public static final String SHEET_NAME = "Original CyTI";
    public static final String DIVISION = "CBI";

    private TrimestralExcelLayout() {}

    /** Verified A→Y header order; TIPO DE UEA is column G (index 6). */
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
