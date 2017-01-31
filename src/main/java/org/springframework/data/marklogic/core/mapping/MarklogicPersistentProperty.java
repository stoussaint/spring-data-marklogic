package org.springframework.data.marklogic.core.mapping;

import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;

import javax.xml.namespace.QName;

/**
 * Marklogic specific {@link org.springframework.data.mapping.PersistentProperty} extension.
 *
 * @author Stéphane Toussaint
 */
public interface MarklogicPersistentProperty extends PersistentProperty<MarklogicPersistentProperty> {

    /**
     * Returns whether the property is explicitly marked as an identifier property of the owning {@link PersistentEntity}.
     *
     * @return
     */
    boolean isExplicitIdProperty();

    /**
     * Retrieve the full qualified name of the property ()
     * @return
     */
    QName getQName();

}