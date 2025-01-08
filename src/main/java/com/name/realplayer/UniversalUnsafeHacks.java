package com.name.realplayer;

import sun.misc.Unsafe;

import java.lang.reflect.Field;

public class UniversalUnsafeHacks {
    private static final Unsafe UNSAFE;

    static {
        try {
            final Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            UNSAFE = (Unsafe) theUnsafe.get(null);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new RuntimeException("Could not get Unsafe", e);
        }
    }

    public static void setField(Field data, Object object, Object value)
    {
        long offset = UNSAFE.objectFieldOffset(data);
        UNSAFE.putObject(object, offset, value);
    }
}
