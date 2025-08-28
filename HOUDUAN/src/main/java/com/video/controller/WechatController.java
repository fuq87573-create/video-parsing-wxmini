package com.video.controller;

import com.video.common.Result;
import com.video.service.WechatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * 微信小程序相关接口控制器
 *
 * @author video-parsing
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/wx")
public class WechatController {

    @Autowired
    private WechatService wechatService;

    /**
     * 微信小程序登录授权
     *
     * @param jsCode 微信登录凭证
     * @param request HTTP请求
     * @return 授权结果
     */
    @PostMapping("/auth")
    public Result<String> auth(@RequestParam("js_code") String jsCode, HttpServletRequest request) {
        try {
            if (!StringUtils.hasText(jsCode)) {
                return Result.error("登录凭证不能为空");
            }

            String userIp = getClientIp(request);
            String openId = wechatService.auth(jsCode, userIp);
            
            return Result.success("登录成功", openId);
        } catch (Exception e) {
            log.error("微信登录授权失败: {}", e.getMessage(), e);
            return Result.error("登录失败: " + e.getMessage());
        }
    }

    /**
     * 用户登录
     *
     * @param openId 用户openId
     * @param request HTTP请求
     * @return 用户信息
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestParam("openId") String openId, HttpServletRequest request) {
        try {
            if (!StringUtils.hasText(openId)) {
                return Result.error("用户标识不能为空");
            }

            log.info("用户登录: {}", openId);
            
            // 获取用户IP
            String userIp = getClientIp(request);
            
            // 获取用户信息
            Map<String, Object> userInfo = wechatService.login(openId, userIp);
            
            log.info("用户登录成功: {}", openId);
            return Result.success("获取用户信息成功", userInfo);
            
        } catch (Exception e) {
            log.error("用户登录失败: {}", e.getMessage(), e);
            return Result.error("获取用户信息失败: " + e.getMessage());
        }
    }

    /**
     * 获取小程序初始化配置
     *
     * @return 配置信息
     */
    @PostMapping("/initConfig")
    public Result<Object> initConfig() {
        try {
            Map<String, Object> result = wechatService.getInitConfig();
            Integer status = (Integer) result.get("status");
            
            if (status != null && status == 1) {
                return Result.success(result.get("data"));
            } else {
                return Result.error((String) result.get("msg"));
            }
        } catch (Exception e) {
            log.error("获取小程序配置异常：{}", e.getMessage(), e);
            return Result.error("获取配置失败");
        }
    }

    /**
     * 用户签到
     *
     * @param openId 用户openId
     * @return 签到结果
     */
    @PostMapping("/signIn")
    public Result<Object> signIn(@RequestParam String openId) {
        try {
            if (!StringUtils.hasText(openId)) {
                return Result.error("用户标识不能为空");
            }
            
            Map<String, Object> result = wechatService.signIn(openId);
            Integer status = (Integer) result.get("status");
            
            if (status != null && status == 1) {
                return Result.success(result.get("data"));
            } else {
                return Result.error((String) result.get("msg"));
            }
        } catch (Exception e) {
            log.error("用户签到异常：{}", e.getMessage(), e);
            return Result.error("签到失败");
        }
    }

    /**
     * 显示二维码（预留接口）
     *
     * @return 二维码信息
     */
    @PostMapping("/showQRCode")
    public Result<Object> showQRCode() {
        try {
            Map<String, Object> result = wechatService.showQRCode();
            Integer status = (Integer) result.get("status");
            
            if (status != null && status == 1) {
                return Result.success(result.get("data"));
            } else {
                return Result.error((String) result.get("msg"));
            }
        } catch (Exception e) {
            log.error("获取二维码异常：{}", e.getMessage(), e);
            return Result.error("获取二维码失败");
        }
    }

    /**
     * 获取客户端IP地址
     */
    private String getClientIp(HttpServletRequest request) {
        String xip = request.getHeader("X-Real-IP");
        String xfor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xfor) && !"unKnown".equalsIgnoreCase(xfor)) {
            int index = xfor.indexOf(",");
            if (index != -1) {
                return xfor.substring(0, index);
            } else {
                return xfor;
            }
        }
        xfor = xip;
        if (StringUtils.hasText(xfor) && !"unKnown".equalsIgnoreCase(xfor)) {
            return xfor;
        }
        if (StringUtils.hasText(xfor = request.getHeader("Proxy-Client-IP")) && !"unKnown".equalsIgnoreCase(xfor)) {
            return xfor;
        }
        if (StringUtils.hasText(xfor = request.getHeader("WL-Proxy-Client-IP")) && !"unKnown".equalsIgnoreCase(xfor)) {
            return xfor;
        }
        if (StringUtils.hasText(xfor = request.getHeader("HTTP_CLIENT_IP")) && !"unKnown".equalsIgnoreCase(xfor)) {
            return xfor;
        }
        if (StringUtils.hasText(xfor = request.getHeader("HTTP_X_FORWARDED_FOR")) && !"unKnown".equalsIgnoreCase(xfor)) {
            return xfor;
        }
        return request.getRemoteAddr();
    }
}