package org.springframework.data.marklogic.core.convert;

import org.springframework.core.convert.ConverterNotFoundException;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.marklogic.core.mapping.MarklogicPersistentEntity;
import org.springframework.data.marklogic.core.mapping.MarklogicPersistentProperty;
import org.springframework.expression.Expression;
import org.springframework.expression.ParserContext;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.util.StringUtils;

import java.io.Serializable;

/**
 * {@link MarklogicConverter} that uses a {@link MappingContext} to compute extra
 * information such as uri or defaultCollection.
 *
 * @author Stéphane Toussaint
 */
public class MappingMarklogicConverter extends AbstractMarklogicConverter  {

    private static final SpelExpressionParser PARSER = new SpelExpressionParser();

    protected final MappingContext<? extends MarklogicPersistentEntity<?>, MarklogicPersistentProperty> mappingContext;

    public MappingMarklogicConverter(MappingContext<? extends MarklogicPersistentEntity<?>, MarklogicPersistentProperty> mappingContext) {
        super(new DefaultConversionService());

        this.mappingContext = mappingContext;
    }

    @Override
    public <R extends Object> R read(Class<R> type, MarklogicContentHolder holder) {
        return null;
    }

    @Override
    public void write(Object source, MarklogicContentHolder holder) {
        if (null == source) {
            return;
        }

        TypeDescriptor sourceDescriptor = TypeDescriptor.forObject(source);
        sourceDescriptor.getAnnotations();

        TypeDescriptor targetDescriptor = TypeDescriptor.valueOf(Serializable.class);

        if (getConversionService().canConvert(sourceDescriptor, targetDescriptor)) {
            Serializable content = getConversionService().convert(source, Serializable.class);
            holder.setContent(content);
        } else {
            throw new ConverterNotFoundException(sourceDescriptor, targetDescriptor);
        }
    }

    /*
	 * (non-Javadoc)
	 * @see org.springframework.data.convert.EntityConverter#getMappingContext()
	 */
    public MappingContext<? extends MarklogicPersistentEntity<?>, MarklogicPersistentProperty> getMappingContext() {
        return mappingContext;
    }

    @Override
    public String computeUri(Object source) {
        String uri = getMappingContext().getPersistentEntity(source.getClass()).getUri();
        Expression expression = detectExpression(uri);
        return expression == null ? uri : expression.getValue(source, String.class);
    }

    @Override
    public String computeDefaultCollection(Object source) {
        String defaultCollection = getMappingContext().getPersistentEntity(source.getClass()).getDefaultCollection();

        Expression expression = detectExpression(defaultCollection);
        return expression == null ? defaultCollection : expression.getValue(source, String.class);
    }

    /**
     * Returns a SpEL {@link Expression} for the uri pattern expressed if present or {@literal null} otherwise.
     * Will also return {@literal null} if the uri pattern {@link String} evaluates
     * to a {@link LiteralExpression} (indicating that no subsequent evaluation is necessary).
     *
     * @param urlPattern can be {@literal null}
     * @return
     */
    private static Expression detectExpression(String urlPattern) {
        if (!StringUtils.hasText(urlPattern)) {
            return null;
        }

        Expression expression = PARSER.parseExpression(urlPattern, ParserContext.TEMPLATE_EXPRESSION);

        return expression instanceof LiteralExpression ? null : expression;
    }
}
