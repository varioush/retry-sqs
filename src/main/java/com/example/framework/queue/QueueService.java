package com.example.framework.queue;

import com.example.framework.model.Task;

public interface QueueService {
    /**
     * Send a task to the queue
     * @param task The task to send
     * @return True if the task was sent successfully
     */
    boolean sendTask(Task task);
    
    /**
     * Receive a task from the queue
     * @return The received task, or null if no task is available
     */
    Task receiveTask();
    
    /**
     * Delete a task from the queue
     * @param task The task to delete
     * @return True if the task was deleted successfully
     */
    boolean deleteTask(Task task);
}


