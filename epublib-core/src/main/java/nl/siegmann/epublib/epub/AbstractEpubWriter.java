package nl.siegmann.epublib.epub;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.Resource;
import nl.siegmann.epublib.service.MediatypeService;
import nl.siegmann.epublib.util.IOUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Pieter Heyvaert <pheyvaer.heyvaert@ugent.be>
 */
public abstract class AbstractEpubWriter implements EpubWriter {

    private final static Logger log = LoggerFactory.getLogger(AbstractEpubWriter.class);
    protected static final String EMPTY_NAMESPACE_PREFIX = "";

    protected BookProcessor bookProcessor = BookProcessor.IDENTITY_BOOKPROCESSOR;

    public AbstractEpubWriter() {
        this(BookProcessor.IDENTITY_BOOKPROCESSOR);
    }

    public AbstractEpubWriter(BookProcessor bookProcessor) {
        this.bookProcessor = bookProcessor;
    }

    /**
     * Writes the META-INF/container.xml file.
     *
     * @param resultStream
     * @throws IOException
     */
    protected void writeContainer(ZipOutputStream resultStream) throws IOException {
        resultStream.putNextEntry(new ZipEntry("META-INF/container.xml"));
        Writer out = new OutputStreamWriter(resultStream);
        out.write("<?xml version=\"1.0\"?>\n");
        out.write("<container version=\"1.0\" xmlns=\"urn:oasis:names:tc:opendocument:xmlns:container\">\n");
        out.write("\t<rootfiles>\n");
        out.write("\t\t<rootfile full-path=\"OEBPS/content.opf\" media-type=\"application/oebps-package+xml\"/>\n");
        out.write("\t</rootfiles>\n");
        out.write("</container>");
        out.flush();
    }

    protected Book processBook(Book book) {
        if (bookProcessor != null) {
            book = bookProcessor.processBook(book);
        }
        return book;
    }

    protected void writeResources(Book book, ZipOutputStream resultStream) throws IOException {
        for (Resource resource : book.getResources().getAll()) {
            writeResource(resource, resultStream);
        }
    }

    /**
     * Writes the resource to the resultStream.
     *
     * @param resource
     * @param resultStream
     * @throws IOException
     */
    protected void writeResource(Resource resource, ZipOutputStream resultStream) throws IOException {
        if (resource == null) {
            return;
        }

        try {
            resultStream.putNextEntry(new ZipEntry("OEBPS/" + resource.getHref()));
            InputStream inputStream = resource.getInputStream();
            IOUtil.copy(inputStream, resultStream);
            inputStream.close();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * Stores the mimetype as an uncompressed file in the ZipOutputStream.
     *
     * @param resultStream
     * @throws IOException
     */
    protected void writeMimeType(ZipOutputStream resultStream) throws IOException {
        ZipEntry mimetypeZipEntry = new ZipEntry("mimetype");
        mimetypeZipEntry.setMethod(ZipEntry.STORED);
        byte[] mimetypeBytes = MediatypeService.EPUB.getName().getBytes();
        mimetypeZipEntry.setSize(mimetypeBytes.length);
        mimetypeZipEntry.setCrc(calculateCrc(mimetypeBytes));
        resultStream.putNextEntry(mimetypeZipEntry);
        resultStream.write(mimetypeBytes);
    }

    private long calculateCrc(byte[] data) {
        CRC32 crc = new CRC32();
        crc.update(data);
        return crc.getValue();
    }

    @Override
    public BookProcessor getBookProcessor() {
        return bookProcessor;
    }

    @Override
    public void setBookProcessor(BookProcessor bookProcessor) {
        this.bookProcessor = bookProcessor;
    }

    protected void initTOCResource(Book book, Resource tocResource) {
        Resource currentTocResource = book.getSpine().getTocResource();
        if (currentTocResource != null) {
            book.getResources().remove(currentTocResource.getHref());
        }
        book.getSpine().setTocResource(tocResource);
        book.getResources().add(tocResource);
    }
}
