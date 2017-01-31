package org.springframework.data.marklogic.core.convert;

import org.springframework.data.convert.EntityConverter;
import org.springframework.data.convert.EntityReader;
import org.springframework.data.marklogic.core.mapping.MarklogicPersistentEntity;
import org.springframework.data.marklogic.core.mapping.MarklogicPersistentProperty;

/**
 * Central Marklogic specific converter interface which combines {@link MarklogicWriter} and {@link MarklogicReader}.
 *
 * @author Stéphane Toussaint
 */
public interface MarklogicConverter extends
        EntityConverter<MarklogicPersistentEntity<?>, MarklogicPersistentProperty, Object, MarklogicContentHolder>,
        MarklogicWriter<Object>,
        EntityReader<Object, MarklogicContentHolder> {

    /**
     * {@link MarklogicConverter} has the ability to compute content uri
     * @param source the stored object
     * @return the computed uri
     */
    String computeUri(Object source);

    /**
     * {@link MarklogicConverter} has the ability to compute default collection
     * @param source the stored object
     * @return the computed default collection
     */
    String computeDefaultCollection(Object source);

}
