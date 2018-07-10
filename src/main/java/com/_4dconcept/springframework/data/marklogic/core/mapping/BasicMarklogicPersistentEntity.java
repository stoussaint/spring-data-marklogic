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

import com._4dconcept.springframework.data.marklogic.MarklogicUrlUtils;
import org.springframework.data.mapping.model.BasicPersistentEntity;
import org.springframework.data.mapping.model.MappingException;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.StringUtils;

import java.util.Comparator;

/**
 * Specialized value object to capture information of {@link MarklogicPersistentEntity}s.
 * It provide access to entity 'uri', 'defaultCollection'
 *
 * @author Stéphane Toussaint
 */
public class BasicMarklogicPersistentEntity<T> extends BasicPersistentEntity<T, MarklogicPersistentProperty> implements MarklogicPersistentEntity<T> {

    private final String uri;
    private final String defaultCollection;
    private final boolean idInPropertyFragment;

    public BasicMarklogicPersistentEntity(TypeInformation<T> information) {
        this(information, null);
    }

    public BasicMarklogicPersistentEntity(TypeInformation<T> information, Comparator<MarklogicPersistentProperty> comparator) {
        super(information, comparator);

        Class<T> rawType = getTypeInformation().getType();
        String fallback = MarklogicUrlUtils.getPreferredUrlPattern(rawType);

        Document document = this.findAnnotation(Document.class);
        Collection collection = this.findAnnotation(Collection.class);

        if (document != null) {
            this.uri = StringUtils.hasText(document.uri()) ? document.uri() : fallback;
            this.idInPropertyFragment = document.idInPropertyFragment();
        } else {
            this.uri = fallback;
            this.idInPropertyFragment = false;
        }

        if (collection != null) {
            this.defaultCollection = buildDefaultCollection(collection);
        } else {
            this.defaultCollection = null;
        }
    }

    private String buildDefaultCollection(Collection collection) {
        StringBuilder sb = new StringBuilder();
        if (StringUtils.hasText(collection.prefix())) {
            sb.append(collection.prefix());
            sb.append(":");
        }
        if (StringUtils.hasText(collection.value())) {
            sb.append(collection.value());
        }
        return sb.length() > 0 ? sb.toString() : null;
    }

    /**
     * @return the default Class level defined uri
     */
    @Override
    public String getUri() {
        return uri;
    }

    /**
     * @return the default Class level defined defaultCollection
     */
    public String getDefaultCollection() {
        return defaultCollection;
    }

    @Override
    public boolean idInPropertyFragment() {
        return idInPropertyFragment;
    }

    @Override
    protected MarklogicPersistentProperty returnPropertyIfBetterIdPropertyCandidateOrNull(MarklogicPersistentProperty property) {
        if (!property.isIdProperty()) {
            return null; // Not a potential id property. Early exit
        }

        MarklogicPersistentProperty currentIdProperty = getIdProperty();

        if (currentIdProperty != null && currentIdProperty.isExplicitIdProperty()) {
            return null; // An explicit Id property is already set
        } else if (property.isExplicitIdProperty()) {
            return property; // Use this explicit id property
        } else if (currentIdProperty == null) {
            return property;
        }

        throw new MappingException(String.format("Attempt to add id property %s but already have property %s registered "
                + "as id. Check your mapping configuration!", property.getField(), currentIdProperty.getField()));
    }
}