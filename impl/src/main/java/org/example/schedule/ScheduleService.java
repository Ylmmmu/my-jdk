package org.example.schedule;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.locks.LockSupport;

public class ScheduleService {



     Executor executorService = Executors.newFixedThreadPool(10);

     Trigger trigger = new Trigger();

    class Trigger{
        PriorityBlockingQueue<Job> queue = new PriorityBlockingQueue<>();
        Thread thread  = new Thread(() -> {
            while (true) {
                Job latelyJob = queue.peek();
                if (latelyJob == null) {
                    LockSupport.park();
                } else {
                    if (latelyJob.getStartTime() < System.currentTimeMillis()) {
                        latelyJob = queue.poll();
                        executorService.execute(latelyJob.getTask());
                        latelyJob.setStartTime(System.currentTimeMillis() + latelyJob.getDelay());
                        queue.offer(latelyJob);
                    }else {
                        LockSupport.parkUntil(latelyJob.getStartTime());
                    }
                }
            }
        });
        {

            thread.start();
            System.out.println("thread started");
        }

        public void wakeUp(){
            LockSupport.unpark(thread);
        }
    }



    public void schedule(Runnable task, long delay) {
        Job job = new Job(task, System.currentTimeMillis() +delay,delay);
        trigger.queue.offer(job);
        trigger.wakeUp();
    }
}