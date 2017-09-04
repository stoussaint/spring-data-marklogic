package com._4dconcept.springframework.data.marklogic.core.mapping;

import org.springframework.data.mapping.context.AbstractMappingContext;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.util.TypeInformation;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;

/**
 * --Description--
 *
 * @author Stéphane Toussaint
 */
public class MarklogicMappingContext extends AbstractMappingContext<BasicMarklogicPersistentEntity<?>, MarklogicPersistentProperty> {

    @Override
    protected <T> BasicMarklogicPersistentEntity<T> createPersistentEntity(TypeInformation<T> typeInformation) {
        return new BasicMarklogicPersistentEntity<>(typeInformation);
    }

    @Override
    protected MarklogicPersistentProperty createPersistentProperty(Field field, PropertyDescriptor descriptor,
            BasicMarklogicPersistentEntity<?> owner, SimpleTypeHolder simpleTypeHolder) {
        return new BasicMarklogicPersistentProperty(field, descriptor, owner, simpleTypeHolder);
    }
}
