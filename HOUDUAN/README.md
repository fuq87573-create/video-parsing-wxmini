# 视频解析后端 API

## 项目简介

这是一个基于 Spring Boot 的视频解析后端服务，支持多个主流视频平台的视频解析和去水印功能。

## 技术栈

- **Java 8**
- **Spring Boot 2.x**
- **Spring Data JPA**
- **MySQL**
- **Caffeine Cache**
- **Apache HttpClient**
- **Jackson**
- **Lombok**

## 主要功能

### 1. 视频解析
- 支持多个主流视频平台的视频解析
- 提取视频信息（标题、作者、封面、视频链接等）
- 支持去水印功能

### 2. 支持的平台
- 抖音 (Douyin)
- 快手 (Kuaishou)
- 小红书 (Xiaohongshu)
- 哔哩哔哩 (Bilibili)
- 微博 (Weibo)
- 西瓜视频 (Xigua)
- 好看视频 (Haokan)
- 皮皮虾 (Pipixia)
- 火山小视频 (Huoshan)
- 最右 (Zuiyou)

### 3. 视频代理
- 提供视频文件代理服务
- 解决跨域访问问题
- 支持不同平台的 User-Agent 和 Referer 设置

### 4. 解析记录
- 记录用户解析历史
- 支持分页查询
- 与微信小程序用户系统集成

## API 接口

### 视频解析
```
POST /api/video/getVideoInfo
```
**参数：**
- `url`: 视频链接
- `openId`: 微信用户 openId

**响应：**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "title": "视频标题",
    "author": "作者",
    "cover": "封面图片链接",
    "videoUrl": "视频下载链接",
    "platform": "平台名称"
  }
}
```

### 获取解析记录
```
GET /api/video/getParsingInfo
```
**参数：**
- `openId`: 微信用户 openId
- `page`: 页码（可选，默认1）
- `size`: 每页数量（可选，默认10）

### 获取支持的平台
```
GET /api/video/getSupportedPlatforms
```

### 健康检查
```
GET /api/video/health
```

### 视频代理
```
GET /api/proxy/{platform}/{encodedUrl}
```
**参数：**
- `platform`: 平台名称
- `encodedUrl`: Base64 编码的视频链接

## 配置说明

### 数据库配置
```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/video_parsing?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&useSSL=true&serverTimezone=GMT%2B8
    username: root
    password: your_password
```

### 微信小程序配置
```yaml
wechat:
  miniapp:
    app-id: your_wechat_appid
    app-secret: your_wechat_app_secret
```

### 外部API配置
```yaml
external:
  watermark:
    domain: your-api-domain.com
    app-id: your_api_app_id
    url: https://{domain}/Watermark/Index
```

### 代理配置
```yaml
proxy:
  domain: https://domain.com
  enabled: true
```

## 部署说明

### 1. 环境要求
- Java 8+
- MySQL 5.7+
- Maven 3.6+

### 2. 数据库初始化
创建数据库并导入相关表结构（项目会自动创建表）

### 3. 配置修改
修改 `application.yml` 中的相关配置：
- 数据库连接信息
- 微信小程序配置
- 外部API配置

### 4. 编译运行
```bash
# 编译
mvn clean package

# 运行
java -jar target/video-parsing-api-1.0.0.jar
```

### 5. 访问地址
默认端口：8086

## 项目结构

```
src/main/java/com/video/
├── VideoParsingApplication.java    # 主启动类
├── common/                         # 通用工具类
├── config/                         # 配置类
├── controller/                     # 控制器层
├── entity/                         # 实体类
├── exception/                      # 异常处理
├── repository/                     # 数据访问层
└── service/                        # 业务逻辑层
```

## 注意事项

1. **隐私保护**：请确保不要在代码中硬编码敏感信息
2. **API限制**：部分平台可能有访问频率限制
3. **合规使用**：请遵守各平台的使用条款和相关法律法规
4. **缓存策略**：项目使用 Caffeine 进行缓存，可根据需要调整缓存配置

## 许可证

本项目采用 MIT 许可证，详情请参阅 LICENSE 文件。