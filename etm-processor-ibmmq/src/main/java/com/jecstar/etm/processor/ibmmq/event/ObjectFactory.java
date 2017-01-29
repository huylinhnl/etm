//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.7-b41 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2016.06.21 at 03:55:56 PM CEST 
//


package com.jecstar.etm.processor.ibmmq.event;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the com.jecstar.etm.processor.ibmmq.event package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _Event_QNAME = new QName("http://www.ibm.com/xmlns/prod/websphere/messagebroker/6.1.0/monitoring/event", "event");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: com.jecstar.etm.processor.ibmmq.event
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link EventPointData }
     * 
     */
    public EventPointData createEventPointData() {
        return new EventPointData();
    }

    /**
     * Create an instance of {@link EventPointData.MessageFlowData }
     * 
     */
    public EventPointData.MessageFlowData createEventPointDataMessageFlowData() {
        return new EventPointData.MessageFlowData();
    }

    /**
     * Create an instance of {@link EventPointData.EventData }
     * 
     */
    public EventPointData.EventData createEventPointDataEventData() {
        return new EventPointData.EventData();
    }

    /**
     * Create an instance of {@link ApplicationData }
     * 
     */
    public ApplicationData createApplicationData() {
        return new ApplicationData();
    }

    /**
     * Create an instance of {@link BitstreamData }
     * 
     */
    public BitstreamData createBitstreamData() {
        return new BitstreamData();
    }

    /**
     * Create an instance of {@link Event }
     * 
     */
    public Event createEvent() {
        return new Event();
    }

    /**
     * Create an instance of {@link EventPointData.MessageFlowData.Broker }
     * 
     */
    public EventPointData.MessageFlowData.Broker createEventPointDataMessageFlowDataBroker() {
        return new EventPointData.MessageFlowData.Broker();
    }

    /**
     * Create an instance of {@link EventPointData.MessageFlowData.ExecutionGroup }
     * 
     */
    public EventPointData.MessageFlowData.ExecutionGroup createEventPointDataMessageFlowDataExecutionGroup() {
        return new EventPointData.MessageFlowData.ExecutionGroup();
    }

    /**
     * Create an instance of {@link EventPointData.MessageFlowData.MessageFlow }
     * 
     */
    public EventPointData.MessageFlowData.MessageFlow createEventPointDataMessageFlowDataMessageFlow() {
        return new EventPointData.MessageFlowData.MessageFlow();
    }

    /**
     * Create an instance of {@link EventPointData.MessageFlowData.Node }
     * 
     */
    public EventPointData.MessageFlowData.Node createEventPointDataMessageFlowDataNode() {
        return new EventPointData.MessageFlowData.Node();
    }

    /**
     * Create an instance of {@link EventPointData.EventData.EventIdentity }
     * 
     */
    public EventPointData.EventData.EventIdentity createEventPointDataEventDataEventIdentity() {
        return new EventPointData.EventData.EventIdentity();
    }

    /**
     * Create an instance of {@link EventPointData.EventData.EventSequence }
     * 
     */
    public EventPointData.EventData.EventSequence createEventPointDataEventDataEventSequence() {
        return new EventPointData.EventData.EventSequence();
    }

    /**
     * Create an instance of {@link EventPointData.EventData.EventCorrelation }
     * 
     */
    public EventPointData.EventData.EventCorrelation createEventPointDataEventDataEventCorrelation() {
        return new EventPointData.EventData.EventCorrelation();
    }

    /**
     * Create an instance of {@link ApplicationData.SimpleContent }
     * 
     */
    public ApplicationData.SimpleContent createApplicationDataSimpleContent() {
        return new ApplicationData.SimpleContent();
    }

    /**
     * Create an instance of {@link ApplicationData.ComplexContent }
     * 
     */
    public ApplicationData.ComplexContent createApplicationDataComplexContent() {
        return new ApplicationData.ComplexContent();
    }

    /**
     * Create an instance of {@link BitstreamData.Bitstream }
     * 
     */
    public BitstreamData.Bitstream createBitstreamDataBitstream() {
        return new BitstreamData.Bitstream();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Event }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.ibm.com/xmlns/prod/websphere/messagebroker/6.1.0/monitoring/event", name = "event")
    public JAXBElement<Event> createEvent(Event value) {
        return new JAXBElement<Event>(_Event_QNAME, Event.class, null, value);
    }

}