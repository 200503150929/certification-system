package com.certification.backend.service;

public interface LoginAttemptService {

    /**
     * 检查 IP 是否被锁定
     */
    boolean isLocked(String ipAddress);

    /**
     * 获取剩余锁定时间（分钟）
     */
    long getRemainingLockMinutes(String ipAddress);

    /**
     * 获取当前失败次数
     */
    int getAttemptCount(String ipAddress);

    /**
     * 记录登录失败
     */
    void recordFailedAttempt(String ipAddress);

    /**
     * 登录成功，清除失败记录
     */
    void resetAttempts(String ipAddress);
}