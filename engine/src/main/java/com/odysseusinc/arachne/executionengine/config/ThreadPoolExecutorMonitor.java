package com.odysseusinc.arachne.executionengine.config;

import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@ManagedResource
public class ThreadPoolExecutorMonitor {

    private final ThreadPoolTaskExecutor threadPoolExecutor;

    public ThreadPoolExecutorMonitor(ThreadPoolTaskExecutor threadPoolExecutor) {

        this.threadPoolExecutor = threadPoolExecutor;
    }

    @ManagedAttribute
    public int getQueueSize(){

        return threadPoolExecutor.getThreadPoolExecutor().getQueue().size();
    }

    @ManagedAttribute
    public int getCorePoolSize(){

        return threadPoolExecutor.getCorePoolSize();
    }

    @ManagedAttribute
    public int getMaximumPoolSize(){

        return threadPoolExecutor.getThreadPoolExecutor().getMaximumPoolSize();
    }

    @ManagedAttribute
    public int getPoolSize(){

        return threadPoolExecutor.getPoolSize();
    }

    @ManagedAttribute
    public int getActiveSize(){

        return threadPoolExecutor.getActiveCount();
    }

    @ManagedAttribute
    public long getTaskCount(){

        return threadPoolExecutor.getThreadPoolExecutor().getTaskCount();
    }

    @ManagedAttribute
    public long getCompletedTaskCount(){

        return threadPoolExecutor.getThreadPoolExecutor().getCompletedTaskCount();
    }

    @ManagedAttribute
    public int getLargestPoolSize(){

        return threadPoolExecutor.getThreadPoolExecutor().getLargestPoolSize();
    }
}
