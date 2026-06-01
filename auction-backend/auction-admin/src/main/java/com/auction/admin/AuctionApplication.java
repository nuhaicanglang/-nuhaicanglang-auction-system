package com.auction.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 后端应用启动入口。
 * scanBasePackages 指定扫描 com.auction 下所有模块，让 common、framework、system 等模块中的 Bean 都能被 Spring 管理。
 */
@SpringBootApplication(scanBasePackages = "com.auction")
@EnableElasticsearchRepositories(basePackages = "com.auction.search.repository")
@EnableScheduling
public class AuctionApplication {

    /**
     * 程序主入口，启动 Spring Boot 应用。
     */
    public static void main(String[] args) {
        SpringApplication.run(AuctionApplication.class, args);
    }
}
