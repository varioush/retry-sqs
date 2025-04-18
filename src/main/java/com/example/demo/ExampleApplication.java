package com.example.demo;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.example.framework.RetryFramework;
import com.example.framework.RetryFrameworkBuilder;
import com.example.framework.retry.RetryConfig;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ExampleApplication {
    private static final Logger LOGGER = Logger.getLogger(ExampleApplication.class.getName());
    
    public static void main(String[] args) {
        // Example with AWS SQS
        if (useAwsSqs()) {
            runWithAwsSqs();
        } else {
            // Example with in-memory queue (for testing)
            runWithInMemoryQueue();
        }
    }
    
    private static boolean useAwsSqs() {
        // Check if AWS credentials are available
        String accessKey = System.getenv("AWS_ACCESS_KEY");
        String secretKey = System.getenv("AWS_SECRET_KEY");
        return accessKey != null && !accessKey.isEmpty() && secretKey != null && !secretKey.isEmpty();
    }
    
    private static void runWithAwsSqs() {
        String accessKey = System.getenv("AWS_ACCESS_KEY");
        String secretKey = System.getenv("AWS_SECRET_KEY");
        String region = System.getenv("AWS_REGION") != null ? System.getenv("AWS_REGION") : "us-east-1";
        String queueUrl = System.getenv("AWS_SQS_QUEUE_URL");
        
        if (queueUrl == null || queueUrl.isEmpty()) {
            LOGGER.severe("AWS_SQS_QUEUE_URL environment variable is required");
            return;
        }
        
        // Create custom retry config
        RetryConfig retryConfig = new RetryConfig(
            3,                  // maxRetries
            1000,               // initialDelayMs
            2.0,                // backoffMultiplier
            Exception.class     // retryable exceptions
        );
        
        // Create AWS SQS client
        var sqsClient = AmazonSQSClientBuilder.standard()
            .withRegion(Regions.fromName(region))
            .withCredentials(new AWSStaticCredentialsProvider(
                new BasicAWSCredentials(accessKey, secretKey)))
            .build();
        
        // Build the framework
        RetryFramework framework = new RetryFrameworkBuilder()
            .withRetryConfig(retryConfig)
            .withAwsSqsQueueService(sqsClient, queueUrl)
            .withTaskProcessor(new ExampleTaskProcessor(retryConfig, null)) // Queue service will be injected by builder
            .withSchedulerConfig(0, 30, TimeUnit.SECONDS)
            .build();
        
        runExampleTasks(framework);
    }
    
    private static void runWithInMemoryQueue() {
        // Create custom retry config
        RetryConfig retryConfig = new RetryConfig(
            3,                  // maxRetries
            1000,               // initialDelayMs
            2.0,                // backoffMultiplier
            Exception.class     // retryable exceptions
        );
        
        // Build the framework with in-memory queue
        RetryFramework framework = new RetryFrameworkBuilder()
            .withRetryConfig(retryConfig)
            .withInMemoryQueueService()
            .withTaskProcessor(new ExampleTaskProcessor(retryConfig, null)) // Queue service will be injected by builder
            .withSchedulerConfig(0, 10, TimeUnit.SECONDS) // Check queue more frequently for demo
            .build();
        
        runExampleTasks(framework);
    }
    
    private static void runExampleTasks(RetryFramework framework) {
        // Start the framework scheduler
        framework.start();
        
        try {
            // Submit some sync tasks
            for (int i = 0; i < 5; i++) {
                try {
                    String result = framework.submitSyncTask("Sync task " + i);
                    LOGGER.info("Sync task result: " + result);
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Sync task failed after all retries", e);
                }
            }
            
            // Submit some async tasks
            for (int i = 0; i < 5; i++) {
                CompletableFuture<String> future = framework.submitAsyncTask("Async task " + i);
                future.whenComplete((result, ex) -> {
                    if (ex != null) {
                        LOGGER.log(Level.SEVERE, "Async task failed after all retries", ex);
                    } else {
                        LOGGER.info("Async task result: " + result);
                    }
                });
            }
            
            // Keep the application running to allow the scheduler to process failed tasks
            LOGGER.info("Tasks submitted. Waiting for scheduler to process any failed tasks...");
            Thread.sleep(120000); // Wait for 2 minutes
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error in example application", e);
        } finally {
            // Stop the framework
            framework.stop();
            LOGGER.info("Framework stopped");
        }
    }
}

