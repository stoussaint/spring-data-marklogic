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
package com._4dconcept.springframework.data.marklogic.repository.query;

import com._4dconcept.springframework.data.marklogic.MarklogicCollectionUtils;
import com._4dconcept.springframework.data.marklogic.core.mapping.MarklogicMappingContext;
import com._4dconcept.springframework.data.marklogic.core.mapping.MarklogicPersistentProperty;
import com._4dconcept.springframework.data.marklogic.core.query.Criteria;
import com._4dconcept.springframework.data.marklogic.core.query.CriteriaDefinition;
import com._4dconcept.springframework.data.marklogic.core.query.Query;
import com._4dconcept.springframework.data.marklogic.core.query.QueryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.TypeMismatchDataAccessException;
import org.springframework.data.domain.Sort;
import org.springframework.data.mapping.PersistentPropertyPath;
import org.springframework.data.repository.query.ParameterAccessor;
import org.springframework.data.repository.query.parser.AbstractQueryCreator;
import org.springframework.data.repository.query.parser.Part;
import org.springframework.data.repository.query.parser.PartTree;
import org.springframework.lang.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Custom query creator to create Marklogic criterias.
 *
 * @author stoussaint
 * @since 2017-08-03
 */
public class MarklogicQueryCreator extends AbstractQueryCreator<Query, Criteria> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MarklogicQueryCreator.class);

    private MarklogicMappingContext context;

    private Class<?> returnedType;

    private MarklogicCollectionUtils marklogicCollectionUtils = new MarklogicCollectionUtils() {};

    MarklogicQueryCreator(PartTree tree, ParameterAccessor parameters, MarklogicMappingContext context, Class<?> returnedType) {
        super(tree, parameters);

        this.context = context;
        this.returnedType = returnedType;
    }

    @Override
    protected Criteria create(Part part, Iterator<Object> iterator) {
        PersistentPropertyPath<MarklogicPersistentProperty> path = context.getPersistentPropertyPath(part.getProperty());
        MarklogicPersistentProperty property = path.getLeafProperty();

        if (property == null) {
            throw new TypeMismatchDataAccessException(String.format("No persistent pntity information found for the path %s", path));
        }

        return from(part, property, iterator);
    }

    @Override
    protected Criteria and(Part part, Criteria base, Iterator<Object> iterator) {
        Criteria newCriteria = create(part, iterator);
        if (! Criteria.Operator.AND.equals(base.getOperator())) {
            return new Criteria(Criteria.Operator.AND, new ArrayList<>(Arrays.asList(base, newCriteria)));
        }

        base.add(newCriteria);
        return base;
    }

    @Override
    protected Criteria or(Criteria base, Criteria criteria) {
        if (! Criteria.Operator.OR.equals(base.getOperator())) {
            return new Criteria(Criteria.Operator.OR, new ArrayList<>(Arrays.asList(base, criteria)));
        }

        base.add(criteria);
        return base;
    }

    @Override
    protected Query complete(@Nullable Criteria criteria, Sort sort) {
        QueryBuilder queryBuilder = new QueryBuilder(context);

        Query query = queryBuilder.ofType(returnedType).with(criteria).with(sort).build();

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Created query {}", query);
        }

        return query;
    }

    /**
     * Populates the given {@link CriteriaDefinition} depending on the {@link Part} given.
     *
     * @param part the current method part
     * @param property the resolve entity property
     * @param parameters the method parameters iterator
     * @return the build criteria
     */
    private Criteria from(Part part, MarklogicPersistentProperty property, Iterator<Object> parameters) {

        Part.Type type = part.getType();

        switch (type) {
//            case BEFORE:
//            case AFTER:
//            case GREATER_THAN:
//            case GREATER_THAN_EQUAL:
//            case LESS_THAN:
//            case LESS_THAN_EQUAL:
//            case BETWEEN:
//            case NOT_IN:
//            case NOT_LIKE
//            case LIKE:
//            case STARTING_WITH:
//            case ENDING_WITH:
//            case NEAR:
//            case WITHIN:
//            case REGEX:
//            case NOT_CONTAINING:
            case IN:
            case CONTAINING:
                return computeContainingCriteria(property, parameters.next());
            case IS_NULL:
            case IS_EMPTY:
                return new Criteria(Criteria.Operator.EMPTY, computeSimpleCriteria(property, null));
            case IS_NOT_NULL:
            case IS_NOT_EMPTY:
            case EXISTS:
                return new Criteria(Criteria.Operator.EXISTS, computeSimpleCriteria(property, null));
            case TRUE:
                return computeSimpleCriteria(property, true);
            case FALSE:
                return computeSimpleCriteria(property, false);
            case NEGATING_SIMPLE_PROPERTY:
                return computeSimpleCriteria(property, parameters.next(), true);
            case SIMPLE_PROPERTY:
                return computeSimpleCriteria(property, parameters.next());
            default:
                throw new IllegalArgumentException("Unsupported keyword : " + type);
        }
    }

    private Criteria computeContainingCriteria(MarklogicPersistentProperty property, Object parameter) {
        return buildSimpleCriteria(property, parameter, Criteria.Operator.OR);
    }

    private Criteria computeSimpleCriteria(MarklogicPersistentProperty property, @Nullable Object parameter) {
        return computeSimpleCriteria(property, parameter, false);
    }

    private Criteria computeSimpleCriteria(MarklogicPersistentProperty property, @Nullable Object parameter, boolean inverse) {
        Criteria criteria = buildSimpleCriteria(property, parameter, Criteria.Operator.AND);
        if (inverse)
            return new Criteria(Criteria.Operator.NOT, criteria);
        else {
            return criteria;
        }
    }

    private Criteria buildSimpleCriteria(MarklogicPersistentProperty property, @Nullable Object parameter, Criteria.Operator groupOperator) {
        if (parameter instanceof List) {
            List<?> list = (List<?>) parameter;
            List<Criteria> criteriaList = list.stream().map(o -> buildCriteria(property, o)).collect(Collectors.toList());
            return new Criteria(groupOperator, criteriaList);
        } else {
            return buildCriteria(property, parameter);
        }
    }

    private Criteria buildCriteria(MarklogicPersistentProperty property, @Nullable Object value) {
        if (marklogicCollectionUtils.getCollectionAnnotation(property).isPresent()) {
            return new Criteria(Criteria.Operator.COLLECTION, value);
        } else {
            return new Criteria(property.getQName(), value);
        }
    }

}
