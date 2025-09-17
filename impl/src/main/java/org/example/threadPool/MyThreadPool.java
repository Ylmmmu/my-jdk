package org.example.threadPool;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.Data;

@Data
public class MyThreadPool implements Executor {

    public MyThreadPool(BlockingQueue<Runnable> blockingQueue, long timeout,
                        TimeUnit timeUnit,
                        int maxPoolSize, int corePoolSize, MyAbortPolicy policy) {
        this.blockingQueue = blockingQueue;
        this.timeout = timeout;
        this.timeUnit = timeUnit;
        this.maxPoolSize = maxPoolSize;
        this.corePoolSize = corePoolSize;
        this.myAbortPolicy = policy;
    }

    public void execute(Runnable task) {
        if (this.addWorker(task)){
            return;
        }
        if (blockingQueue.offer(task)){
            return;
        }
        if (!blockingQueue.offer(task)){
            myAbortPolicy.abort(task);
        }
    }

    private boolean addWorker(Runnable task){
        while (true){
            int count = workerCount.get();
            if(count >= maxPoolSize){
                return false;
            }
            workerCount.compareAndSet(count,count+1);
            break;
        }
        MyWorker myWorker = new MyWorker(task);
        threads.add(myWorker);
        myWorker.thread.start();
        return true;
    }

    class MyWorker implements Runnable{
        private Thread thread;
        Runnable task;

        public MyWorker(Runnable task) {
            this.task = task;
            this.thread = Executors.defaultThreadFactory().newThread(this);
        }

        public List<MyWorker> getThreads(){
            return threads;
        }

        @Override
        public void run() {
            task.run();
            while (true) {
                Runnable runnable;
                try {
                    runnable = blockingQueue.poll(timeout,timeUnit);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                if (runnable != null) {
                    runnable.run();
                    continue;
                }
                int count = workerCount.get();
                if(count > corePoolSize && workerCount.compareAndSet(count,count-1)){
                    threads.remove(this);
                    System.out.println(this.thread.getName() + "线程已删除");
                    break;
                }
            }
        }
    }


    private AtomicInteger workerCount = new AtomicInteger(0);
    private BlockingQueue<Runnable> blockingQueue;
    private List<MyWorker> threads = new ArrayList<MyWorker>();
    private long timeout;
    private TimeUnit timeUnit;
    private int maxPoolSize;
    private int corePoolSize;
    private MyAbortPolicy myAbortPolicy;

}
