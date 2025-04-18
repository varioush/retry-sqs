package com.example.framework.retry;

public class RetryConfig {
    private int maxRetries;
    private long initialDelayMs;
    private double backoffMultiplier;
    private Class<? extends Throwable>[] retryableExceptions;
    
    public RetryConfig(int maxRetries, long initialDelayMs, double backoffMultiplier, 
                      Class<? extends Throwable>... retryableExceptions) {
        this.maxRetries = maxRetries;
        this.initialDelayMs = initialDelayMs;
        this.backoffMultiplier = backoffMultiplier;
        this.retryableExceptions = retryableExceptions;
    }
    
    public static RetryConfig defaultConfig() {
        return new RetryConfig(3, 1000, 2.0, Exception.class);
    }
    
    // Getters
    public int getMaxRetries() {
        return maxRetries;
    }
    
    public long getInitialDelayMs() {
        return initialDelayMs;
    }
    
    public double getBackoffMultiplier() {
        return backoffMultiplier;
    }
    
    public Class<? extends Throwable>[] getRetryableExceptions() {
        return retryableExceptions;
    }
    
    public long calculateDelayForAttempt(int attempt) {
        return (long) (initialDelayMs * Math.pow(backoffMultiplier, attempt - 1));
    }
    
    public boolean isRetryable(Throwable t) {
        for (Class<? extends Throwable> exceptionClass : retryableExceptions) {
            if (exceptionClass.isAssignableFrom(t.getClass())) {
                return true;
            }
        }
        return false;
    }
}

