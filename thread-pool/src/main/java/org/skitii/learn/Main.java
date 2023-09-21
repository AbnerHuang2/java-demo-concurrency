package org.skitii.learn;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author skitii
 * @since 2023/09/20
 **/
public class Main {

    public static void main(String[] args) throws InterruptedException {
        LearnThreadPoolExecutor executor = new LearnThreadPoolExecutor(5, 8, 10, new LinkedBlockingQueue<>(3));
        for (int i = 0; i < 10; i++) {
            Thread.sleep(100L);
            int finalI = i;
            executor.execute(() -> {
                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println(Thread.currentThread().getName()+" 执行任务 "+ finalI);
            });
        }

        executor.shutdown();

    }

}
