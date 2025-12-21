package de.t14d3.rapunzelcore.util;

import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;

import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Predicate;
import java.util.logging.Logger;

/**
 * Utility class for reflection operations across all platforms.
 * This is a platform-agnostic version that can be used by Paper, Velocity, and other platforms.
 */
@SuppressWarnings("unused")
public class ReflectionsUtil {

    private static final Logger LOGGER = Logger.getLogger(ReflectionsUtil.class.getName());

    private static void log(String message) {
        LOGGER.info("[ReflectionsUtil] " + message);
    }

    /**
     * Get all subtypes of the given class using reflection.
     * 
     * @param clazz The base class
     * @param <T> The type parameter
     * @return Set of all subtypes, empty set if none found or on error
     */
    public static <T> Set<Class<? extends T>> getSubTypes(Class<T> clazz) {
        Reflections reflections = new Reflections(clazz.getPackage().getName(), new SubTypesScanner(false));
        try {
            return reflections.getSubTypesOf(clazz);
        } catch (Exception e) {
            log("Failed to get subtypes of " + clazz.getName() + ": " + e.getMessage());
            return Set.of();
        }
    }

    /**
     * Instantiate all subtypes of the given class.
     * 
     * @param clazz The base class
     * @param <T> The type parameter
     * @return Set of instantiated subtypes
     */
    public static <T> Set<T> instantiateSubTypes(Class<T> clazz) {
        Set<Class<? extends T>> subTypes = getSubTypes(clazz);
        Set<T> instantiatedSubTypes = new HashSet<>();
        for (Class<? extends T> subType : subTypes) {
            try {
                instantiatedSubTypes.add(subType.getDeclaredConstructor().newInstance());
            } catch (Exception e) {
                log("Failed to instantiate " + subType.getName() + ": " + e.getMessage());
            }
        }
        return Set.copyOf(instantiatedSubTypes);
    }

    /**
     * Instantiate all subtypes of the given class and call an init method on each.
     * 
     * @param clazz The base class
     * @param initMethodName The name of the init method to call
     * @param initMethodArgs Arguments to pass to the init method
     * @param <T> The type parameter
     * @return Set of instantiated subtypes
     */
    public static <T> Set<T> instantiateSubTypes(Class<T> clazz, String initMethodName, Object... initMethodArgs) {
        Set<Class<? extends T>> subTypes = getSubTypes(clazz);
        Set<T> instantiatedSubTypes = instantiateSubTypes(clazz);
        for (Class<? extends T> subType : subTypes) {
            try {
                Method initMethod = subType.getDeclaredMethod(initMethodName, initMethodArgs.getClass());
                initMethod.invoke(subType.getDeclaredConstructor().newInstance(), initMethodArgs);
            } catch (Exception e) {
                log("Failed to instantiate " + subType.getName() + ": " + e.getMessage());
            }
        }
        return Set.copyOf(instantiatedSubTypes);
    }

    /**
     * Instantiate subtypes that match a predicate and call an init method on each.
     * 
     * @param clazz The base class
     * @param subTypes The subtypes to consider
     * @param shouldInstantiate Predicate to determine if a subtype should be instantiated
     * @param initMethodName The name of the init method to call
     * @param initMethodArgs Arguments to pass to the init method
     * @param <T> The type parameter
     * @return Set of instantiated subtypes
     */
    public static <T> Set<T> instantiateSubTypes(Class<T> clazz, Set<Class<? extends T>> subTypes, 
                                                 Predicate<Class<? extends T>> shouldInstantiate, 
                                                 String initMethodName, Object... initMethodArgs) {
        Set<T> instantiatedSubTypes = new HashSet<>();
        for (Class<? extends T> subType : subTypes) {
            try {
                if (shouldInstantiate.test(subType)) {
                    Method initMethod;
                    T instance = subType.getDeclaredConstructor().newInstance();
                    if (initMethodArgs.length == 0) {
                        initMethod = subType.getDeclaredMethod(initMethodName);
                        initMethod.invoke(instance);
                    } else {
                        Class<?>[] paramTypes = Arrays.stream(initMethodArgs)
                                .map(Object::getClass)
                                .toArray(Class<?>[]::new);

                        initMethod = subType.getDeclaredMethod(initMethodName, paramTypes);
                        initMethod.invoke(instance, initMethodArgs);
                    }
                    instantiatedSubTypes.add(instance);
                }
            } catch (Exception e) {
                log("Failed to run init method " + initMethodName + " on " + subType.getName() + ": " + e.getMessage());
            }
        }
        return Set.copyOf(instantiatedSubTypes);
    }

    /**
     * Instantiate subtypes that match a predicate and call an init method on each.
     * 
     * @param clazz The base class
     * @param shouldInstantiate Predicate to determine if a subtype should be instantiated
     * @param initMethodName The name of the init method to call
     * @param initMethodArgs Arguments to pass to the init method
     * @param <T> The type parameter
     * @return Set of instantiated subtypes
     */
    public static <T> Set<T> instantiateSubTypes(Class<T> clazz, Predicate<Class<? extends T>> shouldInstantiate, 
                                                 String initMethodName, Object... initMethodArgs) {
        Set<Class<? extends T>> subTypes = getSubTypes(clazz);
        return instantiateSubTypes(clazz, subTypes, shouldInstantiate, initMethodName, initMethodArgs);
    }
}
