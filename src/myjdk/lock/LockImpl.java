package myjdk.lock;

import myjdk.lock.spring.Autowire;
import myjdk.lock.spring.Component;
import myjdk.lock.spring.PostConstruct;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;
@Component()
public class LockImpl implements Lock {
    @Autowire
    private Map map;
    static Unsafe unsafe;
    static long offset;
    @Autowire
            private Lock lockImpl;

    List<Thread> threads = new ArrayList<Thread>();
    AtomicInteger atomicInteger = new AtomicInteger(0);
    volatile Thread owner;

    @PostConstruct
    private void post(){
        System.out.println("post");
    }

    static  {
        try {
            Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            unsafe = (Unsafe) theUnsafe.get(null);
            offset =  unsafe.objectFieldOffset(
                    LockImpl.class.getDeclaredField("owner"));
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
    @Override
    public boolean lock() {
        /**
         * 1、不进队列 头节点为空
         * 2、先进队列 再拿到资源 自己成为头节点、
         *
         * 1、直接请求到锁
         * 2、进入队列 请求唤醒信号 阻塞 被唤醒 请求到锁 将自己作为头节点 释放锁 唤醒下一个节点
         * 3、进入队列 请求唤醒信号 请求到锁 将自己作为头节点 释放锁 唤醒下一个节点
         *
         * 阻塞的情况：请求失败 进队 排队请求
         * 释放的情况 释放 唤醒
         *
         *
         *
         * 释放的时候让第一个非持有锁的
         */
        if(owner == Thread.currentThread()){
            atomicInteger.incrementAndGet();
            return true;
        }
        threads.add(Thread.currentThread());
        while (true) {
            boolean success = false;
            if(Thread.currentThread() == threads.get(0)){
                success = atomicInteger.compareAndSet(0,1);
            }
            if (!success) {
                LockSupport.park();
            }else {
                owner = Thread.currentThread();
                threads.remove(Thread.currentThread());
                return true;
            }
        }
    }

    @Override
    public boolean unlock() {
        if (owner == Thread.currentThread()) {
            int i = atomicInteger.decrementAndGet();
            if (i == 0) {
                if (threads.size() > 0) {
                    Thread remove = threads.remove(0);
                    LockSupport.unpark(remove);
                }
                owner = null;
            }
            return true;
        }

        return false;
    }

    @Override
    public Map getMap() {
        return map;
    }

    public void setMap(Map map) {
        this.map = map;
    }

    @Override
    public Lock getLockImpl() {
        return lockImpl;
    }

    public void setLockImpl(Lock lockImpl) {
        this.lockImpl = lockImpl;
    }
}
