package org.github.jamm;

/**
 * Interface that allows users to avoid crawling via reflection by adding children manually to the stack, therefore
 * speeding up the computation. The downside of this approach is that it pushes the responsibility to the user to ensure
 * that all the children are provided.
 */
public interface Measurable {

    /**
     * Allow the implementation to pre-compute and cache the {@code Measurable} shallow size.
     *
     * @param strategy the {@code MemoryMeterStrategy}
     * @return the object shallow size
     */
    default long shallowSize(MemoryMeterStrategy strategy) {
        return strategy.measure(this);
    }

    /**
     * Adds the children that should be part of the measurement to the stack
     * @param stack the stack to which the children must be added
     */
    void addChildrenTo(MeasurementStack stack);
}
