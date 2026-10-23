package dev.ricky.galaxywatchtweaker.core;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/** Fail closed when a plugin update makes an obfuscated identity contract ambiguous. */
public final class CompanionIdentityModel {
    private final Field model;
    private final Field manufacturer;
    private final Field vendor;

    public CompanionIdentityModel(Class<?> type) throws ReflectiveOperationException {
        model = stringField(type, "m");
        manufacturer = stringField(type, "n");
        vendor = stringField(type, "q");
    }

    public static Method findSingleArgumentMethod(Class<?> type, String name)
            throws NoSuchMethodException {
        Method found = null;
        for (Method method : type.getDeclaredMethods()) {
            if (!name.equals(method.getName()) || method.getParameterCount() != 1
                    || Modifier.isStatic(method.getModifiers()) || method.isBridge() || method.isSynthetic()) continue;
            if (found != null) throw new NoSuchMethodException("Ambiguous " + type.getName() + "#" + name);
            found = method;
        }
        if (found == null) throw new NoSuchMethodException(type.getName() + "#" + name);
        return found;
    }

    private static Field stringField(Class<?> type, String name) throws ReflectiveOperationException {
        Field field = type.getDeclaredField(name);
        if (field.getType() != String.class || Modifier.isStatic(field.getModifiers())
                || Modifier.isFinal(field.getModifiers())) {
            throw new NoSuchFieldException("Unsupported identity field " + name);
        }
        field.setAccessible(true);
        return field;
    }

    public void apply(Object target, String modelValue, String manufacturerValue) throws IllegalAccessException {
        if (target == null) return;
        synchronized (target) {
            Object oldModel = model.get(target);
            Object oldManufacturer = manufacturer.get(target);
            Object oldVendor = vendor.get(target);
            try {
                model.set(target, modelValue);
                manufacturer.set(target, manufacturerValue);
                vendor.set(target, manufacturerValue);
            } catch (IllegalAccessException | RuntimeException e) {
                model.set(target, oldModel);
                manufacturer.set(target, oldManufacturer);
                vendor.set(target, oldVendor);
                throw e;
            }
        }
    }
}
