package com._4dconcept.springframework.data.marklogic;

import com._4dconcept.springframework.data.marklogic.core.mapping.Collection;
import com._4dconcept.springframework.data.marklogic.core.mapping.MarklogicPersistentProperty;
import org.springframework.core.annotation.AnnotationUtils;

import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;

public interface MarklogicCollectionUtils {

    default Optional<Collection> getCollectionAnnotation(MarklogicPersistentProperty property) {
        ArrayList<AnnotatedElement> annotatedElements = new ArrayList<>();

        if (property.getPropertyDescriptor() != null && property.getPropertyDescriptor().getReadMethod() != null) {
            annotatedElements.add(property.getPropertyDescriptor().getReadMethod());
        }

        if (property.getField() != null) {
            annotatedElements.add(property.getField());
        }

        return annotatedElements.stream().map(e -> AnnotationUtils.findAnnotation(e, Collection.class)).filter(Objects::nonNull).findFirst();
    }

}
