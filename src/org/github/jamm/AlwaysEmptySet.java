package org.github.jamm;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

public class AlwaysEmptySet<T> implements Set<T> {
    public static final Set EMPTY_SET = new AlwaysEmptySet();

    private AlwaysEmptySet() {
    }

    public static <T> Set<T> create() {
    	return (Set<T>) EMPTY_SET;
    }

    public int size() {
        return 0;
    }

    public boolean isEmpty() {
        return true;
    }

    public boolean contains(Object o) {
        return false;
    }

    public Iterator<T> iterator() {
        return (Iterator<T>) Collections.emptySet().iterator();
    }

    public Object[] toArray() {
        return new Object[0];
    }

    public <T> T[] toArray(T[] a) {
        return (T[]) Collections.emptySet().toArray();
    }

    public boolean add(T t) {
        return false;
    }

    public boolean remove(Object o) {
        return false;
    }

    public boolean containsAll(Collection<?> c) {
        return false;
    }

    public boolean addAll(Collection<? extends T> c) {
        return false;
    }

    public boolean retainAll(Collection<?> c) {
        return false;
    }

    public boolean removeAll(Collection<?> c) {
        return false;
    }

    public void clear() {
    }
}
