package org.github.jamm;

public interface MemoryMeterStrategy
{
    /**
     * @return the shallow memory usage of @param object
     * @throws NullPointerException if object is null
     */
    long measure(Object object);
}
