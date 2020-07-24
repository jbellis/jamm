package org.github.jamm;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.ref.Reference;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

abstract class MemoryMeterBase extends MemoryMeter
{
    private static final String outerClassReference = "this\\$[0-9]+";

    private final ClassValue<MethodHandle[]> declaredClassFieldsCache = new ClassValue<MethodHandle[]>()
    {
        @Override
        protected MethodHandle[] computeValue(Class<?> type)
        {
            return declaredClassFields0(type);
        }
    };

    MemoryMeterBase(Builder builder)
    {
        super(builder);
    }

    @Override
    public long measure(Object obj)
    {
        Class<?> type = obj.getClass();

        if (type.isArray())
            return measureArray(obj, type);

        return measureNonArray(obj, type);
    }

    abstract long measureArray(Object obj, Class<?> type);

    abstract long measureNonArray(Object obj, Class<?> type);

    /**
     * @return the memory usage of @param object including referenced objects
     * @throws NullPointerException if object is null
     */
    @Override
    public final long measureDeep(Object object)
    {
        Objects.requireNonNull(object);

        if (ignoreClass.get(object.getClass()))
            return 0;

        VisitedSet tracker = new VisitedSet();
        tracker.add(object);

        // track stack manually so we can handle deeper hierarchies than recursion
        Deque<Object> stack = new ArrayDeque<>();
        stack.push(object);

        long total = 0;
        Object current;
        Class<?> type;
        long size;
        while (!stack.isEmpty())
        {
            current = stack.pop();
            type = current.getClass();
            size = measure(current);
            total += size;

            if (type.isArray())
            {
                if (!type.getComponentType().isPrimitive())
                    for (Object child : (Object[]) current)
                        if (child != null && tracker.add(child) && !ignoreClass.get(child.getClass()))
                            stack.push(child);
                continue;
            }

            if (byteBufferMode != BB_MODE_NORMAL && ByteBuffer.class.isAssignableFrom(type))
            {
                ByteBuffer bb = (ByteBuffer) current;
                if (byteBufferMode == BB_MODE_OMIT_SHARED)
                {
                    total += bb.remaining();
                    continue;
                }
                if (byteBufferMode == BB_MODE_SHALLOW)
                {
                    continue;
                }
                if (byteBufferMode == BB_MODE_HEAP_ONLY_NO_SLICE)
                {
                    if (bb.isDirect())
                        continue;
                    // if we're only referencing a sub-portion of the ByteBuffer, don't count the array overhead (assume it's slab
                    // allocated, so amortized over all the allocations the overhead is negligible and better to undercount than over)
                    if (bb.capacity() > bb.remaining())
                    {
                        total -= size;
                        total += bb.remaining();
                        continue;
                    }
                }
            }

            Object referent = (ignoreNonStrongReferences && (current instanceof Reference)) ? ((Reference<?>) current).get() : null;
            try
            {
                Class<?> cls = current.getClass();
                Object child;
                for (MethodHandle field : declaredClassFields(cls))
                {
                    child = field.invoke(current);

                    if (child != null && tracker.add(child) && !ignoreClass.get(child.getClass()) && child != referent)
                        stack.push(child);
                }
            }
            catch (Throwable t)
            {
                throw new RuntimeException(t);
            }
        }

        return total;
    }

    // visible for testing
    static final class VisitedSet
    {
        int size;
        // Open-addressing table for this set.
        // This table will never be fully populated (1/3) to keep enough "spare slots" that are `null`
        // so a loop checking for an element would not have to check too many slots (iteration stops
        // when an entry in the table is `null`).
        Object[] table = new Object[16];

        boolean add(Object o)
        {
            // no need for a null-check here, see call-sites

            Object[] tab;
            Object item;
            int len, mask, i, s;
            for (; true; resize())
            {
                tab = table;
                len = tab.length;
                mask = len - 1;
                i = index(o, mask);

                while (true)
                {
                    item = tab[i];
                    if (item == null)
                        break;
                    if (item == o)
                        return false;
                    i = inc(i, len);
                }

                s = size + 1;
                // 3 as the "magic size factor" to have enough 'null's in the open-addressing-map
                if (s * 3 <= len)
                {
                    size = s;
                    tab[i] = o;
                    return true;
                }
            }
        }

        private void resize()
        {
            Object[] tab = table;

            int newLength = tab.length << 1;
            if (newLength < 0)
                throw new IllegalStateException("too many objects visited");

            Object[] n = new Object[newLength];
            int mask = newLength - 1;
            int i;
            for (Object o : tab)
            {
                if (o != null)
                {
                    i = index(o, mask);
                    while (n[i] != null)
                        i = inc(i, newLength);
                    n[i] = o;
                }
            }
            table = n;
        }

        private static int index(Object o, int mask)
        {
            return System.identityHashCode(o) & mask;
        }

        private static int inc(int i, int len)
        {
            int n = i + 1;
            return n >= len ? 0 : n;
        }
    }

    private MethodHandle[] declaredClassFields(Class<?> cls)
    {
        return declaredClassFieldsCache.get(cls);
    }

    @SuppressWarnings("deprecation")
    private MethodHandle[] declaredClassFields0(Class<?> cls)
    {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        List<MethodHandle> mhs = new ArrayList<>();
        for (; !skipClass(cls); cls = cls.getSuperclass())
        {
            for (Field f : cls.getDeclaredFields())
            {
                if (!f.getType().isPrimitive()
                    && !Modifier.isStatic(f.getModifiers())
                    && !f.isAnnotationPresent(Unmetered.class)
                    && !(ignoreOuterClassReference && f.getName().matches(outerClassReference))
                    && !ignoreClass.get(f.getType()))
                {
                    boolean acc = f.isAccessible();
                    try
                    {
                        if (!acc)
                            f.setAccessible(true);
                        mhs.add(lookup.unreflectGetter(f));
                    }
                    catch (Exception e)
                    {
                        throw new RuntimeException(e);
                    }
                    finally
                    {
                        if (!acc)
                            f.setAccessible(false);
                    }
               }
            }
        }
        return mhs.toArray(new MethodHandle[0]);
    }
}
