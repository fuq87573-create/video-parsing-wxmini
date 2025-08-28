package com.video.repository;

import com.video.entity.SystemConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 系统配置Repository接口
 *
 * @author video-parsing
 * @since 1.0.0
 */
@Repository
public interface SystemConfigRepository extends JpaRepository<SystemConfig, Long> {

    /**
     * 根据配置键查询配置
     *
     * @param configKey 配置键
     * @param isDeleted 是否删除
     * @return 配置信息
     */
    @Query("SELECT s FROM SystemConfig s WHERE s.configKey = :configKey AND s.isDeleted = :isDeleted")
    Optional<SystemConfig> findByConfigKeyAndIsDeleted(
            @Param("configKey") String configKey, 
            @Param("isDeleted") Integer isDeleted);

    /**
     * 根据配置键查询有效配置
     *
     * @param configKey 配置键
     * @return 配置信息
     */
    default Optional<SystemConfig> findActiveByConfigKey(String configKey) {
        return findByConfigKeyAndIsDeleted(configKey, 0);
    }

    /**
     * 根据配置类型查询配置列表
     *
     * @param configType 配置类型
     * @param isEnabled 是否启用
     * @param isDeleted 是否删除
     * @return 配置列表
     */
    @Query("SELECT s FROM SystemConfig s WHERE s.configType = :configType AND s.isEnabled = :isEnabled AND s.isDeleted = :isDeleted ORDER BY s.createTime DESC")
    List<SystemConfig> findByConfigTypeAndIsEnabledAndIsDeleted(
            @Param("configType") String configType,
            @Param("isEnabled") Integer isEnabled,
            @Param("isDeleted") Integer isDeleted);

    /**
     * 查询指定类型的启用配置
     *
     * @param configType 配置类型
     * @return 配置列表
     */
    default List<SystemConfig> findEnabledByConfigType(String configType) {
        return findByConfigTypeAndIsEnabledAndIsDeleted(configType, 1, 0);
    }

    /**
     * 查询所有启用的配置
     *
     * @param isEnabled 是否启用
     * @param isDeleted 是否删除
     * @return 配置列表
     */
    @Query("SELECT s FROM SystemConfig s WHERE s.isEnabled = :isEnabled AND s.isDeleted = :isDeleted ORDER BY s.configType, s.createTime DESC")
    List<SystemConfig> findByIsEnabledAndIsDeleted(
            @Param("isEnabled") Integer isEnabled,
            @Param("isDeleted") Integer isDeleted);

    /**
     * 查询所有启用的配置
     *
     * @return 配置列表
     */
    default List<SystemConfig> findAllEnabled() {
        return findByIsEnabledAndIsDeleted(1, 0);
    }
}