按自己的思路实现最简单的java线程池
实现思路：
核心，一个线程池Executor， 一个任务队列BlockingQueue，一个线程管理工厂
1. 线程池Executor，用来管理线程池，提供提交任务，关闭线程池等方法
2. 任务队列BlockingQueue，用来存放任务，提供任务的添加，获取，删除等方法
3. 线程管理工厂，用来创建线程，提供创建线程的方法

实现过程：
1. 初始化Executor， 线程工厂，任务队列
2. 提交任务，判断线程池状态，判断线程池中的线程数，判断任务队列是否满了，线程管理工厂获取线程，执行任务
3. 关闭线程池，判断线程池状态，判断任务队列是否为空，判断线程池中的线程是否为空，关闭线程池