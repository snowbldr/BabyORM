package com.babyorm;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Utils for reflecting on your life choices
 */
public class ReflectiveUtils {
    /**
     * Use me
     */
    public static final Map<Class<?>, Class<?>> PRIMITIVE_INVERSE;

    static {
        Map<Class<?>, Class<?>> map = new HashMap<>(18);
        map.put(Boolean.TYPE, Boolean.class);
        map.put(Character.TYPE, Character.class);
        map.put(Byte.TYPE, Byte.class);
        map.put(Short.TYPE, Short.class);
        map.put(Integer.TYPE, Integer.class);
        map.put(Long.TYPE, Long.class);
        map.put(Float.TYPE, Float.class);
        map.put(Double.TYPE, Double.class);
        map.put(Boolean.class, Boolean.TYPE);
        map.put(Character.class, Character.TYPE);
        map.put(Byte.class, Byte.TYPE);
        map.put(Short.class, Short.TYPE);
        map.put(Integer.class, Integer.TYPE);
        map.put(Long.class, Long.TYPE);
        map.put(Float.class, Float.TYPE);
        map.put(Double.class, Double.TYPE);
        PRIMITIVE_INVERSE = Collections.unmodifiableMap(map);
    }

    /**
     * Filter and aggregate methods of a given set of predicates
     * @param methods The set of methods to filter
     * @param resultMapKeyFunction A function to determine the key class of the resulting map. 
     * @param predicates A set of predicates to filter the methods with
     * @return The map of filtered methods
     */
    @SafeVarargs
    public static Map<Class<?>, Method> findMethods(Method[] methods, Function<Method, Class<?>> resultMapKeyFunction, Predicate<Method>... predicates) {
        return Arrays.stream(methods)
                .filter(allOf(predicates))
                .collect(Collectors.groupingBy(resultMapKeyFunction::apply, Collectors.toList()))
                .entrySet().stream()
                .filter(e -> e.getValue().size() == 1)
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get(0)));
    }

    /**
     * Find all the fields matching a set of predicates
     * @param clazz The class to find the fields on
     * @param predicates The predicates to apply
     * @return The list of fields that match the predicates
     */
    @SafeVarargs
    public static List<Field> findFields(Class<?> clazz, Predicate<Field>... predicates){
        return Arrays.stream(clazz.getDeclaredFields())
                .filter(allOf(predicates))
                .collect(Collectors.toList());
    }

    /**
     * AND together a set of predicates
     */
    @SafeVarargs
    public static <T> Predicate<T> allOf(Predicate<T>... predicates){
        return m -> {
            boolean r = true;
            for (Predicate<T> p : predicates) {
                r = r && p.test(m);
            }
            return r;
        };
    }

    /**
     * This method will insure that all primitive keys have their associated wrapper type added to the map with the same value and vice versa.
     * @param map The map to add to
     * @return The map with the added primitive or wrapper classes
     */
    public static Map<Class<?>, Method> addPrimitivesToMap(Map<Class<?>, Method> map){
        return map.entrySet().stream()
                .flatMap(e -> {
                    Class<?> c = PRIMITIVE_INVERSE.get(e.getKey());
                    if (c != null) {
                        return Stream.of(e, new HashMap.SimpleEntry<>(c, e.getValue()));
                    } else {
                        return Stream.of(e);
                    }
                })
                .distinct()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * For the given map, for each key, look up the class, and add it's super types to the map with the same value
     * @param map The map to add super classes for
     * @return The map with all the super classes added
     */
    public static Map<Class<?>, Method> addKeySuperTypes(Map<Class<?>, Method> map) {
        Map<Class<?>, Method> newMap = new HashMap<>();
        map.forEach((c, v) -> {
            Class<?> d = c;
            while (d != null && !Object.class.equals(d)) {
                newMap.put(d, map.get(c));
                d = d.getSuperclass();
            }
        });
        return newMap;
    }

    /**
     * Not really safe, just makes the exception unchecked
     * @param f The field to get
     * @param target The object to get the field from
     * @return The value of the field on the object
     */
    public static Object getSafe(Field f, Object target){
        try {
            return f.get(target);
        } catch (IllegalAccessException e) {
            throw new BabyDBException("Failed to get property of field " + f.getDeclaringClass().getCanonicalName() + "#" + f.getName());
        }
    }

    /**
     * Get a set of field values from an object
     * @param val The object to get values from
     * @param fields The fields to get from the object
     * @return The list of field values
     */
    public static List<Object> getFieldValues(Object val, List<Field> fields) {
        return fields.stream().map(f -> ReflectiveUtils.getSafe(f, val)).collect(Collectors.toList());
    }

    /**
     * Not really safe, just makes the exception unchecked
     * @param m The method to invoke
     * @param target The object to invoked the method on
     * @return The returned value of the method
     */
    public static Object invokeSafe(Method m, Object target, Object... args){
        try {
            return m.invoke(target, args);
        } catch (ReflectiveOperationException e) {
            throw new BabyDBException("Invocation failed for: " + m.getDeclaringClass().getCanonicalName()+"#"+m.getName(), e);
        }
    }
}
