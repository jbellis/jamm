package org.github.jamm;

/**
 * Simple set that use object equality to compare elements. This set is used in {@code MemoryMeter} to keep track of
 * the objects already visited to avoid circular dependencies.
 *
 * <p>This class provides constant-time performance for the {@code add} operation,
 * assuming the system identity hash function ({@link System#identityHashCode(Object)})
 * disperses elements properly among the buckets.</p>
 * 
 * <p>{@code IdentityHashSet} uses linear probing to resolve hash collisions. When a hash collision occurs it will look 
 * for the next {@code null} bucket available. To minimize the risk of clustering impacting the performance,
 * {@code IdentityHashSet} will ensure that the underlying array is at most 2/3 full by resizing the array when this
 * limit is reached.</p>
 */
public final class IdentityHashSet
{
    int size;
    // Open-addressing table for this set.
    // This table will never be fully populated (2/3) to keep enough "spare slots" that are `null`
    // so a loop checking for an element would not have to check too many slots (iteration stops
    // when an entry in the table is `null`).
    Object[] table = new Object[32]; // 32 2/3 populated = 21 elements

    boolean add(Object o)
    {
        // no need for a null-check here, see call-sites
        for (; true; resize())
        {
            Object[] tab = table;
            int len = tab.length;
            int mask = len - 1;
            int i = index(o, mask);

            while (true)
            {
                Object item = tab[i];
                if (item == null)
                    break;
                if (item == o)
                    return false;
                i = inc(i, len);
            }

            int s = size + 1;
            // Ensure that the array is only at most 2/3 full
            // if ( s <= ((2 * len) / 3)
            // if ((3 * s) <= (2 * len))
            // if ((s + (2 * s)) <= (2 * len))
            if (s + (s << 1) <= (len << 1))
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
