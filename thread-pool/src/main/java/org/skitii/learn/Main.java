package org.skitii.learn;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author skitii
 * @since 2023/09/20
 **/
public class Main {
    private static AtomicInteger ato = new AtomicInteger(0);
    public static void main(String[] args) throws InterruptedException {
        LearnThreadPoolExecutor executor = new LearnThreadPoolExecutor(2, 4, 10, new LinkedBlockingQueue<>(10));
        executor.execute(() -> {
            System.out.println("hello");
        });
    }

    public static void testRequest() {
        retry1:
        for (int i = 0; i < 10; i++) {
            while (i == 5) {
                continue retry1;
            }
            System.out.print(i + " ");
        }
    }

}
