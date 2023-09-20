package org.skitii.my;

public class Main {

    public static void main(String[] args) throws InterruptedException {
        // 创建
        MyExecutor executor = new MyExecutor(1, 1, 10);
        // 执行
        executor.execute(() -> System.out.println("hello"));

        for (int i = 0; i < 5; i++) {
            int finalI = i;
            new Thread(() -> {
                try {
                    if (finalI == 3) {
                        Thread.sleep(1100L);
                    }
                    executor.execute(() -> System.out.println("job "+ finalI));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }
        Thread.sleep(1000L);
        // 关闭
        executor.shutdown();

    }

}
