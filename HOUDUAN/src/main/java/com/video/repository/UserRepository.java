package com.video.repository;

import com.video.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 用户Repository接口
 *
 * @author video-parsing
 * @since 1.0.0
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * 根据openId查找用户
     *
     * @param openId 微信openId
     * @return 用户信息
     */
    Optional<User> findByOpenIdAndIsDeleted(String openId, Integer isDeleted);

    /**
     * 根据openId查找用户（默认未删除）
     *
     * @param openId 微信openId
     * @return 用户信息
     */
    default Optional<User> findByOpenId(String openId) {
        return findByOpenIdAndIsDeleted(openId, 0);
    }

    /**
     * 更新用户最后登录信息
     *
     * @param openId 微信openId
     * @param lastLoginTime 最后登录时间
     * @param lastLoginIp 最后登录IP
     */
    @Modifying
    @Query("UPDATE User u SET u.lastLoginTime = :lastLoginTime, u.lastLoginIp = :lastLoginIp WHERE u.openId = :openId")
    void updateLastLoginInfo(@Param("openId") String openId, 
                           @Param("lastLoginTime") LocalDateTime lastLoginTime, 
                           @Param("lastLoginIp") String lastLoginIp);

    /**
     * 增加用户积分
     *
     * @param openId 微信openId
     * @param points 积分数量
     */
    @Modifying
    @Query("UPDATE User u SET u.points = u.points + :points WHERE u.openId = :openId")
    void addUserPoints(@Param("openId") String openId, @Param("points") Integer points);

    /**
     * 增加用户解析视频数量
     *
     * @param openId 微信openId
     */
    @Modifying
    @Query("UPDATE User u SET u.videoParseCount = u.videoParseCount + 1 WHERE u.openId = :openId")
    void incrementVideoParseCount(@Param("openId") String openId);

    /**
     * 更新用户签到信息
     *
     * @param openId 微信openId
     * @param signInCount 签到次数
     * @param continuousSignDays 连续签到天数
     * @param lastSignTime 最后签到时间
     */
    @Modifying
    @Query("UPDATE User u SET u.signInCount = :signInCount, u.continuousSignDays = :continuousSignDays, u.lastSignTime = :lastSignTime WHERE u.openId = :openId")
    void updateSignInfo(@Param("openId") String openId, 
                       @Param("signInCount") Integer signInCount,
                       @Param("continuousSignDays") Integer continuousSignDays,
                       @Param("lastSignTime") LocalDateTime lastSignTime);
}