package org.springframework.data.marklogic.core.mapping.event;

import com.marklogic.xcc.Content;
import org.springframework.context.ApplicationEvent;

/**
 * Base {@link ApplicationEvent} triggered by Spring Data Marklogic.
 *
 * @author Stéphane Toussaint
 */
public class MarklogicMappingEvent<T> extends ApplicationEvent {

    private final String uri;
    private final Content content;

    public MarklogicMappingEvent(T source, Content content, String uri) {
        super(source);
        this.content = content;
        this.uri = uri;
    }

    /**
     * @return the uri
     */
    public String getUri() {
        return uri;
    }

    /**
     * @return the content
     */
    public Content getContent() {
        return content;
    }

    /*
     * (non-Javadoc)
     * @see java.util.EventObject#getSource()
     */
    @SuppressWarnings("unchecked")
    @Override
    public T getSource() {
        return (T) super.getSource();
    }
}
