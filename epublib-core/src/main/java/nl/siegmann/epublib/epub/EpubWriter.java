package nl.siegmann.epublib.epub;

import java.io.IOException;
import java.io.OutputStream;
import nl.siegmann.epublib.domain.Book;

/**
 *
 * @author Pieter Heyvaert <pheyvaer.heyvaert@ugent.be>
 */
public interface EpubWriter {
    
    void write(Book book, OutputStream out) throws IOException;
    
    BookProcessor getBookProcessor();
    
    void setBookProcessor(BookProcessor bookProcessor);
}
