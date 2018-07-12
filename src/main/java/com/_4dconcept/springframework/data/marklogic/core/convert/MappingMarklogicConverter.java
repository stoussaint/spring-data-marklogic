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
package com._4dconcept.springframework.data.marklogic.core.convert;

import com._4dconcept.springframework.data.marklogic.MarklogicTypeUtils;
import com._4dconcept.springframework.data.marklogic.core.mapping.MarklogicPersistentEntity;
import com._4dconcept.springframework.data.marklogic.core.mapping.MarklogicPersistentProperty;
import com.marklogic.xcc.ResultItem;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.ConverterNotFoundException;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.lang.Nullable;

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

    public MappingMarklogicConverter(MappingContext<? extends MarklogicPersistentEntity<?>, MarklogicPersistentProperty> mappingContext, @Nullable GenericConversionService conversionService) {
        super(conversionService);
        this.mappingContext = mappingContext;
    }

    @Override
    public <R> R read(Class<R> returnType, MarklogicContentHolder holder) {
        ResultItem resultItem = (ResultItem) holder.getContent();
        if (String.class.equals(returnType)) {
            return returnType.cast(resultItem.asString());
        }

        R result = null;
        if (returnType.isPrimitive()) {
            result = MarklogicTypeUtils.convertStringToPrimitive(returnType, resultItem.asString());
        }

        if (result != null) {
            return result;
        }

        ConversionService conversionService = getConversionService();

        if (conversionService.canConvert(resultItem.getClass(), returnType)) {
            R convert = conversionService.convert(resultItem, returnType);

            if (convert == null) {
                throw new ConversionFailedException(TypeDescriptor.forObject(resultItem), TypeDescriptor.valueOf(returnType), resultItem, new NullPointerException());
            }

            return convert;
        } else {
            throw new ConverterNotFoundException(TypeDescriptor.forObject(resultItem), TypeDescriptor.valueOf(returnType));
        }
    }

    @Override
    public void write(Object source, MarklogicContentHolder holder) {
        TypeDescriptor sourceDescriptor = TypeDescriptor.forObject(source);
        TypeDescriptor targetDescriptor = TypeDescriptor.valueOf(String.class);

        if (getConversionService().canConvert(sourceDescriptor, targetDescriptor)) {
            String content = getConversionService().convert(source, String.class);

            if (content == null) {
                throw new ConversionFailedException(sourceDescriptor, targetDescriptor, source, new NullPointerException("Conversion result is not e"));
            } else {
                holder.setContent(content);
            }
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
