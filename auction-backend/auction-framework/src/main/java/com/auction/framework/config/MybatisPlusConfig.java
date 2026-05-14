package com.auction.framework.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 基础配置。
 * MapperScan 会扫描所有模块的 mapper 包，让 Spring 能创建 Mapper 代理对象。
 */
@Configuration
@MapperScan("com.auction.**.mapper")
public class MybatisPlusConfig {
}
