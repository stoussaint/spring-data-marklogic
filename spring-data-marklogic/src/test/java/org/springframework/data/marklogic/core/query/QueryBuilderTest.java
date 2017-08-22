package org.springframework.data.marklogic.core.query;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.data.domain.Example;
import org.springframework.data.marklogic.core.MarklogicOperationOptions;
import org.springframework.data.marklogic.core.cts.CTSQueryParser;
import org.springframework.data.marklogic.sample.Address;
import org.springframework.data.marklogic.sample.Person;

import java.util.Arrays;
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
        assertThat(query.getCriteria(), nullValue());
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
        assertThat(query.getCollection(), is("Person"));
        assertThat(query.getCriteria(), notNullValue());
        assertThat(query.getCriteria().getOperator(), is(Criteria.Operator.and));
        assertThat(query.getCriteria().getCriteriaObject(), instanceOf(List.class));

        List<Criteria> criteriaList = (List<Criteria>) query.getCriteria().getCriteriaObject();
        assertThat(criteriaList, hasSize(3));
        assertThat(criteriaList.get(0).getCriteriaObject(), is("Me"));
        assertThat(criteriaList.get(1).getCriteriaObject(), is(38));
        assertThat(criteriaList.get(2).getOperator(), is(Criteria.Operator.and));
        System.out.println(new CTSQueryParser(query).asCtsQuery());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void considerCollectionValuesAsOrQuery() throws Exception {
        Person person = new Person();
        person.setSkills(Arrays.asList("java", "xml"));
        person.setAge(38);

        Query query = new QueryBuilder().alike(Example.of(person)).build();
        System.out.println(new CTSQueryParser(query).asCtsQuery());

        assertThat(query, notNullValue());
        assertThat(query.getCollection(), is("Person"));
        assertThat(query.getCriteria(), notNullValue());
        assertThat(query.getCriteria().getOperator(), is(Criteria.Operator.and));
        assertThat(query.getCriteria().getCriteriaObject(), instanceOf(List.class));

        List<Criteria> criteriaList = (List<Criteria>) query.getCriteria().getCriteriaObject();
        assertThat(criteriaList, hasSize(2));


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