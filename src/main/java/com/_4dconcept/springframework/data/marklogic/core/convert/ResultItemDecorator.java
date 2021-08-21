package com._4dconcept.springframework.data.marklogic.core.convert;

import com.marklogic.xcc.ResultItem;
import com.marklogic.xcc.types.ItemType;
import com.marklogic.xcc.types.ValueType;
import com.marklogic.xcc.types.XdmItem;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;

public class ResultItemDecorator implements ResultItem {

    private final ResultItem resultItem;

    public ResultItemDecorator(ResultItem resultItem) {
        this.resultItem = resultItem;
    }

    @Override
    public String getDocumentURI() {
        return resultItem.getDocumentURI();
    }

    @Override
    public String getNodePath() {
        return resultItem.getNodePath();
    }

    @Override
    public XdmItem getItem() {
        return resultItem.getItem();
    }

    @Override
    public int getIndex() {
        return resultItem.getIndex();
    }

    @Override
    public boolean isFetchable() {
        return resultItem.isFetchable();
    }

    @Override
    public void cache() {
        resultItem.cache();
    }

    @Override
    public ItemType getItemType() {
        return resultItem.getItemType();
    }

    @Override
    public Reader asReader() {
        return resultItem.asReader();
    }

    @Override
    public InputStream asInputStream() {
        return resultItem.asInputStream();
    }

    @Override
    public void writeTo(Writer writer) throws IOException {
        resultItem.writeTo(writer);
    }

    @Override
    public void writeTo(OutputStream outputStream) throws IOException {
        resultItem.writeTo(outputStream);
    }

    @Override
    public boolean isCached() {
        return resultItem.isCached();
    }

    @Override
    public ValueType getValueType() {
        return resultItem.getValueType();
    }

    @Override
    public String asString() {
        return resultItem.asString();
    }

    @Override
    public String toString() {
        return "ResultItem{" +
                "uri=" + resultItem.getDocumentURI() +
                '}';
    }
}
