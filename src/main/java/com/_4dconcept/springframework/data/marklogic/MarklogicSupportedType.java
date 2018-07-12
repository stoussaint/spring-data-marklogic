package com._4dconcept.springframework.data.marklogic;

import com.fasterxml.jackson.databind.JsonNode;
import com.marklogic.xcc.Content;
import com.marklogic.xcc.ContentCreateOptions;
import com.marklogic.xcc.ContentFactory;
import com.marklogic.xcc.types.XdmNode;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.URI;
import java.net.URL;
import java.util.Optional;
import java.util.stream.Stream;

public enum MarklogicSupportedType {

    DOCUMENT(Document.class) {
        @Override
        public Content createContentObject(String uri, Object contentToSave) {
            return ContentFactory.newContent(uri, (Document) contentToSave, ContentCreateOptions.newXmlInstance());
        }
    },
    NODE(Node.class) {
        @Override
        public Content createContentObject(String uri, Object contentToSave) {
            return ContentFactory.newContent(uri, (Node) contentToSave, ContentCreateOptions.newXmlInstance());
        }
    },
    JSON_NODE(JsonNode.class) {
        @Override
        public Content createContentObject(String uri, Object contentToSave) {
            return ContentFactory.newJsonContent(uri, (JsonNode) contentToSave, ContentCreateOptions.newJsonInstance());
        }
    },
    XDM_NODE(XdmNode.class) {
        @Override
        public Content createContentObject(String uri, Object contentToSave) {
            return ContentFactory.newContent(uri, (XdmNode) contentToSave, ContentCreateOptions.newXmlInstance());
        }
    },
    FILE(File.class) {
        @Override
        public Content createContentObject(String uri, Object contentToSave) {
            return ContentFactory.newContent(uri, (File) contentToSave, ContentCreateOptions.newXmlInstance());
        }
    },
    RANDOMACCESSFILE(RandomAccessFile.class) {
        @Override
        public Content createContentObject(String uri, Object contentToSave) throws IOException  {
            return ContentFactory.newContent(uri, (RandomAccessFile) contentToSave, ContentCreateOptions.newXmlInstance());
        }
    },
    URL(URL.class) {
        @Override
        public Content createContentObject(String uri, Object contentToSave) throws IOException  {
            return ContentFactory.newContent(uri, (URL) contentToSave, ContentCreateOptions.newXmlInstance());
        }
    },
    URI(URI.class) {
        @Override
        public Content createContentObject(String uri, Object contentToSave) throws IOException  {
            return ContentFactory.newContent(uri, (URI) contentToSave, ContentCreateOptions.newXmlInstance());
        }
    },
    STRING(String.class) {
        @Override
        public Content createContentObject(String uri, Object contentToSave) {
            return ContentFactory.newContent(uri, contentToSave.toString(), ContentCreateOptions.newXmlInstance());
        }
    },
    BYTE_ARRY(byte[].class) {
        @Override
        public Content createContentObject(String uri, Object contentToSave) {
            return ContentFactory.newContent(uri, (byte[]) contentToSave, ContentCreateOptions.newXmlInstance());
        }
    },
    INPUTSTREAM(InputStream.class) {
        @Override
        public Content createContentObject(String uri, Object contentToSave) throws IOException {
            return ContentFactory.newContent(uri, (InputStream) contentToSave, ContentCreateOptions.newXmlInstance());
        }
    };

    private Class<?> type;

    MarklogicSupportedType(Class<?> type) {
        this.type = type;
    }

    public static Optional<MarklogicSupportedType> fromClass(Class<?> type) {
        return Stream.of(MarklogicSupportedType.values()).filter(c -> c.getType().isAssignableFrom(type)).findAny();
    }

    Class<?> getType() {
        return type;
    }
    
    public abstract Content createContentObject(String uri, Object contentToSave) throws IOException;

}
