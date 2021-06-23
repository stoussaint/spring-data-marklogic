package com._4dconcept.springframework.data.marklogic;

import org.junit.Test;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class MarklogicTypeUtilsTest {

    @Test
    public void resolveXmlType_FromTypeBoundaries() {
        assertThat(MarklogicTypeUtils.resolveXmlType(DeeperType.class, OutterType.class), equalTo(InnerType.class));
        assertThat(MarklogicTypeUtils.resolveXmlType(DeeperType2.class, OutterType2.class), equalTo(OutterType2.class));
        assertThat(MarklogicTypeUtils.resolveXmlType(DeeperType3.class, OutterType3.class), equalTo(DeeperType3.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void resolveXmlType_FromTypeFullXmlTransientHierarchy_ThrowsException() {
        MarklogicTypeUtils.resolveXmlType(DeeperTypeTransient.class, OutterTypeTransient.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void resolveXmlType_FromUnexpectedHierarchy_ThrowsIllegalArgumentException() {
        MarklogicTypeUtils.resolveXmlType(DeeperType.class, OutterType2.class);
    }

    @XmlRootElement
    public static class OutterType extends InnerType {
    }

    @XmlRootElement
    public static class InnerType extends DeeperType {
    }

    @XmlTransient
    public static abstract class DeeperType {
    }

    @XmlRootElement
    public static class OutterType2 extends InnerType2 {
    }

    @XmlTransient
    public static class InnerType2 extends DeeperType2 {
    }

    @XmlTransient
    public static abstract class DeeperType2 {
    }

    @XmlRootElement
    public static class OutterType3 extends InnerType3 {
    }

    @XmlTransient
    public static class InnerType3 extends DeeperType3 {
    }

    @XmlRootElement
    public static abstract class DeeperType3 {
    }

    @XmlTransient
    public static class OutterTypeTransient extends InnerTypeTransient {
    }

    @XmlTransient
    public static class InnerTypeTransient extends DeeperTypeTransient {
    }

    @XmlTransient
    public static abstract class DeeperTypeTransient {
    }

}