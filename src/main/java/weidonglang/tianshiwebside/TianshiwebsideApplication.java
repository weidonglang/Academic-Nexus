package weidonglang.tianshiwebside;

import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 项目后端启动入口。
 *
 * 这个类负责启动 Spring Boot 应用，并让 Spring 自动扫描 Controller、Service、Mapper、
 * Repository、Redis 配置和权限配置等组件。答辩时可以从这里说明：本项目后端不是单独页面演示，
 * 而是一个完整的 Spring Boot 教务系统服务，启动后对外提供 /api/** 接口。
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class TianshiwebsideApplication {

    public static void main(String[] args) {
        SpringApplication.run(TianshiwebsideApplication.class, args);
    }

}
