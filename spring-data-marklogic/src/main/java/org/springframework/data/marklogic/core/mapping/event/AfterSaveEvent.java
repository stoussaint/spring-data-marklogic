package org.springframework.data.marklogic.core.mapping.event;

import com.marklogic.xcc.Content;

/**
 * Even being thrown after content is saved
 *
 * @author Stéphane Toussaint
 */
public class AfterSaveEvent<T> extends MarklogicMappingEvent<T> {

    /**
     * Creates new {@link AfterSaveEvent}
     *
     * @param source The stored domain object
     * @param content The content saved
     * @param uri The uri the content is saved
     */
    public AfterSaveEvent(T source, Content content, String uri) {
        super(source, content, uri);
    }
}
