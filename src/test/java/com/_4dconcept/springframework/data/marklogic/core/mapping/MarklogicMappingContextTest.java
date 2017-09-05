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
package com._4dconcept.springframework.data.marklogic.core.mapping;

import com._4dconcept.springframework.data.marklogic.core.query.Criteria;
import com._4dconcept.springframework.data.marklogic.repository.Address;
import com._4dconcept.springframework.data.marklogic.repository.Person;
import org.hamcrest.collection.IsCollectionWithSize;
import org.junit.Test;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.mapping.PropertyHandler;
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
        person.setLastname("Toussaint");
        person.setAge(38);

        Address address = new Address();
        address.setTown("Plaisir");
        address.setStreet("68 rue René Cassin");
        person.setAddress(address);

        MarklogicMappingContext mappingContext = new MarklogicMappingContext();
        BasicMarklogicPersistentEntity<?> entity = mappingContext.getPersistentEntity(Person.class);

        List<Criteria> criteria = doWith(entity, person);
        assertThat(criteria, IsCollectionWithSize.hasSize(4));
        assertThat(criteria.get(0).getCriteriaObject(), is("1"));
        assertThat(criteria.get(1).getCriteriaObject(), is("Toussaint"));
        assertThat(criteria.get(3).getCriteriaObject(), instanceOf(List.class));
        assertThat(((List<Criteria>)criteria.get(3).getCriteriaObject()).get(0).getCriteriaObject(), is("Plaisir"));
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

                    criteria.setCriteriaObject(value);
                }

                criteriaList.add(criteria);
            }
        });

        return criteriaList;
    }


}