# 视频解析微信小程序前端

## 项目简介

这是一个基于微信小程序原生开发的视频解析工具前端，支持多个主流视频平台的视频解析、下载和去水印功能。

## 功能特性

### 1. 视频解析
- 支持多平台视频链接解析
- 一键复制视频链接
- 实时解析状态显示
- 解析结果预览

### 2. 视频下载
- 支持视频文件下载到本地
- 实时下载进度显示
- 下载速度和剩余时间显示
- 支持下载取消功能

### 3. 解析记录
- 自动保存解析历史
- 支持历史记录查看
- 时间格式化显示
- 支持重新下载

### 4. 用户体验
- 响应式设计
- 流畅的动画效果
- 友好的错误提示
- 支持暗色主题

## 技术栈

- **微信小程序原生开发**
- **WXML** - 页面结构
- **WXSS** - 样式设计
- **JavaScript** - 业务逻辑
- **ColorUI** - UI组件库

## 项目结构

```
QIANDUAN/
├── app.js                 # 小程序入口文件
├── app.json              # 小程序配置文件
├── app.wxss              # 全局样式文件
├── project.config.json   # 项目配置文件
├── pages/                # 页面目录
│   ├── index/           # 首页（视频解析）
│   │   ├── index.js
│   │   ├── index.json
│   │   ├── index.wxml
│   │   └── index.wxss
│   ├── photos/          # 解析记录页
│   │   ├── photos.js
│   │   ├── photos.json
│   │   ├── photos.wxml
│   │   └── photos.wxss
│   ├── help/            # 帮助页面
│   │   ├── help.js
│   │   ├── help.json
│   │   ├── help.wxml
│   │   └── help.wxss
│   └── resolution/      # 解析结果页
│       ├── resolution.js
│       ├── resolution.json
│       ├── resolution.wxml
│       └── resolution.wxss
├── utils/               # 工具类
│   └── request.js       # 网络请求封装
└── colorui/            # ColorUI组件库
    ├── components/
    ├── icon.wxss
    ├── main.wxss
    └── ...
```

## 页面说明

### 1. 首页 (index)
- **功能**：视频链接输入和解析
- **特性**：
  - 支持粘贴板自动识别
  - 链接格式验证
  - 一键清空输入
  - 解析状态反馈

### 2. 解析结果页 (resolution)
- **功能**：显示解析结果和下载
- **特性**：
  - 视频信息展示
  - 下载进度条
  - 下载速度显示
  - 取消下载功能

### 3. 解析记录页 (photos)
- **功能**：历史解析记录管理
- **特性**：
  - 分页加载
  - 时间格式化
  - 重新下载
  - 记录删除

### 4. 帮助页面 (help)
- **功能**：使用说明和支持平台
- **特性**：
  - 详细使用教程
  - 支持平台列表
  - 常见问题解答

## 核心功能实现

### 1. 网络请求封装 (utils/request.js)
```javascript
// 主要功能
- performLogin()        // 用户登录
- getOpenIdAndLogin()   // 获取OpenID
- requestApi()          // API请求封装
- makeRequest()         // 底层请求方法
```

### 2. 视频解析流程
1. 用户输入视频链接
2. 前端验证链接格式
3. 调用后端解析API
4. 显示解析结果
5. 支持视频下载

### 3. 下载功能实现
```javascript
// 下载相关方法
- downloadWithProgress()  // 带进度的下载
- cancelDownload()       // 取消下载
- formatDateTime()       // 时间格式化
```

## 配置说明

### 1. 服务器地址配置 (app.js)
```javascript
globalData: {
  url: "https://domain.com/Video/", // 后端API地址
  // 其他配置...
}
```

### 2. 微信小程序配置 (project.config.json)
```json
{
  "appid": "your_wechat_appid",
  "projectname": "video-parsing-wxmini",
  "setting": {
    "urlCheck": false,
    "es6": true,
    "minified": true
  }
}
```

### 3. 页面配置 (app.json)
```json
{
  "pages": [
    "pages/index/index",
    "pages/photos/photos",
    "pages/help/help",
    "pages/resolution/resolution"
  ],
  "window": {
    "navigationStyle": "custom",
    "navigationBarBackgroundColor": "#39b54a",
    "navigationBarTextStyle": "white"
  }
}
```

## 开发环境搭建

### 1. 环境要求
- 微信开发者工具
- 微信小程序开发账号
- Node.js (可选，用于包管理)

### 2. 项目配置
1. 使用微信开发者工具打开项目
2. 修改 `project.config.json` 中的 `appid`
3. 修改 `app.js` 中的服务器地址
4. 配置合法域名（开发时可关闭域名校验）

### 3. 本地调试
1. 在微信开发者工具中打开项目
2. 点击「编译」按钮
3. 在模拟器中测试功能
4. 使用真机调试测试完整功能

## API 接口说明

### 1. 用户登录
```javascript
// 获取用户OpenID
POST /api/user/login
```

### 2. 视频解析
```javascript
// 解析视频信息
POST /api/video/getVideoInfo
参数: { url, openId }
```

### 3. 解析记录
```javascript
// 获取解析历史
GET /api/video/getParsingInfo
参数: { openId, page, size }
```

### 4. 支持平台
```javascript
// 获取支持的平台列表
GET /api/video/getSupportedPlatforms
```

## 部署说明

### 1. 开发版部署
1. 在微信开发者工具中点击「上传」
2. 填写版本号和项目备注
3. 在微信公众平台设置为开发版

### 2. 体验版部署
1. 在微信公众平台选择开发版本
2. 设置为体验版
3. 添加体验成员

### 3. 正式版发布
1. 提交审核
2. 等待审核通过
3. 发布上线

## 注意事项

### 1. 域名配置
- 需要在微信公众平台配置合法域名
- 开发时可在开发者工具中关闭域名校验
- 正式环境必须使用 HTTPS 协议

### 2. 权限申请
- 需要申请网络访问权限
- 需要申请文件下载权限
- 需要申请相册访问权限

### 3. 性能优化
- 图片资源压缩
- 代码分包加载
- 合理使用缓存
- 避免频繁的网络请求

### 4. 用户体验
- 提供清晰的操作反馈
- 合理的加载状态显示
- 友好的错误提示
- 支持离线缓存

## 常见问题

### 1. 网络请求失败
- 检查域名配置
- 确认后端服务状态
- 检查网络连接

### 2. 下载失败
- 检查文件权限
- 确认存储空间
- 检查网络稳定性

### 3. 解析失败
- 确认链接格式
- 检查平台支持
- 查看错误日志

## 许可证

本项目采用 MIT 许可证，详情请参阅 LICENSE 文件。