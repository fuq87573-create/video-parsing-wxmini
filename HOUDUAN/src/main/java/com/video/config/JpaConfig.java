package com.video.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * JPA配置类
 *
 * @author video-parsing
 * @since 1.0.0
 */
@Configuration
@EnableJpaAuditing
@EnableJpaRepositories(basePackages = "com.video.repository")
public class JpaConfig {
}