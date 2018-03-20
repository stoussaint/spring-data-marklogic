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
import org.springframework.expression.Expression;
import org.springframework.expression.ParserContext;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Stack;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Default {@link Query} builder
 *
 * @author stoussaint
 * @since 2017-07-31
 */
public class QueryBuilder {

    private static final SpelExpressionParser PARSER = new SpelExpressionParser();
    private @Nullable
    Class<?> type;
    private @Nullable
    Example example;
    private @Nullable
    Sort sort;
    private @Nullable
    Pageable pageable;
    private MappingContext<? extends MarklogicPersistentEntity<?>, MarklogicPersistentProperty> mappingContext = new MarklogicMappingContext();
    private MarklogicOperationOptions options = new MarklogicOperationOptions() {
    };

    public QueryBuilder() {}

    public QueryBuilder(MarklogicOperations marklogicOperations) {
        this.mappingContext = marklogicOperations.getConverter().getMappingContext();
    }

    /**
     * Returns a SpEL {@link Expression} for the uri pattern expressed if present or {@literal null} otherwise.
     * Will also return {@literal null} if the uri pattern {@link String} evaluates
     * to a {@link LiteralExpression} (indicating that no subsequent evaluation is necessary).
     *
     * @param urlPattern can be {@literal null}
     * @return the dynamic Expression if any or {@literal null}
     */
    @Nullable
    private static Expression detectExpression(@Nullable String urlPattern) {
        if (!StringUtils.hasText(urlPattern)) {
            return null;
        }

        Expression expression = PARSER.parseExpression(urlPattern, ParserContext.TEMPLATE_EXPRESSION);

        return expression instanceof LiteralExpression ? null : expression;
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


        String collection = buildTargetCollection();
        if (collection != null) {
            query.setCollection(collection);
        }

        if (example != null) {
            Criteria criteria = prepareCriteria(example);
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

    private List<SortCriteria> prepareSortCriteria(Sort sort) {
        Class<?> targetType = example != null ? example.getProbeType() : options.entityClass();
        Assert.notNull(targetType, "Query needs a explicit type to resolve sort order");

        return buildSortCriteria(sort, retrievePersistentEntity(targetType));
    }

    @Nullable
    private Criteria prepareCriteria(Example example) {
        return buildCriteria(example.getProbe(), retrievePersistentEntity(example.getProbeType()));
    }

    @Nullable
    private Criteria buildCriteria(Object bean, MarklogicPersistentEntity<?> entity) {
        Stack<Criteria> stack = new Stack<>();
        PersistentPropertyAccessor propertyAccessor = entity.getPropertyAccessor(bean);

        entity.doWithProperties((PropertyHandler<MarklogicPersistentProperty>) property -> {
            Object value = propertyAccessor.getProperty(property);
            if (hasContent(value)) {
                if (stack.empty()) {
                    stack.push(buildCriteria(property, value));
                } else {
                    Criteria criteria = stack.peek();
                    if (criteria.getOperator() == null) {
                        Criteria andCriteria = new Criteria(Criteria.Operator.and, new ArrayList<>(Arrays.asList(criteria, buildCriteria(property, value))));
                        stack.pop();
                        stack.push(andCriteria);
                    } else {
                        Criteria subCriteria = buildCriteria(property, value);
                        if (subCriteria != null) {
                            criteria.add(subCriteria);
                        }
                    }
                }
            }
        });

        return stack.empty() ? null : stack.peek();
    }

    private boolean hasContent(@Nullable Object value) {
        return value != null && (!(value instanceof Collection) || !((Collection) value).isEmpty());
    }

    @Nullable
    private Criteria buildCriteria(MarklogicPersistentProperty property, Object value) {
        Optional<? extends TypeInformation<?>> typeInformation = StreamSupport.stream(property.getPersistentEntityTypes().spliterator(), false).findFirst();
        if (typeInformation.isPresent()) {
            MarklogicPersistentEntity<?> nestedEntity = mappingContext.getPersistentEntity(typeInformation.get());
            return nestedEntity != null ? buildCriteria(value, nestedEntity) : null;
        } else {
            Criteria criteria = new Criteria();

            if (value instanceof Collection) {
                criteria.setOperator(Criteria.Operator.or);
                Collection<?> collection = (Collection<?>) value;
                criteria.setCriteriaObject(collection.stream()
                        .map(o -> buildCriteria(property, o))
                        .collect(Collectors.toList())
                );
            } else {
                criteria.setQname(property.getQName());
                criteria.setCriteriaObject(value);
            }

            return criteria;
        }
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

    @Nullable
    private String buildTargetCollection() {
        final Class targetClass = buildTargetClass();
        final String targetCollection = buildTargetCollection(targetClass);

        return expandDefaultCollection(targetCollection, new DocumentExpressionContext() {
            @Override
            public Class<?> getEntityClass() {
                return targetClass;
            }

            @Override
            public Object getEntity() {
                return null;
            }

            @Override
            public Object getId() {
                return null;
            }
        });
    }

    @Nullable
    private Class buildTargetClass() {
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

    @Nullable
    private String buildTargetCollection(@Nullable Class targetClass) {
        if (options.defaultCollection() != null) {
            return options.defaultCollection();
        } else if (targetClass != null) {
            MarklogicPersistentEntity<?> persistentEntity = retrievePersistentEntity(targetClass);
            return persistentEntity.getDefaultCollection();
        }

        return null;
    }

    @Nullable
    private String expandDefaultCollection(@Nullable String collection, DocumentExpressionContext identifierContext) {
        Expression expression = detectExpression(collection);

        if (expression == null) {
            return collection;
        } else {
            return expression.getValue(identifierContext, String.class);
        }
    }

    private MarklogicPersistentEntity<?> retrievePersistentEntity(Class<?> aClass) {
        MarklogicPersistentEntity<?> persistentEntity = mappingContext.getPersistentEntity(aClass);

        if (persistentEntity == null) {
            throw new TypeMismatchDataAccessException(String.format("No Persistent Entity information found for the class %s", aClass));
        }

        return persistentEntity;
    }

    interface DocumentExpressionContext {
        @Nullable
        Class<?> getEntityClass();

        @Nullable
        Object getEntity();

        @Nullable
        Object getId();
    }


}