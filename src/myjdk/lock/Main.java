package myjdk.lock;

import myjdk.lock.spring.Factory;
import myjdk.lock.spring.FactoryImpl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class Main {
    public static void main(String[] args) throws Exception {
        Factory factory = new FactoryImpl();
        factory.init(Main.class.getPackage().getName());
        Lock bean =  factory.getBean(Lock.class);
        System.out.println(bean);
        System.out.println(bean.getMap());
        System.out.println(bean.getLockImpl());
    }

    @Test
    void testApi() {
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

    }
}
