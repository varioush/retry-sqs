package com.example.framework.queue;

import com.example.framework.model.Task;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;

public class InMemoryQueueService implements QueueService {
    private static final Logger LOGGER = Logger.getLogger(InMemoryQueueService.class.getName());
    
    private final ConcurrentLinkedQueue<Task> queue = new ConcurrentLinkedQueue<>();
    private Task lastReceivedTask;
    
    @Override
    public boolean sendTask(Task task) {
        LOGGER.info("Sending task to in-memory queue: " + task.getId());
        return queue.offer(task);
    }
    
    @Override
    public Task receiveTask() {
        lastReceivedTask = queue.peek();
        LOGGER.info("Received task from in-memory queue: " + 
                   (lastReceivedTask != null ? lastReceivedTask.getId() : "none"));
        return lastReceivedTask;
    }
    
    @Override
    public boolean deleteTask(Task task) {
        if (lastReceivedTask != null && lastReceivedTask.getId().equals(task.getId())) {
            queue.poll(); // Remove the first element
            LOGGER.info("Deleted task from in-memory queue: " + task.getId());
            lastReceivedTask = null;
            return true;
        }
        LOGGER.warning("Task not found or not the first in queue: " + task.getId());
        return false;
    }
    
    public int getQueueSize() {
        return queue.size();
    }
}

