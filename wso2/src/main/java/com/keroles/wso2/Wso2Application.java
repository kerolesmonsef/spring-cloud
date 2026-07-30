package com.keroles.wso2;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.filter.CommonsRequestLoggingFilter;

@SpringBootApplication
public class Wso2Application {
    public static void main(String[] args) {
        SpringApplication.run(Wso2Application.class, args);
    }

    @Bean
    CommonsRequestLoggingFilter requestLoggingFilter() {
        var filter = new CommonsRequestLoggingFilter();
        filter.setIncludeQueryString(true);
        filter.setIncludePayload(true);
        filter.setMaxPayloadLength(1000);
        filter.setAfterMessagePrefix("REQ: ");
        return filter;
    }
}
