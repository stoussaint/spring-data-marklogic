package org.springframework.data.marklogic.core.query;

import org.springframework.data.domain.Example;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.mapping.PropertyHandler;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.marklogic.core.MarklogicOperationOptions;
import org.springframework.data.marklogic.core.mapping.MarklogicMappingContext;
import org.springframework.data.marklogic.core.mapping.MarklogicPersistentEntity;
import org.springframework.data.marklogic.core.mapping.MarklogicPersistentProperty;
import org.springframework.data.util.TypeInformation;
import org.springframework.expression.Expression;
import org.springframework.expression.ParserContext;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * --Description--
 *
 * @author stoussaint
 * @since 2017-07-31
 */
public class QueryBuilder {

    private Class<?> type;
    private Example example;
    private Sort sort;
    private Pageable pageable;

    private MappingContext<? extends MarklogicPersistentEntity<?>, MarklogicPersistentProperty> mappingContext = new MarklogicMappingContext();
    private MarklogicOperationOptions options = new MarklogicOperationOptions() {
    };

    private static final SpelExpressionParser PARSER = new SpelExpressionParser();

    public QueryBuilder(MappingContext<? extends MarklogicPersistentEntity<?>, MarklogicPersistentProperty> mappingContext) {
        this.mappingContext = mappingContext;
    }

    public QueryBuilder() {
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


        query.setCollection(buildTargetCollection());

        if (example != null) {
            query.setCriteria(prepareCriteria(example));
        }

        if (sort != null) {
            query.setSortCriteria(prepareSortCriteria(sort));
        } else if (pageable != null) {
            if (pageable.getSort() != null) {
                query.setSortCriteria(prepareSortCriteria(pageable.getSort()));
            }
            query.setSkip(pageable.getOffset());
            query.setLimit(pageable.getPageSize());
        }

        return query;
    }

    private List<SortCriteria> prepareSortCriteria(Sort sort) {
        Class<?> targetType = example != null ? example.getProbeType() : options.entityClass();
        Assert.notNull(targetType, "Query needs a explicit type to resolve sort order");

        MarklogicPersistentEntity<?> persistentEntity = mappingContext.getPersistentEntity(targetType);
        return buildSortCriteria(sort, persistentEntity);
    }

    private List<Criteria> prepareCriteria(Example example) {
        MarklogicPersistentEntity<?> persistentEntity = mappingContext.getPersistentEntity(example.getProbeType());
        return buildCriteria(example.getProbe(), persistentEntity);
    }

    private List<Criteria> buildCriteria(Object bean, MarklogicPersistentEntity<?> entity) {
        ArrayList<Criteria> criteriaList = new ArrayList<>();
        PersistentPropertyAccessor propertyAccessor = entity.getPropertyAccessor(bean);

        entity.doWithProperties((PropertyHandler<MarklogicPersistentProperty>) property -> {
            Object value = propertyAccessor.getProperty(property);
            if (value != null) {
                if (!(value instanceof Iterable)) {
                    Criteria criteria = new Criteria();
                    criteria.setQname(property.getQName());

                    if (property.getPersistentEntityType() != null) {
                        for (TypeInformation<?> typeInformation : property.getPersistentEntityType()) {
                            MarklogicPersistentEntity<?> nestedEntity = mappingContext.getPersistentEntity(typeInformation);
                            value = buildCriteria(value, nestedEntity);
                        }

                        criteria.setValue(value);
                    }

                    criteriaList.add(criteria);
                }
            }
        });

        return criteriaList;
    }

    private List<SortCriteria> buildSortCriteria(Sort sort, MarklogicPersistentEntity<?> entity) {
        ArrayList<SortCriteria> sortCriteriaList = new ArrayList<>();

        for (Sort.Order order : sort) {
            MarklogicPersistentProperty persistentProperty = entity.getPersistentProperty(order.getProperty());
            SortCriteria sortCriteria = new SortCriteria(persistentProperty.getQName());
            if (! order.isAscending()) {
                sortCriteria.setDescending(true);
            }
            sortCriteriaList.add(sortCriteria);
        }

        return sortCriteriaList;
    }

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

    private String buildTargetCollection(Class targetClass) {
        if (options.defaultCollection() != null) {
            return options.defaultCollection();
        } else if (targetClass != null) {
            MarklogicPersistentEntity<?> persistentEntity = mappingContext.getPersistentEntity(targetClass);
            return persistentEntity.getDefaultCollection();
        }

        return null;
    }

    private String expandDefaultCollection(String collection, DocumentExpressionContext identifierContext) {
        Expression expression = detectExpression(collection);

        if (expression == null) {
            return collection;
        } else {
            if (options.entityClass() == null && example == null) {
                throw new IllegalArgumentException("An example object or an explicit entityClass must be provided in order to expand collection expression");
            }

            return expression.getValue(identifierContext, String.class);
        }
    }

    /**
     * Returns a SpEL {@link Expression} for the uri pattern expressed if present or {@literal null} otherwise.
     * Will also return {@literal null} if the uri pattern {@link String} evaluates
     * to a {@link LiteralExpression} (indicating that no subsequent evaluation is necessary).
     *
     * @param urlPattern can be {@literal null}
     * @return the dynamic Expression if any or {@literal null}
     */
    private static Expression detectExpression(String urlPattern) {
        if (!StringUtils.hasText(urlPattern)) {
            return null;
        }

        Expression expression = PARSER.parseExpression(urlPattern, ParserContext.TEMPLATE_EXPRESSION);

        return expression instanceof LiteralExpression ? null : expression;
    }

    interface DocumentExpressionContext {
        Class<?> getEntityClass();

        Object getEntity();

        Object getId();
    }
}