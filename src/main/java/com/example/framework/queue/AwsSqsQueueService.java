package com.example.framework.queue;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.example.framework.model.Task;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AwsSqsQueueService implements QueueService {
    private static final Logger LOGGER = Logger.getLogger(AwsSqsQueueService.class.getName());
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    
    private final AmazonSQS sqsClient;
    private final String queueUrl;
    private String lastReceiptHandle;
    
    public AwsSqsQueueService(AmazonSQS sqsClient, String queueUrl) {
        this.sqsClient = sqsClient;
        this.queueUrl = queueUrl;
    }
    
    @Override
    public boolean sendTask(Task task) {
        try {
            String taskJson = OBJECT_MAPPER.writeValueAsString(task);
            SendMessageRequest sendRequest = new SendMessageRequest()
                .withQueueUrl(queueUrl)
                .withMessageBody(taskJson);
            
            sqsClient.sendMessage(sendRequest);
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to send task to SQS: " + task.getId(), e);
            return false;
        }
    }
    
    @Override
    public Task receiveTask() {
        try {
            ReceiveMessageRequest receiveRequest = new ReceiveMessageRequest()
                .withQueueUrl(queueUrl)
                .withMaxNumberOfMessages(1)
                .withVisibilityTimeout(30); // 30 seconds visibility timeout
            
            List<Message> messages = sqsClient.receiveMessage(receiveRequest).getMessages();
            
            if (messages.isEmpty()) {
                return null;
            }
            
            Message message = messages.get(0);
            lastReceiptHandle = message.getReceiptHandle();
            
            return OBJECT_MAPPER.readValue(message.getBody(), Task.class);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to receive task from SQS", e);
            return null;
        }
    }
    
    @Override
    public boolean deleteTask(Task task) {
        if (lastReceiptHandle == null) {
            LOGGER.warning("No receipt handle available for task: " + task.getId());
            return false;
        }
        
        try {
            DeleteMessageRequest deleteRequest = new DeleteMessageRequest()
                .withQueueUrl(queueUrl)
                .withReceiptHandle(lastReceiptHandle);
            
            sqsClient.deleteMessage(deleteRequest);
            lastReceiptHandle = null;
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to delete task from SQS: " + task.getId(), e);
            return false;
        }
    }
}

