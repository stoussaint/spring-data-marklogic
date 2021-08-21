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

import com._4dconcept.springframework.data.marklogic.core.mapping.Collection;
import com._4dconcept.springframework.data.marklogic.core.mapping.Document;
import com._4dconcept.springframework.data.marklogic.core.mapping.MarklogicMappingContext;
import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.springframework.core.convert.ConverterNotFoundException;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.lang.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;

public class MappingMarklogicConverterTest {

    @Test(expected = ConverterNotFoundException.class)
    public void throwsExceptionIfUnConvertibleObject() {
        MappingMarklogicConverter mappingMarklogicConverter = createConverterWithDelegates();
        mappingMarklogicConverter.write(new UnConvertibleObject(), new MarklogicContentHolder());
    }

    @Test
    public void marklogicContentHolderSetWithConvertedContentWithExplicitConverter() {
        MappingMarklogicConverter mappingMarklogicConverter = createConverterWithDelegates(new ConvertibleObjectConverter());

        MarklogicContentHolder contentHolder = new MarklogicContentHolder();
        mappingMarklogicConverter.write(new ConvertibleObject(), contentHolder);
        assertThat(contentHolder.getContent(), CoreMatchers.notNullValue());
        assertThat(contentHolder.getContent(), CoreMatchers.is("<empty />"));
    }

    @Test
    public void marklogicContentHolderSetWithConvertedContentWithConditionalConverter() {
        MappingMarklogicConverter mappingMarklogicConverter = createConverterWithDelegates(new DocumentConverter());

        MarklogicContentHolder contentHolder = new MarklogicContentHolder();
        mappingMarklogicConverter.write(new Person("1"), contentHolder);
        assertThat(contentHolder.getContent(), CoreMatchers.notNullValue());
        assertThat(contentHolder.getContent(), CoreMatchers.is("<person><id>1</id></person>"));
    }

    static class UnConvertibleObject {}

    static class ConvertibleObject {}

    @Document(uri = "/person/#{id}.xml")
    @Collection(prefix = "collection", value = "#{getClass().getSimpleName()}")
    static class Person {
        public String id;

        public Person(String id) {
            this.id = id;
        }
    }

    static class ConvertibleObjectConverter implements GenericConverter {

        @Override
        public Set<ConvertiblePair> getConvertibleTypes() {
            return Collections.singleton(new ConvertiblePair(ConvertibleObject.class, String.class));
        }

        @Override
        public Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
            return "<empty />";
        }

    }

    static class DocumentConverter implements ConditionalGenericConverter {
        @Override
        public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
            return sourceType.getObjectType().isAnnotationPresent(Document.class);
        }

        @Override
        public Set<ConvertiblePair> getConvertibleTypes() {
            return Collections.singleton(new ConvertiblePair(Object.class, String.class));
        }

        @Override
        public Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
            return "<person><id>1</id></person>";
        }

    }

    private MappingMarklogicConverter createConverterWithDelegates(GenericConverter... converters) {
        MappingMarklogicConverter mappingMarklogicConverter = new MappingMarklogicConverter(new MarklogicMappingContext());
        mappingMarklogicConverter.setConverters(Arrays.asList(converters));
        mappingMarklogicConverter.afterPropertiesSet();
        return mappingMarklogicConverter;
    }

}