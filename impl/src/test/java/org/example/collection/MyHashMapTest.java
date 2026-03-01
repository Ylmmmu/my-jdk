package org.example.collection;

import org.example.collection.MyHashMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.RunWith;
import org.junit.runner.notification.Failure;
import org.junit.runners.JUnit4;

import static org.junit.Assert.*;
@RunWith(JUnit4.class)
public class MyHashMapTest {
    private MyHashMap<String, Integer> map;

    public static void main(String[] args) {



        MyHashMapTest myHashMapTest = new MyHashMapTest();
        myHashMapTest.map = new MyHashMap<>();

        MyHashMap<String, String> myHashMap = new MyHashMap<>();
        int count = 100000;
        for (int i = 0; i < count; i++) {
            myHashMap.put(String.valueOf(i), String.valueOf(i));
        }

        assertEquals(count, myHashMap.size());

        for (int i = 0; i < count; i++) {
            assertEquals(String.valueOf(i), myHashMap.get(String.valueOf(i)));
        }

        myHashMap.remove("8");
        assertNull(myHashMap.get("8"));

        assertEquals(count - 1, myHashMap.size());



        myHashMapTest.testEmptyMap();
        myHashMapTest.map = new MyHashMap<>();
        myHashMapTest.testPutAndGet();
        myHashMapTest.map = new MyHashMap<>();
        myHashMapTest.testRemove();
        myHashMapTest.map = new MyHashMap<>();
        myHashMapTest.testMapResize();
    }

    @Before
    public void setUp() {
        map = new MyHashMap<>();
    }

    @Test
    public void testEmptyMap() {
        assertTrue("新创建的map应该是空的", map.isEmpty());
        assertEquals("新创建的map大小应该是0", 0, map.size());
        assertNull("获取不存在的key应该返回null", map.get("nonexistent"));
        assertFalse("containsKey对于不存在的key应该返回false", map.containsKey("nonexistent"));
    }

    @Test
    public void testPutAndGet() {
        // 测试基本put和get
        assertNull(map.put("one", 1));
        assertEquals(Integer.valueOf(1), map.get("one"));

        // 测试覆盖已有key的值
        assertEquals(Integer.valueOf(1), map.put("one", 100));
        assertEquals(Integer.valueOf(100), map.get("one"));

        // 测试添加多个元素
        assertNull(map.put("two", 2));
        assertNull(map.put("three", 3));
        assertEquals(3, map.size());


    }

    @Test
    public void testRemove() {
        map.put("one", 1);
        map.put("two", 2);

        // 测试移除存在的key
        assertEquals(Integer.valueOf(1), map.remove("one"));
        assertNull(map.get("one"));
        assertFalse(map.containsKey("one"));
        assertEquals(1, map.size());

        // 测试移除不存在的key
        assertNull(map.remove("nonexistent"));
        assertEquals(1, map.size());
    }




    @Test
    public void testMapResize() {
        // 假设我们的map初始容量是16，负载因子0.75
        // 添加足够多的元素触发扩容
        for (int i = 0; i < 20; i++) {
            map.put("key" + i, i);
        }

        // 验证所有元素仍然存在
        for (int i = 0; i < 20; i++) {
            assertEquals(Integer.valueOf(i), map.get("key" + i));
        }

        // 验证大小正确
        assertEquals(20, map.size());
    }

    @Test
    public void testKeySetAndValues() {
        map.put("one", 1);
        map.put("two", 2);
        map.put("three", 3);

        // 测试keySet
        assertTrue(map.keySet().contains("one"));
        assertTrue(map.keySet().contains("two"));
        assertTrue(map.keySet().contains("three"));
        assertEquals(3, map.keySet().size());

        // 测试values
        assertTrue(map.values().contains(1));
        assertTrue(map.values().contains(2));
        assertTrue(map.values().contains(3));
        assertEquals(3, map.values().size());
    }
}