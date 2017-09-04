package org.springframework.data.marklogic.core.mapping.event;

import com.marklogic.xcc.Content;

/**
 * Even being thrown before content is saved
 *
 * @author Stéphane Toussaint
 */
public class BeforeSaveEvent<T> extends MarklogicMappingEvent<T> {

    /**
     * Creates new {@link BeforeSaveEvent}
     *
     * @param source the domain object to be stored
     * @param content the content to be stored
     * @param uri the location the content will be stored
     */
    public BeforeSaveEvent(T source, Content content, String uri) {
        super(source, content, uri);
    }
}
