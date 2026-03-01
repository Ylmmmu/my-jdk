package org.example.schedule;

import lombok.Data;



@Data
public class Job implements Comparable<Job> {
    private long startTime;
    private Runnable task;
    private long delay;

    public Job(Runnable task, long startTime, long delay) {
        this.startTime = startTime;
        this.task = task;
        this.delay = delay;
    }
    @Override
    public int compareTo(Job o) {
        return Long.compare(this.startTime, o.startTime);
    }
}