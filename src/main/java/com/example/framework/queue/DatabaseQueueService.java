package com.example.framework.queue;

import com.example.framework.model.Task;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sql.DataSource;

public class DatabaseQueueService implements QueueService {
    private static final Logger LOGGER = Logger.getLogger(DatabaseQueueService.class.getName());
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    
    private final DataSource dataSource;
    private String currentTaskId;
    
    public DatabaseQueueService(DataSource dataSource) {
        this.dataSource = dataSource;
        initializeTable();
    }
    
    private void initializeTable() {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "CREATE TABLE IF NOT EXISTS task_queue (" +
                 "id VARCHAR(36) PRIMARY KEY, " +
                 "payload TEXT, " +
                 "task_type VARCHAR(10), " +
                 "retry_count INT, " +
                 "last_retry_timestamp TIMESTAMP, " +
                 "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                 "processing BOOLEAN DEFAULT FALSE)"
             )) {
            stmt.executeUpdate();
            LOGGER.info("Task queue table initialized");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize task queue table", e);
        }
    }
    
    @Override
    public boolean sendTask(Task task) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "INSERT INTO task_queue (id, payload, task_type, retry_count, last_retry_timestamp) " +
                 "VALUES (?, ?, ?, ?, ?)"
             )) {
            stmt.setString(1, task.getId());
            stmt.setString(2, task.getPayload());
            stmt.setString(3, task.getType().name());
            stmt.setInt(4, task.getRetryCount());
            stmt.setTimestamp(5, task.getLastRetryTimestamp() > 0 ? 
                             new Timestamp(task.getLastRetryTimestamp()) : null);
            
            int rowsAffected = stmt.executeUpdate();
            LOGGER.info("Task sent to database queue: " + task.getId());
            return rowsAffected > 0;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to send task to database queue: " + task.getId(), e);
            return false;
        }
    }
    
    @Override
    public Task receiveTask() {
        try (Connection conn = dataSource.getConnection()) {
            // Begin transaction
            conn.setAutoCommit(false);
            
            // Get the oldest non-processing task
            try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT id, payload, task_type, retry_count, last_retry_timestamp " +
                "FROM task_queue " +
                "WHERE processing = FALSE " +
                "ORDER BY created_at ASC " +
                "LIMIT 1"
            )) {
                ResultSet rs = stmt.executeQuery();
                
                if (!rs.next()) {
                    conn.commit();
                    return null;
                }
                
                String id = rs.getString("id");
                String payload = rs.getString("payload");
                String taskType = rs.getString("task_type");
                int retryCount = rs.getInt("retry_count");
                Timestamp lastRetryTs = rs.getTimestamp("last_retry_timestamp");
                
                // Mark the task as processing
                try (PreparedStatement updateStmt = conn.prepareStatement(
                    "UPDATE task_queue SET processing = TRUE WHERE id = ?"
                )) {
                    updateStmt.setString(1, id);
                    updateStmt.executeUpdate();
                }
                
                // Commit transaction
                conn.commit();
                
                // Create and return the task
                Task task = new Task();
                task.setId(id);
                task.setPayload(payload);
                task.setType(Task.TaskType.valueOf(taskType));
                task.setRetryCount(retryCount);
                if (lastRetryTs != null) {
                    task.setLastRetryTimestamp(lastRetryTs.getTime());
                }
                
                currentTaskId = id;
                LOGGER.info("Task received from database queue: " + id);
                return task;
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to receive task from database queue", e);
            return null;
        }
    }
    
    @Override
    public boolean deleteTask(Task task) {
        if (currentTaskId == null || !currentTaskId.equals(task.getId())) {
            LOGGER.warning("No current task or ID mismatch for task: " + task.getId());
            return false;
        }
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "DELETE FROM task_queue WHERE id = ?"
             )) {
            stmt.setString(1, task.getId());
            int rowsAffected = stmt.executeUpdate();
            
            if (rowsAffected > 0) {
                LOGGER.info("Task deleted from database queue: " + task.getId());
                currentTaskId = null;
                return true;
            } else {
                LOGGER.warning("Task not found in database queue: " + task.getId());
                return false;
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to delete task from database queue: " + task.getId(), e);
            return false;
        }
    }
}

