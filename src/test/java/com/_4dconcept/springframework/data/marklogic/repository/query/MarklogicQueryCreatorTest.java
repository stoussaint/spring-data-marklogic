package com._4dconcept.springframework.data.marklogic.repository.query;

import com._4dconcept.springframework.data.marklogic.core.mapping.MarklogicMappingContext;
import com._4dconcept.springframework.data.marklogic.core.query.Criteria;
import com._4dconcept.springframework.data.marklogic.core.query.Query;
import com._4dconcept.springframework.data.marklogic.repository.MarklogicRepository;
import com._4dconcept.springframework.data.marklogic.repository.Person;
import org.junit.Test;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.repository.core.support.DefaultRepositoryMetadata;
import org.springframework.data.repository.query.ParameterAccessor;
import org.springframework.data.repository.query.ParametersParameterAccessor;
import org.springframework.data.repository.query.parser.PartTree;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class MarklogicQueryCreatorTest {

    private MarklogicMappingContext mappingContext = new MarklogicMappingContext();

    @Test
    public void createQueryWithSingleParameter() throws Exception {
        final MarklogicQueryMethod method = buildMethod("findByLastname", String.class);
        MarklogicQueryCreator creator = new MarklogicQueryCreator(buildTree(method), buildAccessor(method, "name"), mappingContext, Person.class);
        Query query = creator.createQuery();

        assertThat(query.getCriteria().getQname().getLocalPart(), is("lastname"));
        assertThat(query.getCriteria().getCriteriaObject(), is("name"));
    }

    @Test
    public void createQueryWithTwoParameters() throws Exception {
        final MarklogicQueryMethod method = buildMethod("findByLastnameAndAddressCountry", String.class, String.class);
        MarklogicQueryCreator creator = new MarklogicQueryCreator(buildTree(method), buildAccessor(method, "name", "country"), mappingContext, Person.class);
        Query query = creator.createQuery();

        assertThat(query.getCriteria().getOperator(), is(Criteria.Operator.AND));
        assertThat(query.getCriteria().getCriteriaObject(), instanceOf(List.class));
        List<?> list = (List<?>) query.getCriteria().getCriteriaObject();
        assertThat(list, hasSize(2));
    }

    @Test
    public void createQueryWithThreeParameters() throws Exception {
        final MarklogicQueryMethod method = buildMethod("findByLastnameAndFirstnameAndAge", String.class, String.class, Integer.class);
        MarklogicQueryCreator creator = new MarklogicQueryCreator(buildTree(method), buildAccessor(method, "lastname", "firstname", 38), mappingContext, Person.class);
        Query query = creator.createQuery();

        assertThat(query.getCriteria().getOperator(), is(Criteria.Operator.AND));
        assertThat(query.getCriteria().getCriteriaObject(), instanceOf(List.class));
        List<?> list = (List<?>) query.getCriteria().getCriteriaObject();
        assertThat(list, hasSize(3));
    }

    @Test
    public void createQueryWithNegativeSingleParameter() throws Exception {
        final MarklogicQueryMethod method = buildMethod("findByAddressCountryIsNot", String.class);
        MarklogicQueryCreator creator = new MarklogicQueryCreator(buildTree(method), buildAccessor(method, "France"), mappingContext, Person.class);
        Query query = creator.createQuery();

        assertThat(query.getCriteria().getOperator(), is(Criteria.Operator.NOT));
        assertThat(query.getCriteria().getCriteriaObject(), instanceOf(Criteria.class));
        Criteria innerCriteria = (Criteria) query.getCriteria().getCriteriaObject();
        assertThat(innerCriteria.getQname().getLocalPart(), is("country"));
        assertThat(innerCriteria.getCriteriaObject(), is("France"));
    }

    @Test
    public void createQueryWithExistsParameter() throws Exception {
        final MarklogicQueryMethod method = buildMethod("findByAddressExists");
        MarklogicQueryCreator creator = new MarklogicQueryCreator(buildTree(method), buildAccessor(method), mappingContext, Person.class);
        Query query = creator.createQuery();

        assertThat(query.getCriteria().getOperator(), is(Criteria.Operator.EXISTS));
        assertThat(query.getCriteria().getCriteriaObject(), instanceOf(Criteria.class));
        Criteria innerCriteria = (Criteria) query.getCriteria().getCriteriaObject();
        assertThat(innerCriteria.getQname().getLocalPart(), is("address"));
        assertThat(innerCriteria.getCriteriaObject(), nullValue());
    }

    @Test
    public void createQueryWithNotExistsParameter() throws Exception {
        final MarklogicQueryMethod method = buildMethod("findByAddressEmpty");
        MarklogicQueryCreator creator = new MarklogicQueryCreator(buildTree(method), buildAccessor(method), mappingContext, Person.class);
        Query query = creator.createQuery();

        assertThat(query.getCriteria().getOperator(), is(Criteria.Operator.EMPTY));
        assertThat(query.getCriteria().getCriteriaObject(), instanceOf(Criteria.class));
        Criteria innerCriteria = (Criteria) query.getCriteria().getCriteriaObject();
        assertThat(innerCriteria.getQname().getLocalPart(), is("address"));
        assertThat(innerCriteria.getCriteriaObject(), nullValue());
    }

    @Test
    public void createQueryWithContainingSetParameter() throws Exception {
        final MarklogicQueryMethod method = buildMethod("findBySkillsContaining", HashSet.class);
        String[] skills = {"créativité", "flexibilité"};
        MarklogicQueryCreator creator = new MarklogicQueryCreator(buildTree(method), buildAccessor(method, new HashSet<String>(Arrays.asList(skills))), mappingContext, Person.class);
        Query query = creator.createQuery();

        assertThat(query.getCriteria().getOperator(), is(Criteria.Operator.OR));
        assertThat(query.getCriteria().getCriteriaObject(), instanceOf(List.class));

        @SuppressWarnings("unchecked")
        List<Criteria> criteriaList = (List<Criteria>) query.getCriteria().getCriteriaObject();
        criteriaList.stream().map(criteria -> criteria.getQname().getLocalPart()).forEach(lp -> assertThat(lp, is("skill")));
        assertThat(criteriaList.stream().map(Criteria::getCriteriaObject).map(String.class::cast).collect(Collectors.toList()), containsInAnyOrder(skills));
    }

    @Test
    public void createQueryWithContainingListParameter() throws Exception {
        final MarklogicQueryMethod method = buildMethod("findBySkillsContaining", ArrayList.class);
        String[] skills = {"créativité", "flexibilité"};
        MarklogicQueryCreator creator = new MarklogicQueryCreator(buildTree(method), buildAccessor(method, Arrays.asList(skills)), mappingContext, Person.class);
        Query query = creator.createQuery();

        assertThat(query.getCriteria().getOperator(), is(Criteria.Operator.OR));
        assertThat(query.getCriteria().getCriteriaObject(), instanceOf(List.class));

        @SuppressWarnings("unchecked")
        List<Criteria> criteriaList = (List<Criteria>) query.getCriteria().getCriteriaObject();
        criteriaList.stream().map(criteria -> criteria.getQname().getLocalPart()).forEach(lp -> assertThat(lp, is("skill")));
        assertThat(criteriaList.stream().map(Criteria::getCriteriaObject).map(String.class::cast).collect(Collectors.toList()), containsInAnyOrder(skills));
    }

    @Test
    public void createQueryWithListParameter() throws Exception {
        final MarklogicQueryMethod method = buildMethod("findBySkills", ArrayList.class);
        String[] skills = {"créativité", "flexibilité"};
        MarklogicQueryCreator creator = new MarklogicQueryCreator(buildTree(method), buildAccessor(method, Arrays.asList(skills)), mappingContext, Person.class);
        Query query = creator.createQuery();

        assertThat(query.getCriteria().getOperator(), is(Criteria.Operator.AND));
        assertThat(query.getCriteria().getCriteriaObject(), instanceOf(List.class));

        @SuppressWarnings("unchecked")
        List<Criteria> criteriaList = (List<Criteria>) query.getCriteria().getCriteriaObject();
        criteriaList.stream().map(criteria -> criteria.getQname().getLocalPart()).forEach(lp -> assertThat(lp, is("skill")));
        assertThat(criteriaList.stream().map(Criteria::getCriteriaObject).map(String.class::cast).collect(Collectors.toList()), containsInAnyOrder(skills));
    }

    @Test
    public void createQueryWithSetParameter() throws Exception {
        final MarklogicQueryMethod method = buildMethod("findBySkills", HashSet.class);
        String[] skills = {"créativité", "flexibilité"};
        MarklogicQueryCreator creator = new MarklogicQueryCreator(buildTree(method), buildAccessor(method, new HashSet<>(Arrays.asList(skills))), mappingContext, Person.class);
        Query query = creator.createQuery();

        assertThat(query.getCriteria().getOperator(), is(Criteria.Operator.AND));
        assertThat(query.getCriteria().getCriteriaObject(), instanceOf(List.class));

        @SuppressWarnings("unchecked")
        List<Criteria> criteriaList = (List<Criteria>) query.getCriteria().getCriteriaObject();
        criteriaList.stream().map(criteria -> criteria.getQname().getLocalPart()).forEach(lp -> assertThat(lp, is("skill")));
        assertThat(criteriaList.stream().map(Criteria::getCriteriaObject).map(String.class::cast).collect(Collectors.toList()), containsInAnyOrder(skills));
    }

    private ParameterAccessor buildAccessor(MarklogicQueryMethod method, Object... parameters) {
        return new ParametersParameterAccessor(method.getParameters(), parameters);
    }

    private PartTree buildTree(MarklogicQueryMethod method) {
        return new PartTree(method.getName(), method.getResultProcessor().getReturnedType().getDomainType());
    }

    private MarklogicQueryMethod buildMethod(String methodName, Class<?>... paramTypes) throws NoSuchMethodException {
        Method method = Repo.class.getMethod(methodName, paramTypes);
        ProjectionFactory factory = new SpelAwareProxyProjectionFactory();
        return new MarklogicQueryMethod(method, new DefaultRepositoryMetadata(PartTreeMarklogicQueryTest.Repo.class), factory);
    }


    interface Repo extends MarklogicRepository<Person, Long> {

        Person findByLastname(String lastname);

        Person findByLastnameAndAddressCountry(String lastname, String country);

        Person findByLastnameAndFirstnameAndAge(String lastname, String firstname, Integer age);

        Person findByAddressCountry(String country);

        List<Person> findByAddressCountryIsNot(String country);

        List<Person> findBySkills(String skill);

        List<Person> findBySkills(ArrayList<String> skills);

        List<Person> findBySkills(HashSet<String> skills);

        List<Person> findBySkillsContaining(ArrayList<String> skills);

        List<Person> findBySkillsContaining(HashSet<String> skills);

        Person findByActiveIsTrue();

        Person findByActiveIsFalse();

        Person findByAddressExists();

        Person findByAddressEmpty();

    }
}