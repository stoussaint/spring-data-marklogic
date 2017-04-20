package org.springframework.data.marklogic.core.mapping;

import org.springframework.data.mapping.PersistentEntity;

/**
 * --Description--
 *
 * @author Stéphane Toussaint
 */
public interface MarklogicPersistentEntity<T> extends PersistentEntity<T, MarklogicPersistentProperty> {

    /**
     * Returns the uri the entity shall be persisted to.
     * @return
     */
    String getUri();

    /**
     * Returns the default defaultCollection the entity should be created with
     * @return
     */
    String getDefaultCollection();

    /**
     * Return true if the id is to be found in property fragment
     * @return
     */
    boolean idInPropertyFragment();

}
