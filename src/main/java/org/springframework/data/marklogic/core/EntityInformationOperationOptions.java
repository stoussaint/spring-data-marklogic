package org.springframework.data.marklogic.core;

import org.springframework.data.marklogic.repository.query.MarklogicEntityInformation;

import java.io.Serializable;

/**
 * --Description--
 *
 * @author Stéphane Toussaint
 */
public class EntityInformationOperationOptions<T, ID extends Serializable> implements MarklogicCreateOperationOptions {

    private MarklogicEntityInformation<T, ID> entityInformation;

    private String[] extraCollections;

    public EntityInformationOperationOptions(MarklogicEntityInformation<T, ID> entityInformation) {
        this(entityInformation, null);
    }

    public EntityInformationOperationOptions(MarklogicEntityInformation<T, ID> entityInformation, String[] extraCollections) {
        this.entityInformation = entityInformation;
        this.extraCollections = extraCollections;
    }

    @Override
    public String defaultCollection() {
        return entityInformation.getDefaultCollection();
    }

    @Override
    public String uri() {
        return entityInformation.getUri();
    }

    @Override
    public String[] extraCollections() {
        return extraCollections;
    }

    @Override
    public boolean idInPropertyFragment() {
        return entityInformation.idInPropertyFragment();
    }
}
