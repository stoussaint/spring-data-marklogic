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

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

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

    private Criteria prepareCriteria(Example example) {
        MarklogicPersistentEntity<?> persistentEntity = mappingContext.getPersistentEntity(example.getProbeType());
        return buildCriteria(example.getProbe(), persistentEntity);
    }

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
                        criteria.add(buildCriteria(property, value));
                    }
                }
            }
        });

        return stack.empty() ? null : stack.peek();
    }

    private boolean hasContent(Object value) {
        return value != null && (!(value instanceof Collection) || !((Collection) value).isEmpty());
    }

    private Criteria buildCriteria(MarklogicPersistentProperty property, Object value) {
        Optional<? extends TypeInformation<?>> typeInformation = StreamSupport.stream(property.getPersistentEntityType().spliterator(), false).findFirst();
        if (typeInformation.isPresent()) {
            MarklogicPersistentEntity<?> nestedEntity = mappingContext.getPersistentEntity(typeInformation.get());
            return buildCriteria(value, nestedEntity);
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