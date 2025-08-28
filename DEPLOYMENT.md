# 部署指南

本文档详细说明了视频解析项目的完整部署流程，包括后端服务、Nginx配置、SSL证书设置和视频代理功能。

## 📋 部署架构

```
用户请求 → Nginx (HTTPS) → Spring Boot 后端 (8086端口)
                ↓
            视频代理服务
                ↓
            各大视频平台
```

## 🔧 环境要求

### 服务器环境
- **操作系统**: Linux (推荐 Ubuntu 18.04+ 或 CentOS 7+)
- **内存**: 最低 2GB，推荐 4GB+
- **存储**: 最低 20GB 可用空间
- **网络**: 公网IP，开放 80、443 端口

### 软件依赖
- **Java**: JDK 8+
- **MySQL**: 5.7+
- **Nginx**: 1.16+
- **SSL证书**: 有效的HTTPS证书

## 🚀 部署步骤

### 1. 服务器基础环境配置

#### 安装 Java
```bash
# Ubuntu/Debian
sudo apt update
sudo apt install openjdk-8-jdk

# CentOS/RHEL
sudo yum install java-1.8.0-openjdk

# 验证安装
java -version
```

#### 安装 MySQL
```bash
# Ubuntu/Debian
sudo apt install mysql-server

# CentOS/RHEL
sudo yum install mysql-server

# 启动服务
sudo systemctl start mysql
sudo systemctl enable mysql

# 安全配置
sudo mysql_secure_installation
```

#### 安装 Nginx
```bash
# Ubuntu/Debian
sudo apt install nginx

# CentOS/RHEL
sudo yum install nginx

# 启动服务
sudo systemctl start nginx
sudo systemctl enable nginx
```

### 2. 数据库配置

#### 创建数据库
```sql
-- 登录 MySQL
mysql -u root -p

-- 创建数据库
CREATE DATABASE video_parsing CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 创建用户（可选）
CREATE USER 'video_user'@'localhost' IDENTIFIED BY 'your_secure_password';
GRANT ALL PRIVILEGES ON video_parsing.* TO 'video_user'@'localhost';
FLUSH PRIVILEGES;
```

### 3. 后端服务部署

#### 配置应用
```bash
# 上传项目文件到服务器
scp -r HOUDUAN/ user@your-server:/opt/video-parsing/

# 进入项目目录
cd /opt/video-parsing/HOUDUAN

# 修改配置文件
vim src/main/resources/application.yml
```

#### 修改配置文件
```yaml
# application.yml
server:
  port: 8086
  address: 127.0.0.1  # 只监听本地，通过Nginx代理

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/video_parsing?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&useSSL=true&serverTimezone=GMT%2B8
    username: video_user  # 或 root
    password: your_secure_password

wechat:
  miniapp:
    app-id: your_wechat_appid
    app-secret: your_wechat_app_secret

proxy:
  domain: https://yourdomain.com
  enabled: true
```

#### 编译和运行
```bash
# 编译项目
mvn clean package -DskipTests

# 创建运行脚本
cat > start.sh << 'EOF'
#!/bin/bash
JAVA_OPTS="-Xms512m -Xmx1024m -server"
nohup java $JAVA_OPTS -jar target/video-parsing-api-1.0.0.jar > app.log 2>&1 &
echo $! > app.pid
EOF

chmod +x start.sh

# 启动服务
./start.sh

# 检查服务状态
tail -f app.log
```

#### 创建系统服务（推荐）
```bash
# 创建服务文件
sudo vim /etc/systemd/system/video-parsing.service
```

```ini
[Unit]
Description=Video Parsing API Service
After=network.target mysql.service

[Service]
Type=simple
User=www-data
WorkingDirectory=/opt/video-parsing/HOUDUAN
ExecStart=/usr/bin/java -Xms512m -Xmx1024m -jar target/video-parsing-api-1.0.0.jar
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

```bash
# 启用服务
sudo systemctl daemon-reload
sudo systemctl enable video-parsing
sudo systemctl start video-parsing

# 检查状态
sudo systemctl status video-parsing
```

### 4. SSL 证书配置

#### 方式一：使用 Let's Encrypt（免费）
```bash
# 安装 Certbot
sudo apt install certbot python3-certbot-nginx  # Ubuntu/Debian
sudo yum install certbot python3-certbot-nginx  # CentOS/RHEL

# 获取证书
sudo certbot --nginx -d yourdomain.com -d www.yourdomain.com

# 设置自动续期
sudo crontab -e
# 添加以下行
0 12 * * * /usr/bin/certbot renew --quiet
```

#### 方式二：使用购买的SSL证书
```bash
# 创建证书目录
sudo mkdir -p /etc/nginx/ssl

# 上传证书文件
sudo cp yourdomain.com.pem /etc/nginx/ssl/
sudo cp yourdomain.com.key /etc/nginx/ssl/

# 设置权限
sudo chmod 600 /etc/nginx/ssl/yourdomain.com.key
sudo chmod 644 /etc/nginx/ssl/yourdomain.com.pem
```

### 5. Nginx 配置

#### 使用提供的配置模板
```bash
# 复制配置模板
sudo cp nginx.conf.example /etc/nginx/sites-available/video-parsing

# 修改配置文件
sudo vim /etc/nginx/sites-available/video-parsing
```

#### 关键配置修改
```nginx
# 修改域名
server_name  www.yourdomain.com yourdomain.com *.yourdomain.com;

# 修改SSL证书路径
ssl_certificate      /etc/nginx/ssl/yourdomain.com.pem;
ssl_certificate_key  /etc/nginx/ssl/yourdomain.com.key;

# 如果使用 Let's Encrypt
ssl_certificate      /etc/letsencrypt/live/yourdomain.com/fullchain.pem;
ssl_certificate_key  /etc/letsencrypt/live/yourdomain.com/privkey.pem;
```

#### 启用配置
```bash
# 创建软链接
sudo ln -s /etc/nginx/sites-available/video-parsing /etc/nginx/sites-enabled/

# 删除默认配置（可选）
sudo rm /etc/nginx/sites-enabled/default

# 测试配置
sudo nginx -t

# 重载配置
sudo systemctl reload nginx
```

#### 添加HTTP到HTTPS重定向
```nginx
# 在配置文件中添加HTTP服务器块
server {
    listen 80;
    server_name yourdomain.com www.yourdomain.com;
    return 301 https://$server_name$request_uri;
}
```

## 🌐 视频代理功能说明

### 代理机制

视频代理是本项目的核心功能之一，解决了以下问题：

1. **跨域访问限制**: 微信小程序无法直接访问第三方视频链接
2. **防盗链保护**: 部分平台检查 Referer 头
3. **User-Agent 限制**: 需要特定的浏览器标识
4. **访问频率限制**: 通过代理分散请求

### 代理路径说明

| 平台 | 代理路径 | 说明 |
|------|----------|------|
| 抖音 | `/proxy/douyin/{encodedUrl}` | 支持短视频和长视频 |
| 快手 | `/proxy/kuaishou/{encodedUrl}` | 支持普通视频内容 |
| 小红书 | `/proxy/xiaohongshu/{encodedUrl}` | 支持图片和视频 |
| B站 | `/proxy/bilibili/{encodedUrl}` | 支持普通视频 |
| 微博 | `/proxy/weibo/{encodedUrl}` | 支持视频内容 |
| 西瓜视频 | `/proxy/xigua/{encodedUrl}` | 支持短视频 |
| 好看视频 | `/proxy/haokan/{encodedUrl}` | 支持短视频 |
| 皮皮虾 | `/proxy/pipixia/{encodedUrl}` | 支持搞笑视频 |
| 火山小视频 | `/proxy/huoshan/{encodedUrl}` | 支持短视频 |
| 最右 | `/proxy/zuiyou/{encodedUrl}` | 支持短视频 |
| 通用 | `/proxy/general/{encodedUrl}` | 通用代理 |

### 大文件优化配置

配置中包含了针对大文件下载的优化：

- **流式传输**: `proxy_buffering off` 关闭缓冲，直接流式传输
- **超时优化**: 针对大文件设置了更长的超时时间
- **连接优化**: 使用 HTTP/1.1 长连接
- **客户端优化**: 支持大文件上传和下载

## 🔒 安全配置

### 防火墙设置
```bash
# Ubuntu/Debian (UFW)
sudo ufw allow 22/tcp
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw enable

# CentOS/RHEL (firewalld)
sudo firewall-cmd --permanent --add-service=ssh
sudo firewall-cmd --permanent --add-service=http
sudo firewall-cmd --permanent --add-service=https
sudo firewall-cmd --reload
```

### Nginx 安全配置
```nginx
# 在 server 块中添加安全头
add_header X-Frame-Options DENY;
add_header X-Content-Type-Options nosniff;
add_header X-XSS-Protection "1; mode=block";
add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;

# 隐藏 Nginx 版本
server_tokens off;

# 限制请求大小
client_max_body_size 100M;

# 限制连接数
limit_conn_zone $binary_remote_addr zone=conn_limit_per_ip:10m;
limit_conn conn_limit_per_ip 20;

# 限制请求频率
limit_req_zone $binary_remote_addr zone=req_limit_per_ip:10m rate=10r/s;
limit_req zone=req_limit_per_ip burst=20 nodelay;
```

### 数据库安全
```bash
# 修改 MySQL 配置
sudo vim /etc/mysql/mysql.conf.d/mysqld.cnf

# 添加以下配置
[mysqld]
bind-address = 127.0.0.1
skip-networking = false
max_connections = 100

# 重启 MySQL
sudo systemctl restart mysql
```

## 📊 监控和日志

### 应用日志
```bash
# 查看应用日志
tail -f /opt/video-parsing/HOUDUAN/app.log

# 查看系统服务日志
sudo journalctl -u video-parsing -f
```

### Nginx 日志
```bash
# 访问日志
tail -f /var/log/nginx/access.log

# 错误日志
tail -f /var/log/nginx/error.log
```

### 系统监控
```bash
# 查看系统资源
htop

# 查看端口占用
netstat -tlnp | grep :8086
netstat -tlnp | grep :443

# 查看磁盘使用
df -h

# 查看内存使用
free -h
```

## 🔧 故障排除

### 常见问题

#### 1. 后端服务无法启动
```bash
# 检查端口占用
sudo netstat -tlnp | grep :8086

# 检查 Java 进程
jps -l

# 查看详细错误
tail -f app.log
```

#### 2. Nginx 配置错误
```bash
# 测试配置语法
sudo nginx -t

# 查看错误日志
sudo tail -f /var/log/nginx/error.log

# 重新加载配置
sudo systemctl reload nginx
```

#### 3. SSL 证书问题
```bash
# 检查证书有效期
openssl x509 -in /etc/nginx/ssl/yourdomain.com.pem -text -noout | grep "Not After"

# 测试 SSL 配置
openssl s_client -connect yourdomain.com:443

# Let's Encrypt 续期
sudo certbot renew --dry-run
```

#### 4. 数据库连接问题
```bash
# 测试数据库连接
mysql -u video_user -p -h localhost video_parsing

# 检查 MySQL 状态
sudo systemctl status mysql

# 查看 MySQL 错误日志
sudo tail -f /var/log/mysql/error.log
```

### 性能优化

#### JVM 调优
```bash
# 修改启动参数
JAVA_OPTS="-Xms1024m -Xmx2048m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
```

#### MySQL 优化
```sql
-- 查看慢查询
SHOW VARIABLES LIKE 'slow_query_log';
SET GLOBAL slow_query_log = 'ON';
SET GLOBAL long_query_time = 2;

-- 查看连接数
SHOW STATUS LIKE 'Threads_connected';
SHOW VARIABLES LIKE 'max_connections';
```

#### Nginx 优化
```nginx
# 在 nginx.conf 中添加
worker_processes auto;
worker_connections 1024;

# 启用 gzip 压缩
gzip on;
gzip_vary on;
gzip_min_length 1024;
gzip_types text/plain text/css application/json application/javascript text/xml application/xml;
```

## 📝 维护建议

### 定期维护任务

1. **每日检查**
   - 查看应用日志是否有错误
   - 检查系统资源使用情况
   - 验证服务可用性

2. **每周维护**
   - 清理旧日志文件
   - 检查磁盘空间
   - 更新系统安全补丁

3. **每月维护**
   - 备份数据库
   - 检查 SSL 证书有效期
   - 分析访问日志和性能指标

### 备份策略

```bash
# 数据库备份脚本
#!/bin/bash
DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR="/backup/mysql"
mkdir -p $BACKUP_DIR

mysqldump -u video_user -p video_parsing > $BACKUP_DIR/video_parsing_$DATE.sql
gzip $BACKUP_DIR/video_parsing_$DATE.sql

# 保留最近30天的备份
find $BACKUP_DIR -name "*.sql.gz" -mtime +30 -delete
```

## 📞 技术支持

如果在部署过程中遇到问题，请：

1. 查看相关日志文件
2. 检查配置文件语法
3. 验证网络连接和端口开放
4. 参考故障排除章节
5. 提交 Issue 到项目仓库

---

**注意**: 请确保在生产环境中使用强密码，定期更新系统和依赖包，并遵循安全最佳实践。