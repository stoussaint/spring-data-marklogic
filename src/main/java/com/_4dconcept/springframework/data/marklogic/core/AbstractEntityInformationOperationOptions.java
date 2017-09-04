package com._4dconcept.springframework.data.marklogic.core;

import com._4dconcept.springframework.data.marklogic.repository.query.MarklogicEntityInformation;

import java.io.Serializable;

/**
 * --Description--
 *
 * @author Stéphane Toussaint
 */
public class AbstractEntityInformationOperationOptions<T, ID extends Serializable> implements MarklogicOperationOptions {

    protected MarklogicEntityInformation<T, ID> entityInformation;

    public AbstractEntityInformationOperationOptions(MarklogicEntityInformation<T, ID> entityInformation) {
        this.entityInformation = entityInformation;
    }

    @Override
    public String defaultCollection() {
        return entityInformation.getDefaultCollection();
    }

}
