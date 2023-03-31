package org.github.jamm;

/**
 * Represents a strategy to measure the shallow memory used by a Java object. 
 */
@FunctionalInterface
public interface MemoryMeterStrategy
{
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
     * @return the shallow memory used by the specified array.
     */
    default long measureArray(Object[] array)
    {
        return measure(array);
    }

    /**
     * Measures the shallow memory used by the specified byte array.
     *
     * @param array the array to measure
     * @return the shallow memory used by the specified byte array.
     */
    default long measureArray(byte[] array)
    {
        return measure(array);
    }

    /**
     * Measures the shallow memory used by the specified boolean array.
     *
     * @param array the boolean array to measure
     * @return the shallow memory used by the specified boolean array.
     */
    default long measureArray(boolean[] array)
    {
        return measure(array);
    }

    /**
     * Measures the shallow memory used by the specified short array.
     *
     * @param array the short array to measure
     * @return the shallow memory used by the specified short array.
     */
    default long measureArray(short[] array)
    {
        return measure(array);
    }
    
    /**
     * Measures the shallow memory used by the specified char array.
     *
     * @param array the char array to measure
     * @return the shallow memory used by the specified char array.
     */
    default long measureArray(char[] array)
    {
        return measure(array);
    }
    
    /**
     * Measures the shallow memory used by the specified int array.
     *
     * @param array the int array to measure
     * @return the shallow memory used by the specified int array.
     */
    default long measureArray(int[] array)
    {
        return measure(array);
    }
    
    /**
     * Measures the shallow memory used by the specified float array.
     *
     * @param array the float array to measure
     * @return the shallow memory used by the specified float array.
     */
    default long measureArray(float[] array)
    {
        return measure(array);
    }

    /**
     * Measures the shallow memory used by the specified long array.
     *
     * @param array the long array to measure
     * @return the shallow memory used by the specified long array.
     */
    default long measureArray(long[] array)
    {
        return measure(array);
    }

    /**
     * Measures the shallow memory used by the specified double array.
     *
     * @param array the long array to measure
     * @return the shallow memory used by the specified double array.
     */
    default long measureArray(double[] array)
    {
        return measure(array);
    }

    /**
     * Measures the shallow memory used by the specified {@code String}.
     *
     * @param s the {@code String} to measure
     * @return the shallow memory used by the specified {@code String}.
     */
    default long measureString(String s)
    {
        return measure(s);
    }
}
