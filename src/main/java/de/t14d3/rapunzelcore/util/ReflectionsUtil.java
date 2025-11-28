package de.t14d3.rapunzelcore.util;

import de.t14d3.rapunzelcore.Main;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;

import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Predicate;
import java.util.logging.Logger;

@SuppressWarnings("unused")
public class ReflectionsUtil {

    private static final Logger LOGGER = Main.getInstance().getLogger();

    private static void log(String message) {
        LOGGER.info("[ReflectionsUtil] " + message);
    }

    public static <T> Set<Class<? extends T>> getSubTypes(Class<T> clazz) {
        Reflections reflections = new Reflections(clazz.getPackage().getName(), new SubTypesScanner(false));
        try {
            return reflections.getSubTypesOf(clazz);
        } catch (Exception e) {
            return Set.of();
        }
    }

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

    public static <T> Set<T> instantiateSubTypes(Class<T> clazz, Set<Class<? extends T>> subTypes, Predicate<Class<? extends T>> shouldInstantiate, String initMethodName, Object... initMethodArgs) {
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

    public static <T> Set<T> instantiateSubTypes(Class<T> clazz, Predicate<Class<? extends T>> shouldInstantiate, String initMethodName, Object... initMethodArgs) {
        Set<Class<? extends T>> subTypes = getSubTypes(clazz);
        return instantiateSubTypes(clazz, subTypes, shouldInstantiate, initMethodName, initMethodArgs);
    }

}
