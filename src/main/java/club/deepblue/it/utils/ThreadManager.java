package club.deepblue.it.utils;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * 线程池管理的工具类:ThreadManager
 */
public class ThreadManager {
    //通过ThreadPoolExecutor的代理类来对线程池的管理
    private static ThreadPollProxy mThreadPollProxy; //单例对象

    public static ThreadPollProxy getThreadPollProxy() {
        synchronized (ThreadPollProxy.class) {
            if (mThreadPollProxy == null) {
                mThreadPollProxy = new ThreadPollProxy(3, 6, 1000);
            }
        }
        return mThreadPollProxy;
    }

    //通过ThreadPoolExecutor的代理类来对线程池的管理
    public static class ThreadPollProxy {
        //线程池执行者, java内部通过该api实现对线程池管理
        private ThreadPoolExecutor poolExecutor;
        private final int corePoolSize;
        private final int maximumPoolSize;
        private final long keepAliveTime;

        ThreadPollProxy(int corePoolSize, int maximumPoolSize, long keepAliveTime) {
            this.corePoolSize = corePoolSize;
            this.maximumPoolSize = maximumPoolSize;
            this.keepAliveTime = keepAliveTime;
        } //对外提供一个执行任务的方法

        public void execute(Runnable r) {
            if (poolExecutor == null || poolExecutor.isShutdown()) {
                ThreadFactory namedThreadFactory = new ThreadFactory() {
                    final AtomicInteger threadNumber = new AtomicInteger(1);

                    public Thread newThread(Runnable r) {
                        Thread t = new Thread(Thread.currentThread().getThreadGroup(), r, "topPatternTasklet-thread"
                                + (threadNumber.getAndIncrement()));
                        t.setUncaughtExceptionHandler(new ThreadHandler());
                        return t;
                    }
                };

                BlockingQueue<Runnable> workingQueue = new ArrayBlockingQueue<>(1024);
                RejectedExecutionHandler rejectedExecutionHandler = new ThreadPoolExecutor.AbortPolicy();
                poolExecutor = new ThreadPoolExecutor(
                        //核心线程数量
                        corePoolSize,
                        //最大线程数量
                        maximumPoolSize,
                        //当线程空闲时，保持活跃的时间
                        keepAliveTime, //时间单元 ，
                        // 毫秒级
                        TimeUnit.MILLISECONDS,
                        //线程任务队列
                        workingQueue,
                        //创建线程的工厂
                        namedThreadFactory,
                        // 拒绝策略
                        rejectedExecutionHandler);
            }
            /**
             * execute和submit的区别与联系：https://www.cnblogs.com/jpfss/p/11192220.html
             * （1）execute和submit都属于线程池的方法，execute只能提交Runnable类型的任务，而submit既能提交Runnable类型任务也能提交Callable类型任务。
             * （2）execute会直接抛出任务执行时的异常，submit会吃掉异常，可通过Future的get方法将任务执行时的异常重新抛出。
             * （3）execute所属顶层接口是Executor,submit所属顶层接口是ExecutorService，实现类ThreadPoolExecutor重写了execute方法,抽象类AbstractExecutorService重写了submit方法。
             */
            poolExecutor.execute(r);
        }
    }
}

