package com._4dconcept.springframework.data.marklogic.repository.support;

import com._4dconcept.springframework.data.marklogic.core.mapping.MarklogicPersistentEntity;
import com._4dconcept.springframework.data.marklogic.repository.query.MarklogicEntityInformation;
import org.springframework.data.repository.core.support.PersistentEntityInformation;

import java.io.Serializable;

/**
 * {@link MarklogicEntityInformation} implementation using a {@link MarklogicPersistentEntity} instance to lookup the necessary
 * information. Can be configured with a custom uri or default collection to be returned which will override the one returned by the
 * {@link MarklogicPersistentEntity} if given.
 *
 * @author Stéphane Toussaint
 */
public class MappingMarklogicEntityInformation<T, ID extends Serializable> extends PersistentEntityInformation<T, ID>
        implements MarklogicEntityInformation<T, ID> {

    private final MarklogicPersistentEntity<T> entityMetadata;
    private final String customUri;
    private final String customDefaultCollection;

    public MappingMarklogicEntityInformation(MarklogicPersistentEntity<T> entity) {
        this(entity, null, null);
    }

    public MappingMarklogicEntityInformation(MarklogicPersistentEntity<T> entity, String customUri, String customDefaultCollection) {
        super(entity);

        this.entityMetadata = entity;
        this.customUri = customUri;
        this.customDefaultCollection = customDefaultCollection;
    }

    @Override
    public String getUri() {
        return customUri == null ? entityMetadata.getUri() : customUri;
    }

    @Override
    public String getDefaultCollection() {
        return customDefaultCollection == null ? entityMetadata.getDefaultCollection() : customDefaultCollection;
    }

    @Override
    public boolean idInPropertyFragment() {
        return entityMetadata.idInPropertyFragment();
    }
}
