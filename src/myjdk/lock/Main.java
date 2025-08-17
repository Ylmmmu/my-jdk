package myjdk.lock;

import myjdk.lock.spring.Factory;
import myjdk.lock.spring.FactoryImpl;

public class Main {
    public static void main(String[] args) throws Exception {
        Factory factory = new FactoryImpl();
        factory.init(Main.class.getPackage().getName());
        Lock bean =  factory.getBean(Lock.class);
        System.out.println(bean);
        System.out.println(bean.getMap());
        System.out.println(bean.getLockImpl());
    }
}
