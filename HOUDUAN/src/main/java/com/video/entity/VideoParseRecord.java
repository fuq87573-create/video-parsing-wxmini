package com.video.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;

/**
 * 视频解析记录实体类
 *
 * @author video-parsing
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "t_video_parse_record")
public class VideoParseRecord extends BaseEntity {

    /**
     * 用户ID
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 用户openId
     */
    @Column(name = "open_id", nullable = false, length = 64)
    private String openId;

    /**
     * 原始视频链接
     */
    @Column(name = "original_url", nullable = false, length = 1000)
    private String originalUrl;

    /**
     * 视频标题
     */
    @Column(name = "video_title", length = 500)
    private String videoTitle;

    /**
     * 解析后的视频链接
     */
    @Column(name = "parsed_video_url", length = 1000)
    private String parsedVideoUrl;

    /**
     * 视频封面链接
     */
    @Column(name = "cover_image_url", length = 1000)
    private String coverImageUrl;

    /**
     * 图集链接（JSON格式）
     */
    @Column(name = "image_atlas", columnDefinition = "TEXT")
    private String imageAtlas;

    /**
     * 视频平台：抖音、快手、小红书等
     */
    @Column(name = "platform", length = 50)
    private String platform;

    /**
     * 解析状态：0-解析中，1-解析成功，2-解析失败
     */
    @Column(name = "parse_status", nullable = false)
    private Integer parseStatus = 0;

    /**
     * 解析失败原因
     */
    @Column(name = "fail_reason", length = 500)
    private String failReason;

    /**
     * 解析耗时（毫秒）
     */
    @Column(name = "parse_duration")
    private Long parseDuration;

    /**
     * 用户IP地址
     */
    @Column(name = "user_ip", length = 50)
    private String userIp;

    /**
     * 用户代理
     */
    @Column(name = "user_agent", length = 500)
    private String userAgent;
}