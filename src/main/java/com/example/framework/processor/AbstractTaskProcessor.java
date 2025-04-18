package com.example.framework.processor;

import com.example.framework.model.Task;
import com.example.framework.queue.QueueService;
import com.example.framework.retry.RetryConfig;
import com.example.framework.retry.RetryExecutor;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class AbstractTaskProcessor<T> implements TaskProcessor<T> {
    private static final Logger LOGGER = Logger.getLogger(AbstractTaskProcessor.class.getName());
    
    protected final RetryExecutor retryExecutor;
    protected final QueueService queueService;
    
    public AbstractTaskProcessor(RetryConfig retryConfig, QueueService queueService) {
        this.queueService = queueService;
        this.retryExecutor = new RetryExecutor(retryConfig, queueService);
    }
    
    /**
     * Implement this method to define the actual task processing logic
     */
    protected abstract T processTask(Task task) throws Exception;
    
    @Override
    public T processSyncTask(Task task) throws Exception {
        LOGGER.info("Processing sync task: " + task.getId());
        return retryExecutor.executeWithRetry(
            () -> processTask(task),
            task,
            (failedTask, exception) -> {
                LOGGER.log(Level.SEVERE, "All retries failed for sync task: " + failedTask.getId(), exception);
                handleFailedTask(failedTask, exception);
            }
        );
    }
    
    @Override
    public CompletableFuture<T> processAsyncTask(Task task) {
        LOGGER.info("Processing async task: " + task.getId());
        return retryExecutor.executeWithRetryAsync(
            () -> processTask(task),
            task,
            (failedTask, exception) -> {
                LOGGER.log(Level.SEVERE, "All retries failed for async task: " + failedTask.getId(), exception);
                handleFailedTask(failedTask, exception);
            }
        );
    }
    
    @Override
    public boolean handleFailedTask(Task task, Throwable exception) {
        if (task.getType() == Task.TaskType.ASYNC) {
            LOGGER.info("Sending failed async task to queue: " + task.getId());
            return queueService.sendTask(task);
        } else {
            // For sync tasks, we don't send to queue by default
            LOGGER.info("Sync task failed without recovery: " + task.getId());
            return false;
        }
    }
}

