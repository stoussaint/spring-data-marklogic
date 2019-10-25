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
package com._4dconcept.springframework.data.marklogic.repository.query;

import com._4dconcept.springframework.data.marklogic.core.mapping.MarklogicPersistentEntity;
import com._4dconcept.springframework.data.marklogic.repository.Person;
import com._4dconcept.springframework.data.marklogic.repository.support.MappingMarklogicEntityInformation;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link MappingMarklogicEntityInformation}.
 *
 * @author Stéphane Toussaint
 */
@RunWith(MockitoJUnitRunner.class)
public class MappingMarklogicEntityInformationTest {

    @Mock
    MarklogicPersistentEntity<Person> info;

    @Before
    public void setUp() {
        when(info.getUri()).thenReturn("/content/person/${id}.xml");
    }

    @Test
    public void usesEntityUrlPatternIfNoCustomOneGiven() throws Exception {
        MarklogicEntityInformation<Person, Long> information = new MappingMarklogicEntityInformation<>(info);
        assertThat(information.getUri(), CoreMatchers.is("/content/person/${id}.xml"));

    }

    @Test
    public void usesCustomUrlPatternIfGiven() throws Exception {
        MarklogicEntityInformation<Person, Long> information = new MappingMarklogicEntityInformation<>(info, "/person/${id}.xml", null);
        assertThat(information.getUri(), CoreMatchers.is("/person/${id}.xml"));
    }

    @Test
    public void usesCustomDefaultCollectionIfGiven() throws Exception {
        MarklogicEntityInformation<Person, Long> information = new MappingMarklogicEntityInformation<>(info, "/person/${id}.xml", "Person");
        assertThat(information.getDefaultCollection(), CoreMatchers.is("Person"));
    }
}