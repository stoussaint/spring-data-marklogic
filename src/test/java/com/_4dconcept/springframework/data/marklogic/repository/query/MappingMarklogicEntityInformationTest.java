package com._4dconcept.springframework.data.marklogic.repository.query;

import com._4dconcept.springframework.data.marklogic.core.mapping.MarklogicPersistentEntity;
import com._4dconcept.springframework.data.marklogic.repository.Person;
import com._4dconcept.springframework.data.marklogic.repository.support.MappingMarklogicEntityInformation;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

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
        when(info.getType()).thenReturn(Person.class);
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