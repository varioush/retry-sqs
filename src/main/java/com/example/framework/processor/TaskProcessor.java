package com.example.framework.processor;

import com.example.framework.model.Task;
import java.util.concurrent.CompletableFuture;

public interface TaskProcessor<T> {
    /**
     * Process a task synchronously
     * @param task The task to process
     * @return The result of processing
     * @throws Exception If processing fails
     */
    T processSyncTask(Task task) throws Exception;
    
    /**
     * Process a task asynchronously
     * @param task The task to process
     * @return A CompletableFuture with the result
     */
    CompletableFuture<T> processAsyncTask(Task task);
    
    /**
     * Handle a failed task
     * @param task The failed task
     * @param exception The exception that caused the failure
     * @return True if recovery was successful, false otherwise
     */
    boolean handleFailedTask(Task task, Throwable exception);
}


