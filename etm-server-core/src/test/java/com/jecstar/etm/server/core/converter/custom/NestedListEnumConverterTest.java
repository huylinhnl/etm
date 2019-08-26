package com.jecstar.etm.server.core.converter.custom;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class NestedListEnumConverterTest {

    private enum MyEnum {
        ONE, TWO, THREE;

        public static MyEnum safeValueOf(String value) {
            if (value == null) {
                return null;
            }
            try {
                return MyEnum.valueOf(value.toUpperCase());
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }

    private class EnumListHolder {

        private List<MyEnum> enums;
    }

    @Test
    void testEnumListConversion() throws NoSuchFieldException {
        var converter = new NestedListEnumConverter<>(s -> {
            try {
                Method safeValueOf = MyEnum.class.getDeclaredMethod("safeValueOf", String.class);
                return (MyEnum) safeValueOf.invoke(null, s);
            } catch (InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
                return null;
            }
        });

        var enums = new ArrayList<MyEnum>();
        enums.add(MyEnum.ONE);
        enums.add(MyEnum.TWO);
        enums.add(MyEnum.THREE);
        StringBuilder json = new StringBuilder();
        converter.addToJsonBuffer("enums", enums, json, true);

        assertEquals("\"enums\": [ONE,TWO,THREE]", json.toString());

        var enumHolder = new EnumListHolder();
        Field field = enumHolder.getClass().getDeclaredField("enums");
        field.setAccessible(true);
        converter.setValueOnEntity(field, enumHolder, List.of("ONE", "TWO", "THREE"));
        assertEquals(enums.size(), enumHolder.enums.size());
    }

}

