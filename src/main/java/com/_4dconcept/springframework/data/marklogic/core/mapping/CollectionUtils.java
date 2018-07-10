package com._4dconcept.springframework.data.marklogic.core.mapping;

import org.springframework.core.annotation.AnnotationUtils;

import java.lang.reflect.AnnotatedElement;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public class CollectionUtils {

    private CollectionUtils() {}

    public static  Optional<Collection> getCollectionAnnotation(MarklogicPersistentProperty property) {
        final AnnotatedElement[] elements = {
                property.getPropertyDescriptor().getReadMethod(),
                property.getField()
        };

        return Stream.of(elements).map(e -> AnnotationUtils.findAnnotation(e, Collection.class)).filter(Objects::nonNull).findFirst();
    }

}
