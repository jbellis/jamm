package org.github.jamm;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * 
 */
public final class MeasurementStack {

    /**
     * Tracker used to ensure that we do not visit the same instance twice.
     */
    private final IdentityHashSet tracker = new IdentityHashSet();

    /**
     * The listener
     */
    private final MemoryMeterListener listener;

    /**
     * Filter used to determine which classes should be ignored.
     */
    private final FieldAndClassFilter classFilter;

    /**
     * Stack of objects that need to be measured.
     */
    private final Deque<Object> stack = new ArrayDeque<Object>();

    MeasurementStack(FieldAndClassFilter classFilter, MemoryMeterListener listener) {
        this.classFilter = classFilter;
        this.listener = listener;
    }

    /**
     * Push the specified object into the stack.
     *
     * @param parent the parent object
     * @param name the field name
     * @param child the child to be added
     */
    public void pushObject(Object parent, String name, Object child) {
        if (child != null && tracker.add(child)) {
            stack.push(child);
            listener.fieldAdded(parent, name, child);
        }
    }

    /**
     * Push the root object into the stack.
     * @param object the root of the object tree to measure. 
     */
    void pushRoot(Object object) {
        stack.push(object);
        tracker.add(object);
        listener.started(object);
    }

    /**
     * Push all the eligible elements from the array into the stack.
     *
     * @param array the array
     */
    void pushArrayElements(Object[] array) {
        for (int i = 0; i < array.length; i++) {
            Object element = array[i];
            if (element != null && !classFilter.ignore(element.getClass()) && tracker.add(element)) {
                stack.push(element);
                listener.arrayElementAdded(array, i, element);
            }
        }
    }

    /**
     * Checks if this stack is empty.
     * @return {@code true} if the stack is empty, {@code false} otherwise.
     */
    boolean isEmpty() {
        return stack.isEmpty();
    }

    /**
     * Returns the listener used by this stack.
     * @return the listener used by this stack.
     */
    MemoryMeterListener listener() {
        return listener;
    }

    /**
     * Pop an element from this stack.
     * @return the element at the top of this stack.
     */
    Object pop() {
        return stack.pop();
    }
}
