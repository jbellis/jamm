package org.github.jamm;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.Stack;

public class MemoryMeter
{
    private static Instrumentation inst;
    public static void premain(String options, Instrumentation inst) {
        MemoryMeter.inst = inst;
    }

    /**
     * @return the shallow memory usage of @param object
     * @throws NullPointerException if object is null
     */
    public long measure(Object object) {
        if (inst == null) {
            throw new IllegalStateException("Instrumentation is not set; Jamm must be set as -javaagent");
        }
        return inst.getObjectSize(object);
    }

    /**
     * @return the memory usage of @param object including referenced objects
     * @throws NullPointerException if object is null
     */
    public long measureDeep(Object object) {
        if (object == null) {
            throw new NullPointerException(); // match getObjectSize behavior
        }

        // using a normal HashSet to track seen objects screws things up in two ways:
        // - it can undercount objects that are "equal"
        // - calling equals() can actually change object state (e.g. creating entrySet in HashMap)
        Set<Object> seen = Collections.newSetFromMap(new IdentityHashMap<Object, Boolean>());
        seen.add(object);

        // track stack manually so we can handle deeper heirarchies than recursion
        Stack<Object> stack = new Stack<Object>();
        stack.push(object);

        long total = 0;
        while (!stack.isEmpty()) {
            Object current = stack.pop();
            assert current != null;
            total += measure(current);

            if (current instanceof Object[]) {
                addArrayChildren((Object[]) current, stack, seen);
            } else {
                addFieldChildren(current, stack, seen);
            }
        }

        return total;
    }

    private void addFieldChildren(Object current, Stack<Object> stack, Set<Object> seen) {
        Class cls = current.getClass();
        while (cls != null) {
            for (Field field : cls.getDeclaredFields()) {
                if (field.getType().isPrimitive() || Modifier.isStatic(field.getModifiers())) {
                    continue;
                }

                field.setAccessible(true);
                Object child;
                try {
                    child = field.get(current);
                }
                catch (IllegalArgumentException e) {
                    throw new RuntimeException(e);
                }
                catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }

                if (child != null && !seen.contains(child)) {
                    stack.push(child);
                    seen.add(child);
                }
            }

            cls = cls.getSuperclass();
        }
    }

    private void addArrayChildren(Object[] current, Stack<Object> stack, Set<Object> seen) {
        for (Object child : current) {
            if (child != null && !seen.contains(child)) {
                stack.push(child);
                seen.add(child);
            }
        }
    }
}
