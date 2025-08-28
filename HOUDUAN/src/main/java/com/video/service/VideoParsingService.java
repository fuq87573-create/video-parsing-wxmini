package com.video.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.video.entity.User;
import com.video.entity.VideoParseRecord;
import com.video.repository.UserRepository;
import com.video.repository.VideoParseRecordRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Base64;

/**
 * 视频解析服务类
 *
 * @author video-parsing
 * @since 1.0.0
 */
@Slf4j
@Service
public class VideoParsingService {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private VideoParseRecordRepository videoParseRecordRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private VideoUrlPreprocessor videoUrlPreprocessor;

    @Value("${watermark.api.url}")
    private String watermarkApiUrl;

    @Value("${watermark.api.appid}")
    private String watermarkAppId;
    
    @Value("${proxy.domain}")
    private String proxyDomain;

    // 支持的平台正则表达式
    private static final Map<String, Pattern> PLATFORM_PATTERNS = new HashMap<>();
    
    static {
        PLATFORM_PATTERNS.put("抖音", Pattern.compile("(douyin|iesdouyin)"));
        PLATFORM_PATTERNS.put("快手", Pattern.compile("kuaishou"));
        PLATFORM_PATTERNS.put("小红书", Pattern.compile("xiaohongshu|xhslink"));
        PLATFORM_PATTERNS.put("微视", Pattern.compile("weishi"));
        PLATFORM_PATTERNS.put("火山", Pattern.compile("huoshan"));
        PLATFORM_PATTERNS.put("西瓜视频", Pattern.compile("ixigua"));
        PLATFORM_PATTERNS.put("皮皮虾", Pattern.compile("pipix"));
        PLATFORM_PATTERNS.put("最右", Pattern.compile("zuiyou"));
        PLATFORM_PATTERNS.put("微博", Pattern.compile("weibo"));
    }

    /**
     * 解析视频
     *
     * @param openId 用户openId
     * @param videoUrl 视频链接
     * @param request HTTP请求对象
     * @return 解析结果
     */
    @Transactional
    public Map<String, Object> parseVideo(String openId, String videoUrl, HttpServletRequest request) {
        log.info("开始解析视频，用户：{}，链接：{}", openId, videoUrl);
        
        long startTime = System.currentTimeMillis();
        VideoParseRecord record = new VideoParseRecord();
        record.setOpenId(openId);
        record.setOriginalUrl(videoUrl);
        record.setPlatform(detectPlatform(videoUrl));
        record.setUserIp(getClientIpAddress(request));
        record.setUserAgent(request.getHeader("User-Agent"));
        record.setParseStatus(0); // 解析中
        
        try {
            // 查找用户信息
            Optional<User> userOpt = userRepository.findByOpenIdAndIsDeleted(openId, 0);
            if (userOpt.isPresent()) {
                record.setUserId(userOpt.get().getId());
            }
            
            // 保存解析记录
            record = videoParseRecordRepository.save(record);
            
            // 调用外部API解析视频
            Map<String, Object> parseResult = callWatermarkApi(videoUrl);
            
            // 处理解析结果
            if (parseResult != null && "1".equals(String.valueOf(parseResult.get("code")))) {
                // 解析成功
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) parseResult.get("data");
                
                String videoSrc = (String) data.get("videoSrc");
                
                // 预处理视频URL
                VideoUrlPreprocessor.VideoUrlInfo urlInfo = videoUrlPreprocessor.preprocessVideoUrl(videoSrc);
                if (!urlInfo.isSuccess()) {
                    log.warn("视频URL预处理失败：{}", urlInfo.getErrorMessage());
                    // 即使预处理失败，也继续使用原始URL
                } else {
                    log.info("视频URL预处理成功 - Host: {}, NeedsProxy: {}", 
                            urlInfo.getHost(), urlInfo.isNeedsProxy());
                }
                
                // 转换视频URL为代理URL
                String proxyVideoUrl = convertToProxyUrl(videoSrc);
                String proxyCoverUrl = convertToProxyUrl((String) data.get("imageSrc"));
                
                record.setVideoTitle((String) data.get("title"));
                record.setParsedVideoUrl(proxyVideoUrl);
                record.setCoverImageUrl(proxyCoverUrl);
                
                // 更新返回数据中的URL
                data.put("videoSrc", proxyVideoUrl);
                data.put("imageSrc", proxyCoverUrl);
                
                // 处理图集
                Object imageAtlas = data.get("imageAtlas");
                if (imageAtlas != null) {
                    try {
                        record.setImageAtlas(objectMapper.writeValueAsString(imageAtlas));
                    } catch (Exception e) {
                        log.warn("序列化图集失败：{}", e.getMessage());
                    }
                }
                
                record.setParseStatus(1); // 解析成功
                record.setParseDuration(System.currentTimeMillis() - startTime);
                
                // 更新用户解析次数
                if (userOpt.isPresent()) {
                    userRepository.incrementVideoParseCount(openId);
                }
                
                log.info("视频解析成功，用户：{}，标题：{}", openId, data.get("title"));
                
                Map<String, Object> result = new HashMap<>();
                result.put("status", 1);
                result.put("msg", "解析成功");
                
                // 添加URL预处理信息到返回数据中
                if (urlInfo.isSuccess()) {
                    Map<String, Object> enhancedData = new HashMap<>(data);
                    Map<String, Object> urlInfoMap = new HashMap<>();
                    urlInfoMap.put("host", urlInfo.getHost());
                    urlInfoMap.put("needsProxy", urlInfo.isNeedsProxy());
                    urlInfoMap.put("accessible", urlInfo.isAccessible());
                    enhancedData.put("urlInfo", urlInfoMap);
                    result.put("data", enhancedData);
                } else {
                    result.put("data", data);
                }
                
                return result;
                
            } else {
                // 解析失败
                String errorCode = String.valueOf(parseResult != null ? parseResult.get("code") : "-1");
                String errorMsg = getErrorMessage(errorCode);
                
                record.setParseStatus(-1); // 解析失败
                record.setFailReason(errorMsg);
                record.setParseDuration(System.currentTimeMillis() - startTime);
                
                log.warn("视频解析失败，用户：{}，错误码：{}，错误信息：{}", openId, errorCode, errorMsg);
                
                Map<String, Object> result = new HashMap<>();
                result.put("status", 0);
                result.put("msg", errorMsg);
                return result;
            }
            
        } catch (Exception e) {
            log.error("视频解析异常，用户：{}，错误：{}", openId, e.getMessage(), e);
            
            record.setParseStatus(-1); // 解析失败
            record.setFailReason("系统异常：" + e.getMessage());
            record.setParseDuration(System.currentTimeMillis() - startTime);
            
            Map<String, Object> result = new HashMap<>();
            result.put("status", 0);
            result.put("msg", "解析失败，请稍后重试");
            return result;
            
        } finally {
            // 更新解析记录
            videoParseRecordRepository.save(record);
        }
    }

    /**
     * 调用外部去水印API
     *
     * @param videoUrl 视频链接
     * @return API响应结果
     */
    private Map<String, Object> callWatermarkApi(String videoUrl) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Type", "application/x-www-form-urlencoded");
            headers.add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("appid", watermarkAppId);
            params.add("link", videoUrl);
            
            HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(params, headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                watermarkApiUrl, 
                HttpMethod.POST, 
                requestEntity, 
                String.class
            );
            
            if (response.getStatusCode().is2xxSuccessful() && StringUtils.hasText(response.getBody())) {
                JsonNode jsonNode = objectMapper.readTree(response.getBody());
                return objectMapper.convertValue(jsonNode, Map.class);
            }
            
        } catch (Exception e) {
            log.error("调用外部API失败：{}", e.getMessage(), e);
        }
        
        return null;
    }

    /**
     * 检测视频平台
     *
     * @param videoUrl 视频链接
     * @return 平台名称
     */
    private String detectPlatform(String videoUrl) {
        if (!StringUtils.hasText(videoUrl)) {
            return "未知";
        }
        
        String lowerUrl = videoUrl.toLowerCase();
        for (Map.Entry<String, Pattern> entry : PLATFORM_PATTERNS.entrySet()) {
            Matcher matcher = entry.getValue().matcher(lowerUrl);
            if (matcher.find()) {
                return entry.getKey();
            }
        }
        
        return "其他";
    }

    /**
     * 获取错误信息
     *
     * @param errorCode 错误码
     * @return 错误信息
     */
    private String getErrorMessage(String errorCode) {
        switch (errorCode) {
            case "-1":
                return "该账号已被禁用";
            case "-100":
                return "IP访问受限";
            case "109":
                return "套餐提取次数不足";
            case "301":
                return "未检测到链接，请检查内容是否正确";
            case "400":
                return "提取链接无效或暂不支持此平台";
            default:
                return "解析失败，错误码：" + errorCode;
        }
    }

    /**
     * 获取客户端IP地址
     *
     * @param request HTTP请求对象
     * @return IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xForwardedFor) && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(xRealIp) && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }

    /**
     * 将原始视频URL转换为代理URL
     *
     * @param originalUrl 原始URL
     * @return 代理URL
     */
    private String convertToProxyUrl(String originalUrl) {
        if (!StringUtils.hasText(originalUrl)) {
            return originalUrl;
        }
        
        try {
            URL url = new URL(originalUrl);
            String host = url.getHost().toLowerCase();
            String platform = detectPlatformByHost(host);
            
            // 对URL进行Base64编码
            String encodedUrl = Base64.getUrlEncoder().encodeToString(originalUrl.getBytes(StandardCharsets.UTF_8));
            
            // 根据平台生成代理URL
            switch (platform) {
                case "抖音":
                    return proxyDomain + "/proxy/douyin/" + encodedUrl;
                case "快手":
                    return proxyDomain + "/proxy/kuaishou/" + encodedUrl;
                case "小红书":
                    return proxyDomain + "/proxy/xiaohongshu/" + encodedUrl;
                default:
                    return proxyDomain + "/proxy/general/" + encodedUrl;
            }
        } catch (Exception e) {
            log.warn("URL转换失败，使用原始URL：{}", e.getMessage());
            return originalUrl;
        }
    }
    
    /**
     * 根据域名检测平台
     *
     * @param host 域名
     * @return 平台名称
     */
    private String detectPlatformByHost(String host) {
        if (host.contains("douyin") || host.contains("iesdouyin") || host.contains("bytedance")) {
            return "抖音";
        } else if (host.contains("kuaishou")) {
            return "快手";
        } else if (host.contains("xiaohongshu") || host.contains("xhslink")) {
            return "小红书";
        }
        return "其他";
    }

    /**
     * 获取用户解析记录
     *
     * @param openId 用户openId
     * @param retentionMinutes 记录保留时间（分钟）
     * @return 解析记录列表
     */
    public List<VideoParseRecord> getUserParseRecords(String openId, int retentionMinutes) {
        LocalDateTime startTime = LocalDateTime.now().minusMinutes(retentionMinutes);
        return videoParseRecordRepository.findRecentRecordsByOpenId(openId, startTime);
    }
}