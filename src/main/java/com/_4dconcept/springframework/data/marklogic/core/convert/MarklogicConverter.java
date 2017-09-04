package com._4dconcept.springframework.data.marklogic.core.convert;

import com._4dconcept.springframework.data.marklogic.core.mapping.MarklogicPersistentEntity;
import com._4dconcept.springframework.data.marklogic.core.mapping.MarklogicPersistentProperty;
import org.springframework.data.convert.EntityConverter;
import org.springframework.data.convert.EntityReader;

/**
 * Central Marklogic specific converter interface which combines {@link MarklogicWriter} and {@link MarklogicReader}.
 *
 * @author Stéphane Toussaint
 */
public interface MarklogicConverter extends
        EntityConverter<MarklogicPersistentEntity<?>, MarklogicPersistentProperty, Object, MarklogicContentHolder>,
        MarklogicWriter<Object>,
        EntityReader<Object, MarklogicContentHolder> {

}
