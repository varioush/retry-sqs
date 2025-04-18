package com.example.framework;

import com.example.framework.model.Task;
import com.example.framework.processor.TaskProcessor;
import com.example.framework.queue.QueueService;
import com.example.framework.scheduler.TaskScheduler;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class RetryFramework {
    private static final Logger LOGGER = Logger.getLogger(RetryFramework.class.getName());
    
    private final TaskProcessor<?> taskProcessor;
    private final QueueService queueService;
    private final TaskScheduler scheduler;
    private final long schedulerInitialDelay;
    private final long schedulerPeriod;
    private final TimeUnit schedulerTimeUnit;
    
    RetryFramework(TaskProcessor<?> taskProcessor, QueueService queueService, 
                  TaskScheduler scheduler, long schedulerInitialDelay, 
                  long schedulerPeriod, TimeUnit schedulerTimeUnit) {
        this.taskProcessor = taskProcessor;
        this.queueService = queueService;
        this.scheduler = scheduler;
        this.schedulerInitialDelay = schedulerInitialDelay;
        this.schedulerPeriod = schedulerPeriod;
        this.schedulerTimeUnit = schedulerTimeUnit;
    }
    
    /**
     * Start the framework scheduler
     */
    public void start() {
        LOGGER.info("Starting retry framework");
        scheduler.start(schedulerInitialDelay, schedulerPeriod, schedulerTimeUnit);
    }
    
    /**
     * Stop the framework scheduler
     */
    public void stop() {
        LOGGER.info("Stopping retry framework");
        scheduler.stop();
    }
    
    /**
     * Submit a synchronous task
     */
    public <T> T submitSyncTask(String payload) throws Exception {
        Task task = new Task(payload, Task.TaskType.SYNC);
        return (T) taskProcessor.processSyncTask(task);
    }
    
    /**
     * Submit an asynchronous task
     */
    public <T> CompletableFuture<T> submitAsyncTask(String payload) {
        Task task = new Task(payload, Task.TaskType.ASYNC);
        return (CompletableFuture<T>) taskProcessor.processAsyncTask(task);
    }
    
    /**
     * Get the queue service
     */
    public QueueService getQueueService() {
        return queueService;
    }
    
    /**
     * Get the task processor
     */
    public TaskProcessor<?> getTaskProcessor() {
        return taskProcessor;
    }
}

