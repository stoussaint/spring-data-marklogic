package org.springframework.data.marklogic.core.convert;

import com.marklogic.xcc.ResultItem;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;

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

/**
 * Wrapper class to contain useful converters for the usage with Marklogic
 *
 * @author Stéphane Toussaint
 */
abstract class MarklogicConverters {

    private MarklogicConverters() {}

    static Collection<GenericConverter> getConvertersToRegister() {
        List<GenericConverter> converters = new ArrayList<>();

        converters.add(ResultItemToEntityJAXBConverter.INSTANCE);
        converters.add(EntityToStringJAXBConverter.INSTANCE);

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

}
