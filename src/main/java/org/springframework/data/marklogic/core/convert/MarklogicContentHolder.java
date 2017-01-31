package org.springframework.data.marklogic.core.convert;

import java.io.Serializable;

/**
 * --Description--
 *
 * @author Stéphane Toussaint
 */
public class MarklogicContentHolder {

    private Serializable content;

    /**
     * @return the content
     */
    public Serializable getContent() {
        return content;
    }

    /**
     * @param content the content to set
     */
    public void setContent(Serializable content) {
        this.content = content;
    }
}
