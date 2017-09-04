package com._4dconcept.springframework.data.marklogic.repository.query;

import org.springframework.data.repository.core.EntityInformation;

import java.io.Serializable;

/**
 * Marklogic specific {@link EntityInformation}.
 *
 * @author Stéphane Toussaint
 */
public interface MarklogicEntityInformation<T, ID extends Serializable> extends EntityInformation<T, ID> {

    /**
     * Returns the uri the entity shall be persisted to.
     * @return
     */
    String getUri();

    /**
     * Returns the default collection the entity will be persisted in
     * @return
     */
    String getDefaultCollection();

    /**
     * Return true if the id element is expected to be find in property fragment
     * @return
     */
    boolean idInPropertyFragment();

}
