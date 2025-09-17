package org.example.collection;

import java.util.*;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.Setter;

public class MyHashMap<K,V> implements Map<K,V> {
    private int capacity;

    public MyHashMap(int capacity) {
        list = new MyArrayList<>(capacity);
        this.capacity = capacity;
    }
    List<Node<K,V>> list;
    int size = 0;

    public MyHashMap() {
        list = new MyArrayList<>(8);
        capacity = 8;
    }

    class Node<K,V> implements Entry<K,V> {
        public K key;
        public V value;
        public Node<K,V> next;

        public Node<K,V> getNext() {
            return next;
        }
        public void setNext(Node<K,V> next) {
            this.next = next;
        }

        public Node (K key, V value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public K getKey() {
            return key;
        }

        @Override
        public V getValue() {
            return value;
        }

        @Override
        public V setValue(V value) {
            V oldValue = this.value;
            this.value = value;
            return oldValue;
        }

    }


    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public boolean containsKey(Object key) {
        return this.get(key) != null;
    }

    @Override
    public boolean containsValue(Object value) {
        return this.list.stream().anyMatch(node -> node.getValue().equals(value));
    }

    @Override
    public V get(Object key) {
        Node<K, V> headNode = getHeadNode(key);
        Node<K,V> oldNode = this.getNode(key,headNode);
        return oldNode ==  null ? null : oldNode.getValue();
    }

    @Override
    public V put(K key, V value) {
        Node<K, V> headNode = getHeadNode(key);
        Node<K,V> oldNode = this.getNode(key,headNode);
        if (oldNode != null) {
            V oldValue = oldNode.getValue();
            oldNode.setValue(value);
            return oldValue;
        }else{
            Node<K, V> node = new Node<>(key, value);
            int index = getIndex(key);
            list.set(index,node);
            node.setNext(headNode);
            size++;

            ensureCapacity();

            return null;
        }
    }

    private void ensureCapacity() {
        int oldCapacity = this.capacity;
        if (size > capacity * 0.75 ){
            List<Node<K, V>> oldList = list;
            this.capacity = capacity * 2;
            list = new MyArrayList<>(capacity);
            for (int i = 0; i < oldCapacity; i++) {
                Node<K,V> oldNode = oldList.get(i);
                while (oldNode != null) {
                    Node<K, V> nextNode = oldNode.getNext();

                    setHead(oldNode);

                    oldNode = nextNode;
                }
            }

            System.out.println("扩容到了" + this.capacity);
        }
    }

    private void setHead(Node<K, V> oldNode) {
        Node<K, V> headNode = getHeadNode(oldNode.getKey());

        list.set(this.getIndex(oldNode.getKey()), oldNode);
        oldNode.setNext(headNode);
    }

    public Node<K,V> getNode(Object key,Node<K, V> headNode) {
        while (headNode != null) {
            if (headNode.getKey().equals(key)) {
                return headNode;
            }
            headNode = headNode.getNext();
        }
        return null;
    }

    private Node<K, V> getHeadNode(Object key) {
        int index = getIndex(key);
        Node<K, V> headNode = list.get(index);
        return headNode;
    }

    private int getIndex(Object key) {
        int index = key.hashCode() % capacity;
        return index;
    }

    @Override
    public V remove(Object key) {

        Node<K, V> headNode = getHeadNode(key);
        Node<K,V> oldNode = this.getNode(key,headNode);
        if (oldNode == null) {
            return null;
        }

        size--;

        if (headNode == oldNode) {
            list.set(this.getIndex(oldNode.getKey()),oldNode.getNext());
            return oldNode.getValue();
        }

        while (headNode != null) {
            if (oldNode.equals(headNode.next)){
                headNode.next = oldNode.getNext();
                return oldNode.getValue();
            }
            headNode = headNode.getNext();
        }
        return oldNode.getValue();
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        m.forEach(this::put);
    }

    @Override
    public void clear() {
        list.clear();
        size = 0;
    }

    @Override
    public Set<K> keySet() {
        throw  new UnsupportedOperationException();
    }

    @Override
    public Collection<V> values() {
        throw  new UnsupportedOperationException();
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        throw  new UnsupportedOperationException();
    }
}
