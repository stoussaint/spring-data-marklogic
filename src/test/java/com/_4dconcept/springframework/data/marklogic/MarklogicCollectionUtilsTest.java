package com._4dconcept.springframework.data.marklogic;

import com._4dconcept.springframework.data.marklogic.core.mapping.Collection;
import com._4dconcept.springframework.data.marklogic.core.mapping.MarklogicMappingContext;
import org.junit.Test;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;

public class MarklogicCollectionUtilsTest {

    private MarklogicCollectionUtils marklogicCollectionUtils = new MarklogicCollectionUtils() {};

    @Test
    public void extractCollections() {
        assertThat(marklogicCollectionUtils.extractCollections(new SampleEntity("test1", "test2"), new MarklogicMappingContext()), containsInAnyOrder("test1", "field:test2", "computed:TEST1"));
        assertThat(marklogicCollectionUtils.extractCollections(new ComposedSampleEntity(new SampleEntity("test1", "test2")), new MarklogicMappingContext()), containsInAnyOrder("test1", "field:test2", "computed:TEST1"));
    }

    private class SampleEntity extends BaseSampleEntity {

        SampleEntity(String field1, String field2) {
            super(field1);
            this.field2 = field2;
        }

        private String field2;

        @Collection(prefix = "field")
        @SuppressWarnings("unused") // Used by reflexion for test
        public String getField2() {
            return field2;
        }

    }

    private class BaseSampleEntity {

        @Collection
        private String field1;

        BaseSampleEntity(String field1) {
            this.field1 = field1;
        }

        @Collection(prefix = "computed")
        @SuppressWarnings("unused") // Used by reflexion for test
        public String field1ToUpperCase() {
            return field1.toUpperCase();
        }
    }

    private class ComposedSampleEntity {

        private BaseSampleEntity sample;

        ComposedSampleEntity(BaseSampleEntity sample) {
            this.sample = sample;
        }

        public BaseSampleEntity getSample() {
            return sample;
        }
    }
}