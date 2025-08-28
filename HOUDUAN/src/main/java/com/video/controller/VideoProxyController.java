package com.video.controller;

import com.video.common.Result;
import com.video.service.VideoUrlPreprocessor;
import com.video.service.DownloadProgressService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * 视频代理下载控制器
 * 用于绕过防盗链限制，代理下载视频文件
 *
 * @author video-parsing
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/proxy")
public class VideoProxyController {

    @Autowired
    private RestTemplate restTemplate;
    
    @Autowired
    private VideoUrlPreprocessor videoUrlPreprocessor;
    
    @Autowired
    private DownloadProgressService downloadProgressService;
    
    // 线程池用于异步下载
    private final Executor downloadExecutor = Executors.newFixedThreadPool(10);
    
    // 优化后的缓冲区大小：64KB
    private static final int BUFFER_SIZE = 512 * 1024; // 增加到512KB缓冲区

    // 支持的视频文件扩展名
    private static final List<String> SUPPORTED_VIDEO_EXTENSIONS = Arrays.asList(
            ".mp4", ".avi", ".mov", ".wmv", ".flv", ".webm", ".m4v", ".3gp"
    );

    // 常用的视频平台域名，用于设置合适的Referer
    private static final String[] PLATFORM_DOMAINS = {
            "douyin.com", "tiktok.com", "kuaishou.com", "xiaohongshu.com",
            "weishi.qq.com", "ixigua.com", "pipix.com", "zuiyou.com", "weibo.com"
    };

    /**
     * 代理下载视频（支持Range请求）
     *
     * @param videoUrl 视频URL
     * @param request HTTP请求对象
     * @param response HTTP响应对象
     */
    @RequestMapping(value = "/download", method = {RequestMethod.GET, RequestMethod.POST})
    public void proxyDownload(@RequestParam("url") String videoUrl,
                             @RequestParam(value = "taskId", required = false) String taskId,
                             HttpServletRequest request,
                             HttpServletResponse response) {
        
        if (!StringUtils.hasText(videoUrl)) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        try {
            // URL解码
            String decodedUrl = URLDecoder.decode(videoUrl, StandardCharsets.UTF_8.name());
            log.info("开始代理下载视频：{}", decodedUrl);
            
            // 预处理URL，获取合适的Referer
            VideoUrlPreprocessor.VideoUrlInfo urlInfo = videoUrlPreprocessor.preprocessVideoUrl(decodedUrl);

            // 创建HTTP连接
            URL url = new URL(decodedUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            
            // 设置请求头以绕过防盗链
            setupRequestHeaders(connection, decodedUrl, urlInfo);
            
            // 处理Range请求（断点续传）
            String rangeHeader = request.getHeader("Range");
            long startByte = 0;
            long endByte = -1;
            
            if (StringUtils.hasText(rangeHeader)) {
                String[] ranges = parseRangeHeader(rangeHeader);
                if (ranges != null && ranges.length >= 2) {
                    startByte = Long.parseLong(ranges[0]);
                    if (!ranges[1].isEmpty()) {
                        endByte = Long.parseLong(ranges[1]);
                    }
                }
                connection.setRequestProperty("Range", rangeHeader);
                log.info("Range请求: {} (startByte: {}, endByte: {})", rangeHeader, startByte, endByte);
            }
            
            // 设置连接超时（优化性能）
            connection.setConnectTimeout(8000);  // 减少连接超时到8秒
            connection.setReadTimeout(60000);    // 减少读取超时到60秒
            
            // 启用HTTP Keep-Alive和其他性能优化
            if (connection instanceof HttpURLConnection) {
                HttpURLConnection httpConn = (HttpURLConnection) connection;
                httpConn.setInstanceFollowRedirects(true);
                httpConn.setUseCaches(false);
                // 设置网络性能优化参数
                System.setProperty("http.keepAlive", "true");
                System.setProperty("http.maxConnections", "20");
                System.setProperty("http.keepAliveTimeout", "30000");
                System.setProperty("sun.net.useExclusiveBind", "false");
                // 优化TCP缓冲区大小
                System.setProperty("java.net.preferIPv4Stack", "true");
                System.setProperty("networkaddress.cache.ttl", "60");
            }
            
            // 获取响应
            int responseCode = connection.getResponseCode();
            String contentType = connection.getContentType();
            long contentLength = connection.getContentLengthLong();
            
            log.info("视频信息 - ResponseCode: {}, ContentType: {}, ContentLength: {}", 
                    responseCode, contentType, contentLength);
            
            // 创建或获取下载进度跟踪
            DownloadProgressService.DownloadProgress progress = null;
            if (StringUtils.hasText(taskId)) {
                progress = downloadProgressService.getProgress(taskId);
                if (progress == null) {
                    progress = downloadProgressService.createDownloadTask(taskId, decodedUrl, contentLength);
                } else if (progress.getTotalSize() == 0 && contentLength > 0) {
                    // 更新总大小（如果之前没有设置）
                    progress.setTotalSize(contentLength);
                }
            }
            
            // 设置响应状态码和头
            if (responseCode == HttpURLConnection.HTTP_PARTIAL) {
                response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
                String contentRange = connection.getHeaderField("Content-Range");
                if (StringUtils.hasText(contentRange)) {
                    response.setHeader("Content-Range", contentRange);
                }
            } else {
                response.setStatus(HttpServletResponse.SC_OK);
            }
            
            setupResponseHeaders(response, contentType, contentLength, decodedUrl, rangeHeader != null);
            
            // 优化的流式传输
            try (InputStream inputStream = connection.getInputStream();
                 OutputStream outputStream = response.getOutputStream()) {
                
                byte[] buffer = new byte[BUFFER_SIZE]; // 使用512KB缓冲区
                int bytesRead;
                long totalBytes = 0;
                long startTime = System.currentTimeMillis();
                long lastLogTime = startTime;
                long lastProgressUpdate = startTime;
                long lastLogBytes = 0;
                
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    // 检查是否已取消下载
                    if (StringUtils.hasText(taskId) && downloadProgressService.isCancelled(taskId)) {
                        log.info("下载已被用户取消: {}", taskId);
                        response.setStatus(HttpServletResponse.SC_REQUEST_TIMEOUT);
                        return;
                    }
                    
                    outputStream.write(buffer, 0, bytesRead);
                    totalBytes += bytesRead;
                    
                    long currentTime = System.currentTimeMillis();
                    
                    // 每1秒更新一次进度（进一步减少频率提高性能）
                    if (progress != null && (currentTime - lastProgressUpdate > 1000)) {
                        downloadProgressService.updateProgress(taskId, totalBytes);
                        lastProgressUpdate = currentTime;
                    }
                    
                    // 每3秒记录一次日志
                    if (currentTime - lastLogTime > 3000) {
                        double totalMB = totalBytes / (1024.0 * 1024.0);
                        double intervalSeconds = (currentTime - lastLogTime) / 1000.0;
                        double intervalMB = (totalBytes - lastLogBytes) / (1024.0 * 1024.0);
                        double currentSpeed = intervalMB / intervalSeconds;
                        double avgSpeed = totalMB / ((currentTime - startTime) / 1000.0);
                        
                        log.debug("已传输 {} MB, 当前速度: {} MB/s, 平均速度: {} MB/s", 
                                String.format("%.2f", totalMB),
                                String.format("%.2f", currentSpeed),
                                String.format("%.2f", avgSpeed));
                        
                        lastLogTime = currentTime;
                        lastLogBytes = totalBytes;
                    }
                    
                    // 每4MB刷新一次输出流（进一步减少频率）
                    if (totalBytes % (4 * 1024 * 1024) == 0) {
                        outputStream.flush();
                    }
                }
                
                outputStream.flush();
                log.info("视频下载完成，总大小：{} bytes", totalBytes);
                
                // 最后更新一次进度确保100%
                if (progress != null) {
                    downloadProgressService.updateProgress(taskId, totalBytes);
                    downloadProgressService.markCompleted(taskId);
                }
                
            }
            
        } catch (Exception e) {
            log.error("代理下载视频失败：{}", e.getMessage(), e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            
            // 标记下载失败
            if (StringUtils.hasText(taskId)) {
                downloadProgressService.markFailed(taskId, e.getMessage());
            }
        }
    }

    /**
     * 获取视频信息（不下载，仅获取元数据）
     *
     * @param videoUrl 视频URL
     * @return 视频信息
     */
    @GetMapping("/info")
    public Result<Object> getVideoInfo(@RequestParam("url") String videoUrl) {
        
        if (!StringUtils.hasText(videoUrl)) {
            return Result.error("视频URL不能为空");
        }

        try {
            // 预处理URL，获取合适的Referer
            VideoUrlPreprocessor.VideoUrlInfo urlInfo = videoUrlPreprocessor.preprocessVideoUrl(videoUrl);
            
            URL url = new URL(videoUrl);
            URLConnection connection = url.openConnection();
            
            // 设置请求头
            setupRequestHeaders(connection, videoUrl, urlInfo);
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(10000);
            
            // 仅获取头信息，不下载内容
            connection.connect();
            
            String contentType = connection.getContentType();
            int contentLength = connection.getContentLength();
            
            // 构建返回信息
            java.util.Map<String, Object> info = new java.util.HashMap<>();
            info.put("contentType", contentType);
            info.put("contentLength", contentLength);
            info.put("isVideo", isVideoContent(contentType, videoUrl));
            info.put("fileSizeMB", contentLength > 0 ? String.format("%.2f", contentLength / (1024.0 * 1024.0)) : "未知");
            
            return Result.success("获取成功", info);
            
        } catch (Exception e) {
            log.error("获取视频信息失败：{}", e.getMessage(), e);
            return Result.error("获取视频信息失败：" + e.getMessage());
        }
    }

    /**
     * 设置请求头以绕过防盗链
     *
     * @param connection URL连接
     * @param videoUrl 视频URL
     * @param urlInfo URL预处理信息
     */
    private void setupRequestHeaders(URLConnection connection, String videoUrl, VideoUrlPreprocessor.VideoUrlInfo urlInfo) {
        // 设置User-Agent
        connection.setRequestProperty("User-Agent", 
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
        
        // 根据URL预处理结果设置合适的Referer
        String referer = null;
        if (urlInfo.isSuccess() && StringUtils.hasText(urlInfo.getReferer())) {
            referer = urlInfo.getReferer();
            log.info("使用预处理的Referer：{}", referer);
        } else {
            referer = getAppropriateReferer(videoUrl);
            log.info("使用默认Referer：{}", referer);
        }
        
        if (referer != null) {
            connection.setRequestProperty("Referer", referer);
        }
        
        // 设置其他常用请求头
        connection.setRequestProperty("Accept", "video/webm,video/ogg,video/*;q=0.9,application/ogg;q=0.7,audio/*;q=0.6,*/*;q=0.5");
        connection.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
        connection.setRequestProperty("Accept-Encoding", "identity"); // 不使用压缩，便于流式传输
        connection.setRequestProperty("Connection", "keep-alive");
        connection.setRequestProperty("Cache-Control", "no-cache");
        connection.setRequestProperty("Pragma", "no-cache");
        connection.setRequestProperty("Sec-Fetch-Dest", "video");
        connection.setRequestProperty("Sec-Fetch-Mode", "no-cors");
        connection.setRequestProperty("Sec-Fetch-Site", "cross-site");
    }

    /**
     * 设置响应头
     *
     * @param response HTTP响应
     * @param contentType 内容类型
     * @param contentLength 内容长度
     * @param videoUrl 视频URL
     * @param isRangeRequest 是否为Range请求
     */
    private void setupResponseHeaders(HttpServletResponse response, String contentType, 
                                    long contentLength, String videoUrl, boolean isRangeRequest) {
        
        // 设置内容类型
        if (StringUtils.hasText(contentType)) {
            response.setContentType(contentType);
        } else {
            response.setContentType("video/mp4"); // 默认视频类型
        }
        
        // 设置内容长度
        if (contentLength > 0) {
            response.setContentLengthLong(contentLength);
        }
        
        // 支持Range请求
        if (!isRangeRequest) {
            response.setHeader("Accept-Ranges", "bytes");
        }
        
        // 设置下载文件名
        String fileName = extractFileName(videoUrl);
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
        
        // 优化缓存控制（允许适当缓存以提高性能）
        response.setHeader("Cache-Control", "public, max-age=3600");
        response.setHeader("ETag", "\"" + videoUrl.hashCode() + "\"");
        
        // 支持跨域
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "*");
    }

    /**
     * 根据视频URL获取合适的Referer
     *
     * @param videoUrl 视频URL
     * @return Referer值
     */
    private String getAppropriateReferer(String videoUrl) {
        try {
            URL url = new URL(videoUrl);
            String host = url.getHost().toLowerCase();
            
            // 根据不同平台设置不同的Referer
            for (String domain : PLATFORM_DOMAINS) {
                if (host.contains(domain)) {
                    return "https://" + domain + "/";
                }
            }
            
            // 默认使用视频URL的域名作为Referer
            return url.getProtocol() + "://" + url.getHost() + "/";
            
        } catch (Exception e) {
            log.warn("解析视频URL失败：{}", e.getMessage());
            return null;
        }
    }

    /**
     * 从URL中提取文件名
     *
     * @param videoUrl 视频URL
     * @return 文件名
     */
    private String extractFileName(String videoUrl) {
        try {
            URL url = new URL(videoUrl);
            String path = url.getPath();
            
            if (StringUtils.hasText(path) && path.contains("/")) {
                String fileName = path.substring(path.lastIndexOf("/") + 1);
                if (StringUtils.hasText(fileName) && isVideoContent(null, fileName)) {
                    return fileName;
                }
            }
            
            // 默认文件名
            return "video_" + System.currentTimeMillis() + ".mp4";
            
        } catch (Exception e) {
            return "video_" + System.currentTimeMillis() + ".mp4";
        }
    }

    /**
     * 判断是否为视频内容
     *
     * @param contentType 内容类型
     * @param url URL或文件名
     * @return 是否为视频
     */
    private boolean isVideoContent(String contentType, String url) {
        // 检查Content-Type
        if (StringUtils.hasText(contentType) && contentType.toLowerCase().startsWith("video/")) {
            return true;
        }
        
        // 检查文件扩展名
        if (StringUtils.hasText(url)) {
            String lowerUrl = url.toLowerCase();
            return SUPPORTED_VIDEO_EXTENSIONS.stream().anyMatch(lowerUrl::contains);
        }
        
        return false;
    }
    
    /**
     * 解析Range请求头
     *
     * @param rangeHeader Range请求头值
     * @return [startByte, endByte] 数组，如果解析失败返回null
     */
    private String[] parseRangeHeader(String rangeHeader) {
        try {
            if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
                String range = rangeHeader.substring(6); // 去掉"bytes="
                String[] parts = range.split("-");
                if (parts.length >= 1) {
                    String startStr = parts[0].trim();
                    String endStr = parts.length > 1 ? parts[1].trim() : "";
                    return new String[]{startStr, endStr};
                }
            }
        } catch (Exception e) {
            log.warn("解析Range请求头失败: {}", rangeHeader, e);
        }
        return null;
    }
    
    /**
     * 异步下载视频（用于大文件预加载）
     *
     * @param videoUrl 视频URL
     * @return CompletableFuture
     */
    @GetMapping("/async-download")
    public CompletableFuture<Result<String>> asyncDownload(@RequestParam("url") String videoUrl) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 这里可以实现异步下载逻辑，比如预热缓存
                log.info("开始异步预加载视频: {}", videoUrl);
                return Result.success("异步下载已启动", videoUrl);
            } catch (Exception e) {
                log.error("异步下载失败: {}", e.getMessage(), e);
                return Result.error("异步下载失败: " + e.getMessage());
            }
        }, downloadExecutor);
    }
}