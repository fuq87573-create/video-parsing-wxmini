package com.video.service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 下载进度跟踪服务
 * 用于跟踪视频下载进度和状态
 *
 * @author video-parsing
 * @since 1.0.0
 */
@Slf4j
@Service
public class DownloadProgressService {

    // 存储下载进度信息
    private final ConcurrentHashMap<String, DownloadProgress> progressMap = new ConcurrentHashMap<>();

    /**
     * 创建下载任务
     *
     * @param taskId 任务ID
     * @param videoUrl 视频URL
     * @param totalSize 总大小
     * @return 下载进度对象
     */
    public DownloadProgress createDownloadTask(String taskId, String videoUrl, long totalSize) {
        DownloadProgress progress = new DownloadProgress();
        progress.setTaskId(taskId);
        progress.setVideoUrl(videoUrl);
        progress.setTotalSize(totalSize);
        progress.setDownloadedSize(new AtomicLong(0));
        progress.setStatus(DownloadStatus.PREPARING);
        progress.setStartTime(System.currentTimeMillis());
        progress.setSpeed(0.0);
        progress.setEstimatedTime(0L);
        
        progressMap.put(taskId, progress);
        log.info("创建下载任务: {}, URL: {}, 总大小: {} bytes", taskId, videoUrl, totalSize);
        return progress;
    }

    /**
     * 更新下载进度
     *
     * @param taskId 任务ID
     * @param downloadedBytes 已下载字节数
     */
    public void updateProgress(String taskId, long downloadedBytes) {
        DownloadProgress progress = progressMap.get(taskId);
        if (progress != null) {
            long currentTime = System.currentTimeMillis();
            long previousDownloaded = progress.getDownloadedSize().get();
            progress.getDownloadedSize().set(downloadedBytes);
            
            // 计算下载速度
            long timeDiff = currentTime - progress.getLastUpdateTime();
            if (timeDiff > 1000) { // 每秒更新一次速度
                long bytesDiff = downloadedBytes - previousDownloaded;
                double speed = (bytesDiff * 1000.0) / timeDiff; // bytes/second
                progress.setSpeed(speed);
                progress.setLastUpdateTime(currentTime);
                
                // 计算预估剩余时间
                if (speed > 0 && progress.getTotalSize() > 0) {
                    long remainingBytes = progress.getTotalSize() - downloadedBytes;
                    long estimatedTime = (long) (remainingBytes / speed * 1000); // milliseconds
                    progress.setEstimatedTime(estimatedTime);
                }
            }
            
            // 更新状态
            if (progress.getStatus() == DownloadStatus.PREPARING) {
                progress.setStatus(DownloadStatus.DOWNLOADING);
            }
        }
    }

    /**
     * 标记下载完成
     *
     * @param taskId 任务ID
     */
    public void markCompleted(String taskId) {
        DownloadProgress progress = progressMap.get(taskId);
        if (progress != null) {
            progress.setStatus(DownloadStatus.COMPLETED);
            progress.setEndTime(System.currentTimeMillis());
            log.info("下载任务完成: {}, 耗时: {} ms", taskId, 
                    progress.getEndTime() - progress.getStartTime());
        }
    }

    /**
     * 标记下载失败
     *
     * @param taskId 任务ID
     * @param errorMessage 错误信息
     */
    public void markFailed(String taskId, String errorMessage) {
        DownloadProgress progress = progressMap.get(taskId);
        if (progress != null) {
            progress.setStatus(DownloadStatus.FAILED);
            progress.setErrorMessage(errorMessage);
            progress.setEndTime(System.currentTimeMillis());
            log.error("下载任务失败: {}, 错误: {}", taskId, errorMessage);
        }
    }

    /**
     * 取消下载任务
     *
     * @param taskId 任务ID
     */
    public void cancelDownload(String taskId) {
        DownloadProgress progress = progressMap.get(taskId);
        if (progress != null) {
            progress.setStatus(DownloadStatus.CANCELLED);
            progress.setEndTime(System.currentTimeMillis());
            log.info("下载任务已取消: {}", taskId);
        }
    }

    /**
     * 检查下载任务是否已取消
     *
     * @param taskId 任务ID
     * @return 是否已取消
     */
    public boolean isCancelled(String taskId) {
        DownloadProgress progress = progressMap.get(taskId);
        return progress != null && progress.getStatus() == DownloadStatus.CANCELLED;
    }

    /**
     * 获取下载进度
     *
     * @param taskId 任务ID
     * @return 下载进度对象
     */
    public DownloadProgress getProgress(String taskId) {
        return progressMap.get(taskId);
    }

    /**
     * 移除下载任务
     *
     * @param taskId 任务ID
     */
    public void removeTask(String taskId) {
        progressMap.remove(taskId);
        log.info("移除下载任务: {}", taskId);
    }

    /**
     * 清理过期任务（超过1小时的已完成或失败任务）
     */
    public void cleanupExpiredTasks() {
        long currentTime = System.currentTimeMillis();
        long expireTime = 60 * 60 * 1000; // 1小时
        
        progressMap.entrySet().removeIf(entry -> {
            DownloadProgress progress = entry.getValue();
            boolean isExpired = (progress.getStatus() == DownloadStatus.COMPLETED || 
                               progress.getStatus() == DownloadStatus.FAILED) &&
                               (currentTime - progress.getEndTime()) > expireTime;
            
            if (isExpired) {
                log.debug("清理过期任务: {}", entry.getKey());
            }
            return isExpired;
        });
    }

    /**
     * 下载进度信息
     */
    @Data
    public static class DownloadProgress {
        private String taskId;
        private String videoUrl;
        private long totalSize;
        private AtomicLong downloadedSize;
        private DownloadStatus status;
        private double speed; // bytes/second
        private long estimatedTime; // milliseconds
        private long startTime;
        private long endTime;
        private long lastUpdateTime;
        private String errorMessage;
        
        /**
         * 获取下载百分比
         */
        public double getPercentage() {
            if (totalSize <= 0) {
                return 0.0;
            }
            return (downloadedSize.get() * 100.0) / totalSize;
        }
        
        /**
         * 获取格式化的速度字符串
         */
        public String getFormattedSpeed() {
            if (speed < 1024) {
                return String.format("%.2f B/s", speed);
            } else if (speed < 1024 * 1024) {
                return String.format("%.2f KB/s", speed / 1024);
            } else {
                return String.format("%.2f MB/s", speed / (1024 * 1024));
            }
        }
        
        /**
         * 获取格式化的剩余时间字符串
         */
        public String getFormattedEstimatedTime() {
            if (estimatedTime <= 0) {
                return "未知";
            }
            
            long seconds = estimatedTime / 1000;
            if (seconds < 60) {
                return seconds + "秒";
            } else if (seconds < 3600) {
                return (seconds / 60) + "分" + (seconds % 60) + "秒";
            } else {
                long hours = seconds / 3600;
                long minutes = (seconds % 3600) / 60;
                return hours + "小时" + minutes + "分钟";
            }
        }
    }

    /**
     * 下载状态枚举
     */
    public enum DownloadStatus {
        PREPARING("准备中"),
        DOWNLOADING("下载中"),
        COMPLETED("已完成"),
        FAILED("失败"),
        CANCELLED("已取消");
        
        private final String description;
        
        DownloadStatus(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
}