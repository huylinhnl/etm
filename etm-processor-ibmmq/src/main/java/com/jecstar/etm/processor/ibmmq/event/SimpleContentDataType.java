//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.7-b41 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2016.06.21 at 03:55:56 PM CEST 
//


package com.jecstar.etm.processor.ibmmq.event;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for simpleContentDataType.
 * <p>
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="simpleContentDataType">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="boolean"/>
 *     &lt;enumeration value="date"/>
 *     &lt;enumeration value="dateTime"/>
 *     &lt;enumeration value="decimal"/>
 *     &lt;enumeration value="duration"/>
 *     &lt;enumeration value="integer"/>
 *     &lt;enumeration value="string"/>
 *     &lt;enumeration value="time"/>
 *     &lt;enumeration value="hexBinary"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 */
@XmlType(name = "simpleContentDataType")
@XmlEnum
public enum SimpleContentDataType {

    @XmlEnumValue("boolean")
    BOOLEAN("boolean"),
    @XmlEnumValue("date")
    DATE("date"),
    @XmlEnumValue("dateTime")
    DATE_TIME("dateTime"),
    @XmlEnumValue("decimal")
    DECIMAL("decimal"),
    @XmlEnumValue("duration")
    DURATION("duration"),
    @XmlEnumValue("integer")
    INTEGER("integer"),
    @XmlEnumValue("string")
    STRING("string"),
    @XmlEnumValue("time")
    TIME("time"),
    @XmlEnumValue("hexBinary")
    HEX_BINARY("hexBinary");
    private final String value;

    SimpleContentDataType(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static SimpleContentDataType fromValue(String v) {
        for (SimpleContentDataType c : SimpleContentDataType.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
