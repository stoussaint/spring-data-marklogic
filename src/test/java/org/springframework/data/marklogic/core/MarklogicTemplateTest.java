package org.springframework.data.marklogic.core;

import com.marklogic.xcc.Content;
import com.marklogic.xcc.ContentSource;
import com.marklogic.xcc.DocumentFormat;
import com.marklogic.xcc.Session;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.ConverterNotFoundException;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.marklogic.core.convert.MappingMarklogicConverter;
import org.springframework.data.marklogic.core.convert.MarklogicContentHolder;
import org.springframework.data.marklogic.core.convert.MarklogicConverter;
import org.springframework.data.marklogic.core.mapping.BasicMarklogicPersistentEntity;
import org.springframework.data.marklogic.core.mapping.MarklogicMappingContext;
import org.springframework.data.marklogic.core.mapping.MarklogicPersistentProperty;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

/**
 * --Description--
 *
 * @author Stéphane Toussaint
 */
@RunWith(MockitoJUnitRunner.class)
public class MarklogicTemplateTest {

    @Mock
    ContentSource contentSource;

    @Mock
    Session session;

    @Mock
    MarklogicConverter marklogicConverter;

    @Mock
    ConversionService conversionService;

    @Captor
    ArgumentCaptor<Content> contentArgumentCaptor;

    @Before
    public void setup() {
        when(contentSource.newSession()).thenReturn(session);
        MappingContext<BasicMarklogicPersistentEntity<?>, MarklogicPersistentProperty> marklogicMappingContext = new MarklogicMappingContext();
        when(marklogicConverter.getMappingContext()).thenReturn((MappingContext)marklogicMappingContext);
        when(marklogicConverter.getConversionService()).thenReturn(conversionService);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNullContentSource() throws Exception {
        new MarklogicTemplate(null);
    }

    @Test
    public void defaultsConverterToMappingMarklogicConverter() throws Exception {
        MarklogicTemplate template = new MarklogicTemplate(contentSource);
        assertTrue(ReflectionTestUtils.getField(template, "marklogicConverter") instanceof MappingMarklogicConverter);
    }

    @Test
    public void insertionOfSimpleEntity() throws Exception {
        final String SAMPLE_CONTENT = "<simpleEntity><id>1</id><name>entity</name></simpleEntity>";

        doAnswer(invocationOnMock -> {
            MarklogicContentHolder holder = invocationOnMock.getArgumentAt(1, MarklogicContentHolder.class);
            holder.setContent(SAMPLE_CONTENT);
            return null;
        }).when(marklogicConverter).write(Mockito.any(SimpleEntity.class), Mockito.any(MarklogicContentHolder.class));

        MarklogicTemplate template = new MarklogicTemplate(contentSource, marklogicConverter);
        template.insert(new SimpleEntity(null, "entity"), buildCreateOperationOptions("/test/entity/1.xml"));
        verify(session).insertContent(contentArgumentCaptor.capture());

        assertThat(contentArgumentCaptor.getValue().getUri(), CoreMatchers.equalTo("/test/entity/1.xml"));
        assertThat(contentArgumentCaptor.getValue().getCreateOptions().getFormat(), CoreMatchers.equalTo(DocumentFormat.XML));
        assertThat(toString(contentArgumentCaptor.getValue().openDataStream()), CoreMatchers.equalTo(SAMPLE_CONTENT));
    }

    @Test(expected = ConverterNotFoundException.class)
    public void rejectsInsertionOfNonAnnotatedEntity() throws Exception {
        MarklogicTemplate template = new MarklogicTemplate(contentSource);
        template.insert(new NonAnnotatedEntity("1", "entity"), buildCreateOperationOptions("/test/entity/1.xml"));
    }

    public void insertionStringContent() throws Exception {
        final String content = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><simpleEntity><id>1</id><name>entity</name></simpleEntity>";

        MarklogicTemplate template = new MarklogicTemplate(contentSource);
        template.insert(content, buildCreateOperationOptions("/test/entity/1.xml"));
        verify(session).insertContent(contentArgumentCaptor.capture());

        assertThat(contentArgumentCaptor.getValue().getUri(), CoreMatchers.equalTo("/test/entity/1.xml"));
        assertThat(contentArgumentCaptor.getValue().getCreateOptions().getFormat(), CoreMatchers.equalTo(DocumentFormat.XML));
        assertThat(toString(contentArgumentCaptor.getValue().openDataStream()), CoreMatchers.equalTo(content));
    }

    static class SimpleEntity {

        public String id;
        public String name;

        SimpleEntity() {}

        SimpleEntity(String id, String name) {
            this.id = id;
            this.name = name;
        }

    }

    static class NonAnnotatedEntity {
        public String id;
        public String name;

        NonAnnotatedEntity(String id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    public static String toString(InputStream input) throws IOException {
        return new Scanner(input).useDelimiter("\\Z").next();
    }

    private MarklogicCreateOperationOptions buildCreateOperationOptions(String uri) {
        return new MarklogicCreateOperationOptions() {
            @Override
            public String uri() {
                return uri;
            }

            @Override
            public String[] extraCollections() {
                return null;
            }

            @Override
            public String defaultCollection() {
                return null;
            }
        };
    }

}