package com.campus.delivery;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.campus.delivery.mapper")
public class CampusDeliveryApplication {

    public static void main(String[] args) {
        SpringApplication.run(CampusDeliveryApplication.class, args);
        System.out.println("========================================");
        System.out.println("校园外卖配送系统启动成功！");
        System.out.println("接口文档地址: http://localhost:8080/api/doc.html");
        System.out.println("========================================");
    }
}
