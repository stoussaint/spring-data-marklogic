package com._4dconcept.springframework.data.marklogic.repository.query;

import com._4dconcept.springframework.data.marklogic.core.MarklogicOperations;
import com._4dconcept.springframework.data.marklogic.core.convert.MarklogicConverter;
import com._4dconcept.springframework.data.marklogic.core.convert.MarklogicMappingConverter;
import com._4dconcept.springframework.data.marklogic.core.mapping.MarklogicMappingContext;
import com._4dconcept.springframework.data.marklogic.core.query.Criteria;
import com._4dconcept.springframework.data.marklogic.core.query.Query;
import com._4dconcept.springframework.data.marklogic.repository.MarklogicRepository;
import com._4dconcept.springframework.data.marklogic.repository.Person;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.repository.core.support.DefaultRepositoryMetadata;

import javax.xml.namespace.QName;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PartTreeMarklogicQuery}.
 *
 * @author Stéphane Toussaint
 */
@RunWith(MockitoJUnitRunner.class)
public class PartTreeMarklogicQueryTest {

    @Mock
    MarklogicOperations marklogicOperationsMock;

    MarklogicMappingContext mappingContext;

    public @Rule
    ExpectedException exception = ExpectedException.none();

    @Before
    public void setUp() {
        mappingContext = new MarklogicMappingContext();
        MarklogicConverter converter = new MarklogicMappingConverter(mappingContext);

        when(marklogicOperationsMock.getConverter()).thenReturn(converter);
    }

    @Test
    public void singleFieldShouldBeConsidered() {
        Query query = deriveQueryFromMethod("findByLastname", "foo");

        assertCriteria(
                query.getCriteria(),
                is(new QName("http://spring.data.marklogic/test/contact", "lastname")),
                is("foo")
        );
    }

    @Test
    public void multipleFieldsShouldBeConsidered() {
        Query query = deriveQueryFromMethod("findByLastnameAndFirstname", "foo", "bar");

        assertThat(query.getCriteria().getOperator(), is(Criteria.Operator.and));
        assertThat(query.getCriteria().getCriteriaObject(), instanceOf(List.class));

        List<Criteria> criteriaList = extractListCriteria(query.getCriteria());
        assertCriteria(
                criteriaList.get(0),
                is(new QName("http://spring.data.marklogic/test/contact", "lastname")),
                is("foo")
        );

        assertCriteria(
                criteriaList.get(1),
                is(new QName("http://spring.data.marklogic/test/contact", "firstname")),
                is("bar")
        );
    }

    @Test
    public void deepFieldsShouldBeConsidered() {
        Query query = deriveQueryFromMethod("findByAddressCountry", "France");
        assertCriteria(query.getCriteria(),
                is(new QName("http://spring.data.marklogic/test/contact", "country")),
                is("France")
        );
    }

    @Test
    public void differentFieldsLevelShouldBeConsidered() {
        Query query = deriveQueryFromMethod("findByLastnameAndAddressCountry", "foo", "France");

        assertThat(query.getCriteria().getOperator(), is(Criteria.Operator.and));
        assertThat(query.getCriteria().getCriteriaObject(), instanceOf(List.class));

        List<Criteria> criteriaList = extractListCriteria(query.getCriteria());

        assertCriteria(
                criteriaList.get(0),
                is(new QName("http://spring.data.marklogic/test/contact", "lastname")),
                is("foo")
        );

        assertCriteria(
                criteriaList.get(1),
                is(new QName("http://spring.data.marklogic/test/contact", "country")),
                is("France")
        );
    }

    @Test
    public void collectionFieldsShouldBeConsideredWithASingleParameter() {
        Query query = deriveQueryFromMethod("findBySkills", "foo");

        assertCriteria(
                query.getCriteria(),
                is(new QName("http://spring.data.marklogic/test/contact", "skill")),
                is("foo")
        );
    }

    @Test
    public void collectionFieldsShouldBeConsideredAsAndCriteria() {
        List<String> skills = new ArrayList<>();
        skills.add("foo");
        skills.add("bar");

        Query query = deriveQueryFromMethod("findBySkills", skills);

        assertThat(query.getCriteria().getOperator(), is(Criteria.Operator.and));
        assertThat(query.getCriteria().getCriteriaObject(), instanceOf(List.class));

        List<Criteria> criteriaList = extractListCriteria(query.getCriteria());

        assertCriteria(
                criteriaList.get(0),
                is(new QName("http://spring.data.marklogic/test/contact", "skill")),
                is("foo")
        );

        assertCriteria(
                criteriaList.get(1),
                is(new QName("http://spring.data.marklogic/test/contact", "skill")),
                is("bar")
        );
    }

    @Test
    public void collectionFieldsWithContainingKeyWordShouldBeConsideredAsOrCriteria() {
        List<String> skills = new ArrayList<>();
        skills.add("foo");
        skills.add("bar");

        Query query = deriveQueryFromMethod("findBySkillsContaining", skills);

        assertThat(query.getCriteria().getOperator(), is(Criteria.Operator.or));
        assertThat(query.getCriteria().getCriteriaObject(), instanceOf(List.class));

        List<Criteria> criteriaList = extractListCriteria(query.getCriteria());

        assertCriteria(
                criteriaList.get(0),
                is(new QName("http://spring.data.marklogic/test/contact", "skill")),
                is("foo")
        );

        assertCriteria(
                criteriaList.get(1),
                is(new QName("http://spring.data.marklogic/test/contact", "skill")),
                is("bar")
        );
    }

    @Test
    public void booleanFieldShouldBeConsideredTrue() {
        Query query = deriveQueryFromMethod("findByActiveIsTrue");
        assertCriteria(
                query.getCriteria(),
                is(new QName("http://spring.data.marklogic/test/contact", "active")),
                is(true)
        );
    }

    @Test
    public void booleanFieldShouldBeConsideredFalse() {
        Query query = deriveQueryFromMethod("findByActiveIsFalse");
        assertCriteria(
                query.getCriteria(),
                is(new QName("http://spring.data.marklogic/test/contact", "active")),
                is(false)
        );
    }

    private void assertCriteria(Criteria criteria, Matcher<QName> nameMatcher, Matcher<Object> valueMatcher) {
        assertThat(criteria, notNullValue());
        assertThat(criteria.getOperator(), nullValue());
        assertThat(criteria.getQname(), nameMatcher);
        assertThat(criteria.getCriteriaObject(), valueMatcher);
    }

    private Query deriveQueryFromMethod(String method, Object... args) {

        Class<?>[] types = new Class<?>[args.length];

        for (int i = 0; i < args.length; i++) {
            types[i] = args[i].getClass();
        }

        PartTreeMarklogicQuery partTreeQuery = createQueryForMethod(method, types);

        MarklogicParametersParameterAccessor accessor = new MarklogicParametersParameterAccessor(partTreeQuery.getQueryMethod(), args);
        return partTreeQuery.createQuery(accessor);
    }

    private PartTreeMarklogicQuery createQueryForMethod(String methodName, Class<?>... paramTypes) {

        try {

            Method method = Repo.class.getMethod(methodName, paramTypes);
            ProjectionFactory factory = new SpelAwareProxyProjectionFactory();
            MarklogicQueryMethod queryMethod = new MarklogicQueryMethod(method, new DefaultRepositoryMetadata(Repo.class), factory,
                    mappingContext);

            return new PartTreeMarklogicQuery(queryMethod, marklogicOperationsMock);
        } catch (NoSuchMethodException | SecurityException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    interface Repo extends MarklogicRepository<Person, Long> {

        Person findByLastname(String lastname);

        Person findByLastnameAndFirstname(String lastname, String firstname);

        Person findByLastnameAndAddressCountry(String lastname, String country);

        Person findByAddressCountry(String country);

        List<Person> findBySkills(String skill);

        List<Person> findBySkills(ArrayList<String> skills);

        List<Person> findBySkillsContaining(ArrayList<String> skills);

        Person findByActiveIsTrue();

        Person findByActiveIsFalse();

    }

    @SuppressWarnings("unchecked")
    private List<Criteria> extractListCriteria(Criteria criteria) {
        return (List<Criteria>) criteria.getCriteriaObject();
    }
}