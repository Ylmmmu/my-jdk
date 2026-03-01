import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public class ScheduledExecutor {
    
    private final PriorityQueue<Task> tasks;
    private final ExecutorService executor;
    private final AtomicInteger idGenerator = new AtomicInteger(0);
    private final int maxQueueSize;
    private volatile boolean shutdown = false;
    private final Object lock = new Object();
    private Thread scheduler;
    
    private static class Task implements Comparable<Task> {
        final int id;
        final long executeTime;
        final Runnable command;
        final AtomicBoolean cancelled = new AtomicBoolean(false);
        final boolean isPeriodic;
        final long period;
        final boolean fixedRate;
        
        Task(int id, long executeTime, Runnable command) {
            this(id, executeTime, command, false, 0, false);
        }
        
        Task(int id, long executeTime, Runnable command, boolean isPeriodic, long period, boolean fixedRate) {
            this.id = id;
            this.executeTime = executeTime;
            this.command = command;
            this.isPeriodic = isPeriodic;
            this.period = period;
            this.fixedRate = fixedRate;
        }
        
        @Override
        public int compareTo(Task other) {
            int cmp = Long.compare(this.executeTime, other.executeTime);
            return cmp != 0 ? cmp : Integer.compare(this.id, other.id);
        }
    }
    
    public ScheduledExecutor() {
        this(1000);
    }
    
    public ScheduledExecutor(int maxQueueSize) {
        this.maxQueueSize = maxQueueSize;
        this.tasks = new PriorityQueue<>(16);
        this.executor = Executors.newCachedThreadPool();
        startScheduler();
    }
    
    private void startScheduler() {
        scheduler = new Thread(() -> {
            while (!shutdown) {
                Task task;
                synchronized (lock) {
                    while (!shutdown && (tasks.isEmpty() || tasks.peek().executeTime > System.currentTimeMillis())) {
                        if (tasks.isEmpty()) {
                            try {
                                lock.wait();
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                return;
                            }
                        } else {
                            long waitTime = tasks.peek().executeTime - System.currentTimeMillis();
                            if (waitTime > 0) {
                                try {
                                    lock.wait(waitTime);
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                    return;
                                }
                            }
                        }
                    }
                    
                    if (shutdown) break;
                    task = tasks.poll();
                }
                
                if (task == null || task.cancelled.get()) continue;
                
                try {
                    executor.execute(() -> {
                        try {
                            task.command.run();
                        } catch (Throwable t) {
                            System.err.println("Task execution error: " + t.getMessage());
                        }
                    });
                    
                    if (task.isPeriodic) {
                        long nextTime = task.fixedRate 
                            ? task.executeTime + task.period 
                            : System.currentTimeMillis() + task.period;
                        Task nextTask = new Task(
                            idGenerator.incrementAndGet(),
                            nextTime,
                            task.command,
                            true,
                            task.period,
                            task.fixedRate
                        );
                        synchronized (lock) {
                            if (!shutdown) {
                                tasks.offer(nextTask);
                                lock.notifyAll();
                            }
                        }
                    }
                } catch (RejectedExecutionException e) {
                    break;
                }
            }
        });
        scheduler.setName("ScheduledExecutor-Scheduler");
        scheduler.setDaemon(true);
        scheduler.start();
    }
    
    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        return schedule0(command, delay, unit, false, 0, false);
    }
    
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
        return schedule0(command, initialDelay, unit, true, period, true);
    }
    
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
        return schedule0(command, initialDelay, unit, true, delay, false);
    }
    
    private ScheduledFuture<?> schedule0(Runnable command, long delay, TimeUnit unit, boolean isPeriodic, long period, boolean fixedRate) {
        if (shutdown) {
            throw new RejectedExecutionException("Executor has been shutdown");
        }
        
        long executeTime = System.currentTimeMillis() + unit.toMillis(delay);
        
        Task task = new Task(
            idGenerator.incrementAndGet(),
            executeTime,
            Objects.requireNonNull(command),
            isPeriodic,
            unit.toMillis(period),
            fixedRate
        );
        
        synchronized (lock) {
            if (tasks.size() >= maxQueueSize) {
                throw new RejectedExecutionException("Task queue is full");
            }
            tasks.offer(task);
            lock.notifyAll();
        }
        
        return new ScheduledFuture<>(task);
    }
    
    public void shutdown() {
        synchronized (lock) {
            shutdown = true;
            lock.notifyAll();
        }
        executor.shutdown();
    }
    
    public List<Runnable> shutdownNow() {
        synchronized (lock) {
            shutdown = true;
            lock.notifyAll();
        }
        List<Runnable> remaining = new ArrayList<>();
        Task task;
        while ((task = tasks.poll()) != null) {
            remaining.add(task.command);
        }
        List<Runnable> unstarted = executor.shutdownNow();
        remaining.addAll(unstarted);
        return remaining;
    }
    
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        long nanos = unit.toNanos(timeout);
        executor.shutdown();
        while (!executor.isTerminated()) {
            if (nanos <= 0) return false;
            long ms = Math.min(1, TimeUnit.MILLISECONDS.convert(nanos, TimeUnit.NANOSECONDS));
            nanos -= TimeUnit.NANOSECONDS.convert(ms, TimeUnit.MILLISECONDS);
            if (executor.awaitTermination(ms, TimeUnit.MILLISECONDS)) break;
        }
        return true;
    }
    
    public int getQueueSize() {
        synchronized (lock) {
            return tasks.size();
        }
    }
    
    public static class ScheduledFuture<T> {
        private final Task task;
        
        ScheduledFuture(Task task) {
            this.task = task;
        }
        
        public boolean cancel() {
            return task.cancelled.getAndSet(true);
        }
        
        public boolean isCancelled() {
            return task.cancelled.get();
        }
    }
    
    public static void main(String[] args) throws Exception {
        ScheduledExecutor scheduler = new ScheduledExecutor(100);
        
        System.out.println("=== Basic schedule test ===");
        scheduler.schedule(() -> System.out.println("Task A (1s)"), 1000, TimeUnit.MILLISECONDS);
        scheduler.schedule(() -> System.out.println("Task B (500ms)"), 500, TimeUnit.MILLISECONDS);
        scheduler.schedule(() -> System.out.println("Task C (2s)"), 2000, TimeUnit.MILLISECONDS);
        
        System.out.println("\n=== Cancel test ===");
        ScheduledFuture<?> future = scheduler.schedule(() -> System.out.println("Task D (should not run)"), 500, TimeUnit.MILLISECONDS);
        Thread.sleep(50);
        future.cancel();
        System.out.println("Task D cancelled");
        
        System.out.println("\n=== Fixed rate test ===");
        final AtomicInteger count = new AtomicInteger(0);
        scheduler.scheduleAtFixedRate(() -> {
            int n = count.incrementAndGet();
            System.out.println("Rate task count: " + n + " at " + System.currentTimeMillis() % 10000);
            if (n >= 3) throw new RuntimeException("Stop after 3 times");
        }, 0, 500, TimeUnit.MILLISECONDS);
        
        System.out.println("\n=== Fixed delay test ===");
        final AtomicInteger count2 = new AtomicInteger(0);
        scheduler.scheduleWithFixedDelay(() -> {
            int n = count2.incrementAndGet();
            System.out.println("Delay task count: " + n + " at " + System.currentTimeMillis() % 10000);
        }, 0, 300, TimeUnit.MILLISECONDS);
        
        Thread.sleep(2000);
        
        System.out.println("\n=== Shutdown test ===");
        scheduler.shutdown();
        scheduler.awaitTermination(5, TimeUnit.SECONDS);
        System.out.println("Scheduler terminated");
    }
}
