package com.video;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 短视频去水印工具后端应用启动类
 *
 * @author video-parsing
 * @since 1.0.0
 */
@SpringBootApplication
@EnableCaching
@EnableAsync
public class VideoParsingApplication {

    public static void main(String[] args) {
        SpringApplication.run(VideoParsingApplication.class, args);
        System.out.println("\n" +
                "██╗   ██╗██╗██████╗ ███████╗ ██████╗     ██████╗  █████╗ ██████╗ ███████╗██╗███╗   ██╗ ██████╗ \n" +
                "██║   ██║██║██╔══██╗██╔════╝██╔═══██╗    ██╔══██╗██╔══██╗██╔══██╗██╔════╝██║████╗  ██║██╔════╝ \n" +
                "██║   ██║██║██║  ██║█████╗  ██║   ██║    ██████╔╝███████║██████╔╝███████╗██║██╔██╗ ██║██║  ███╗\n" +
                "╚██╗ ██╔╝██║██║  ██║██╔══╝  ██║   ██║    ██╔═══╝ ██╔══██║██╔══██╗╚════██║██║██║╚██╗██║██║   ██║\n" +
                " ╚████╔╝ ██║██████╔╝███████╗╚██████╔╝    ██║     ██║  ██║██║  ██║███████║██║██║ ╚████║╚██████╔╝\n" +
                "  ╚═══╝  ╚═╝╚═════╝ ╚══════╝ ╚═════╝     ╚═╝     ╚═╝  ╚═╝╚═╝  ╚═╝╚══════╝╚═╝╚═╝  ╚═══╝ ╚═════╝ \n" +
                "\n短视频去水印工具后端服务启动成功！\n" +
                "访问地址: http://localhost:8086\n" +
                "API文档: http://localhost:8086/doc.html\n");
    }
}