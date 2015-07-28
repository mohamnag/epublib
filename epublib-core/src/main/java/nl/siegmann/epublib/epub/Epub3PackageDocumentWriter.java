package nl.siegmann.epublib.epub;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import javax.xml.stream.XMLStreamException;

import nl.siegmann.epublib.Constants;
import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.Guide;
import nl.siegmann.epublib.domain.GuideReference;
import nl.siegmann.epublib.domain.MediaType;
import nl.siegmann.epublib.domain.Resource;
import nl.siegmann.epublib.domain.Spine;
import nl.siegmann.epublib.domain.SpineReference;
import nl.siegmann.epublib.service.MediatypeService;
import nl.siegmann.epublib.util.StringUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xmlpull.v1.XmlSerializer;

/**
 * Writes the opf package document as defined by namespace
 * http://www.idpf.org/2007/opf
 *
 * @author paul
 *
 */
public class Epub3PackageDocumentWriter extends PackageDocumentBase {

    private static final Logger log = LoggerFactory.getLogger(Epub3PackageDocumentWriter.class);

    public static void write(Epub3Writer epubWriter, XmlSerializer serializer, Book book) throws IOException, IllegalArgumentException, IllegalStateException, ParserConfigurationException, SAXException {
        try {
            serializer.startDocument(Constants.CHARACTER_ENCODING, false);
            //serializer.setPrefix(PREFIX_OPF, NAMESPACE_OPF);
            serializer.setPrefix(PREFIX_DUBLIN_CORE, NAMESPACE_DUBLIN_CORE);
            serializer.startTag(null, OPFTags.packageTag);
            serializer.attribute(Epub3Writer.EMPTY_NAMESPACE_PREFIX, "xmlns", "http://www.idpf.org/2007/opf");
            serializer.attribute(Epub3Writer.EMPTY_NAMESPACE_PREFIX, OPFAttributes.version, "3.0");
            serializer.attribute(Epub3Writer.EMPTY_NAMESPACE_PREFIX, OPFAttributes.uniqueIdentifier, BOOK_ID_ID);

            Epub3PackageDocumentMetadataWriter.writeMetaData(book, serializer);

            writeManifest(book, epubWriter, serializer);
            writeSpine(book, epubWriter, serializer);
//			writeGuide(book, epubWriter, serializer);
//			
            serializer.endTag(null, OPFTags.packageTag);
            serializer.endDocument();
            serializer.flush();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Writes the package's spine.
     *
     * @param book
     * @param epubWriter
     * @param serializer
     * @throws IOException
     * @throws IllegalStateException
     * @throws IllegalArgumentException
     * @throws XMLStreamException
     */
    private static void writeSpine(Book book, Epub3Writer epubWriter, XmlSerializer serializer) throws IllegalArgumentException, IllegalStateException, IOException {
        serializer.startTag(null, OPFTags.spine);

        if (book.getCoverPage() != null // there is a cover page
                && book.getSpine().findFirstResourceById(book.getCoverPage().getId()) < 0) { // cover page is not already in the spine
            // write the cover html file
            serializer.startTag(null, OPFTags.itemref);
            serializer.attribute(Epub3Writer.EMPTY_NAMESPACE_PREFIX, OPFAttributes.idref, book.getCoverPage().getId());
            serializer.attribute(Epub3Writer.EMPTY_NAMESPACE_PREFIX, OPFAttributes.linear, "no");
            serializer.endTag(null, OPFTags.itemref);
        }

        writeSpineItems(book.getSpine(), serializer);
        serializer.endTag(null, OPFTags.spine);
    }

    private static void writeManifest(Book book, Epub3Writer epubWriter, XmlSerializer serializer) throws IllegalArgumentException, IllegalStateException, IOException, ParserConfigurationException, SAXException {
        serializer.startTag(null, OPFTags.manifest);

        for (Resource resource : getAllResourcesSortById(book)) {
            writeItem(book, resource, serializer);
        }

        writeNavItem(book, serializer);

        serializer.endTag(null, OPFTags.manifest);
    }

    private static List<Resource> getAllResourcesSortById(Book book) {
        List<Resource> allResources = new ArrayList<Resource>(book.getResources().getAll());
        Collections.sort(allResources, new Comparator<Resource>() {

            @Override
            public int compare(Resource resource1, Resource resource2) {
                return resource1.getId().compareToIgnoreCase(resource2.getId());
            }
        });
        return allResources;
    }

    /**
     * Writes a resources as an item element
     *
     * @param resource
     * @param serializer
     * @throws IOException
     * @throws IllegalStateException
     * @throws IllegalArgumentException
     * @throws XMLStreamException
     */
    private static void writeItem(Book book, Resource resource, XmlSerializer serializer) throws IllegalArgumentException, IllegalStateException, IOException, ParserConfigurationException, SAXException {
        if (resource == null
                || (resource.getMediaType() == MediatypeService.NCX
                && book.getSpine().getTocResource() != null)) {
            return;
        }

        if (StringUtil.isBlank(resource.getId())) {
            log.error("resource id must not be empty (href: " + resource.getHref() + ", mediatype:" + resource.getMediaType() + ")");
            return;
        }

        if (StringUtil.isBlank(resource.getHref())) {
            log.error("resource href must not be empty (id: " + resource.getId() + ", mediatype:" + resource.getMediaType() + ")");
            return;
        }

        if (resource.getMediaType() == null) {
            log.error("resource mediatype must not be empty (id: " + resource.getId() + ", href:" + resource.getHref() + ")");
            return;
        }

        boolean isScripted = false;

        if (resource.getMediaType() == MediatypeService.XHTML) {
            Document d = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(resource.getInputStream());
            NodeList l = d.getElementsByTagName("script");
            isScripted = (l.getLength() > 0);
        }

        boolean isCoverImage = resource.equals(book.getCoverImage());

        serializer.startTag(null, OPFTags.item);
        serializer.attribute(Epub3Writer.EMPTY_NAMESPACE_PREFIX, OPFAttributes.id, resource.getId());
        serializer.attribute(Epub3Writer.EMPTY_NAMESPACE_PREFIX, OPFAttributes.href, resource.getHref());
        serializer.attribute(Epub3Writer.EMPTY_NAMESPACE_PREFIX, OPFAttributes.media_type, resource.getMediaType().getName());

        String properties = null;

        if (isScripted) {
            properties = "scripted";
        }

        if (isCoverImage) {
            if (properties == null) {
                properties = "cover-image";
            } else {
                properties += " cover-image";
            }
        }

        if (properties != null) {
            serializer.attribute(Epub3Writer.EMPTY_NAMESPACE_PREFIX, OPFAttributes.properties, properties);
        }

        serializer.endTag(null, OPFTags.item);
    }

    private static void writeNavItem(Book book, XmlSerializer serializer) throws IOException {
        serializer.startTag(null, OPFTags.item);
        serializer.attribute(null, OPFAttributes.id, "toc");
        serializer.attribute(null, OPFAttributes.properties, "nav");
        serializer.attribute(null, OPFAttributes.href, "toc.xhtml");
        serializer.attribute(null, OPFAttributes.media_type, "application/xhtml+xml");
        serializer.endTag(null, OPFTags.item);
    }

    /**
     * List all spine references
     *
     * @throws IOException
     * @throws IllegalStateException
     * @throws IllegalArgumentException
     */
    private static void writeSpineItems(Spine spine, XmlSerializer serializer) throws IllegalArgumentException, IllegalStateException, IOException {
        for (SpineReference spineReference : spine.getSpineReferences()) {
            serializer.startTag(null, OPFTags.itemref);
            serializer.attribute(Epub3Writer.EMPTY_NAMESPACE_PREFIX, OPFAttributes.idref, spineReference.getResourceId());
            if (!spineReference.isLinear()) {
                serializer.attribute(Epub3Writer.EMPTY_NAMESPACE_PREFIX, OPFAttributes.linear, OPFValues.no);
            }
            serializer.endTag(null, OPFTags.itemref);
        }
    }

    private static void writeGuide(Book book, Epub3Writer epubWriter, XmlSerializer serializer) throws IllegalArgumentException, IllegalStateException, IOException {
        serializer.startTag(null, OPFTags.guide);
        //ensureCoverPageGuideReferenceWritten(book.getGuide(), epubWriter, serializer);
        for (GuideReference reference : book.getGuide().getReferences()) {
            writeGuideReference(reference, serializer);
        }
        serializer.endTag(null, OPFTags.guide);
    }

    private static void ensureCoverPageGuideReferenceWritten(Guide guide,
            Epub2Writer epubWriter, XmlSerializer serializer) throws IllegalArgumentException, IllegalStateException, IOException {
        if (!(guide.getGuideReferencesByType(GuideReference.COVER).isEmpty())) {
            return;
        }
        Resource coverPage = guide.getCoverPage();
        if (coverPage != null) {
            writeGuideReference(new GuideReference(guide.getCoverPage(), GuideReference.COVER, GuideReference.COVER), serializer);
        }
    }

    private static void writeGuideReference(GuideReference reference, XmlSerializer serializer) throws IllegalArgumentException, IllegalStateException, IOException {
        if (reference == null) {
            return;
        }
        serializer.startTag(NAMESPACE_OPF, OPFTags.reference);
        serializer.attribute(Epub2Writer.EMPTY_NAMESPACE_PREFIX, OPFAttributes.type, reference.getType());
        serializer.attribute(Epub2Writer.EMPTY_NAMESPACE_PREFIX, OPFAttributes.href, reference.getCompleteHref());
        if (StringUtil.isNotBlank(reference.getTitle())) {
            serializer.attribute(Epub2Writer.EMPTY_NAMESPACE_PREFIX, OPFAttributes.title, reference.getTitle());
        }
        serializer.endTag(NAMESPACE_OPF, OPFTags.reference);
    }
}
