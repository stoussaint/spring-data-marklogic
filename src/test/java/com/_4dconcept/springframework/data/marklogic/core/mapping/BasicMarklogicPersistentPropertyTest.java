/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com._4dconcept.springframework.data.marklogic.core.mapping;

import com._4dconcept.springframework.data.marklogic.core.mapping.namespaceaware.SuperType;
import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.springframework.data.annotation.Id;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.util.ReflectionUtils;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.namespace.QName;
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

    @Test
    public void resolveQName() throws Exception {
        MarklogicPersistentProperty property = getPropertyFor(ImplType.class, "id");
        QName qName = property.getQName();
        assertThat(qName.getNamespaceURI(), CoreMatchers.is("/super/type"));
        assertThat(qName.getLocalPart(), CoreMatchers.is("id"));

        MarklogicPersistentProperty property2 = getPropertyFor(ImplType.class, "name");
        QName qName2 = property2.getQName();
        assertThat(qName2.getNamespaceURI(), CoreMatchers.is("/test/type"));
        assertThat(qName2.getLocalPart(), CoreMatchers.is("name"));
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

    class AbstractType extends SuperType {}

    @XmlRootElement
    class ImplType extends AbstractType {
        @XmlElement(namespace = "/test/type")
        String name;
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