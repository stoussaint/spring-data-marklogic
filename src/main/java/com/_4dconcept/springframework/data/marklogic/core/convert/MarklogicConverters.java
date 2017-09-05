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

import com.marklogic.xcc.ResultItem;
import com.marklogic.xcc.ValueFactory;
import com.marklogic.xcc.types.XdmValue;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.ConverterNotFoundException;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.util.CollectionUtils;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Wrapper class to contain useful converters for the usage with Marklogic
 *
 * @author Stéphane Toussaint
 */
abstract class MarklogicConverters {

    private static Map<Class<?>, JAXBContext> cachedJAXBContext = new HashMap<>();

    private MarklogicConverters() {
    }

    private static GenericConversionService conversionService;

    static Collection<Object> getConvertersToRegister(GenericConversionService conversionService) {
        MarklogicConverters.conversionService = conversionService;
        List<Object> converters = new ArrayList<>();

        converters.add(ResultItemToEntityJAXBConverter.INSTANCE);
        converters.add(EntityToStringJAXBConverter.INSTANCE);
        converters.add(CollectionToXdmValueConverter.INSTANCE);
        converters.add(GenericXdmValueConverter.INSTANCE);
        return converters;
    }

    /**
     * Convert entity annotated with {@link XmlRootElement} to String.
     */
    @WritingConverter
    enum EntityToStringJAXBConverter implements ConditionalGenericConverter {
        INSTANCE;

        @Override
        public Set<ConvertiblePair> getConvertibleTypes() {
            return Collections.singleton(new ConvertiblePair(Object.class, String.class));
        }

        @Override
        public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
            return sourceType.getObjectType().isAnnotationPresent(XmlRootElement.class);
        }

        @Override
        public String convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
            try {
                StringWriter writer = new StringWriter();
                initJAXBContext(sourceType).createMarshaller().marshal(source, new StreamResult(writer));
                return writer.toString();
            } catch (JAXBException jaxbe) {
                throw new ConversionFailedException(sourceType, targetType, source, jaxbe);
            }
        }
    }

    /**
     * Convert a {@link ResultItem} content (using it's {@link InputStream}) to the target entity object if annotated with {@link XmlRootElement}
     */
    @ReadingConverter
    enum ResultItemToEntityJAXBConverter implements ConditionalGenericConverter {
        INSTANCE;

        @Override
        public Set<ConvertiblePair> getConvertibleTypes() {
            return Collections.singleton(new ConvertiblePair(ResultItem.class, Object.class));
        }

        @Override
        public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
            return targetType.getObjectType().isAnnotationPresent(XmlRootElement.class);
        }

        @Override
        public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
            ResultItem resultItem = (ResultItem) source;
            InputStream inputStream = resultItem.asInputStream();

            try {
                return initJAXBContext(targetType).createUnmarshaller().unmarshal(new StreamSource(inputStream));
            } catch (JAXBException jaxbe) {
                throw new ConversionFailedException(sourceType, targetType, source, jaxbe);
            }
        }
    }

    /**
     * Return empty collection as empty string
     * Return collection of primitive as string separated by comma : test1,test2,test3
     * Return collection of object as serialized xml fragment within a wrapper element : <wrapper><article id="1"></article><article id="2"></article></wrapper>
     */
    enum CollectionToXdmValueConverter implements Converter<Collection<?>, XdmValue> {
        INSTANCE;

        @Override
        public XdmValue convert(Collection<?> source) {

            if (CollectionUtils.isEmpty(source))
                return ValueFactory.newXSString("");

            Object item = source.toArray()[0];
            final boolean isPrimitiveCollection = item.getClass().isPrimitive() || item.getClass().equals(String.class);

            String sequenceAsString = source.stream()
                    .map(this::serializeCollectionItem)
                    .collect(
                            Collectors.joining(
                                    isPrimitiveCollection ? "," : "",
                                    isPrimitiveCollection ? "" : "<wrapper>",
                                    isPrimitiveCollection ? "" : "</wrapper>"
                            )
                    );

            return ValueFactory.newXSString(sequenceAsString);
        }

        private String serializeCollectionItem(Object item) {
            if (item == null) {
                return "";
            }

            if (item.getClass().isPrimitive() || item.getClass().equals(String.class)) {
                return item.toString();
            }

            if (conversionService.canConvert(item.getClass(), String.class)) {
                String convert = conversionService.convert(item, String.class);
                return convert.replaceAll("^<\\?xml.*?\\?>", ""); // Remove xml prologue
            } else {
                return "--Unknown-type--" + item.getClass();
            }
        }
    }

    enum GenericXdmValueConverter implements Converter<Object, XdmValue> {
        INSTANCE;

        @Override
        public XdmValue convert(Object source) {
            if (source == null) {
                return ValueFactory.newXSString("");
            }

            if (source instanceof String) {
                return ValueFactory.newXSString((String) source);
            }

            if (source instanceof Boolean) {
                return ValueFactory.newXSBoolean((Boolean) source);
            }

            if (source instanceof Integer) {
                return ValueFactory.newXSInteger((Integer) source);
            }

            if (source instanceof Long) {
                return ValueFactory.newXSInteger((Long) source);
            }

            if (conversionService.canConvert(TypeDescriptor.forObject(source), TypeDescriptor.valueOf(String.class))) {
                return ValueFactory.newXSString(conversionService.convert(source, String.class));
            } else {
                throw new ConverterNotFoundException(TypeDescriptor.forObject(source), TypeDescriptor.valueOf(XdmValue.class));
            }
        }
    }

    private static JAXBContext initJAXBContext(TypeDescriptor type) throws JAXBException {
        final Class<?> typeClass = type.getType();
        if (cachedJAXBContext.containsKey(typeClass)) {
            return cachedJAXBContext.get(typeClass);
        } else {
            final JAXBContext jaxbContext = JAXBContext.newInstance(typeClass);
            cachedJAXBContext.put(typeClass, jaxbContext);
            return jaxbContext;
        }
    }
}
