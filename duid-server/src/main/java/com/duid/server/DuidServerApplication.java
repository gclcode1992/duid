package com.duid.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * DUID 分布式唯一 ID 服务启动入口。
 *
 * @author gclcode@126.com
 */
@SpringBootApplication
@EnableScheduling
public class DuidServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(DuidServerApplication.class, args);
    }
}
