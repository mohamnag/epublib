package nl.siegmann.epublib.bookprocessor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

import nl.siegmann.epublib.Constants;
import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.Resource;
import nl.siegmann.epublib.epub.BookProcessor;
import nl.siegmann.epublib.util.NoCloseWriter;
import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.DoctypeToken;

import org.htmlcleaner.EpublibXmlSerializer;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cleans up regular html into xhtml. Uses HtmlCleaner to do this.
 *
 * @author paul
 *
 */
public class Epub2HtmlCleanerBookProcessor extends HtmlBookProcessor implements
        BookProcessor {

    @SuppressWarnings("unused")
    private final static Logger log = LoggerFactory.getLogger(Epub2HtmlCleanerBookProcessor.class);

    private HtmlCleaner htmlCleaner;

    public Epub2HtmlCleanerBookProcessor() {
        this.htmlCleaner = createHtmlCleaner();
    }

    private static HtmlCleaner createHtmlCleaner() {
        HtmlCleaner result = new HtmlCleaner();
        CleanerProperties cleanerProperties = result.getProperties();
        
//        cleanerProperties.setOmitXmlDeclaration(true);
//        cleanerProperties.setOmitDoctypeDeclaration(false);
//        cleanerProperties.setRecognizeUnicodeChars(true);
//        cleanerProperties.setTranslateSpecialEntities(false);
//        cleanerProperties.setIgnoreQuestAndExclam(true);
//        cleanerProperties.setUseEmptyElementTags(false);
        
        cleanerProperties.setOmitXmlDeclaration(true);
        cleanerProperties.setOmitDoctypeDeclaration(false);
        //cleanerProperties.setRecognizeUnicodeChars(true);
        cleanerProperties.setTranslateSpecialEntities(true);
        cleanerProperties.setTransSpecialEntitiesToNCR(true);
        cleanerProperties.setIgnoreQuestAndExclam(true);
        cleanerProperties.setUseEmptyElementTags(false);
        cleanerProperties.setAdvancedXmlEscape(true);
        cleanerProperties.setTransResCharsToNCR(true);
        
        return result;
    }

    public byte[] processHtml(Resource resource, Book book, String outputEncoding) throws IOException {

        // clean html
        TagNode node = htmlCleaner.clean(resource.getReader());

        // post-process cleaned html
        node.addAttribute("xmlns", Constants.NAMESPACE_XHTML);
        node.setDocType(createXHTMLDoctypeToken());

        // write result to output
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Writer writer = new OutputStreamWriter(out, outputEncoding);
        writer = new NoCloseWriter(writer);
        EpublibXmlSerializer xmlSerializer = new EpublibXmlSerializer(htmlCleaner.getProperties(), outputEncoding);
        xmlSerializer.write(node, writer, outputEncoding);
        writer.flush();

        return out.toByteArray();
    }

    private DoctypeToken createXHTMLDoctypeToken() {
        return new DoctypeToken("html", "PUBLIC", "-//W3C//DTD XHTML 1.1//EN", "http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd");
    }
}
