package com._4dconcept.springframework.data.marklogic;

import org.springframework.expression.EvaluationException;
import org.springframework.expression.Expression;
import org.springframework.expression.ParserContext;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * Helper class featuring helper methods for working with Marklogic specific elements.
 * Mainly intended for internal use within the framework.
 *
 * @author stoussaint
 * @since 2017-11-30
 */
public final class MarklogicUtils {

    private static final SpelExpressionParser PARSER = new SpelExpressionParser();

    /**
     * Private constructor to prevent instantiation.
     */
    private MarklogicUtils() {}

    /**
     * Expands the given uri using the provided extension context
     *
     * @param uri the uri to expands
     * @param expressionContext the expression context
     * @return the expanded uri. If the given uri is not an expression, it is return as it.
     */
    public static String expandUri(String uri, DocumentExpressionContext expressionContext) {
        Expression expression = detectExpression(uri);
        if (expression == null) {
            return uri;
        } else {
            String value = expression.getValue(expressionContext, String.class);

            if (value == null)
                throw new EvaluationException(String.format("The uri expression has been evaluated to null [%s]", uri));

            return value;
        }
    }

    /**
     * Expands the given collection using the provided type.
     *
     * @param collection the collection to expand
     * @param entityType the entityType used as context
     * @return the expanded collection. If the given collection is not an expression, it is return as it.
     */
    @Nullable
    public static String expandCollection(@Nullable String collection, Class<?> entityType) {
        return expandCollection(collection, new DocumentExpressionContext() {
            @Override
            public Class<?> getEntityClass() {
                return entityType;
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

    /**
     * Expands the given collection using the provided type.
     *
     * @param collection the collection to expand
     * @param expressionContext the entityType used as context
     * @return the expanded collection. If the given collection is not an expression, it is return as it.
     */
    @Nullable
    public static String expandCollection(@Nullable String collection, DocumentExpressionContext expressionContext) {
        Expression expression = detectExpression(collection);
        return expression == null ? collection : expression.getValue(expressionContext, String.class);
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

    public interface DocumentExpressionContext {
        @Nullable
        Class<?> getEntityClass();

        @Nullable
        Object getEntity();

        @Nullable
        Object getId();
    }

}
