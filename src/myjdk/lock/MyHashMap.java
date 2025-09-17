package myjdk.lock;

import myjdk.lock.spring.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
@Component
public class MyHashMap<K,V> implements Map<K,V> {
    public MyHashMap(int capacity) {
        list = new Node[capacity];
    }
    Node[] list;


    public MyHashMap() {
        list = new Node[8];
    }

    class Node<K,V> implements Map.Entry<K,V> {
        private K key;
        private V value;
        private Node<K,V> next;

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

        public Node<K, V> getNext() {
            return next;
        }

        public void setNext(Node<K, V> next) {
            this.next = next;
        }
    }

    int size = 0;

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
            int index = getIndex(key);
            Node head = list[index];
            Node cur = new  Node<>(key,value);
            list[index] = cur;
            cur.setNext(head);
            size++;
            resizeIfNecessary();
            return null;
        }
    }
    private void resizeIfNecessary() {
        if (this.size < list.length * 0.75) {
            return;
        }
        Node<K, V>[] newlist = new Node[this.list.length * 2];
        for(Node head : list){
            if(head == null){
                continue;
            }
            while(head != null){
                int index = head.getKey().hashCode() & (newlist.length-1);
                Node nextHead = head.getNext();
                head.setNext(newlist[index]);
                newlist[index] = head;
                head = nextHead;
            }
        }
        this.list = newlist;
        System.out.println("扩容了，扩容到" + this.list.length);
    }

    public Node<K,V> getNode(Object key) {
        int index = getIndex(key);
        Node node = list[index];
        for (Node cur = node; cur != null; cur = cur.getNext()) {
            if (cur.getKey().equals(key)) {
                return cur;
            }
        }
        return null;
    }

    private int getIndex(Object key) {
        int index = key.hashCode() & (list.length-1);
        return index;
    }

    @Override
    public V remove(Object key) {
        int index = getIndex(key);
        Node<K,V> node = list[index];
        Node pre = null;
        for (Node cur = node; cur != null; cur = cur.getNext()) {
            if (cur.getKey().equals(key)) {
                pre.setNext(cur.getNext());
                size--;
                return (V) cur.getValue();
            }
            pre = cur;
        }
        return null;
    }

    @Override
    public void putAll(Map m) {

    }

    @Override
    public void clear() {
        list = new Node[list.length];
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
