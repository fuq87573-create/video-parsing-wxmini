package com.video.controller;

import com.video.common.Result;
import com.video.entity.VideoParseRecord;
import com.video.service.VideoParsingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 视频解析控制器
 *
 * @author video-parsing
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/video")
public class VideoController {

    @Autowired
    private VideoParsingService videoParsingService;

    @Value("${business.parsing-record-retention-minutes}")
    private int recordRetentionMinutes;

    /**
     * 获取视频信息（解析视频）
     *
     * @param request HTTP请求对象
     * @return 解析结果
     */
    @PostMapping("/getVideoInfo")
    public Result<Object> getVideoInfo(HttpServletRequest request) {
        try {
            // 从请求参数中获取openId
            String openId = request.getParameter("openId");
            String url = request.getParameter("url");
            
            log.info("收到视频解析请求，用户：{}，链接：{}", openId, url);
            
            // 参数校验
            if (!StringUtils.hasText(openId)) {
                return Result.error("用户标识不能为空");
            }
            
            if (!StringUtils.hasText(url)) {
                return Result.error("视频链接不能为空");
            }
            
            // 简单的URL格式校验
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                return Result.error("请输入有效的视频链接");
            }
            
            // 调用解析服务
            Map<String, Object> parseResult = videoParsingService.parseVideo(openId, url, request);
            
            // 根据解析结果返回响应
            Integer status = (Integer) parseResult.get("status");
            if (status != null && status == 1) {
                // 解析成功
                Object data = parseResult.get("data");
                return Result.success((String) parseResult.get("msg"), data);
            } else {
                // 解析失败
                return Result.error((String) parseResult.get("msg"));
            }
            
        } catch (Exception e) {
            log.error("视频解析接口异常：{}", e.getMessage(), e);
            return Result.error("系统异常，请稍后重试");
        }
    }

    /**
     * 获取解析记录
     *
     * @param openId 用户openId
     * @return 解析记录列表
     */
    @GetMapping("/getParsingInfo")
    public Result<Map<String, Object>> getParsingInfo(@RequestParam String openId) {
        try {
            log.info("获取解析记录，用户：{}", openId);
           
            // 参数校验
            if (!StringUtils.hasText(openId)) {
                return Result.error("用户标识不能为空");
            }
            
            // 获取用户解析记录
            List<VideoParseRecord> records = videoParsingService.getUserParseRecords(openId, recordRetentionMinutes);
            
            // 构建返回数据
            Map<String, Object> data = new HashMap<>();
            data.put("records", records);
            data.put("retentionMinutes", recordRetentionMinutes);
            data.put("totalCount", records.size());
            
            return Result.success("获取成功", data);
            
        } catch (Exception e) {
            log.error("获取解析记录异常：{}", e.getMessage(), e);
            return Result.error("获取解析记录失败");
        }
    }

    /**
     * 获取支持的平台列表
     *
     * @return 支持的平台列表
     */
    @GetMapping("/getSupportedPlatforms")
    public Result<Map<String, Object>> getSupportedPlatforms() {
        try {
            Map<String, Object> data = new HashMap<>();
            
            // 支持的平台列表
            String[] platforms = {
                "抖音", "快手", "小红书", "微视", "火山", 
                "西瓜视频", "皮皮虾", "最右", "微博"
            };
            
            data.put("platforms", platforms);
            data.put("count", platforms.length);
            data.put("description", "支持主流短视频平台的无水印解析");
            
            return Result.success("获取成功", data);
            
        } catch (Exception e) {
            log.error("获取支持平台列表异常：{}", e.getMessage(), e);
            return Result.error("获取平台列表失败");
        }
    }

    /**
     * 健康检查接口
     *
     * @return 健康状态
     */
    @GetMapping("/health")
    public Result<Map<String, Object>> health() {
        Map<String, Object> data = new HashMap<>();
        data.put("status", "UP");
        data.put("timestamp", System.currentTimeMillis());
        data.put("service", "video-parsing-service");
        
        return Result.success("服务正常", data);
    }
}