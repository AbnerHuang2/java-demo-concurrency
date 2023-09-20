package org.skitii.my;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author skitii
 * @since 2023/09/20
 **/
public class MyExecutor extends Thread{
    AtomicInteger state = new AtomicInteger(0);
    int corePoolSize;
    int maximumPoolSize;

    BlockingQueue<Runnable> queue;
    int blockingQueueSize;

    MyThreadFactory threadFactory;


    public MyExecutor(int corePoolSize, int maximumPoolSize, int blockingQueueSize) {
        this.corePoolSize = corePoolSize;
        this.maximumPoolSize = maximumPoolSize;
        queue = new LinkedBlockingQueue(blockingQueueSize);
        this.blockingQueueSize = blockingQueueSize;
        threadFactory = new MyThreadFactory(corePoolSize, maximumPoolSize);
        this.start();
    }
    public void execute(Runnable command) {
        if (state.get() == 1) {
            throw new RuntimeException("executor has been shutdown");
        }
        queue.add(command);
    }

    public void shutdown() {
        threadFactory.threads.forEach(thread -> {
            thread.interrupt();
        });
        state.set(1);
        if (this.isAlive()){
            this.interrupt();
        }
        System.err.println("executor shutdown...");
    }

    @Override public void run() {
        while (state.get() == 0) {
            Runnable take = null;
            try {
                take = queue.take();
                Thread thread = threadFactory.getThread(take);
                thread.start();
                threadFactory.removeThread(thread);
            } catch (InterruptedException e) {
                System.out.println("executor interrupted");
            }
        }
    }
}
