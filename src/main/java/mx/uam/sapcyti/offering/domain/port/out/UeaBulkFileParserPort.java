package mx.uam.sapcyti.offering.domain.port.out;

import java.util.List;
import mx.uam.sapcyti.offering.domain.model.FormationType;
import mx.uam.sapcyti.offering.domain.model.UeaModality;
import mx.uam.sapcyti.offering.domain.model.UeaType;

/**
 * ACL for parsing bulk UEA catalog files (.xlsx / .csv).
 */
public interface UeaBulkFileParserPort {

    List<ParsedBulkRow> parse(String filename, byte[] content);

    record ParsedBulkRow(
            int rowNumber,
            String clave,
            String nombre,
            String tipo,
            String modalidad,
            String horasTeoria,
            String horasPractica,
            String tipoFormacion,
            String creditos,
            boolean parseError) {

        public ParsedBulkRow(
                int rowNumber,
                String clave,
                String nombre,
                String tipo,
                String modalidad,
                String horasTeoria,
                String horasPractica,
                String tipoFormacion,
                String creditos) {
            this(rowNumber, clave, nombre, tipo, modalidad, horasTeoria, horasPractica, tipoFormacion, creditos, false);
        }
    }
}
