package com.video.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.video.entity.SystemConfig;
import com.video.entity.User;
import com.video.repository.SystemConfigRepository;
import com.video.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 微信小程序服务类
 *
 * @author video-parsing
 * @since 1.0.0
 */
@Slf4j
@Service
public class WechatService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SystemConfigRepository systemConfigRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${wechat.miniapp.app-id}")
    private String appId;

    @Value("${wechat.miniapp.app-secret}")
    private String appSecret;

    @Value("${wechat.miniapp.api.code2session}")
    private String code2SessionUrl;

    @Value("${business.user.default-points}")
    private Integer defaultPoints;

    @Value("${business.sign.daily-points}")
    private int dailySigninPoints;

    /**
     * 微信小程序登录授权
     *
     * @param jsCode 微信登录凭证
     * @param userIp 用户IP
     * @return openId
     */
    @Transactional
    public String auth(String jsCode, String userIp) {
        try {
            // 调用微信API获取openId
            String openId = getOpenIdFromWechat(jsCode);
            if (openId == null) {
                throw new RuntimeException("微信登录失败，无法获取openId");
            }

            // 查找或创建用户
            User user = findOrCreateUser(openId, userIp);
            
            // 更新最后登录信息
            updateLastLoginInfo(user, userIp);

            log.info("用户登录成功，openId: {}, IP: {}", openId, userIp);
            return openId;
        } catch (Exception e) {
            log.error("微信登录授权失败，jsCode: {}, error: {}", jsCode, e.getMessage(), e);
            throw new RuntimeException("登录失败: " + e.getMessage());
        }
    }

    /**
     * 用户登录初始化
     *
     * @param openId 用户openId
     * @param userIp 用户IP
     * @return 用户信息
     */
    @Transactional
    public Map<String, Object> login(String openId, String userIp) {
        try {
            Optional<User> userOpt = userRepository.findByOpenId(openId);
            if (!userOpt.isPresent()) {
                // 用户不存在，创建新用户
                try {
                    User newUser = createNewUser(openId, userIp);
                    return buildLoginResponse(newUser);
                } catch (Exception e) {
                    // 如果是唯一约束冲突，说明其他线程已经创建了用户，重新查询
                    if (e.getMessage() != null && e.getMessage().contains("Duplicate entry")) {
                        log.warn("用户创建冲突，重新查询用户，openId: {}", openId);
                        Optional<User> retryUserOpt = userRepository.findByOpenId(openId);
                        if (retryUserOpt.isPresent()) {
                            User user = retryUserOpt.get();
                            updateLastLoginInfo(user, userIp);
                            return buildLoginResponse(user);
                        }
                    }
                    throw e;
                }
            }

            User user = userOpt.get();
            // 更新最后登录信息
            updateLastLoginInfo(user, userIp);
            
            return buildLoginResponse(user);
        } catch (Exception e) {
            log.error("用户登录初始化失败，openId: {}, error: {}", openId, e.getMessage(), e);
            throw new RuntimeException("登录初始化失败: " + e.getMessage());
        }
    }

    /**
     * 从微信API获取openId
     */
    private String getOpenIdFromWechat(String jsCode) {
        try {
            String url = String.format("%s?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code",
                    code2SessionUrl, appId, appSecret, jsCode);
            
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            JsonNode jsonNode = objectMapper.readTree(response.getBody());
            
            if (jsonNode.has("errcode")) {
                int errcode = jsonNode.get("errcode").asInt();
                String errmsg = jsonNode.get("errmsg").asText();
                log.error("微信API调用失败，errcode: {}, errmsg: {}", errcode, errmsg);
                return null;
            }
            
            return jsonNode.get("openid").asText();
        } catch (Exception e) {
            log.error("调用微信API异常: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 查找或创建用户
     */
    private User findOrCreateUser(String openId, String userIp) {
        Optional<User> userOpt = userRepository.findByOpenId(openId);
        if (userOpt.isPresent()) {
            return userOpt.get();
        }
        
        // 处理并发创建用户的情况
        try {
            return createNewUser(openId, userIp);
        } catch (Exception e) {
            // 如果是唯一约束冲突，说明其他线程已经创建了用户，重新查询
            if (e.getMessage() != null && e.getMessage().contains("Duplicate entry")) {
                log.warn("用户创建冲突，重新查询用户，openId: {}", openId);
                Optional<User> retryUserOpt = userRepository.findByOpenId(openId);
                if (retryUserOpt.isPresent()) {
                    return retryUserOpt.get();
                }
            }
            throw e;
        }
    }

    /**
     * 创建新用户
     */
    private User createNewUser(String openId, String userIp) {
        User user = new User();
        user.setOpenId(openId);
        user.setPoints(defaultPoints);
        user.setSignInCount(0);
        user.setContinuousSignDays(0);
        user.setVideoParseCount(0);
        user.setStatus(0);
        user.setLastLoginTime(LocalDateTime.now());
        user.setLastLoginIp(userIp);
        
        return userRepository.save(user);
    }

    /**
     * 更新最后登录信息
     */
    private void updateLastLoginInfo(User user, String userIp) {
        userRepository.updateLastLoginInfo(user.getOpenId(), LocalDateTime.now(), userIp);
    }

    /**
     * 构建登录响应数据
     */
    private Map<String, Object> buildLoginResponse(User user) {
        Map<String, Object> response = new HashMap<>();
        response.put("openId", user.getOpenId());
        response.put("points", user.getPoints());
        response.put("signInSum", user.getSignInCount());
        response.put("videoNumber", user.getVideoParseCount());
        response.put("continuousSignDays", user.getContinuousSignDays());
        response.put("lastSignTime", user.getLastSignTime());
        
        // 添加用户头像和昵称信息
        response.put("avatarUrl", user.getAvatarUrl());
        response.put("nickName", user.getNickname());
        
        // 判断今日是否已签到
        boolean todaySignin = user.getLastSignTime() != null && 
            user.getLastSignTime().toLocalDate().equals(LocalDate.now());
        response.put("todaySignin", todaySignin);
        
        return response;
    }

    /**
     * 用户签到
     *
     * @param openId 用户openId
     * @return 签到结果
     */
    @Transactional
    public Map<String, Object> signIn(String openId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Optional<User> userOpt = userRepository.findByOpenIdAndIsDeleted(openId, 0);
            if (!userOpt.isPresent()) {
                result.put("status", 0);
                result.put("msg", "用户不存在");
                return result;
            }
            
            User user = userOpt.get();
            LocalDate today = LocalDate.now();
            LocalDate lastSignDate = user.getLastSignTime() != null ? 
                user.getLastSignTime().toLocalDate() : null;
            
            // 检查今天是否已签到
            if (lastSignDate != null && lastSignDate.equals(today)) {
                result.put("status", 0);
                result.put("msg", "今日已签到");
                result.put("data", buildSignInData(user));
                return result;
            }
            
            // 计算连续签到天数
            int continuousDays = 1;
            if (lastSignDate != null && lastSignDate.equals(today.minusDays(1))) {
                continuousDays = user.getContinuousSignDays() + 1;
            }
            
            // 更新用户签到信息
            user.setSignInCount(user.getSignInCount() + 1);
            user.setContinuousSignDays(continuousDays);
            user.setLastSignTime(LocalDateTime.now());
            user.setPoints(user.getPoints() + dailySigninPoints);
            user.setUpdateTime(LocalDateTime.now());
            
            userRepository.save(user);
            
            result.put("status", 1);
            result.put("msg", "签到成功");
            result.put("data", buildSignInData(user));
            
            log.info("用户签到成功，openId：{}，连续签到：{}天", openId, continuousDays);
            
        } catch (Exception e) {
            log.error("用户签到失败，openId：{}，错误：{}", openId, e.getMessage(), e);
            result.put("status", 0);
            result.put("msg", "签到失败，请稍后重试");
        }
        
        return result;
    }

    /**
     * 获取小程序配置
     *
     * @return 配置信息
     */
    public Map<String, Object> getInitConfig() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            List<SystemConfig> configs = systemConfigRepository.findAllEnabled();
            Map<String, Object> configData = new HashMap<>();
            
            for (SystemConfig config : configs) {
                configData.put(config.getConfigKey(), config.getConfigValue());
            }
            
            // 添加一些默认配置
            if (!configData.containsKey("appName")) {
                configData.put("appName", "短视频去水印");
            }
            if (!configData.containsKey("version")) {
                configData.put("version", "1.0.0");
            }
            if (!configData.containsKey("dailySigninPoints")) {
                configData.put("dailySigninPoints", dailySigninPoints);
            }
            
            // 添加视频平台图标配置
            List<Map<String, String>> videoIconList = new ArrayList<>();
            
            // 平台名称与图标文件的映射
            Map<String, String> platformIconMap = new HashMap<>();
            platformIconMap.put("抖音", "/images/video-icon/logo-douyin.png");
            platformIconMap.put("快手", "/images/video-icon/logo-gitShow.png");
            platformIconMap.put("小红书", "/images/video-icon/logo-music.png");
            platformIconMap.put("微视", "/images/video-icon/logo-microview.png");
            platformIconMap.put("火山", "/images/video-icon/logo-volcano.png");
            platformIconMap.put("西瓜视频", "/images/video-icon/logo-watermelon.png");
            platformIconMap.put("皮皮虾", "/images/video-icon/logo-ppx.png");
            platformIconMap.put("最右", "/images/video-icon/logo-zuiyou.png");
            platformIconMap.put("美拍", "/images/video-icon/logo-meipai.png");
            platformIconMap.put("微博", "/images/video-icon/logo-365yg.png");
            platformIconMap.put("秒拍", "/images/video-icon/logo-miaopai.png");
            platformIconMap.put("头条", "/images/video-icon/logo-toutiao.png");
            platformIconMap.put("小咖秀", "/images/video-icon/logo-xiaokaxiu.png");
            platformIconMap.put("音乐", "/images/video-icon/logo-music.png");
            platformIconMap.put("365yg", "/images/video-icon/logo-365yg.png");
            
            // 从supported_platforms配置中获取支持的平台列表
            String supportedPlatformsJson = (String) configData.get("supported_platforms");
            if (StringUtils.hasText(supportedPlatformsJson)) {
                try {
                    JsonNode platformsNode = objectMapper.readTree(supportedPlatformsJson);
                    if (platformsNode.isArray()) {
                        for (JsonNode platformNode : platformsNode) {
                            String platformName = platformNode.asText();
                            Map<String, String> iconItem = new HashMap<>();
                            iconItem.put("name", platformName);
                            // 根据平台名称获取对应的图标路径，如果没有则使用抖音图标
                            String iconPath = platformIconMap.getOrDefault(platformName, "/images/video-icon/logo-douyin.png");
                            iconItem.put("imgPath", iconPath);
                            videoIconList.add(iconItem);
                        }
                    }
                } catch (Exception e) {
                    log.warn("解析supported_platforms配置失败: {}", e.getMessage());
                }
            }
            
            // 如果没有配置或解析失败，使用默认的平台图标列表
            if (videoIconList.isEmpty()) {
                String[] defaultPlatforms = {"抖音", "快手", "小红书", "微视", "火山", "西瓜视频", "皮皮虾", "最右", "微博"};
                for (String platform : defaultPlatforms) {
                    Map<String, String> iconItem = new HashMap<>();
                    iconItem.put("name", platform);
                    String iconPath = platformIconMap.getOrDefault(platform, "/images/video-icon/logo-douyin.png");
                    iconItem.put("imgPath", iconPath);
                    videoIconList.add(iconItem);
                }
            }
            
            configData.put("videoIcon", videoIconList);
            
            // 添加顶部小程序跳转信息
            if (!configData.containsKey("topMiniImg")) {
                configData.put("topMiniImg", "/images/watermark_before.png");
            }
            if (!configData.containsKey("topMiniTitle")) {
                configData.put("topMiniTitle", "推荐小程序");
            }
            if (!configData.containsKey("topMiniAppId")) {
                configData.put("topMiniAppId", "");
            }
            if (!configData.containsKey("topMiniPath")) {
                configData.put("topMiniPath", "");
            }
            
            result.put("status", 1);
            result.put("msg", "获取成功");
            result.put("data", configData);
            
        } catch (Exception e) {
            log.error("获取小程序配置失败：{}", e.getMessage(), e);
            result.put("status", 0);
            result.put("msg", "获取配置失败");
        }
        
        return result;
    }

    /**
     * 显示二维码（占位方法）
     *
     * @return 二维码信息
     */
    public Map<String, Object> showQRCode() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", 1);
        result.put("msg", "功能开发中");
        result.put("data", new HashMap<>());
        return result;
    }

    /**
     * 更新用户信息（昵称和头像）
     *
     * @param openId 用户openId
     * @param nickName 用户昵称
     * @param avatarUrl 用户头像URL
     */
    @Transactional
    public void updateUserInfo(String openId, String nickName, String avatarUrl) {
        Optional<User> userOptional = userRepository.findByOpenId(openId);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            user.setNickname(nickName);
            user.setAvatarUrl(avatarUrl);
            userRepository.save(user);
            log.info("用户信息更新成功: openId={}, nickName={}", openId, nickName);
        } else {
            log.warn("用户不存在: openId={}", openId);
            throw new RuntimeException("用户不存在");
        }
    }

    /**
     * 构建签到数据
     *
     * @param user 用户信息
     * @return 签到数据
     */
    private Map<String, Object> buildSignInData(User user) {
        Map<String, Object> data = new HashMap<>();
        data.put("signCount", user.getSignInCount());
        data.put("continuousSignDays", user.getContinuousSignDays());
        data.put("points", user.getPoints());
        data.put("todaySignin", user.getLastSignTime() != null && 
            user.getLastSignTime().toLocalDate().equals(LocalDate.now()));
        data.put("rewardPoints", dailySigninPoints);
        return data;
    }
}