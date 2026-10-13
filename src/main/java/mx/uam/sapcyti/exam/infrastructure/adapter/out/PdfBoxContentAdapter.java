package mx.uam.sapcyti.exam.infrastructure.adapter.out;

import java.io.IOException;
import mx.uam.sapcyti.exam.domain.model.PdfContentInspection;
import mx.uam.sapcyti.exam.domain.port.out.PdfContentPort;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.springframework.stereotype.Component;

/**
 * Production {@link PdfContentPort}. Opens the bytes with PDFBox.
 * A declared content type, a {@code .pdf} extension and a leading signature are not consulted.
 * There is no size ceiling. Do not register a second implementation beside this one.
 * This adapter does not talk to the object bucket.
 */
@Component
public class PdfBoxContentAdapter implements PdfContentPort {

    @Override
    public PdfContentInspection inspect(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return PdfContentInspection.rejected();
        }
        // parse(false) refuses a truncated file. Loader.loadPDF is lenient and would accept it.
        try (RandomAccessReadBuffer source = new RandomAccessReadBuffer(bytes);
                PDDocument document = new PDFParser(source).parse(false)) {
            if (hasActiveContent(document)) {
                return PdfContentInspection.rejected();
            }
            return PdfContentInspection.ok();
        } catch (IOException | RuntimeException ex) {
            return PdfContentInspection.rejected();
        }
    }

    /**
     * Active material the parser can see: JavaScript, launch actions, additional actions and XFA.
     * Finding some of it does not promise that every risk was removed.
     */
    private static boolean hasActiveContent(PDDocument document) throws IOException {
        PDDocumentCatalog catalog = document.getDocumentCatalog();
        if (catalog == null) {
            return false;
        }
        if (dictionaryIsActive(catalog.getCOSObject())) {
            return true;
        }
        PDAcroForm form = catalog.getAcroForm();
        if (form != null && (form.hasXFA() || dictionaryIsActive(form.getCOSObject()))) {
            return true;
        }
        if (form != null) {
            for (PDField field : form.getFieldTree()) {
                if (dictionaryIsActive(field.getCOSObject())) {
                    return true;
                }
            }
        }
        for (PDPage page : document.getPages()) {
            if (dictionaryIsActive(page.getCOSObject())) {
                return true;
            }
            for (PDAnnotation annotation : page.getAnnotations()) {
                if (dictionaryIsActive(annotation.getCOSObject())) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean dictionaryIsActive(COSDictionary dictionary) {
        if (dictionary == null) {
            return false;
        }
        if (dictionary.containsKey(COSName.JS) || dictionary.containsKey(COSName.JAVA_SCRIPT)) {
            return true;
        }
        if (dictionary.containsKey(COSName.AA)) {
            return true;
        }
        if (isActiveAction(dictionary.getDictionaryObject(COSName.A))) {
            return true;
        }
        if (isActiveAction(dictionary.getDictionaryObject(COSName.OPEN_ACTION))) {
            return true;
        }
        COSDictionary names = dictionary.getCOSDictionary(COSName.NAMES);
        return names != null && names.containsKey(COSName.JAVA_SCRIPT);
    }

    private static boolean isActiveAction(COSBase action) {
        if (!(action instanceof COSDictionary dictionary)) {
            return false;
        }
        if (dictionary.containsKey(COSName.JS) || dictionary.containsKey(COSName.JAVA_SCRIPT)) {
            return true;
        }
        COSBase subtype = dictionary.getDictionaryObject(COSName.S);
        if (!(subtype instanceof COSName name)) {
            return false;
        }
        return "JavaScript".equals(name.getName()) || "Launch".equals(name.getName());
    }
}
