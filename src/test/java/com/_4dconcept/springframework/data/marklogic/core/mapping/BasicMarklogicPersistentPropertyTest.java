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
import org.junit.Test;
import org.springframework.data.annotation.Id;
import org.springframework.data.mapping.model.Property;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.util.ReflectionUtils;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.namespace.QName;
import java.lang.reflect.Field;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * Unit test for {@link BasicMarklogicPersistentProperty}.
 *
 * @author Stéphane Toussaint
 */
public class BasicMarklogicPersistentPropertyTest {

    @Test
    public void checkIdPropertyInformation() {
        checkPropertyId(Person.class, "id", true);
        checkPropertyId(Company.class, "id", false);
        checkPropertyId(Identifier.class, "id", false);
        checkPropertyId(Identifier.class, "uuid", true);
    }

    @Test
    public void resolveQName() {
        checkPropertyQName("id", "/super/type");
        checkPropertyQName("name", "/test/type");
        checkPropertyQName("sample", "/test/classtype");
        checkPropertyQName("surname", "/test/classtype");
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

    @XmlTransient
    class AbstractType extends SuperType {
        String sample;
    }

    @XmlRootElement(namespace = "/test/classtype")
    class ImplType extends AbstractType {
        @XmlElement(namespace = "/test/type")
        String name;

        String surname;
    }

    private void checkPropertyId(Class<?> type, String uuid, boolean isExplicit) {
        MarklogicPersistentProperty property = getPropertyFor(type, uuid);
        assertThat(property.isIdProperty(), is(true));
        assertThat(property.isExplicitIdProperty(), is(isExplicit));
    }

    private void checkPropertyQName(String name, String expectedUri) {
        MarklogicPersistentProperty property2 = getPropertyFor(ImplType.class, name);
        QName qName2 = property2.getQName();
        assertThat(qName2.getNamespaceURI(), is(expectedUri));
        assertThat(qName2.getLocalPart(), is(name));
    }

    private <T> MarklogicPersistentProperty getPropertyFor(Class<T> type, String fieldname) {
        return getPropertyFor(new BasicMarklogicPersistentEntity<>(ClassTypeInformation.from(type)), fieldname);
    }

    private MarklogicPersistentProperty getPropertyFor(MarklogicPersistentEntity<?> persistentEntity, String fieldname) {
        return getPropertyFor(persistentEntity, ReflectionUtils.findField(persistentEntity.getType(), fieldname));
    }


    private MarklogicPersistentProperty getPropertyFor(MarklogicPersistentEntity<?> persistentEntity, Field field) {
        return new BasicMarklogicPersistentProperty(Property.of(persistentEntity.getTypeInformation(), field), persistentEntity, SimpleTypeHolder.DEFAULT);
    }

}