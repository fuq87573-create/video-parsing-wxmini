package com.video.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 视频URL预处理服务
 * 用于处理和验证视频URL，确保能够正常访问
 *
 * @author video-parsing
 * @since 1.0.0
 */
@Slf4j
@Service
public class VideoUrlPreprocessor {

    @Autowired
    private RestTemplate restTemplate;

    // 需要特殊处理的域名模式
    private static final Map<Pattern, String> DOMAIN_REFERER_MAP = new HashMap<>();
    
    static {
        // 抖音相关域名
        DOMAIN_REFERER_MAP.put(Pattern.compile(".*douyin.*"), "https://www.douyin.com/");
        DOMAIN_REFERER_MAP.put(Pattern.compile(".*iesdouyin.*"), "https://www.douyin.com/");
        
        // 快手相关域名
        DOMAIN_REFERER_MAP.put(Pattern.compile(".*kuaishou.*"), "https://www.kuaishou.com/");
        
        // 小红书相关域名
        DOMAIN_REFERER_MAP.put(Pattern.compile(".*xiaohongshu.*"), "https://www.xiaohongshu.com/");
        DOMAIN_REFERER_MAP.put(Pattern.compile(".*xhslink.*"), "https://www.xiaohongshu.com/");
        
        // 微视相关域名
        DOMAIN_REFERER_MAP.put(Pattern.compile(".*weishi.*"), "https://weishi.qq.com/");
        
        // 西瓜视频相关域名
        DOMAIN_REFERER_MAP.put(Pattern.compile(".*ixigua.*"), "https://www.ixigua.com/");
        
        // 皮皮虾相关域名
        DOMAIN_REFERER_MAP.put(Pattern.compile(".*pipix.*"), "https://www.pipix.com/");
        
        // 微博相关域名
        DOMAIN_REFERER_MAP.put(Pattern.compile(".*weibo.*"), "https://weibo.com/");
    }

    /**
     * 预处理视频URL
     *
     * @param originalUrl 原始视频URL
     * @return 预处理结果
     */
    public VideoUrlInfo preprocessVideoUrl(String originalUrl) {
        if (!StringUtils.hasText(originalUrl)) {
            return VideoUrlInfo.error("视频URL为空");
        }

        try {
            log.info("开始预处理视频URL：{}", originalUrl);
            
            // 1. URL格式验证
            URL url = new URL(originalUrl);
            String host = url.getHost().toLowerCase();
            
            // 2. 检测是否需要特殊处理
            String referer = getRefererForDomain(host);
            
            // 3. 验证URL可访问性
            boolean accessible = checkUrlAccessibility(originalUrl, referer);
            
            // 4. 构建预处理结果
            VideoUrlInfo info = new VideoUrlInfo();
            info.setOriginalUrl(originalUrl);
            info.setProcessedUrl(originalUrl); // 目前不修改URL，通过代理接口处理
            info.setReferer(referer);
            info.setAccessible(accessible);
            info.setHost(host);
            info.setNeedsProxy(true); // 所有视频都通过代理下载
            info.setSuccess(true);
            
            log.info("视频URL预处理完成 - Host: {}, Referer: {}, Accessible: {}", 
                    host, referer, accessible);
            
            return info;
            
        } catch (Exception e) {
            log.error("预处理视频URL失败：{}", e.getMessage(), e);
            return VideoUrlInfo.error("URL预处理失败：" + e.getMessage());
        }
    }

    /**
     * 检查URL可访问性
     *
     * @param videoUrl 视频URL
     * @param referer Referer头
     * @return 是否可访问
     */
    private boolean checkUrlAccessibility(String videoUrl, String referer) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.add("User-Agent", 
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
            
            if (StringUtils.hasText(referer)) {
                headers.add("Referer", referer);
            }
            
            headers.add("Accept", "video/webm,video/ogg,video/*;q=0.9,*/*;q=0.5");
            headers.add("Range", "bytes=0-1023"); // 只请求前1KB数据进行测试
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<byte[]> response = restTemplate.exchange(
                videoUrl, 
                HttpMethod.GET, 
                entity, 
                byte[].class
            );
            
            boolean accessible = response.getStatusCode().is2xxSuccessful() || 
                               response.getStatusCode().value() == 206; // 206 Partial Content
            
            log.debug("URL可访问性检查 - URL: {}, Status: {}, Accessible: {}", 
                    videoUrl, response.getStatusCode(), accessible);
            
            return accessible;
            
        } catch (Exception e) {
            log.warn("URL可访问性检查失败：{}", e.getMessage());
            return false; // 检查失败时假设不可访问，但仍然尝试通过代理下载
        }
    }

    /**
     * 根据域名获取合适的Referer
     *
     * @param host 主机名
     * @return Referer值
     */
    private String getRefererForDomain(String host) {
        for (Map.Entry<Pattern, String> entry : DOMAIN_REFERER_MAP.entrySet()) {
            if (entry.getKey().matcher(host).matches()) {
                return entry.getValue();
            }
        }
        
        // 默认使用https协议的根域名
        try {
            return "https://" + host + "/";
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 视频URL信息类
     */
    public static class VideoUrlInfo {
        private String originalUrl;
        private String processedUrl;
        private String referer;
        private String host;
        private boolean accessible;
        private boolean needsProxy;
        private boolean success;
        private String errorMessage;

        public static VideoUrlInfo error(String message) {
            VideoUrlInfo info = new VideoUrlInfo();
            info.setSuccess(false);
            info.setErrorMessage(message);
            return info;
        }

        // Getters and Setters
        public String getOriginalUrl() {
            return originalUrl;
        }

        public void setOriginalUrl(String originalUrl) {
            this.originalUrl = originalUrl;
        }

        public String getProcessedUrl() {
            return processedUrl;
        }

        public void setProcessedUrl(String processedUrl) {
            this.processedUrl = processedUrl;
        }

        public String getReferer() {
            return referer;
        }

        public void setReferer(String referer) {
            this.referer = referer;
        }

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public boolean isAccessible() {
            return accessible;
        }

        public void setAccessible(boolean accessible) {
            this.accessible = accessible;
        }

        public boolean isNeedsProxy() {
            return needsProxy;
        }

        public void setNeedsProxy(boolean needsProxy) {
            this.needsProxy = needsProxy;
        }

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }
    }
}