package myjdk.lock;

import myjdk.lock.spring.Autowire;
import myjdk.lock.spring.Component;

import java.util.*;
@Component
public class MyHashMap<K,V> implements Map<K,V> {
    public MyHashMap(int capacity) {
        list = new ArrayList<>(capacity);
    }
    ArrayList<Node<K,V>> list;

    public MyHashMap() {
        list = new ArrayList<>(8);
    }

    @Autowire
    public Lock lock;
    class Node<K,V> implements Map.Entry<K,V> {
        private K key;
        private V value;

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
        return list.size();
    }

    @Override
    public boolean isEmpty() {
        return list.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return false;
    }

    @Override
    public boolean containsValue(Object value) {
        return false;
    }

    @Override
    public V get(Object key) {
        Node<K,V> oldNode = this.getNode(key);
        return oldNode ==  null ? null : oldNode.getValue();
    }

    @Override
    public V put(K key, V value) {
        Node<K,V> oldNode = this.getNode(key);
        if (oldNode != null) {
            V oldValue = oldNode.getValue();
            oldNode.setValue(value);
            return oldValue;
        }else{
            int index = key.hashCode() % list.size();
            list.add(index,new  Node<>(key,value));
            return null;
        }
    }

    public Node<K,V> getNode(Object key) {
        int index = key.hashCode() % list.size();
        return list.get(index);
    }

    @Override
    public V remove(Object key) {
        Node<K,V> oldNode = this.getNode(key);
        int index = key.hashCode() % list.size();
        list.remove(index);
        return oldNode ==   null ? null : oldNode.getValue();
    }

    @Override
    public void putAll(Map m) {

    }

    @Override
    public void clear() {
        list.clear();
    }

    @Override
    public Set keySet() {
        return Collections.emptySet();
    }

    @Override
    public Collection values() {
        return Collections.emptyList();
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return Collections.emptySet();
    }
}
