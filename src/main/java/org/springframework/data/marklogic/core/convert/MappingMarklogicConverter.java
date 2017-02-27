package org.springframework.data.marklogic.core.convert;

import org.springframework.core.convert.ConverterNotFoundException;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.marklogic.core.mapping.MarklogicPersistentEntity;
import org.springframework.data.marklogic.core.mapping.MarklogicPersistentProperty;

import java.io.Serializable;

/**
 * {@link MarklogicConverter} that uses a {@link MappingContext} to compute extra
 * information such as uri or defaultCollection.
 *
 * @author Stéphane Toussaint
 */
public class MappingMarklogicConverter extends AbstractMarklogicConverter  {

    protected final MappingContext<? extends MarklogicPersistentEntity<?>, MarklogicPersistentProperty> mappingContext;

    public MappingMarklogicConverter(MappingContext<? extends MarklogicPersistentEntity<?>, MarklogicPersistentProperty> mappingContext) {
        this(mappingContext, null);
    }

    public MappingMarklogicConverter(MappingContext<? extends MarklogicPersistentEntity<?>, MarklogicPersistentProperty> mappingContext, GenericConversionService conversionService) {
        super(conversionService);
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
}
