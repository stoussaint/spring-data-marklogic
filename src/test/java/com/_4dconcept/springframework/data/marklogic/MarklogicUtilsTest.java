package com._4dconcept.springframework.data.marklogic;

import com._4dconcept.springframework.data.marklogic.repository.Person;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class MarklogicUtilsTest {

    @Test
    public void checkCollectionExpansion() {
        assertThat(MarklogicUtils.expandsExpression("myCollection", null), is("myCollection"));
        assertThat(MarklogicUtils.expandsExpression("#{entityClass.getSimpleName()}", Person.class), is("Person"));
        assertThat(MarklogicUtils.expandsExpression("#{id}", null, null, () -> "12"), is("12"));
    }

    @Test
    public void checkUriExpansion() {
        assertThat(MarklogicUtils.expandsExpression("/content/test.xml", null), is("/content/test.xml"));

        Person person = new Person();
        person.setLastname("Test");
        assertThat(MarklogicUtils.expandsExpression("/content/#{entityClass.getSimpleName()}/#{entity.lastname}/#{id}.xml", Person.class, person, () -> "1"), is("/content/Person/Test/1.xml"));
    }

}