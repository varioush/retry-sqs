package com.example.framework.retry;

import com.example.framework.model.Task;
import com.example.framework.queue.QueueService;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RetryExecutor {
    private static final Logger LOGGER = Logger.getLogger(RetryExecutor.class.getName());
    
    private final RetryConfig retryConfig;
    private final QueueService queueService;
    private final ExecutorService executorService;
    
    public RetryExecutor(RetryConfig retryConfig, QueueService queueService) {
        this.retryConfig = retryConfig;
        this.queueService = queueService;
        this.executorService = Executors.newCachedThreadPool();
    }
    
    /**
     * Execute with retry for synchronous operations
     */
    public <T> T executeWithRetry(Callable<T> task, Task taskInfo, BiConsumer<Task, Throwable> recoveryHandler) 
            throws Exception {
        int attempts = 0;
        Exception lastException = null;
        
        while (attempts <= retryConfig.getMaxRetries()) {
            try {
                if (attempts > 0) {
                    long delay = retryConfig.calculateDelayForAttempt(attempts);
                    LOGGER.info("Retrying task " + taskInfo.getId() + ", attempt " + attempts + 
                               " after " + delay + "ms");
                    Thread.sleep(delay);
                }
                
                return task.call();
            } catch (Exception e) {
                lastException = e;
                attempts++;
                taskInfo.incrementRetryCount();
                
                if (!retryConfig.isRetryable(e) || attempts > retryConfig.getMaxRetries()) {
                    LOGGER.log(Level.SEVERE, "Task " + taskInfo.getId() + " failed after " + 
                              attempts + " attempts", e);
                    break;
                }
                
                LOGGER.log(Level.WARNING, "Task " + taskInfo.getId() + " failed, will retry. Attempt " + 
                          attempts + " of " + retryConfig.getMaxRetries(), e);
            }
        }
        
        // All retries failed, invoke recovery handler
        if (recoveryHandler != null) {
            recoveryHandler.accept(taskInfo, lastException);
        }
        
        throw lastException;
    }
    
    /**
     * Execute with retry for asynchronous operations
     */
    public <T> CompletableFuture<T> executeWithRetryAsync(Callable<T> task, Task taskInfo, 
                                                         BiConsumer<Task, Throwable> recoveryHandler) {
        CompletableFuture<T> future = new CompletableFuture<>();
        
        executorService.submit(() -> {
            try {
                T result = executeWithRetry(task, taskInfo, recoveryHandler);
                future.complete(result);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
        
        return future;
    }
    
    /**
     * Default recovery handler that sends failed tasks to the queue
     */
    public BiConsumer<Task, Throwable> defaultRecoveryHandler() {
        return (task, throwable) -> {
            LOGGER.log(Level.INFO, "Sending failed task " + task.getId() + " to queue");
            queueService.sendTask(task);
        };
    }
    
    /**
     * Shutdown the executor service
     */
    public void shutdown() {
        executorService.shutdown();
    }
}

