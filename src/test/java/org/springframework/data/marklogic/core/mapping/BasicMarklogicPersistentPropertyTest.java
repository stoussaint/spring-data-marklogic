package org.springframework.data.marklogic.core.mapping;

import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.springframework.data.annotation.Id;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;

import static org.junit.Assert.assertThat;

/**
 * Unit test for {@link BasicMarklogicPersistentProperty}.
 *
 * @author Stéphane Toussaint
 */
public class BasicMarklogicPersistentPropertyTest {

    @Test
    public void personIdIsExplicitIdProperty() {
        MarklogicPersistentProperty property = getPropertyFor(Person.class, "id");
        assertThat(property.isIdProperty(), CoreMatchers.is(true));
        assertThat(property.isExplicitIdProperty(), CoreMatchers.is(true));
    }

    @Test
    public void companyIdIsImplicitIdProperty() {
        MarklogicPersistentProperty property = getPropertyFor(Company.class, "id");
        assertThat(property.isIdProperty(), CoreMatchers.is(true));
        assertThat(property.isExplicitIdProperty(), CoreMatchers.is(false));
    }
    @Test
    public void identifierAsOneExplicitAndOneImplicitId() {
        MarklogicPersistentProperty property = getPropertyFor(Identifier.class, "id");
        assertThat(property.isIdProperty(), CoreMatchers.is(true));
        assertThat(property.isExplicitIdProperty(), CoreMatchers.is(false));

        MarklogicPersistentProperty property2 = getPropertyFor(Identifier.class, "uuid");
        assertThat(property2.isIdProperty(), CoreMatchers.is(true));
        assertThat(property2.isExplicitIdProperty(), CoreMatchers.is(true));
    }


    class Person {
        @Id
        String id;

        String firstname;
        String lastname;
    }

    class Company {
        String id;
        String name;
    }

    class Identifier {
        @Id
        String uuid;
        String id;
    }

    private <T> MarklogicPersistentProperty getPropertyFor(Class<T> type, String fieldname) {
        return getPropertyFor(new BasicMarklogicPersistentEntity<>(ClassTypeInformation.from(type)), fieldname);
    }

    private MarklogicPersistentProperty getPropertyFor(MarklogicPersistentEntity<?> persistentEntity, String fieldname) {
        return getPropertyFor(persistentEntity, ReflectionUtils.findField(persistentEntity.getType(), fieldname));
    }


    private MarklogicPersistentProperty getPropertyFor(MarklogicPersistentEntity<?> persistentEntity, Field field) {
        return new BasicMarklogicPersistentProperty(field, null, persistentEntity, new SimpleTypeHolder());
    }

}