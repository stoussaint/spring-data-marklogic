package com._4dconcept.springframework.data.marklogic;

import com._4dconcept.springframework.data.marklogic.repository.Person;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

public class MarklogicUtilsTest {

    @Test
    public void checkCollectionExpansion() throws Exception {
        assertThat(MarklogicUtils.expandCollection("myCollection", null), is("myCollection"));
        assertThat(MarklogicUtils.expandCollection("#{entityClass.getSimpleName()}", Person.class), is("Person"));
    }

    @Test
    public void checkUriExpansion() throws Exception {
        assertThat(MarklogicUtils.expandUri("/content/test.xml", null), is("/content/test.xml"));
        assertThat(MarklogicUtils.expandUri("/content/#{entityClass.getSimpleName()}/#{entity.lastname}/#{id}.xml", new MarklogicUtils.DocumentExpressionContext() {
            @Override
            public Class<?> getEntityClass() {
                return Person.class;
            }

            @Override
            public Object getEntity() {
                Person person = new Person();
                person.setLastname("Test");
                return person;
            }

            @Override
            public Object getId() {
                return "1";
            }
        }), is("/content/Person/Test/1.xml"));
    }

}