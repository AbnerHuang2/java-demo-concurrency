package org.skitii.my;

import java.util.ArrayList;
import java.util.List;

/**
 * @author skitii
 * @since 2023/09/20
 **/
public class MyThreadFactory {
    int corePoolSize;
    int maximumPoolSize;
    List<Thread> threads = new ArrayList<>();

    public MyThreadFactory(int corePoolSize, int maximumPoolSize) {
        this.corePoolSize = corePoolSize;
        this.maximumPoolSize = maximumPoolSize;
    }

    public Thread getThread(Runnable runnable) {
        if (threads.size() < maximumPoolSize) {
            Thread thread = new Thread(runnable);
            threads.add(thread);
            return thread;
        } else {
            throw new RuntimeException("thread pool is full");
        }
    }

    public void removeThread(Thread thread) {
        threads.remove(thread);
    }

}
