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
        if (tracker.add(child)) {
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
     * Push the specified array element into the stack.
     *
     * @param array the array
     * @param index the element index
     */
    void pushArrayElement(Object[] array, int index) {
        Object child = array[index];
        if (child != null && !classFilter.ignore(child.getClass()) && tracker.add(child)) {
            stack.push(child);
            listener.arrayElementAdded(array, index, child);
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
     * Pop an element from this stack.
     * @return the element at the top of this stack.
     */
    Object pop() {
        return stack.pop();
    }
}
