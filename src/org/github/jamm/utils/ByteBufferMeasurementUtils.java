package org.github.jamm.utils;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;

import org.github.jamm.VM;
import org.github.jamm.accessors.FieldAccessor;

/**
 * Utility methods for {@code ByteBuffers} measurements.
 */
public final class ByteBufferMeasurementUtils {

    /**
     * The field used to store heap ByteBuffer underlying array.
     */
    private static final Field HB_FIELD = getDeclaredField(ByteBuffer.class, "hb");

    /**
     * The field used to store direct ByteBuffer attachment.
     */
    private static final Field ATT_FIELD = getDeclaredField(ByteBuffer.allocateDirect(0).getClass(), "att");

    /**
     * Retrieves the underlying capacity of the specified buffer.
     *
     * @param buffer the buffer
     * @param accessor the field accessor for this java version
     * @return the underlying capacity of the specified buffer.
     */
    public static int underlyingCapacity(ByteBuffer buffer, FieldAccessor accessor) {

        if (buffer.isDirect()) {

            if (buffer.isReadOnly() && VM.isPreJava12JVM()) {
                // Pre-java 12, a DirectByteBuffer created from another DirectByteBuffer was using the source buffer as an attachment
                // for liveness rather than the source buffer's attachment (https://bugs.openjdk.org/browse/JDK-8208362)
                // Therefore for checking slices we need to go one level deeper.
                buffer = (ByteBuffer) accessor.getFieldValue(buffer, ATT_FIELD);
            }
            ByteBuffer att = (ByteBuffer) accessor.getFieldValue(buffer, ATT_FIELD);
            return att == null ? buffer.capacity() : att.capacity();
        }

        if (buffer.isReadOnly()) {
            byte[] hb = (byte[]) accessor.getFieldValue(buffer, HB_FIELD);
            return hb.length;
        }

        return buffer.array().length;
    }

    /**
     * Returns the declared field with the specified name for the given class.
     *
     * @param cls the class
     * @param fieldname the field name
     * @return the declared field
     */
    private static Field getDeclaredField(Class<?> cls, String fieldname) {
        try {
            return cls.getDeclaredField(fieldname);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private ByteBufferMeasurementUtils() {

    }
}
