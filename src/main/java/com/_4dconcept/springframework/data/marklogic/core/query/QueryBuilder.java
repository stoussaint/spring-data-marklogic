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
package com._4dconcept.springframework.data.marklogic.core.query;

import com._4dconcept.springframework.data.marklogic.MarklogicCollectionUtils;
import com._4dconcept.springframework.data.marklogic.MarklogicUtils;
import com._4dconcept.springframework.data.marklogic.core.MarklogicOperationOptions;
import com._4dconcept.springframework.data.marklogic.core.MarklogicOperations;
import com._4dconcept.springframework.data.marklogic.core.mapping.MarklogicMappingContext;
import com._4dconcept.springframework.data.marklogic.core.mapping.MarklogicPersistentEntity;
import com._4dconcept.springframework.data.marklogic.core.mapping.MarklogicPersistentProperty;
import org.springframework.dao.TypeMismatchDataAccessException;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.mapping.PropertyHandler;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.util.TypeInformation;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Default {@link Query} builder
 *
 * @author stoussaint
 * @since 2017-07-31
 */
public class QueryBuilder {

    @Nullable
    private Class<?> type;

    @Nullable
    private Example example;

    @Nullable
    private Sort sort;

    @Nullable
    private Pageable pageable;

    private MappingContext<? extends MarklogicPersistentEntity<?>, MarklogicPersistentProperty> mappingContext;

    private MarklogicOperationOptions options = new MarklogicOperationOptions() {};

    private MarklogicCollectionUtils marklogicCollectionUtils = new MarklogicCollectionUtils() {};

    @SuppressWarnings("WeakerAccess") // Authorize client to use a default mapping context
    public QueryBuilder() {
        this.mappingContext = new MarklogicMappingContext();
    }

    public QueryBuilder(MarklogicOperations marklogicOperations) {
        this.mappingContext = marklogicOperations.getConverter().getMappingContext();
    }

    public QueryBuilder ofType(Class<?> type) {
        Assert.isNull(example, "Query by example or by type are mutually exclusive");
        this.type = type;
        return this;
    }

    public QueryBuilder alike(Example example) {
        Assert.isNull(type, "Query by example or by type are mutually exclusive");
        this.example = example;
        return this;
    }

    public QueryBuilder with(Sort sort) {
        this.sort = sort;
        return this;
    }

    public QueryBuilder with(Pageable pageable) {
        this.pageable = pageable;
        return this;
    }

    public QueryBuilder options(MarklogicOperationOptions options) {
        this.options = options;
        return this;
    }

    public Query build() {
        Query query = new Query();

        String collection = MarklogicUtils.expandsExpression(determinePrincipalCollection(), determineTargetClass());
        if (collection != null) {
            query.setCollection(collection);
        }

        if (example != null) {
            Criteria criteria = buildCriteriaFromEntityProperties(example.getProbe());
            if (criteria != null) {
                query.setCriteria(criteria);
            }
        }

        if (sort != null) {
            query.setSortCriteria(prepareSortCriteria(sort));
        } else if (pageable != null) {
            query.setSortCriteria(prepareSortCriteria(pageable.getSort()));
            query.setSkip(pageable.getOffset());
            query.setLimit(pageable.getPageSize());
        }

        return query;
    }

    @Nullable
    private String determinePrincipalCollection() {
        if (options.defaultCollection() != null) {
            return options.defaultCollection();
        } else {
            Class<?> targetClass = determineTargetClass();
            return targetClass != null ? retrievePersistentEntity(targetClass).getDefaultCollection() : null;
        }
    }

    @Nullable
    private Criteria buildCriteriaFromEntityProperties(Object bean) {
        Deque<Criteria> stack = new ArrayDeque<>();

        MarklogicPersistentEntity<?> entity = retrievePersistentEntity(bean.getClass());
        PersistentPropertyAccessor propertyAccessor = entity.getPropertyAccessor(bean);

        entity.doWithProperties((PropertyHandler<MarklogicPersistentProperty>) property -> {
            Object value = propertyAccessor.getProperty(property);
            if (hasContent(value)) {
                Criteria newCriteria = buildCriteriaFromProperty(property, value);
                if (newCriteria != null) {
                    stackNewCriteria(stack, newCriteria);
                }
            }
        });

        return stack.isEmpty() ? null : stack.peek();
    }

    @Nullable
    private Criteria buildCriteriaFromProperty(MarklogicPersistentProperty property, Object value) {
        Optional<? extends TypeInformation<?>> typeInformation = StreamSupport.stream(property.getPersistentEntityTypes().spliterator(), false).findFirst();
        if (typeInformation.isPresent()) {
            return buildCriteriaFromEntityProperties(value);
        } else {
            Criteria criteria = new Criteria();

            if (value instanceof Collection) {
                criteria.setOperator(Criteria.Operator.OR);
                Collection<?> collection = (Collection<?>) value;
                criteria.setCriteriaObject(collection.stream()
                        .map(o -> buildCriteriaFromProperty(property, o))
                        .collect(Collectors.toList())
                );
            } else {
                if (marklogicCollectionUtils.getCollectionAnnotation(property).isPresent()) {
                    criteria.setOperator(Criteria.Operator.COLLECTION);
                } else {
                    criteria.setQname(property.getQName());
                }

                criteria.setCriteriaObject(value);
            }

            return criteria;
        }
    }

    private void stackNewCriteria(Deque<Criteria> stack, Criteria newCriteria) {
        Criteria criteria = stack.peek();
        if (criteria != null) {
            if (Criteria.Operator.AND == criteria.getOperator()) {
                criteria.add(newCriteria);
            } else {
                wrapInAndCriteria(stack, newCriteria, criteria);
            }
        } else {
            stack.push(newCriteria);
        }
    }

    private void wrapInAndCriteria(Deque<Criteria> stack, Criteria newCriteria, Criteria criteria) {
        Criteria andCriteria = new Criteria(Criteria.Operator.AND, new ArrayList<>(Arrays.asList(criteria, newCriteria)));
        stack.pop();
        stack.push(andCriteria);
    }

    private boolean hasContent(@Nullable Object value) {
        return value != null && (!(value instanceof Collection) || !((Collection) value).isEmpty());
    }

    private List<SortCriteria> prepareSortCriteria(Sort sort) {
        Class<?> targetType = example != null ? example.getProbeType() : options.entityClass();
        Assert.notNull(targetType, "Query needs a explicit type to resolve sort order");

        MarklogicPersistentEntity<?> persistentEntity = retrievePersistentEntity(targetType);
        return buildSortCriteria(sort, persistentEntity);
    }

    private List<SortCriteria> buildSortCriteria(Sort sort, MarklogicPersistentEntity<?> entity) {
        ArrayList<SortCriteria> sortCriteriaList = new ArrayList<>();

        for (Sort.Order order : sort) {
            MarklogicPersistentProperty persistentProperty = entity.getPersistentProperty(order.getProperty());

            if (persistentProperty == null) {
                continue;
            }

            SortCriteria sortCriteria = new SortCriteria(persistentProperty.getQName());
            if (!order.isAscending()) {
                sortCriteria.setDescending(true);
            }
            sortCriteriaList.add(sortCriteria);
        }

        return sortCriteriaList;
    }

    private MarklogicPersistentEntity<?> retrievePersistentEntity(Class<?> aClass) {
        MarklogicPersistentEntity<?> persistentEntity = mappingContext.getPersistentEntity(aClass);

        if (persistentEntity == null) {
            throw new TypeMismatchDataAccessException(String.format("No Persistent Entity information found for the class %s", aClass));
        }

        return persistentEntity;
    }

    @Nullable
    private Class<?> determineTargetClass() {
        if (options.entityClass() != null) {
            return options.entityClass();
        }

        if (example != null) {
            return example.getProbeType();
        }

        if (type != null) {
            return type;
        }

        return null;
    }
}