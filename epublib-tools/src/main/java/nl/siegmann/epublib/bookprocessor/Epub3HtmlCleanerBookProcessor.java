package nl.siegmann.epublib.bookprocessor;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Iterator;
import java.util.List;
import nl.siegmann.epublib.Constants;

import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.Resource;
import nl.siegmann.epublib.epub.BookProcessor;
import nl.siegmann.epublib.util.NoCloseWriter;
import org.htmlcleaner.BaseToken;
import org.htmlcleaner.CData;

import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.ContentNode;
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
public class Epub3HtmlCleanerBookProcessor extends HtmlBookProcessor implements
        BookProcessor {

    @SuppressWarnings("unused")
    private final static Logger log = LoggerFactory.getLogger(Epub3HtmlCleanerBookProcessor.class);

    private final HtmlCleaner htmlCleaner;

    public Epub3HtmlCleanerBookProcessor() {
        this.htmlCleaner = createHtmlCleaner();
    }

    private static HtmlCleaner createHtmlCleaner() {
        HtmlCleaner result = new HtmlCleaner();
        CleanerProperties cleanerProperties = result.getProperties();

        cleanerProperties.setOmitXmlDeclaration(true);
        cleanerProperties.setOmitDoctypeDeclaration(false);
        //cleanerProperties.setRecognizeUnicodeChars(true);
        cleanerProperties.setTranslateSpecialEntities(false);
        //cleanerProperties.setTransSpecialEntitiesToNCR(true);
        cleanerProperties.setIgnoreQuestAndExclam(true);
        cleanerProperties.setUseEmptyElementTags(false);
        cleanerProperties.setAdvancedXmlEscape(true);
        cleanerProperties.setTransResCharsToNCR(true);
        cleanerProperties.setUseCdataForScriptAndStyle(true);

        return result;
    }

    public byte[] processHtml(Resource resource, Book book, String outputEncoding) throws IOException {

        BufferedReader in = new BufferedReader(resource.getReader());
        String line;
        StringBuilder rslt = new StringBuilder();
        while ((line = in.readLine()) != null) {
            rslt.append(line);
            rslt.append('\n');
        }
        
        String html = rslt.toString();
        //System.out.println("console: " + resource.getHref() + " --- " + html);
        
        html = HTMLNameToHTMLNumberFixer.fix(html);
        System.out.println(html.indexOf('\n'));

        // clean html
        TagNode node = htmlCleaner.clean(html);
        printer(node);
        
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
        return new DoctypeToken("html", null, null, null);
    }
    
    private void printer(TagNode tagNode) {
        List<? extends BaseToken> tagChildren = tagNode.getAllChildren();
        if (tagChildren.isEmpty()) {
            //printer
        } else {
            Iterator<? extends BaseToken> childrenIt = tagChildren.iterator();
            
            while ( childrenIt.hasNext() ) {
                Object item = childrenIt.next();
                   	
                if (item != null) {
                	if (item instanceof CData) {
                            System.out.println(((CData)item).getContentWithoutStartAndEndTokens());
                	} else if ( item instanceof ContentNode ) {
                            System.out.println(((ContentNode)item).getContent());
                    } else {
                            System.out.println(((BaseToken)item).toString());
                    }
                }
            }
        }
    }
}
