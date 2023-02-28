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
     * @return the shallow memory usage of the @param object
     */
    long measure(Object object);
}
