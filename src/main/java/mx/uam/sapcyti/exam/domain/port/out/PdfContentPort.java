package mx.uam.sapcyti.exam.domain.port.out;

import mx.uam.sapcyti.exam.domain.model.PdfContentInspection;

/**
 * Opens PDF bytes with a parser. The production adapter is the only implementation.
 * This is not the object-storage port.
 */
public interface PdfContentPort {

    PdfContentInspection inspect(byte[] bytes);
}
