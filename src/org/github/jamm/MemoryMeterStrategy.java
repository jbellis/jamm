package org.github.jamm;

/**
 * Represents a strategy to measure the shallow memory used by a Java object. 
 */
@FunctionalInterface
public interface MemoryMeterStrategy {

    /**
     * The memory layout detected by JAMM.
     */
    MemoryLayoutSpecification MEMORY_LAYOUT = MemoryLayoutSpecification.getEffectiveMemoryLayoutSpecification();

    /**
     * Measures the shallow memory used by the specified object.
     *
     * @param object the object to measure
     * @return the shallow memory usage of the specified object
     */
    long measure(Object object);

    /**
     * Measures the shallow memory used by the specified array.
     *
     * @param array the array to measure
     * @param type the array type
     * @return the shallow memory usage of the specified array
     */
    default long measureArray(Object array, Class<?> type) {
        return measure(array);
    }

    /**
     * Measures the shallow memory used by the specified array.
     *
     * @param array the array to measure
     * @return the shallow memory used by the specified array.
     */
    default long measureArray(Object[] array) {
        return measure(array);
    }

    /**
     * Measures the shallow memory used by the specified byte array.
     *
     * @param array the array to measure
     * @return the shallow memory used by the specified byte array.
     */
    default long measureArray(byte[] array) {
        return measure(array);
    }

    /**
     * Measures the shallow memory used by the specified boolean array.
     *
     * @param array the boolean array to measure
     * @return the shallow memory used by the specified boolean array.
     */
    default long measureArray(boolean[] array) {
        return measure(array);
    }

    /**
     * Measures the shallow memory used by the specified short array.
     *
     * @param array the short array to measure
     * @return the shallow memory used by the specified short array.
     */
    default long measureArray(short[] array) {
        return measure(array);
    }

    /**
     * Measures the shallow memory used by the specified char array.
     *
     * @param array the char array to measure
     * @return the shallow memory used by the specified char array.
     */
    default long measureArray(char[] array) {
        return measure(array);
    }

    /**
     * Measures the shallow memory used by the specified int array.
     *
     * @param array the int array to measure
     * @return the shallow memory used by the specified int array.
     */
    default long measureArray(int[] array) {
        return measure(array);
    }

    /**
     * Measures the shallow memory used by the specified float array.
     *
     * @param array the float array to measure
     * @return the shallow memory used by the specified float array.
     */
    default long measureArray(float[] array) {
        return measure(array);
    }

    /**
     * Measures the shallow memory used by the specified long array.
     *
     * @param array the long array to measure
     * @return the shallow memory used by the specified long array.
     */
    default long measureArray(long[] array) {
        return measure(array);
    }

    /**
     * Measures the shallow memory used by the specified double array.
     *
     * @param array the long array to measure
     * @return the shallow memory used by the specified double array.
     */
    default long measureArray(double[] array) {
        return measure(array);
    }

    /**
     * Checks if this instance supports the {@code computeArraySize} operation.
     * @return {@code true} if this instance support the {@code computeArraySize} operation, {@code false} otherwise.
     */
    default boolean supportComputeArraySize() {
        return false;
    }

    /**
     * Computes an array size from its length and element size (optional operation).
     * <p>{@code supportComputeArraySize} should be used before calling this method to check if this operation is supported.</p>
     *
     * @param length the array length
     * @param elementSize the size of the elements
     * @return the array size
     * @throws UnsupportedOperationException if the operation is not supported
     */
    default long computeArraySize(int length, int elementSize) {
        throw new UnsupportedOperationException();
    }
}
