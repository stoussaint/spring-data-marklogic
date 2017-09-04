package org.springframework.data.marklogic.core;

import org.springframework.data.marklogic.repository.query.MarklogicEntityInformation;

/**
 * --Description--
 *
 * @author Stéphane Toussaint
 */
public class EntityInformationOperationOptions implements MarklogicCreateOperationOptions {

    private MarklogicEntityInformation entityInformation;

    private String[] extraCollections;

    public EntityInformationOperationOptions(MarklogicEntityInformation entityInformation) {
        this(entityInformation, null);
    }

    public EntityInformationOperationOptions(MarklogicEntityInformation entityInformation, String[] extraCollections) {
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

    @Override
    public Class entityClass() {
        return entityInformation.getJavaType();
    }
}
