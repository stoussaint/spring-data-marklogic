package org.springframework.data.marklogic.core.query;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.data.domain.Example;
import org.springframework.data.marklogic.core.MarklogicOperationOptions;
import org.springframework.data.marklogic.sample.Address;
import org.springframework.data.marklogic.sample.Person;

import java.util.List;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * --Description--
 *
 * @author stoussaint
 * @since 2017-08-01
 */
public class QueryBuilderTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void buildEmptyQuery() throws Exception {
        Query query = new QueryBuilder().build();

        assertThat(query, notNullValue());
        assertThat(query.getCriteria(), nullValue());
        assertThat(query.getSortCriteria(), nullValue());
        assertThat(query.getSkip(), is(0));
        assertThat(query.getLimit(), is(0));
    }

    @Test
    public void name() throws Exception {
        Query query = new QueryBuilder().options(new MarklogicOperationOptions() {
            @Override
            public Class entityClass() {
                return Person.class;
            }
        }).build();

        assertThat(query.getCollection(), is("Person"));
    }

    @Test
    public void buildEmptyQueryWithUnresolvableCollection_throwsException() throws Exception {
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("An example object or an explicit entityClass must be provided in order to expand collection expression");

        new QueryBuilder().options(new MarklogicOperationOptions() {
            @Override
            public String defaultCollection() {
                return "#{entityClass.getSimpleName()}";
            }
        }).build();
    }

    @Test
    public void buildEmptyQueryWithSpecificCollection() throws Exception {
        Query query = new QueryBuilder().options(new MarklogicOperationOptions() {
            @Override
            public String defaultCollection() {
                return "#{entityClass.getSimpleName()}";
            }

            @Override
            public Class entityClass() {
                return Person.class;
            }
        }).build();

        assertThat(query.getCollection(), is("Person"));
    }

    @Test
    public void buildQueryFromEmptyExample() throws Exception {
        Person person = new Person();
        Query query = new QueryBuilder().alike(Example.of(person)).build();

        assertThat(query, notNullValue());
        assertThat(query.getCriteria(), hasSize(0));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void buildQueryFromFilledExample() throws Exception {
        Person person = new Person();
        person.setName("Me");
        person.setAge(38);

        Address address = new Address();
        address.setCity("Paris");
        address.setCountry("France");
        person.setAddress(address);

        Query query = new QueryBuilder().alike(Example.of(person)).build();

        assertThat(query, notNullValue());
        assertThat(query.getCriteria(), hasSize(3));
        assertThat(query.getCriteria().get(2).getValue(), instanceOf(List.class));
        assertThat(query.getCollection(), is("Person"));

        List<Criteria> value = (List<Criteria>)query.getCriteria().get(2).getValue();
        assertThat(value, hasSize(2));
    }

    @Test
    public void buildQueryWithExplicitCollection() throws Exception {
        Person person = new Person();

        Query query = new QueryBuilder().alike(Example.of(person)).options(new MarklogicOperationOptions() {
            @Override
            public String defaultCollection() {
                return "contact";
            }
        }).build();

        assertThat(query.getCollection(), is("contact"));
    }
}