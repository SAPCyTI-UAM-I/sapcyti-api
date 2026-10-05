package mx.uam.sapcyti.offering.infrastructure.adapter.out;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import mx.uam.sapcyti.offering.domain.exception.FileFormatInvalidException;
import mx.uam.sapcyti.offering.domain.port.out.UeaBulkFileParserPort.ParsedBulkRow;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UeaBulkFileAdapterTest {

    private final UeaBulkFileAdapter adapter = new UeaBulkFileAdapter();

    @Test
    @DisplayName("parses valid CSV with template headers")
    void parseValidCsv() {
        String csv = """
                Clave,NOMBRE UEA,TIPO,MODALIDAD,H. TEOR.,H. PRAC.,Tipo formacion,Creditos
                2156024,REDES Y PROTOCOLOS,OBLIGATORIA,MIXTA,3,3,Basica,9
                """;

        var rows = adapter.parse("catalog.csv", csv.getBytes(StandardCharsets.UTF_8));

        assertThat(rows).hasSize(1);
        ParsedBulkRow row = rows.get(0);
        assertThat(row.rowNumber()).isEqualTo(1);
        assertThat(row.clave()).isEqualTo("2156024");
        assertThat(row.tipoFormacion()).isEqualTo("Basica");
    }

    @Test
    @DisplayName("rejects PDF extension")
    void rejectPdf() {
        assertThatThrownBy(() -> adapter.parse("catalog.pdf", new byte[] {1, 2, 3}))
                .isInstanceOf(FileFormatInvalidException.class);
    }

    @Test
    @DisplayName("rejects CSV with wrong headers")
    void rejectWrongHeaders() {
        String csv = "Clave,Nombre,TIPO\n2156024,REDES,OBLIGATORIA\n";
        assertThatThrownBy(() -> adapter.parse("catalog.csv", csv.getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(FileFormatInvalidException.class);
    }

    @Test
    @DisplayName("parseCsvLine handles quoted commas")
    void parseCsvLineWithQuotes() {
        var cells = UeaBulkFileAdapter.parseCsvLine("\"2156024\",\"REDES, PROTOCOLOS\",OBLIGATORIA,MIXTA,3,3,Basica,9");
        assertThat(cells.get(1)).isEqualTo("REDES, PROTOCOLOS");
    }
}
