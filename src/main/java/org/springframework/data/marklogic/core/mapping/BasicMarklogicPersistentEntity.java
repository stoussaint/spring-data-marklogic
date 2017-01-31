package org.springframework.data.marklogic.core.mapping;

import org.springframework.data.mapping.model.BasicPersistentEntity;
import org.springframework.data.mapping.model.MappingException;
import org.springframework.data.marklogic.MarklogicUrlUtils;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.StringUtils;

import java.util.Comparator;

/**
 * --Description--
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

        if (document != null) {
            this.uri = StringUtils.hasText(document.uri()) ? document.uri() : fallback;
            this.defaultCollection = StringUtils.hasText(document.defaultCollection()) ? document.defaultCollection() : null;
            this.idInPropertyFragment = document.idInPropertyFragment();
        } else {
            this.uri = fallback;
            this.defaultCollection = null;
            this.idInPropertyFragment = false;
        }
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