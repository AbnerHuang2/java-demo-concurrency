package org.skitii.learn;

import java.util.HashSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author skitii
 * @since 2023/09/20
 **/
public class LearnThreadPoolExecutor {
    /**
     * ctl是一个原子变量，用来记录线程池的状态和线程池中的线程数量
     * 线程池状态：RUNNING、SHUTDOWN、STOP、TIDYING、TERMINATED
     * 线程池中的线程数量：线程池中的线程数量
     * 前 3 位表示线程池状态，后29位表示线程池中的线程数量
     * 绝！！！
     */
    private final AtomicInteger ctl = new AtomicInteger(ctlOf(RUNNING, 0));
    private static final int COUNT_BITS = Integer.SIZE - 3;
    private static final int CAPACITY   = (1 << COUNT_BITS) - 1;

    // runState is stored in the high-order bits
    private static final int RUNNING    = -1 << COUNT_BITS;
    private static final int SHUTDOWN   =  0 << COUNT_BITS;
    private static final int STOP       =  1 << COUNT_BITS;
    private static final int TIDYING    =  2 << COUNT_BITS;
    private static final int TERMINATED =  3 << COUNT_BITS;

    // Packing and unpacking ctl
    private static int runStateOf(int c)     { return c & ~CAPACITY; }
    private static int workerCountOf(int c)  { return c & CAPACITY; }
    private static int ctlOf(int rs, int wc) { return rs | wc; }

    BlockingQueue<Runnable> workQueue;
    private final ReentrantLock mainLock = new ReentrantLock();
    HashSet<Worker> workers = new HashSet<>();

    int corePoolSize;
    int maximumPoolSize;
    int keepAliveTime;

    public LearnThreadPoolExecutor(int corePoolSize, int maximumPoolSize, int keepAliveTime, BlockingQueue<Runnable> workQueue) {
        this.corePoolSize = corePoolSize;
        this.maximumPoolSize = maximumPoolSize;
        this.keepAliveTime = keepAliveTime;
        this.workQueue = workQueue;
    }

    /**
     * 执行一个任务
     * @param command 任务
     */
    public void execute(Runnable command){
        if (command == null) {
            throw new NullPointerException();
        }
        /**
         * 执行流程：
         * 1.如果线程池中的线程数 < corePoolSize，那么马上创建线程运行这个任务
         * 2.如果线程池中的线程数 >= corePoolSize，那么将这个任务放入队列
         * 3.如果队列满了且线程池中的线程数 < maximumPoolSize，那么创建非核心线程运行这个任务
         * 4.如果队列满了且线程池中的线程数 >= maximumPoolSize，那么线程池会启动饱和拒绝策略来执行
         * 5.最后都失败了，也尝试创建一下非核心线程，可能在创建的时候，刚好有线程执行完呢
         */
        int c = ctl.get();
        if (workerCountOf(c) < corePoolSize) {
            if (addWorker(command, true)) {
                return;
            }
            c = ctl.get();
        }
        if (isRunning(c) && workQueue.offer(command)) {
            /**
             * 再次检查线程池状态，因为可能在上一步骤中线程池状态发生了变化
             */
            int recheck = ctl.get();
            if (! isRunning(recheck) && remove(command)) {
                reject(command);
            } else if (workerCountOf(recheck) == 0) {
                // 任务放进队列后，线程池中没有线程了，那么尝试创建一个非核心线程去队列中获取任务执行
                addWorker(null, false);
            }
        } else if (!addWorker(command, false)) {    //线程池状态不在运行，也尝试一下创建非核心线程
            reject(command);
        }
    }

    private boolean isRunning(int c) {
        return c < SHUTDOWN;
    }

    private boolean remove(Runnable task) {
        return workQueue.remove(task);
    }

    private void reject(Runnable command) {
        //可以设置一些拒绝策略，这里就简单抛异常了
        throw new RuntimeException("task reject");
    }

    /**
     * 添加一个新的线程
     * 1.看看线程池状态是否允许添加线程
     * 2.尝试创建一个新的线程
     * 3.加锁修改线程池状态和线程池中的线程数量
     * 4.启动新线程
     *
     * @param firstTask 任务
     * @param core 是否是核心线程
     */
    public boolean addWorker(Runnable firstTask, boolean core) {
        retry:
            for (;;){
                int c = ctl.get();
                int rs = runStateOf(c);
                //如果线程池状态是 SHUTDOWN 或者 STOP，那么不允许添加新的线程
                if (rs >= SHUTDOWN && !(rs == SHUTDOWN && firstTask == null && !workQueue.isEmpty())) {
                    return false;
                }
                for (; ; ) {
                    int wc = workerCountOf(c);
                    //如果线程数量超过了最大值，那么不允许添加新的线程
                    if (wc >= CAPACITY || wc >= (core ? corePoolSize : maximumPoolSize)) {
                        return false;
                    }
                    //尝试增加线程数量
                    if (ctl.compareAndSet(c, c + 1)) {
                        break retry;
                    }
                    c = ctl.get();
                    //如果线程池状态发生了变化，那么重新检查
                    if (runStateOf(c) != rs) {
                        continue retry;
                    }
                }
            }
        // cas 修改线程池状态成功，创建一个新的线程
        boolean workerStarted = false;
        boolean workerAdded = false;
        Worker w = null;
        try {
            w = new Worker(firstTask);
            Thread t = w.thread;
            if (t != null) {
                ReentrantLock mainLock = this.mainLock;
                mainLock.lock();
                try {
                    int rs = runStateOf(ctl.get());
                    //如果线程池状态是 RUNNING，那么启动新线程
                    if (rs < SHUTDOWN || (rs == SHUTDOWN && firstTask == null)) {
                        if (t.isAlive()) {
                            throw new IllegalThreadStateException();
                        }
                        workers.add(w);
                        workerAdded = true;
                    }
                } finally {
                    mainLock.unlock();
                }
                if (workerAdded) {
                    t.start();
                    workerStarted = true;
                }
            }
        } finally {
            if (!workerStarted) {
                //如果启动新线程失败，那么减少线程池中的线程数量
                addWorkerFailed(w);
            }
        }
        return workerStarted;
    }

    /**
     * 添加失败，减少线程池中的线程数量
     * @param w
     */
    private void addWorkerFailed(Worker w) {
        ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            if (w != null) {
                workers.remove(w);
            }
            ctl.decrementAndGet();
        } finally {
            mainLock.unlock();
        }
    }

    private class Worker implements Runnable {
        final Thread thread;
        Runnable firstTask;

        Worker(Runnable firstTask) {
            this.firstTask = firstTask;
            this.thread = new Thread(this);
        }

        public void run() {
            runWorker(this);
        }

        private void runWorker(Worker w) {
            Runnable task = w.firstTask;
            w.firstTask = null;
            if (task != null || (task = getTask()) != null) {
                task.run();
                //任务执行完毕，减少线程池中的线程数量
                ctl.decrementAndGet();
            }
        }

        private Runnable getTask() {
            for (; ; ) {
                try {
                    Runnable r = workQueue.take();
                    if (r != null) {
                        return r;
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

    }
}
