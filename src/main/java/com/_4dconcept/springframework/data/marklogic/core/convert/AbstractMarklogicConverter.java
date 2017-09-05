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

import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class for {@link MarklogicConverter} implementations.
 *
 * @author Stéphane Toussaint
 */
public abstract class AbstractMarklogicConverter implements MarklogicConverter, InitializingBean {

    private final GenericConversionService conversionService;

    private List<?> converters = new ArrayList<>();

    public AbstractMarklogicConverter(GenericConversionService conversionService) {
        this.conversionService = conversionService == null ? new DefaultConversionService() : conversionService;
    }

    /**
     * @param converters the converters to set
     */
    public void setConverters(List<?> converters) {
        Assert.notNull(converters, "Converters list can't be null if explicitly provided");
        this.converters = converters;
    }

    @Override
    public ConversionService getConversionService() {
        return conversionService;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        initializeConverters();
    }

    private void initializeConverters() {
        List<Object> toRegister = new ArrayList<>();
        toRegister.addAll(MarklogicConverters.getConvertersToRegister(conversionService));
        toRegister.addAll(converters); // Ensure client converters have precedence over default MarklogicConverters

        for (Object converter : toRegister) {
            boolean added = false;

            if (converter instanceof Converter) {
                conversionService.addConverter((Converter<?, ?>) converter);
                added = true;
            }

            if (converter instanceof ConverterFactory) {
                conversionService.addConverterFactory((ConverterFactory<?, ?>) converter);
                added = true;
            }

            if (converter instanceof GenericConverter) {
                conversionService.addConverter((GenericConverter) converter);
                added = true;
            }

            if (! added) {
                throw new IllegalArgumentException("Given set contains element that is not GenericConversionService registrable converter type!");
            }

        }
    }
}