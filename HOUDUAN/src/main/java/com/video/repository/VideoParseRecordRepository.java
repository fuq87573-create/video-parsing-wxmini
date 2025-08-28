package com.video.repository;

import com.video.entity.VideoParseRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 视频解析记录Repository接口
 *
 * @author video-parsing
 * @since 1.0.0
 */
@Repository
public interface VideoParseRecordRepository extends JpaRepository<VideoParseRecord, Long> {

    /**
     * 根据用户openId查询解析记录（指定时间范围内）
     *
     * @param openId 用户openId
     * @param startTime 开始时间
     * @param isDeleted 是否删除
     * @return 解析记录列表
     */
    @Query("SELECT v FROM VideoParseRecord v WHERE v.openId = :openId AND v.createTime >= :startTime AND v.isDeleted = :isDeleted ORDER BY v.createTime DESC")
    List<VideoParseRecord> findByOpenIdAndCreateTimeAfterAndIsDeleted(
            @Param("openId") String openId, 
            @Param("startTime") LocalDateTime startTime, 
            @Param("isDeleted") Integer isDeleted);

    /**
     * 根据用户openId查询最近的解析记录
     *
     * @param openId 用户openId
     * @param startTime 开始时间
     * @return 解析记录列表
     */
    default List<VideoParseRecord> findRecentRecordsByOpenId(String openId, LocalDateTime startTime) {
        return findByOpenIdAndCreateTimeAfterAndIsDeleted(openId, startTime, 0);
    }

    /**
     * 根据用户openId和解析状态查询记录数量
     *
     * @param openId 用户openId
     * @param parseStatus 解析状态
     * @param isDeleted 是否删除
     * @return 记录数量
     */
    @Query("SELECT COUNT(v) FROM VideoParseRecord v WHERE v.openId = :openId AND v.parseStatus = :parseStatus AND v.isDeleted = :isDeleted")
    Long countByOpenIdAndParseStatusAndIsDeleted(
            @Param("openId") String openId, 
            @Param("parseStatus") Integer parseStatus, 
            @Param("isDeleted") Integer isDeleted);

    /**
     * 统计用户成功解析的视频数量
     *
     * @param openId 用户openId
     * @return 成功解析数量
     */
    default Long countSuccessParseByOpenId(String openId) {
        return countByOpenIdAndParseStatusAndIsDeleted(openId, 1, 0);
    }

    /**
     * 根据平台统计解析数量
     *
     * @param platform 平台名称
     * @param parseStatus 解析状态
     * @param startTime 开始时间
     * @param isDeleted 是否删除
     * @return 解析数量
     */
    @Query("SELECT COUNT(v) FROM VideoParseRecord v WHERE v.platform = :platform AND v.parseStatus = :parseStatus AND v.createTime >= :startTime AND v.isDeleted = :isDeleted")
    Long countByPlatformAndParseStatusAndCreateTimeAfterAndIsDeleted(
            @Param("platform") String platform,
            @Param("parseStatus") Integer parseStatus,
            @Param("startTime") LocalDateTime startTime,
            @Param("isDeleted") Integer isDeleted);
}