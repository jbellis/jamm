package org.github.jamm;

import static org.github.jamm.MathUtils.roundTo;

/**
 * Utility methods to measure arrays.
 */
public final class ArrayMeasurementUtils {

    /**
     * Computes the size of an array from its base offset, length, elementSize and object alignment.
     *
     * @param arrayBaseOffset the array base offset
     * @param length the array length
     * @param elementSize the size of the array elements
     * @param objectAlignment the object alignment (padding) in bytes
     * @return the size of the array
     */
    public static long computeArraySize(int arrayBaseOffset, int length, int elementSize, int objectAlignment) {
        return roundTo(arrayBaseOffset + length * (long) elementSize, objectAlignment);
    }

    private ArrayMeasurementUtils() {
    }

}
