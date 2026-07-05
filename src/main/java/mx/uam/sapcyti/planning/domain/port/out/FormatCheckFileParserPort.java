package mx.uam.sapcyti.planning.domain.port.out;

import java.util.List;

public interface FormatCheckFileParserPort {

    ParsedFormatFile parse(String filename, byte[] content);

    record ParsedUeaRow(String clave, String nombre) {}

    record ParsedFormatFile(List<ParsedUeaRow> rows, List<String> programColumns) {}
}
