package com.video.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;

/**
 * 系统配置实体类
 *
 * @author video-parsing
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "t_system_config")
public class SystemConfig extends BaseEntity {

    /**
     * 配置键
     */
    @Column(name = "config_key", nullable = false, unique = true, length = 100)
    private String configKey;

    /**
     * 配置值
     */
    @Column(name = "config_value", columnDefinition = "TEXT")
    private String configValue;

    /**
     * 配置描述
     */
    @Column(name = "config_desc", length = 500)
    private String configDesc;

    /**
     * 配置类型：1-字符串，2-数字，3-布尔值，4-JSON
     */
    @Column(name = "config_type", nullable = false)
    private Integer configType = 1;

    /**
     * 是否启用：0-禁用，1-启用
     */
    @Column(name = "is_enabled", nullable = false)
    private Integer isEnabled = 1;
}