package org.springframework.data.marklogic.core.convert;

import com.marklogic.xcc.ResultItem;
import com.marklogic.xcc.ValueFactory;
import com.marklogic.xcc.types.XdmValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Wrapper class to contain useful converters for the usage with Marklogic
 *
 * @author Stéphane Toussaint
 */
abstract class MarklogicConverters {

    private static Logger LOGGER = LoggerFactory.getLogger(MarklogicConverters.class);

    private MarklogicConverters() {}

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
     * Convert entity to serializable if annotated with {@link XmlRootElement}
     */
    @WritingConverter
    enum EntityToStringJAXBConverter implements ConditionalGenericConverter {
        INSTANCE;

        @Override
        public Set<ConvertiblePair> getConvertibleTypes() {
            return Collections.singleton(new ConvertiblePair(Object.class, Serializable.class));
        }

        @Override
        public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
            return sourceType.getObjectType().isAnnotationPresent(XmlRootElement.class);
        }

        @Override
        public String convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
            try {
                Marshaller marshaller = JAXBContext.newInstance(sourceType.getType()).createMarshaller();
                StringWriter writer = new StringWriter();
                 marshaller.marshal(source, writer);
                return writer.toString();
            } catch (JAXBException jaxbe) {
                throw new RuntimeException(jaxbe);
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

            try (InputStream inputStream = resultItem.asInputStream()) {
                Unmarshaller unmarshaller = JAXBContext.newInstance(targetType.getType()).createUnmarshaller();
                return unmarshaller.unmarshal(inputStream);
            } catch (JAXBException | IOException me) {
                throw new ConversionFailedException(sourceType, targetType, source, me);
            }
        }
    }

    /**
     * Return empty collection as empty string
     * Return collection of primitive as string separated by comma : test1,test2,test3
     * Return collection of object as serialized xml fragment within a wrapper element : <wrapper><article id="1"></article><article id="2"></article></wrapper>
     */
    enum CollectionToXdmValueConverter implements Converter<Collection<?> , XdmValue> {
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
//                LOGGER.debug("Can't find any specific converter for {}. Fallback with result as String", source.getClass());
//                return ValueFactory.newXSString(source.toString());
            }
        }
    }

}
