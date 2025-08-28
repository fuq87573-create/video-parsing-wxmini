package com.video.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 用户实体类
 *
 * @author video-parsing
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "t_user")
public class User extends BaseEntity {

    /**
     * 微信openId
     */
    @Column(name = "open_id", nullable = false, unique = true, length = 64)
    private String openId;

    /**
     * 微信unionId
     */
    @Column(name = "union_id", length = 64)
    private String unionId;

    /**
     * 微信昵称
     */
    @Column(name = "nickname", length = 100)
    private String nickname;

    /**
     * 微信头像
     */
    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    /**
     * 用户积分
     */
    @Column(name = "points", nullable = false)
    private Integer points = 0;

    /**
     * 签到次数
     */
    @Column(name = "sign_in_count", nullable = false)
    private Integer signInCount = 0;

    /**
     * 连续签到天数
     */
    @Column(name = "continuous_sign_days", nullable = false)
    private Integer continuousSignDays = 0;

    /**
     * 最后签到时间
     */
    @Column(name = "last_sign_time")
    private LocalDateTime lastSignTime;

    /**
     * 解析视频总数
     */
    @Column(name = "video_parse_count", nullable = false)
    private Integer videoParseCount = 0;

    /**
     * 用户状态：0-正常，1-禁用
     */
    @Column(name = "status", nullable = false)
    private Integer status = 0;

    /**
     * 最后登录时间
     */
    @Column(name = "last_login_time")
    private LocalDateTime lastLoginTime;

    /**
     * 最后登录IP
     */
    @Column(name = "last_login_ip", length = 50)
    private String lastLoginIp;

    /**
     * 今日是否已签到
     */
    @Column(name = "today_signin", nullable = false)
    private Boolean todaySignin = false;
}