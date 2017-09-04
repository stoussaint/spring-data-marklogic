package com._4dconcept.springframework.data.marklogic.core.query;

import javax.xml.namespace.QName;

/**
 * @author stoussaint
 * @since 2017-08-03
 */
public interface CriteriaDefinition {

    /**
     * Get {@link Object} representation.
     *
     * @return
     */
    Object getCriteriaObject();

    /**
     * Get the element {@literal QName}.
     *
     * @return
     */
    QName getQname();

}
