package org.example.threadPool;

public class RejectPolicy implements MyAbortPolicy {
    @Override
    public void abort(Runnable runnable) {
        System.out.println("放弃任务" + runnable);
    }
}
