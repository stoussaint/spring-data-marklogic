package org.springframework.data.marklogic.core.mapping;

import org.hamcrest.collection.IsCollectionWithSize;
import org.junit.Test;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.mapping.PropertyHandler;
import org.springframework.data.marklogic.core.query.Criteria;
import org.springframework.data.marklogic.sample.Address;
import org.springframework.data.marklogic.sample.Person;
import org.springframework.data.util.TypeInformation;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * --Description--
 *
 * @author stoussaint
 * @since 2017-07-31
 */
public class MarklogicMappingContextTest {


    @Test
    @SuppressWarnings("unchecked")
    public void name() throws Exception {
        Person person = new Person();
        person.setId("1");
        person.setName("Toussaint");
        person.setAge(38);

        Address address = new Address();
        address.setCity("Plaisir");
        address.setStreet("68 rue René Cassin");
        person.setAddress(address);

        MarklogicMappingContext mappingContext = new MarklogicMappingContext();
        BasicMarklogicPersistentEntity<?> entity = mappingContext.getPersistentEntity(Person.class);

        List<Criteria> criteria = doWith(entity, person);
        assertThat(criteria, IsCollectionWithSize.hasSize(4));
        assertThat(criteria.get(0).getValue(), is("Toussaint"));
        assertThat(criteria.get(2).getValue(), instanceOf(List.class));
        assertThat(((List<Criteria>)criteria.get(2).getValue()).get(0).getValue(), is("Plaisir"));
    }

    private List<Criteria> doWith(BasicMarklogicPersistentEntity<?> entity, Object bean) {
        ArrayList<Criteria> criteriaList = new ArrayList<>();
        PersistentPropertyAccessor propertyAccessor = entity.getPropertyAccessor(bean);

        entity.doWithProperties((PropertyHandler<MarklogicPersistentProperty>) property -> {
            Object value = propertyAccessor.getProperty(property);
            if (value != null) {
                Criteria criteria = new Criteria();
                criteria.setQname(property.getQName());

                if (property.getPersistentEntityType() != null) {
                    for (TypeInformation<?> typeInformation : property.getPersistentEntityType()) {
                        MarklogicMappingContext mappingContext = new MarklogicMappingContext();
                        BasicMarklogicPersistentEntity<?> subEntity = mappingContext.getPersistentEntity(typeInformation);
                        value = doWith(subEntity, value);
                    }

                    criteria.setValue(value);
                }

                criteriaList.add(criteria);
            }
        });

        return criteriaList;
    }


}