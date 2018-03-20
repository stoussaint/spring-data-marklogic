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
package com._4dconcept.springframework.data.marklogic.repository.support;

import com._4dconcept.springframework.data.marklogic.core.MarklogicTemplate;
import com._4dconcept.springframework.data.marklogic.core.convert.MarklogicConverter;
import com._4dconcept.springframework.data.marklogic.core.mapping.MarklogicPersistentEntity;
import com._4dconcept.springframework.data.marklogic.repository.Person;
import com._4dconcept.springframework.data.marklogic.repository.query.MarklogicEntityInformation;
import org.hamcrest.core.IsInstanceOf;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.repository.Repository;

import java.io.Serializable;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link MarklogicRepositoryFactory}.
 *
 * @author Stéphane Toussaint
 */
@RunWith(MockitoJUnitRunner.class)
public class MarklogicRepositoryFactoryTest {

    @Mock
    MarklogicTemplate template;

    @Mock
    MarklogicConverter converter;

    @Mock
    MappingContext mappingContext;

    @Mock
    MarklogicPersistentEntity entity;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        when(template.getConverter()).thenReturn(converter);
        when(converter.getMappingContext()).thenReturn(mappingContext);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void usesMappingMarklogicEntityInformationIfMappingContextSet() throws Exception {
        when(mappingContext.getPersistentEntity(Person.class)).thenReturn(entity);
        when(entity.getType()).thenReturn(Person.class);

        MarklogicRepositoryFactory factory = new MarklogicRepositoryFactory(template);
        MarklogicEntityInformation<Person, Serializable> entityInformation = factory.getEntityInformation(Person.class);
        assertThat(entityInformation, IsInstanceOf.instanceOf(MappingMarklogicEntityInformation.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void createsRepositoryWithIdTypeLong() {

        when(mappingContext.getPersistentEntity(Person.class)).thenReturn(entity);
        when(entity.getType()).thenReturn(Person.class);

        MarklogicRepositoryFactory factory = new MarklogicRepositoryFactory(template);
        MyPersonRepository repository = factory.getRepository(MyPersonRepository.class);
        assertThat(repository, is(notNullValue()));
    }

    interface MyPersonRepository extends Repository<Person, Long> {

    }
}