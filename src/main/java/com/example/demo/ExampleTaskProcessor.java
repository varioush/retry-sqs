package com.example.demo;

import com.example.framework.model.Task;
import com.example.framework.processor.AbstractTaskProcessor;
import com.example.framework.queue.QueueService;
import com.example.framework.retry.RetryConfig;
import java.util.Random;
import java.util.logging.Logger;

public class ExampleTaskProcessor extends AbstractTaskProcessor<String> {
    private static final Logger LOGGER = Logger.getLogger(ExampleTaskProcessor.class.getName());
    private final Random random = new Random();
    
    public ExampleTaskProcessor(RetryConfig retryConfig, QueueService queueService) {
        super(retryConfig, queueService);
    }
    
    @Override
    protected String processTask(Task task) throws Exception {
        LOGGER.info("Processing task: " + task.getId() + " with payload: " + task.getPayload());
        
        // Simulate random failure (70% chance of failure)
        if (random.nextInt(10) < 7) {
            LOGGER.warning("Task failed: " + task.getId());
            throw new RuntimeException("Simulated failure for task: " + task.getId());
        }
        
        LOGGER.info("Task completed successfully: " + task.getId());
        return "Processed task: " + task.getId() + ", payload: " + task.getPayload();
    }
}

