package com.example.framework;

import com.amazonaws.services.sqs.AmazonSQS;
import com.example.framework.processor.TaskProcessor;
import com.example.framework.queue.AwsSqsQueueService;
import com.example.framework.queue.InMemoryQueueService;
import com.example.framework.queue.QueueService;
import com.example.framework.retry.RetryConfig;
import com.example.framework.scheduler.TaskScheduler;
import java.util.concurrent.TimeUnit;

public class RetryFrameworkBuilder {
    private RetryConfig retryConfig;
    private QueueService queueService;
    private TaskProcessor<?> taskProcessor;
    private long schedulerInitialDelay = 0;
    private long schedulerPeriod = 60;
    private TimeUnit schedulerTimeUnit = TimeUnit.SECONDS;
    
    public RetryFrameworkBuilder withRetryConfig(RetryConfig retryConfig) {
        this.retryConfig = retryConfig;
        return this;
    }
    
    public RetryFrameworkBuilder withAwsSqsQueueService(AmazonSQS sqsClient, String queueUrl) {
        this.queueService = new AwsSqsQueueService(sqsClient, queueUrl);
        return this;
    }
    
    public RetryFrameworkBuilder withInMemoryQueueService() {
        this.queueService = new InMemoryQueueService();
        return this;
    }
    
    public RetryFrameworkBuilder withCustomQueueService(QueueService queueService) {
        this.queueService = queueService;
        return this;
    }
    
    public RetryFrameworkBuilder withTaskProcessor(TaskProcessor<?> taskProcessor) {
        this.taskProcessor = taskProcessor;
        return this;
    }
    
    public RetryFrameworkBuilder withSchedulerConfig(long initialDelay, long period, TimeUnit timeUnit) {
        this.schedulerInitialDelay = initialDelay;
        this.schedulerPeriod = period;
        this.schedulerTimeUnit = timeUnit;
        return this;
    }
    
    public RetryFramework build() {
        if (retryConfig == null) {
            retryConfig = RetryConfig.defaultConfig();
        }
        
        if (queueService == null) {
            queueService = new InMemoryQueueService();
        }
        
        if (taskProcessor == null) {
            throw new IllegalStateException("TaskProcessor must be provided");
        }
        
        TaskScheduler scheduler = new TaskScheduler(queueService, taskProcessor);
        
        return new RetryFramework(taskProcessor, queueService, scheduler, 
                                 schedulerInitialDelay, schedulerPeriod, schedulerTimeUnit);
    }
}

