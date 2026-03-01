package org.example;

import org.example.schedule.ScheduleService;
import org.example.spring.Factory;
import org.example.spring.FactoryImpl;
import org.example.threadPool.MyThreadPool;
import org.example.threadPool.RejectPolicy;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

//TIP 要<b>运行</b>代码，请按 <shortcut actionId="Run"/> 或
// 点击装订区域中的 <icon src="AllIcons.Actions.Execute"/> 图标。
public class Main {

    /**
     * 需求
     * 1、执行任务 不阻塞主线程 线程池
     * 2、延迟执行任务 park
     * 3、定时执行任务
     * 4、
     *
     * @param args
     */
    public static void main(String[] args) throws Exception {
        Factory factory = new FactoryImpl();
        factory.init("org.example");

//        testThreadPool();
    }

    public static void testThreadPool() {
        MyThreadPool myThreadPool = new MyThreadPool(new LinkedBlockingQueue<>(100), 1,
                TimeUnit.SECONDS, 10, 1,
                new RejectPolicy());
        IntStream.range(0, 100).boxed().parallel().forEach(i -> {
            myThreadPool.execute(() -> {
                System.out.println(Thread.currentThread().getName() + "--- " + i + " 任务");

            });
        });
        System.out.println("主线程没有被阻塞");
    }

    public static void testSchedule() {
        ScheduleService scheduleService = new ScheduleService();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        System.out.println(dateFormat.format(System.currentTimeMillis()));
        scheduleService.schedule(() -> System.out.println("每5秒一次" + dateFormat.format(System.currentTimeMillis())),
                5000);
        scheduleService.schedule(() -> System.out.println("每10秒一次" + dateFormat.format(System.currentTimeMillis())),
                10000);
        System.out.println(dateFormat.format(System.currentTimeMillis()));
    }
}