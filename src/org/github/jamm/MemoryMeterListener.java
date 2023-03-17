package org.github.jamm;

import java.nio.ByteBuffer;

/**
 * Listener that receive notification form MemoryMeter.
 */
interface MemoryMeterListener {

    /**
     * A factory for <code>MemoryMeterListener</code>.
     */
    interface Factory {
        MemoryMeterListener newInstance();
    }

    /**
     * Notification that <code>MemoryMeter</code> as started analyzing the specified object.
     *
     * @param obj the object being analyzed
     */
    void started(Object obj);

    /**
     * Notification that the field from the specified object has been added.
     *
     * @param obj the object for which a field has been added
     * @param fieldName the field name
     * @param fieldValue the field value
     */
    void fieldAdded(Object obj, String fieldName, Object fieldValue);

    /**
     * Notification that the element from the specified array has been added.
     *
     * @param array the array for which a element has been added
     * @param index the element index
     * @param elementValue the element value
     */
    void arrayElementAdded(Object[] array, int index, Object elementValue);

    /**
     * Notification that the size of the specified object has been measured.
     *
     * @param current the object that has been measured
     * @param size the object size in bytes
     */
    void objectMeasured(Object current, long size);

    /**
     * Notification that the size of the remaining bytes of a {@code ByteBuffer} have been measured.
     *
     * @param buffer the {@code ByteBuffer}
     * @param size the remaining bytes
     */
    void byteBufferRemainingMeasured(ByteBuffer buffer, long size);

    /**
     * Notification that the entire graphs has been measured.
     * @param size the size of the entire graph.
     */
    void done(long size);
}
