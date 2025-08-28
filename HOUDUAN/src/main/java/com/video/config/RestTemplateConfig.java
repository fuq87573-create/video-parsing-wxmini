package com.video.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * RestTemplate配置类
 * 针对大文件视频代理优化超时设置
 *
 * @author video-parsing
 * @since 1.0.0
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate(clientHttpRequestFactory());
    }

    private ClientHttpRequestFactory clientHttpRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        
        // 连接超时时间（毫秒）- 建立连接的最大时间
        factory.setConnectTimeout(60000); // 60秒
        
        // 读取超时时间（毫秒）- 从连接中读取数据的最大时间
        factory.setReadTimeout(300000); // 5分钟，适合大文件下载
        
        // 缓冲请求体（对于大文件上传设为false，但这里主要是下载，保持默认）
        factory.setBufferRequestBody(true);
        
        return factory;
    }
}