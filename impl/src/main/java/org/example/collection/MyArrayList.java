package org.example.collection;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.example.spring.PostProcessor;

public class MyArrayList<T> implements List<T> {
    private T[] array;

    private int size;

    public MyArrayList(int capacity) {
        array = (T[]) new Object[capacity];

    }

    public MyArrayList() {
        array = (T[]) new Object[8];
    }

    @Override
    public int size() {
        return size;
    }

    public int capacity() {
        return array.length;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public boolean contains(Object o) {
        for (T t : array) {
            if (t.equals(o)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Iterator iterator() {
        return null;
    }

    @Override
    public Object[] toArray() {
        return array;
    }

    @Override
    public boolean add(T o) {
        ensureCapacity(size + 1);
        array[size++] = o;
        return true;
    }

    private void ensureCapacity(int minCapacity) {
        if (minCapacity == array.length) {
            array = Arrays.copyOf(array, array.length * 2);
        }
    }

    @Override
    public boolean remove(Object o) {
        for (int i = 0; i < size; i++) {
            if (array[i].equals(o)) {
                System.arraycopy(array, i + 1, array, i, size - i - 1);
                size--;
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        for (T o : c) {
            add(o);
        }
        return true;
    }

    @Override
    public boolean addAll(int index, Collection c) {
        return false;
    }

    @Override
    public void clear() {
        for (int i = 0; i < size; i++) {
            array[i] = null;
        }
    }

    @Override
    public T get(int index) {
        return  array[index];
    }

    @Override
    public T set(int index, T element) {
        T old = array[index];
        array[index] = element;
        if (old == null){
            size++;
        }
        return old;
    }

    @Override
    public void add(int index, T element) {
        array[index] = element;
    }

    @Override
    public T remove(int index) {
        T old = array[index];
        array[index] = null;
        return old;
    }

    @Override
    public int indexOf(Object o) {
        for (int i = 0; i < size; i++) {
            if (array[i].equals(o)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public int lastIndexOf(Object o) {
        int index = -1;
        for (int i = size - 1; i >= 0; i--) {
            if (array[i].equals(o)) {
                index = i;
            }
        }
        return index;
    }

    @Override
    public ListIterator listIterator() {
        return null;
    }

    @Override
    public ListIterator listIterator(int index) {
        return null;
    }

    @Override
    public List subList(int fromIndex, int toIndex) {
        return Collections.emptyList();
    }

    @Override
    public boolean retainAll(Collection c) {
        return false;
    }

    @Override
    public boolean removeAll(Collection c) {
        return false;
    }

    @Override
    public boolean containsAll(Collection c) {
        return false;
    }

    @Override
    public Object[] toArray(Object[] a) {
        return new Object[0];
    }
}
