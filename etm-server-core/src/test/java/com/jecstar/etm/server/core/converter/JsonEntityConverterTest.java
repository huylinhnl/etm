package com.jecstar.etm.server.core.converter;

import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test class for the <code>JsonEntityConverter</code> class.
 */
class JsonEntityConverterTest {

    @SuppressWarnings("unchecked")
    @Test
    void testStringConversion() {
        StringAnnotatedClass entity = new StringAnnotatedClass();
        entity.setFirstValue("First value");
        entity.setSecondValue("Second value");

        JsonEntityConverter converter = new JsonEntityConverter<>(StringAnnotatedClass::new);
        String json = converter.write(entity);
        assertEquals("{\"first_value\": \"First value\", \"second_value\": \"Second value\"}", json);
        StringAnnotatedClass entity2 = (StringAnnotatedClass) converter.read(json);

        assertEquals(entity, entity2);
    }

    @SuppressWarnings("unchecked")
    @Test
    void testStringConversionWithNull() {
        StringAnnotatedClass entity = new StringAnnotatedClass();

        JsonEntityConverter converter = new JsonEntityConverter<>(StringAnnotatedClass::new);
        String json = converter.write(entity);
        assertEquals("{\"second_value\": null}", json);
        StringAnnotatedClass entity2 = (StringAnnotatedClass) converter.read(json);

        assertEquals(entity, entity2);
    }

    @SuppressWarnings("unchecked")
    @Test
    void testLongConversion() {
        LongAnnotatedClass entity = new LongAnnotatedClass();
        entity.setFirstValue(101L);
        entity.setSecondValue(213L);
        entity.setThirdValue(456);

        JsonEntityConverter converter = new JsonEntityConverter<>(LongAnnotatedClass::new);
        String json = converter.write(entity);
        assertEquals("{\"first_value\": 101, \"second_value\": 213, \"third_value\": 456}", json);
        LongAnnotatedClass entity2 = (LongAnnotatedClass) converter.read(json);

        assertEquals(entity, entity2);
    }

    @SuppressWarnings("unchecked")
    @Test
    void testLongConversionWithNull() {
        LongAnnotatedClass entity = new LongAnnotatedClass();

        JsonEntityConverter converter = new JsonEntityConverter<>(LongAnnotatedClass::new);
        String json = converter.write(entity);
        assertEquals("{\"second_value\": null, \"third_value\": 0}", json);
        LongAnnotatedClass entity2 = (LongAnnotatedClass) converter.read(json);

        assertEquals(entity, entity2);
    }

    @SuppressWarnings("unchecked")
    @Test
    void testDoubleConversion() {
        DoubleAnnotatedClass entity = new DoubleAnnotatedClass();
        entity.setFirstValue(101.23);
        entity.setSecondValue(213.45);
        entity.setThirdValue(456.54);

        JsonEntityConverter converter = new JsonEntityConverter<>(DoubleAnnotatedClass::new);
        String json = converter.write(entity);
        assertEquals("{\"first_value\": 101.23, \"second_value\": 213.45, \"third_value\": 456.54}", json);
        DoubleAnnotatedClass entity2 = (DoubleAnnotatedClass) converter.read(json);

        assertEquals(entity, entity2);
    }

    @SuppressWarnings("unchecked")
    @Test
    void testDoubleConversionWithNull() {
        DoubleAnnotatedClass entity = new DoubleAnnotatedClass();

        JsonEntityConverter converter = new JsonEntityConverter<>(DoubleAnnotatedClass::new);
        String json = converter.write(entity);
        assertEquals("{\"second_value\": null, \"third_value\": 0.0}", json);
        DoubleAnnotatedClass entity2 = (DoubleAnnotatedClass) converter.read(json);

        assertEquals(entity, entity2);
    }

    @SuppressWarnings("unchecked")
    @Test
    void testIntegerConversion() {
        IntegerAnnotatedClass entity = new IntegerAnnotatedClass();
        entity.setFirstValue(101);
        entity.setSecondValue(213);
        entity.setThirdValue(456);

        JsonEntityConverter converter = new JsonEntityConverter<>(IntegerAnnotatedClass::new);
        String json = converter.write(entity);
        assertEquals("{\"first_value\": 101, \"second_value\": 213, \"third_value\": 456}", json);
        IntegerAnnotatedClass entity2 = (IntegerAnnotatedClass) converter.read(json);

        assertEquals(entity, entity2);
    }

    @SuppressWarnings("unchecked")
    @Test
    void testIntegerConversionWithNull() {
        IntegerAnnotatedClass entity = new IntegerAnnotatedClass();

        JsonEntityConverter converter = new JsonEntityConverter<>(IntegerAnnotatedClass::new);
        String json = converter.write(entity);
        assertEquals("{\"second_value\": null, \"third_value\": 0}", json);
        IntegerAnnotatedClass entity2 = (IntegerAnnotatedClass) converter.read(json);

        assertEquals(entity, entity2);
    }

    @SuppressWarnings("unchecked")
    @Test
    void testBooleanConversion() {
        BooleanAnnotatedClass entity = new BooleanAnnotatedClass();
        entity.setFirstValue(true);
        entity.setSecondValue(false);
        entity.setThirdValue(true);

        JsonEntityConverter converter = new JsonEntityConverter<>(BooleanAnnotatedClass::new);
        String json = converter.write(entity);
        assertEquals("{\"first_value\": true, \"second_value\": false, \"third_value\": true}", json);
        BooleanAnnotatedClass entity2 = (BooleanAnnotatedClass) converter.read(json);

        assertEquals(entity, entity2);
    }

    @SuppressWarnings("unchecked")
    @Test
    void testBooleanConversionWithNull() {
        BooleanAnnotatedClass entity = new BooleanAnnotatedClass();

        JsonEntityConverter converter = new JsonEntityConverter<>(BooleanAnnotatedClass::new);
        String json = converter.write(entity);
        assertEquals("{\"second_value\": null, \"third_value\": false}", json);
        BooleanAnnotatedClass entity2 = (BooleanAnnotatedClass) converter.read(json);

        assertEquals(entity, entity2);
    }

    @SuppressWarnings("unchecked")
    @Test
    void testZonedDateTimeConversion() {
        ZonedDateTimeAnnotatedClass entity = new ZonedDateTimeAnnotatedClass();
        ZonedDateTime zdt1 = ZonedDateTime.now();
        ZonedDateTime zdt2 = ZonedDateTime.now();
        entity.setFirstValue(zdt1);
        entity.setSecondValue(zdt2);

        JsonEntityConverter converter = new JsonEntityConverter<>(ZonedDateTimeAnnotatedClass::new);
        String json = converter.write(entity);
        assertEquals("{\"first_value\": " + zdt1.toInstant().toEpochMilli() + ", \"second_value\": " + zdt2.toInstant().toEpochMilli() + "}", json);
        ZonedDateTimeAnnotatedClass entity2 = (ZonedDateTimeAnnotatedClass) converter.read(json);

        assertEquals(entity, entity2);
    }

    @SuppressWarnings("unchecked")
    @Test
    void testZonedDateTimeConversionWithNull() {
        ZonedDateTimeAnnotatedClass entity = new ZonedDateTimeAnnotatedClass();

        JsonEntityConverter converter = new JsonEntityConverter<>(ZonedDateTimeAnnotatedClass::new);
        String json = converter.write(entity);
        assertEquals("{\"second_value\": null}", json);
        ZonedDateTimeAnnotatedClass entity2 = (ZonedDateTimeAnnotatedClass) converter.read(json);

        assertEquals(entity, entity2);
    }
}