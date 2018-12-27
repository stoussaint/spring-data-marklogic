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

import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.model.AnnotationBasedPersistentProperty;
import org.springframework.data.mapping.model.SimpleTypeHolder;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlNsForm;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchema;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.namespace.QName;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

/**
 * Special {@link AnnotationBasedPersistentProperty} that resolve entity fields {@link QName}
 *
 * @author Stéphane Toussaint
 */
public class BasicMarklogicPersistentProperty extends AnnotationBasedPersistentProperty<MarklogicPersistentProperty>
        implements MarklogicPersistentProperty {

    private static final Set<String> SUPPORTED_ID_PROPERTY_NAMES = new HashSet<>();

    static {
        SUPPORTED_ID_PROPERTY_NAMES.add("id");
    }

    private static final String XML_DEFAULT = "##default";

    public BasicMarklogicPersistentProperty(Field field, PropertyDescriptor propertyDescriptor,
                                            MarklogicPersistentEntity<?> owner, SimpleTypeHolder simpleTypeHolder) {
        super(field, propertyDescriptor, owner, simpleTypeHolder);
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.mapping.PersistentProperty#isIdProperty()
     */
    @Override
    public boolean isIdProperty() {
        return super.isIdProperty() || SUPPORTED_ID_PROPERTY_NAMES.contains(getName());
    }

    @Override
    public boolean isExplicitIdProperty() {
        return super.isIdProperty(); // Consider an explicit Id if annotation present.
    }

    @Override
    public QName getQName() {
        String namespaceUri = null;
        String localName = null;

        XmlElement xmlElement = this.findAnnotation(XmlElement.class);
        if (xmlElement != null) {
            if (!xmlElement.namespace().equals(XML_DEFAULT)) {
                namespaceUri = xmlElement.namespace();
            }

            localName = xmlElement.name().equals(XML_DEFAULT) ? getName() : xmlElement.name();
        }

        if (namespaceUri == null) {
            namespaceUri = resolvesNamespaceUriFromEnclosingType(this.getField().getDeclaringClass());
        }

        if (namespaceUri == null) namespaceUri = "";
        if (localName == null) localName = getName();

        return new QName(namespaceUri, localName);
    }

    @Override
    protected Association<MarklogicPersistentProperty> createAssociation() {
        return new Association<>(this, null);
    }

    private String resolvesNamespaceUriFromEnclosingType(Class<?> type) {
        XmlTransient xmlTransient = type.getAnnotation(XmlTransient.class);

        if (xmlTransient != null) {
            return resolvesNamespaceUriFromEnclosingType(this.getOwner().getType());
        }

        XmlSchema xmlSchema = type.getPackage().getAnnotation(XmlSchema.class);
        if (xmlSchema != null && xmlSchema.elementFormDefault().equals(XmlNsForm.QUALIFIED)) {
            XmlRootElement xmlRootElement = type.getAnnotation(XmlRootElement.class);
            if (xmlRootElement != null && !xmlRootElement.namespace().equals(XML_DEFAULT)) {
                return xmlRootElement.namespace();
            } else {
                return xmlSchema.namespace();
            }
        }

        return null;
    }
}
