package nl.siegmann.epublib.epub;

import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlpull.v1.XmlSerializer;

/**
 * Generates an epub file. Not thread-safe, single use object.
 *
 * @author paul
 *
 */
public class Epub2Writer extends AbstractEpubWriter {

    private final static Logger log = LoggerFactory.getLogger(Epub2Writer.class);

    public Epub2Writer() {
        super();
    }

    public Epub2Writer(BookProcessor bookProcessor) {
        super(bookProcessor);
    }

    @Override
    public void write(Book book, OutputStream out) throws IOException {
        book = processBook(book);
        ZipOutputStream resultStream = new ZipOutputStream(out);
        writeMimeType(resultStream);
        writeContainer(resultStream);
        initTOCResource(book);
        writeResources(book, resultStream);
        writePackageDocument(book, resultStream);
        resultStream.close();
    }

    private void initTOCResource(Book book) {
        Resource tocResource;
        
        try {
            tocResource = NCXDocument.createNCXResource(book);
            initTOCResource(book, tocResource);
        } catch (Exception e) {
            log.error("Error writing table of contents: " + e.getClass().getName() + ": " + e.getMessage());
        }
    }

    private void writePackageDocument(Book book, ZipOutputStream resultStream) throws IOException {
        resultStream.putNextEntry(new ZipEntry("OEBPS/content.opf"));
        XmlSerializer xmlSerializer = EpubProcessorSupport.createXmlSerializer(resultStream);
        Epub2PackageDocumentWriter.write(this, xmlSerializer, book);
        xmlSerializer.flush();
    }

    public String getNcxId() {
        return "ncx";
    }

    public String getNcxHref() {
        return "toc.ncx";
    }

    public String getNcxMediaType() {
        return "application/x-dtbncx+xml";
    }
}