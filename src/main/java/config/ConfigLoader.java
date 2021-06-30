package config;

import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Properties;

public class ConfigLoader {
    public static void load(Class<?> configClass, File file) {
        try {
            Properties props = new Properties();
            try (InputStream propStream = new FileInputStream(file)) {
                props.load(propStream);
            }
            for (Field field : configClass.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) {
                    field.set(null, getValue(props, field.getName(), field.getType()));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error loading configuration: " + e, e);
        }
    }

    public static void load(ColonySetting colonySetting, File file) {
        try {
            Properties props = new Properties();
            try (InputStream propStream = new FileInputStream(file)) {
                props.load(propStream);
            }
            for (Field field : colonySetting.getClass().getDeclaredFields()) {
                field.set(colonySetting, getValue(props, field.getName(), field.getType()));
            }
        } catch (Exception e) {
            throw new RuntimeException("Error loading configuration: " + e, e);
        }
    }

    private static Object getValue(Properties props, String name, Class<?> type) {
        String value = props.getProperty(name);
        if (value == null) { throw new IllegalArgumentException("Missing configuration value: " + name); }
        if (type == String.class) { return value; }
        if (type == boolean.class) { return Boolean.parseBoolean(value); }
        if (type == int.class) { return Integer.parseInt(value); }
        if (type == float.class) { return Float.parseFloat(value); }
        if (type == double.class) { return Double.parseDouble(value); }
        if (type == Color.class) {
            try {
                return Class.forName("java.awt.Color").getField(value).get(null);
            } catch (Exception e) {
                System.out.println(
                        name + ": No predefined color found for value. Trying to parse to rgb in range 0-255.");
                int[] numbers = Arrays.stream(value.split(",")).mapToInt(Integer::parseInt).toArray();
                return new Color(numbers[0], numbers[1], numbers[2]);
            }
        }
        throw new IllegalArgumentException("Unknown configuration value type: " + type.getName());
    }
}