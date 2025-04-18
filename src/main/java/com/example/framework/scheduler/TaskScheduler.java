package com.example.framework.scheduler;

import com.example.framework.model.Task;
import com.example.framework.processor.TaskProcessor;
import com.example.framework.queue.QueueService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TaskScheduler {
    private static final Logger LOGGER = Logger.getLogger(TaskScheduler.class.getName());
    
    private final QueueService queueService;
    private final TaskProcessor<?> taskProcessor;
    private final ScheduledExecutorService scheduler;
    private boolean isRunning = false;
    
    public TaskScheduler(QueueService queueService, TaskProcessor<?> taskProcessor) {
        this.queueService = queueService;
        this.taskProcessor = taskProcessor;
        this.scheduler = Executors.newScheduledThreadPool(1);
    }
    
    /**
     * Start the scheduler with a fixed delay
     * @param initialDelay Initial delay before first execution
     * @param period Period between successive executions
     * @param timeUnit Time unit for delay values
     */
    public void start(long initialDelay, long period, TimeUnit timeUnit) {
        if (isRunning) {
            return;
        }
        
        isRunning = true;
        scheduler.scheduleWithFixedDelay(this::processNextTask, initialDelay, period, timeUnit);
        LOGGER.info("Task scheduler started");
    }
    
    /**
     * Process the next task from the queue
     */
    private void processNextTask() {
        try {
            LOGGER.info("Checking queue for failed tasks");
            Task task = queueService.receiveTask();
            
            if (task == null) {
                LOGGER.info("No tasks found in queue");
                return;
            }
            
            LOGGER.info("Processing task from queue: " + task.getId());
            boolean success = false;
            
            try {
                if (task.getType() == Task.TaskType.SYNC) {
                    taskProcessor.processSyncTask(task);
                    success = true;
                } else {
                    taskProcessor.processAsyncTask(task).get(); // Wait for completion
                    success = true;
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed to process task from queue: " + task.getId(), e);
                success = taskProcessor.handleFailedTask(task, e);
            }
            
            if (success) {
                LOGGER.info("Task processed successfully, removing from queue: " + task.getId());
                queueService.deleteTask(task);
            } else {
                LOGGER.info("Task processing failed, keeping in queue: " + task.getId());
                // The task will remain in the queue due to visibility timeout
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error in task scheduler", e);
        }
    }
    
    /**
     * Stop the scheduler
     */
    public void stop() {
        if (!isRunning) {
            return;
        }
        
        isRunning = false;
        scheduler.shutdown();
        LOGGER.info("Task scheduler stopped");
    }
}

