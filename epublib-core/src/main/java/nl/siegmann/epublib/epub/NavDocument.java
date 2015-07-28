package nl.siegmann.epublib.epub;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.xml.stream.FactoryConfigurationError;

import nl.siegmann.epublib.Constants;
import nl.siegmann.epublib.domain.CreatorContributor;
import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.Identifier;
import nl.siegmann.epublib.domain.Resource;
import nl.siegmann.epublib.domain.TOCReference;
import nl.siegmann.epublib.domain.TableOfContents;
import nl.siegmann.epublib.service.MediatypeService;
import nl.siegmann.epublib.util.ResourceUtil;
import nl.siegmann.epublib.util.StringUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xmlpull.v1.XmlSerializer;

/**
 * Writes the ncx document as defined by namespace
 * http://www.daisy.org/z3986/2005/ncx/
 *
 * @author paul
 *
 */
public class NavDocument {

    public static final String NAMESPACE_NCX = "http://www.daisy.org/z3986/2005/ncx/";
    public static final String NAMESPACE_EPUB = "http://www.idpf.org/2007/ops";
    public static final String PREFIX_NCX = "ncx";
    public static final String NCX_ITEM_ID = "ncx";
    public static final String DEFAULT_NAV_HREF = "toc.xhtml";
    public static final String PREFIX_DTB = "dtb";

    private static final Logger log = LoggerFactory.getLogger(NavDocument.class);

    private interface NCXTags {

        String ncx = "ncx";
        String meta = "meta";
        String navPoint = "navPoint";
        String navMap = "navMap";
        String navLabel = "navLabel";
        String content = "content";
        String text = "text";
        String docTitle = "docTitle";
        String docAuthor = "docAuthor";
        String head = "head";
    }
    
    private interface EPUBTags {

        String type = "type";
    }

    private interface NCXAttributes {

        String src = "src";
        String name = "name";
        String content = "content";
        String id = "id";
        String playOrder = "playOrder";
        String clazz = "class";
        String version = "version";
    }
    
    public static Resource read(Book book, EpubReader epubReader) {
        Resource ncxResource = null;
        if (book.getSpine().getTocResource() == null) {
            log.error("Book does not contain a table of contents file");
            return ncxResource;
        }
        try {
            ncxResource = book.getSpine().getTocResource();
            if (ncxResource == null) {
                return ncxResource;
            }
            Document ncxDocument = ResourceUtil.getAsDocument(ncxResource);
            Element navMapElement = DOMUtil.getFirstElementByTagNameNS(ncxDocument.getDocumentElement(), NAMESPACE_NCX, NCXTags.navMap);
            TableOfContents tableOfContents = new TableOfContents(readTOCReferences(navMapElement.getChildNodes(), book));
            book.setTableOfContents(tableOfContents);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return ncxResource;
    }

    private static List<TOCReference> readTOCReferences(NodeList navpoints, Book book) {
        if (navpoints == null) {
            return new ArrayList<TOCReference>();
        }
        List<TOCReference> result = new ArrayList<TOCReference>(navpoints.getLength());
        for (int i = 0; i < navpoints.getLength(); i++) {
            Node node = navpoints.item(i);
            if (node.getNodeType() != Document.ELEMENT_NODE) {
                continue;
            }
            if (!(node.getLocalName().equals(NCXTags.navPoint))) {
                continue;
            }
            TOCReference tocReference = readTOCReference((Element) node, book);
            result.add(tocReference);
        }
        return result;
    }

    private static TOCReference readTOCReference(Element navpointElement, Book book) {
        String label = readNavLabel(navpointElement);
        String tocResourceRoot = StringUtil.substringBeforeLast(book.getSpine().getTocResource().getHref(), '/');
        if (tocResourceRoot.length() == book.getSpine().getTocResource().getHref().length()) {
            tocResourceRoot = "";
        } else {
            tocResourceRoot = tocResourceRoot + "/";
        }
        String reference = StringUtil.collapsePathDots(tocResourceRoot + readNavReference(navpointElement));
        String href = StringUtil.substringBefore(reference, Constants.FRAGMENT_SEPARATOR_CHAR);
        String fragmentId = StringUtil.substringAfter(reference, Constants.FRAGMENT_SEPARATOR_CHAR);
        Resource resource = book.getResources().getByHref(href);
        if (resource == null) {
            log.error("Resource with href " + href + " in NCX document not found");
        }
        TOCReference result = new TOCReference(label, resource, fragmentId);
        readTOCReferences(navpointElement.getChildNodes(), book);
        result.setChildren(readTOCReferences(navpointElement.getChildNodes(), book));
        return result;
    }

    private static String readNavReference(Element navpointElement) {
        Element contentElement = DOMUtil.getFirstElementByTagNameNS(navpointElement, NAMESPACE_NCX, NCXTags.content);
        String result = DOMUtil.getAttribute(contentElement, NAMESPACE_NCX, NCXAttributes.src);
        try {
            result = URLDecoder.decode(result, Constants.CHARACTER_ENCODING);
        } catch (UnsupportedEncodingException e) {
            log.error(e.getMessage());
        }
        return result;
    }

    private static String readNavLabel(Element navpointElement) {
        Element navLabel = DOMUtil.getFirstElementByTagNameNS(navpointElement, NAMESPACE_NCX, NCXTags.navLabel);
        return DOMUtil.getTextChildrenContent(DOMUtil.getFirstElementByTagNameNS(navLabel, NAMESPACE_NCX, NCXTags.text));
    }

    public static void write(Epub2Writer epubWriter, Book book, ZipOutputStream resultStream) throws IOException {
        resultStream.putNextEntry(new ZipEntry(book.getSpine().getTocResource().getHref()));
        XmlSerializer out = EpubProcessorSupport.createXmlSerializer(resultStream);
        write(out, book);
        out.flush();
    }

    /**
     * Generates a resource containing an xml document containing the table of
     * contents of the book in ncx format.
     *
     * @param xmlSerializer the serializer used
     * @param book the book to serialize
     *
     * @throws FactoryConfigurationError
     * @throws IOException
     * @throws IllegalStateException
     * @throws IllegalArgumentException
     */
    public static void write(XmlSerializer xmlSerializer, Book book) throws IllegalArgumentException, IllegalStateException, IOException {
        write(xmlSerializer, book.getMetadata().getIdentifiers(), book.getTitle(), book.getMetadata().getAuthors(), book.getTableOfContents());
    }

    public static Resource createNavResource(Book book) throws IllegalArgumentException, IllegalStateException, IOException {
        return createNavResource(book.getMetadata().getIdentifiers(), book.getTitle(), book.getMetadata().getAuthors(), book.getTableOfContents());
    }

    public static Resource createNavResource(List<Identifier> identifiers, String title, List<CreatorContributor> authors, TableOfContents tableOfContents) throws IllegalArgumentException, IllegalStateException, IOException {
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        XmlSerializer out = EpubProcessorSupport.createXmlSerializer(data);
        write(out, identifiers, title, authors, tableOfContents);
        Resource resource = new Resource(NCX_ITEM_ID, data.toByteArray(), DEFAULT_NAV_HREF, MediatypeService.NCX);
        return resource;
    }

    public static void write(XmlSerializer serializer, List<Identifier> identifiers, String title, List<CreatorContributor> authors, TableOfContents tableOfContents) throws IllegalArgumentException, IllegalStateException, IOException {
        serializer.startDocument(Constants.CHARACTER_ENCODING, false);
        
        serializer.startTag(null, "html");
        serializer.attribute(null, "xmlns", "http://www.w3.org/1999/xhtml");
        writeHead(serializer, title);
        writeBody(serializer, tableOfContents);
        serializer.endTag(null, "html");

        serializer.endDocument();
    }
    
    private static void writeHead(XmlSerializer serializer, String title) throws IOException {
        serializer.startTag(null, "head");
        
        serializer.startTag(null, "title");
        serializer.text(title);
        serializer.endTag(null, "title");
                
        serializer.endTag(null, "head");
    }
    
    private static void writeBody(XmlSerializer serializer, TableOfContents tableOfContents) throws IOException {
        serializer.startTag(null, "body");
        serializer.attribute(NAMESPACE_EPUB, EPUBTags.type, "frontmatter");
        
        serializer.startTag(null, "nav");
        serializer.attribute(NAMESPACE_EPUB, EPUBTags.type, "toc");
        
        serializer.startTag(null, "ol");
        
        for (TOCReference ref : tableOfContents.getTocReferences()) {
            Resource resource = ref.getResource();
            serializer.startTag(null, "li");
            serializer.startTag(null, "a");
            serializer.attribute(null, "href", resource.getHref());
            serializer.text(ref.getTitle());
            serializer.endTag(null, "a");
            serializer.endTag(null, "li");
        }
        
        serializer.endTag(null, "ol");
        serializer.endTag(null, "nav");
        
        serializer.endTag(null, "body");
    }
}