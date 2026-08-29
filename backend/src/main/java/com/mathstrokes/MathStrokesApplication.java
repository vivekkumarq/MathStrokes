package com.mathstrokes;

import com.mathstrokes.config.AppProperties;
import com.mathstrokes.config.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling
@EnableConfigurationProperties({JwtProperties.class, AppProperties.class})
public class MathStrokesApplication {

    public static void main(String[] args) {
        SpringApplication.run(MathStrokesApplication.class, args);
    }
}
