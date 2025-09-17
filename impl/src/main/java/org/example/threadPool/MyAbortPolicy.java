package org.example.threadPool;

public  interface MyAbortPolicy {
    void abort(Runnable runnable);
}
