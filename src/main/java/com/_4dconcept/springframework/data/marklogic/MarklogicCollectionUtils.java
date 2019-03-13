package com._4dconcept.springframework.data.marklogic;

import com._4dconcept.springframework.data.marklogic.core.mapping.Collection;
import com._4dconcept.springframework.data.marklogic.core.mapping.MarklogicPersistentEntity;
import com._4dconcept.springframework.data.marklogic.core.mapping.MarklogicPersistentProperty;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.mapping.PropertyHandler;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.util.StringUtils;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public interface MarklogicCollectionUtils {

    default <T> List<String> extractCollections(T entity, MappingContext<? extends MarklogicPersistentEntity<?>, MarklogicPersistentProperty> mappingContext) {
        ArrayList<String> collections = new ArrayList<>();

        collections.addAll(extractCollectionsFromProperties(entity, mappingContext));
        collections.addAll(extractCollectionsFromMethods(entity));

        return collections.stream().distinct().collect(Collectors.toList());
    }

    default Optional<Collection> getCollectionAnnotation(MarklogicPersistentProperty property) {
        ArrayList<AnnotatedElement> annotatedElements = new ArrayList<>();

        property.getReadMethod().ifPresent(annotatedElements::add);

        if (property.getField() != null) {
            annotatedElements.add(property.getField());
        }

        return annotatedElements.stream().map(e -> AnnotationUtils.findAnnotation(e, Collection.class)).filter(Objects::nonNull).findFirst();
    }

    default Optional<Collection> getCollectionAnnotation(Method method) {
        return Optional.ofNullable(AnnotationUtils.findAnnotation(method, Collection.class));
    }

    default List<String> doWithCollectionValue(Object value, Collection collection) {
        if (value instanceof java.util.Collection) {
            ArrayList<String> collections = new ArrayList<>();
            java.util.Collection<?> values = (java.util.Collection<?>) value;
            for (Object o : values) {
                if (StringUtils.hasText(collection.prefix())) {
                    collections.add(String.format("%s:%s", collection.prefix(), o.toString()));
                } else {
                    collections.add(o.toString());
                }
            }
            return collections;
        } else {
            if (StringUtils.hasText(collection.prefix())) {
                return Collections.singletonList(String.format("%s:%s", collection.prefix(), value.toString()));
            } else {
                return Collections.singletonList(value.toString());
            }

        }
    }

    default <T> List<String> extractCollectionsFromProperties(T entity, MappingContext<? extends MarklogicPersistentEntity<?>, MarklogicPersistentProperty> mappingContext) {
        MarklogicPersistentEntity<?> persistentEntity = mappingContext.getPersistentEntity(entity.getClass());

        if (persistentEntity == null) {
            return Collections.emptyList();
        }

        List<String> collections = new ArrayList<>();
        persistentEntity.doWithProperties((PropertyHandler<MarklogicPersistentProperty>) property -> {
            Object value = persistentEntity.getPropertyAccessor(entity).getProperty(property);
            if (value != null) {
                Optional<Collection> collectionAnnotation = getCollectionAnnotation(property);
                if (collectionAnnotation.isPresent()) {
                    collections.addAll(doWithCollectionValue(value, collectionAnnotation.get()));
                } else {
                    StreamSupport.stream(property.getPersistentEntityTypes().spliterator(), false)
                            .findFirst()
                            .ifPresent(ti -> collections.addAll(extractCollections(value, mappingContext)));
                }
            }
        });

        return collections;
    }

    default <T> List<String> extractCollectionsFromMethods(T entity) {
        List<String> collections = new ArrayList<>();

        for (Method method : entity.getClass().getMethods()) {
            if (method.getParameterCount() > 0) {
                continue;
            }

            try {
                Optional<Collection> collectionAnnotation = getCollectionAnnotation(method);
                if (collectionAnnotation.isPresent()) {
                    Object value = method.invoke(entity);
                    if (value != null) {
                        collections.addAll(doWithCollectionValue(value, collectionAnnotation.get()));
                    }
                }
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new UnsupportedOperationException(String.format("Unable to read value from %s", method), e);
            }
        }

        return collections;
    }

}
