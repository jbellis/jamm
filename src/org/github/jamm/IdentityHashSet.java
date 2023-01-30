package org.github.jamm;

/**
 * Simple set that use object equality to compare elements. This set is used {@code MemoryMeter} to keep track of
 * the objects already visited to avoid circular dependencies.
 *
 * <p>This class provides constant-time performance for the {@code add} operation
 * operations assuming the system identity hash function ({@link System#identityHashCode(Object)})
 * disperses elements properly among the buckets.</p>
 * 
 * <p>{@code IdentityHashSet} use linear probing to resolve hash collisions. When a hash collision occurs it will look 
 * for the next {@code null} bucket available. To minimize the risk of clustering impacting the performance,
 * {@code IdentityHashSet} will ensure that the underlying array is at most 1/3 full by resizing the array when this
 * limit is reached.
 */
final class IdentityHashSet
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
