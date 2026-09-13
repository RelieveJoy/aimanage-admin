package com.aimanage;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 爱管理 · 后端启动类。
 *
 * <p>三端（Admin / PM / Member）共用这一份后端，见 docs/admin/ARCHITECTURE.md §2。
 */
@SpringBootApplication
@MapperScan("com.aimanage.mapper")
public class AiManageApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiManageApplication.class, args);
    }
}
