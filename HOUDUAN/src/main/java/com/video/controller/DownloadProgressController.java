package com.video.controller;

import com.video.common.Result;
import com.video.service.DownloadProgressService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 下载进度控制器
 * 提供下载进度查询和管理接口
 *
 * @author video-parsing
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/download")
public class DownloadProgressController {

    @Autowired
    private DownloadProgressService downloadProgressService;

    /**
     * 创建下载任务
     *
     * @param videoUrl 视频URL
     * @param totalSize 总大小（可选）
     * @return 任务ID
     */
    @PostMapping("/create-task")
    public Result<String> createDownloadTask(@RequestParam("url") String videoUrl,
                                           @RequestParam(value = "totalSize", required = false, defaultValue = "0") long totalSize) {
        try {
            String taskId = UUID.randomUUID().toString();
            downloadProgressService.createDownloadTask(taskId, videoUrl, totalSize);
            return Result.success("任务创建成功", taskId);
        } catch (Exception e) {
            log.error("创建下载任务失败: {}", e.getMessage(), e);
            return Result.error("创建下载任务失败: " + e.getMessage());
        }
    }

    /**
     * 查询下载进度
     *
     * @param taskId 任务ID
     * @return 下载进度信息
     */
    @GetMapping("/progress/{taskId}")
    public Result<Map<String, Object>> getDownloadProgress(@PathVariable String taskId) {
        try {
            DownloadProgressService.DownloadProgress progress = downloadProgressService.getProgress(taskId);
            if (progress == null) {
                return Result.error("任务不存在");
            }

            Map<String, Object> result = new HashMap<>();
            result.put("taskId", progress.getTaskId());
            result.put("videoUrl", progress.getVideoUrl());
            result.put("totalSize", progress.getTotalSize());
            result.put("downloadedSize", progress.getDownloadedSize().get());
            result.put("percentage", progress.getPercentage());
            result.put("status", progress.getStatus());
            result.put("statusDescription", progress.getStatus().getDescription());
            result.put("speed", progress.getSpeed());
            result.put("formattedSpeed", progress.getFormattedSpeed());
            result.put("estimatedTime", progress.getEstimatedTime());
            result.put("formattedEstimatedTime", progress.getFormattedEstimatedTime());
            result.put("startTime", progress.getStartTime());
            result.put("endTime", progress.getEndTime());
            result.put("errorMessage", progress.getErrorMessage());

            return Result.success("获取成功", result);
        } catch (Exception e) {
            log.error("获取下载进度失败: {}", e.getMessage(), e);
            return Result.error("获取下载进度失败: " + e.getMessage());
        }
    }

    /**
     * 取消下载任务
     *
     * @param taskId 任务ID
     * @return 操作结果
     */
    @DeleteMapping("/cancel/{taskId}")
    public Result<String> cancelDownload(@PathVariable String taskId) {
        try {
            downloadProgressService.cancelDownload(taskId);
            return Result.success("取消成功", taskId);
        } catch (Exception e) {
            log.error("取消下载失败: {}", e.getMessage(), e);
            return Result.error("取消下载失败: " + e.getMessage());
        }
    }

    /**
     * 移除下载任务
     *
     * @param taskId 任务ID
     * @return 操作结果
     */
    @DeleteMapping("/remove/{taskId}")
    public Result<String> removeDownloadTask(@PathVariable String taskId) {
        try {
            downloadProgressService.removeTask(taskId);
            return Result.success("移除成功", taskId);
        } catch (Exception e) {
            log.error("移除下载任务失败: {}", e.getMessage(), e);
            return Result.error("移除下载任务失败: " + e.getMessage());
        }
    }

    /**
     * 清理过期任务
     *
     * @return 操作结果
     */
    @PostMapping("/cleanup")
    public Result<String> cleanupExpiredTasks() {
        try {
            downloadProgressService.cleanupExpiredTasks();
            return Result.success("清理完成", null);
        } catch (Exception e) {
            log.error("清理过期任务失败: {}", e.getMessage(), e);
            return Result.error("清理过期任务失败: " + e.getMessage());
        }
    }

    /**
     * 批量查询下载进度（用于前端轮询多个任务）
     *
     * @param taskIds 任务ID列表（逗号分隔）
     * @return 下载进度信息列表
     */
    @GetMapping("/progress/batch")
    public Result<Map<String, Object>> getBatchDownloadProgress(@RequestParam("taskIds") String taskIds) {
        try {
            String[] taskIdArray = taskIds.split(",");
            Map<String, Object> results = new HashMap<>();
            
            for (String taskId : taskIdArray) {
                taskId = taskId.trim();
                DownloadProgressService.DownloadProgress progress = downloadProgressService.getProgress(taskId);
                if (progress != null) {
                    Map<String, Object> progressInfo = new HashMap<>();
                    progressInfo.put("percentage", progress.getPercentage());
                    progressInfo.put("status", progress.getStatus());
                    progressInfo.put("formattedSpeed", progress.getFormattedSpeed());
                    progressInfo.put("formattedEstimatedTime", progress.getFormattedEstimatedTime());
                    progressInfo.put("errorMessage", progress.getErrorMessage());
                    results.put(taskId, progressInfo);
                }
            }
            
            return Result.success("获取成功", results);
        } catch (Exception e) {
            log.error("批量获取下载进度失败: {}", e.getMessage(), e);
            return Result.error("批量获取下载进度失败: " + e.getMessage());
        }
    }
}