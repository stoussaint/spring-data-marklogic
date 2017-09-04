package org.springframework.data.marklogic.core.mapping;

import org.junit.Test;
import org.springframework.data.util.ClassTypeInformation;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * Unit tests for {@link BasicMarklogicPersistentEntity}.
 *
 * @author Stéphane Toussaint
 */
public class BasicMarklogicPersistentEntityTest {

    @Test
    public void urlFallbackIfNoSpecificAnnotation() {
        BasicMarklogicPersistentEntity<UnAnnotated> entity = createPersistentEntity(UnAnnotated.class);
        assertThat(entity.getUri(), is("/content/unannotated/#{id}.xml"));
    }

    @Test
    public void urlFallbackIfAnnotationWithoutSpecificUrl() {
        BasicMarklogicPersistentEntity<AnnotatedNoUrl> entity = createPersistentEntity(AnnotatedNoUrl.class);
        assertThat(entity.getUri(), is("/content/annotatednourl/#{id}.xml"));
    }

    @Test
    public void urlDocumentAnnotation() {
        BasicMarklogicPersistentEntity<Contact> entity = createPersistentEntity(Contact.class);
        assertThat(entity.getUri(), is("/content/contact/#{id}.xml"));
    }

    @Test
    public void urlFromSubclassInheritsDocumentAnnotation() {
        BasicMarklogicPersistentEntity<Person> entity = createPersistentEntity(Person.class);
        assertThat(entity.getUri(), is("/content/contact/#{id}.xml"));
    }

    @Document(uri = "/content/contact/#{id}.xml")
    class Contact {}

    class Person extends Contact {}

    class UnAnnotated {}

    @Document
    class AnnotatedNoUrl {}

    private <T> BasicMarklogicPersistentEntity<T> createPersistentEntity(Class<T> clazz) {
        return new BasicMarklogicPersistentEntity<>(ClassTypeInformation.from(clazz));
    }
}