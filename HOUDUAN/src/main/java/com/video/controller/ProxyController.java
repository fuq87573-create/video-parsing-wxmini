package com.video.controller;

import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.ClientAbortException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.SocketException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;

/**
 * 视频代理控制器
 * 用于解码Base64编码的URL并代理请求到实际的视频源
 *
 * @author video-parsing
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/proxy")
public class ProxyController {

    @Autowired
    private RestTemplate restTemplate;

    /**
     * 抖音视频代理
     */
    @GetMapping("/decode/douyin/{encodedUrl}")
    public ResponseEntity<byte[]> proxyDouyinVideo(@PathVariable String encodedUrl,
                                                   HttpServletRequest request,
                                                   HttpServletResponse response) {
        return proxyVideo(encodedUrl, "douyin", request, response);
    }

    /**
     * 快手视频代理
     */
    @GetMapping("/decode/kuaishou/{encodedUrl}")
    public ResponseEntity<byte[]> proxyKuaishouVideo(@PathVariable String encodedUrl,
                                                     HttpServletRequest request,
                                                     HttpServletResponse response) {
        return proxyVideo(encodedUrl, "kuaishou", request, response);
    }

    /**
     * 小红书视频代理
     */
    @GetMapping("/decode/xiaohongshu/{encodedUrl}")
    public ResponseEntity<byte[]> proxyXiaohongshuVideo(@PathVariable String encodedUrl,
                                                        HttpServletRequest request,
                                                        HttpServletResponse response) {
        return proxyVideo(encodedUrl, "xiaohongshu", request, response);
    }

    /**
     * B站视频代理
     */
    @GetMapping("/decode/bilibili/{encodedUrl}")
    public ResponseEntity<byte[]> proxyBilibiliVideo(@PathVariable String encodedUrl,
                                                     HttpServletRequest request,
                                                     HttpServletResponse response) {
        return proxyVideo(encodedUrl, "bilibili", request, response);
    }

    /**
     * 微博视频代理
     */
    @GetMapping("/decode/weibo/{encodedUrl}")
    public ResponseEntity<byte[]> proxyWeiboVideo(@PathVariable String encodedUrl,
                                                  HttpServletRequest request,
                                                  HttpServletResponse response) {
        return proxyVideo(encodedUrl, "weibo", request, response);
    }

    /**
     * 西瓜视频代理
     */
    @GetMapping("/decode/xigua/{encodedUrl}")
    public ResponseEntity<byte[]> proxyXiguaVideo(@PathVariable String encodedUrl,
                                                  HttpServletRequest request,
                                                  HttpServletResponse response) {
        return proxyVideo(encodedUrl, "xigua", request, response);
    }

    /**
     * 好看视频代理
     */
    @GetMapping("/decode/haokan/{encodedUrl}")
    public ResponseEntity<byte[]> proxyHaokanVideo(@PathVariable String encodedUrl,
                                                   HttpServletRequest request,
                                                   HttpServletResponse response) {
        return proxyVideo(encodedUrl, "haokan", request, response);
    }

    /**
     * 皮皮虾视频代理
     */
    @GetMapping("/decode/pipixia/{encodedUrl}")
    public ResponseEntity<byte[]> proxyPipixiaVideo(@PathVariable String encodedUrl,
                                                    HttpServletRequest request,
                                                    HttpServletResponse response) {
        return proxyVideo(encodedUrl, "pipixia", request, response);
    }

    /**
     * 火山小视频代理
     */
    @GetMapping("/decode/huoshan/{encodedUrl}")
    public ResponseEntity<byte[]> proxyHuoshanVideo(@PathVariable String encodedUrl,
                                                    HttpServletRequest request,
                                                    HttpServletResponse response) {
        return proxyVideo(encodedUrl, "huoshan", request, response);
    }

    /**
     * 最右视频代理
     */
    @GetMapping("/decode/zuiyou/{encodedUrl}")
    public ResponseEntity<byte[]> proxyZuiyouVideo(@PathVariable String encodedUrl,
                                                   HttpServletRequest request,
                                                   HttpServletResponse response) {
        return proxyVideo(encodedUrl, "zuiyou", request, response);
    }

    /**
     * 通用视频代理
     */
    @GetMapping("/decode/general/{encodedUrl}")
    public ResponseEntity<byte[]> proxyGeneralVideo(@PathVariable String encodedUrl,
                                                    HttpServletRequest request,
                                                    HttpServletResponse response) {
        return proxyVideo(encodedUrl, "general", request, response);
    }

    /**
     * 代理视频请求的核心方法
     */
    private ResponseEntity<byte[]> proxyVideo(String encodedUrl, String platform,
                                              HttpServletRequest request,
                                              HttpServletResponse response) {
        try {
            // 解码Base64 URL
            String decodedUrl = new String(Base64.getUrlDecoder().decode(encodedUrl), StandardCharsets.UTF_8);
            log.info("代理{}视频请求: {}", platform, decodedUrl);

            // 验证URL格式
            URL url = new URL(decodedUrl);
            
            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 14_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.0 Mobile/15E148 Safari/604.1");
            headers.set("Accept", "*/*");
            headers.set("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
            
            // 根据平台设置不同的Referer
            switch (platform) {
                case "douyin":
                    headers.set("Referer", "https://www.douyin.com/");
                    break;
                case "kuaishou":
                    headers.set("Referer", "https://www.kuaishou.com/");
                    break;
                case "xiaohongshu":
                    headers.set("Referer", "https://www.xiaohongshu.com/");
                    break;
                case "bilibili":
                    headers.set("Referer", "https://www.bilibili.com/");
                    break;
                case "weibo":
                    headers.set("Referer", "https://weibo.com/");
                    break;
                case "xigua":
                    headers.set("Referer", "https://www.ixigua.com/");
                    break;
                case "haokan":
                    headers.set("Referer", "https://haokan.baidu.com/");
                    break;
                case "pipixia":
                    headers.set("Referer", "https://h5.pipix.com/");
                    break;
                case "huoshan":
                    headers.set("Referer", "https://www.huoshan.com/");
                    break;
                case "zuiyou":
                    headers.set("Referer", "https://www.izuiyou.com/");
                    break;
                default:
                    // 通用平台不设置特定Referer
                    break;
            }

            // 处理Range请求（支持断点续传和视频分片下载）
            String rangeHeader = request.getHeader("Range");
            if (StringUtils.hasText(rangeHeader)) {
                headers.set("Range", rangeHeader);
                log.debug("处理Range请求: {}", rangeHeader);
            }
            
            // 添加支持断点续传的请求头
            headers.set("Accept-Ranges", "bytes");
            headers.set("Cache-Control", "no-cache");

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            // 发起代理请求
            ResponseEntity<byte[]> proxyResponse = restTemplate.exchange(
                    decodedUrl,
                    HttpMethod.GET,
                    entity,
                    byte[].class
            );

            // 设置响应头
            HttpHeaders responseHeaders = new HttpHeaders();
            
            // 复制重要的响应头
            if (proxyResponse.getHeaders().getContentType() != null) {
                responseHeaders.setContentType(proxyResponse.getHeaders().getContentType());
            }
            
            if (proxyResponse.getHeaders().getContentLength() > 0) {
                responseHeaders.setContentLength(proxyResponse.getHeaders().getContentLength());
            }
            
            // 处理Range响应和断点续传
            if (proxyResponse.getHeaders().containsKey("Content-Range")) {
                responseHeaders.put("Content-Range", proxyResponse.getHeaders().get("Content-Range"));
                log.debug("返回Content-Range: {}", proxyResponse.getHeaders().get("Content-Range"));
            }
            
            // 确保支持断点续传
            responseHeaders.put("Accept-Ranges", Collections.singletonList("bytes"));
            
            // 处理ETag和Last-Modified，支持缓存验证
            if (proxyResponse.getHeaders().containsKey("ETag")) {
                responseHeaders.put("ETag", proxyResponse.getHeaders().get("ETag"));
            }
            if (proxyResponse.getHeaders().containsKey("Last-Modified")) {
                responseHeaders.put("Last-Modified", proxyResponse.getHeaders().get("Last-Modified"));
            }

            // 根据是否为Range请求设置不同的缓存策略
            if (StringUtils.hasText(request.getHeader("Range"))) {
                // Range请求使用较短的缓存时间
                responseHeaders.setCacheControl("public, max-age=300");
            } else {
                // 完整文件请求使用较长的缓存时间
                responseHeaders.setCacheControl("public, max-age=3600");
            }
            
            // 允许跨域
            responseHeaders.setAccessControlAllowOrigin("*");
            responseHeaders.setAccessControlAllowMethods(Collections.singletonList(HttpMethod.GET));
            responseHeaders.setAccessControlAllowHeaders(Collections.singletonList("Range"));

            log.info("代理{}视频成功，响应大小: {} bytes", platform, 
                    proxyResponse.getBody() != null ? proxyResponse.getBody().length : 0);

            return new ResponseEntity<>(proxyResponse.getBody(), responseHeaders, proxyResponse.getStatusCode());

        } catch (Exception e) {
            // 处理各种异常情况
            String exceptionName = e.getClass().getSimpleName();
            String message = e.getMessage();
            
            // 客户端断开连接相关异常 - 记录为INFO级别
            if ("ClientAbortException".equals(exceptionName)) {
                log.info("客户端断开{}视频代理连接: {}", platform, message);
                return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT).build();
            }
            
            // 网络连接异常
            if (e instanceof SocketException) {
                if (message != null && message.contains("Connection reset")) {
                    log.info("{}视频代理连接被重置: {}", platform, message);
                    return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT).build();
                }
                log.error("代理{}视频网络异常: {}", platform, message, e);
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
            }
            
            // IO异常，可能是大文件传输中断
            if (e instanceof IOException) {
                if (message != null && (message.contains("你的主机中的软件中止了一个已建立的连接") 
                        || message.contains("Connection aborted")
                        || message.contains("Broken pipe"))) {
                    log.info("{}视频代理传输中断: {}", platform, message);
                    return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT).build();
                }
                log.error("代理{}视频IO异常: {}", platform, message, e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
            
            // 其他异常
            log.error("代理{}视频失败: {}", platform, message, e);
            return ResponseEntity.badRequest().build();
        }
    }
}