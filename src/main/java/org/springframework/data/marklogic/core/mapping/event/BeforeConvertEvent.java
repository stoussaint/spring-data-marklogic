package org.springframework.data.marklogic.core.mapping.event;

/**
 * Even being thrown before a domain object is converted to be persisted
 *
 * @author Stéphane Toussaint
 */
public class BeforeConvertEvent<T> extends MarklogicMappingEvent<T> {

    /**
     * Create new {@link BeforeConvertEvent}
     * @param source The domain object to be converted
     * @param uri The location the content will be persisted
     */
    public BeforeConvertEvent(T source, String uri) {
        super(source, null, uri);
    }
}
