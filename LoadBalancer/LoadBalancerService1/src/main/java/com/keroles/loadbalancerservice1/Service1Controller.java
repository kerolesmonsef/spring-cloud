package com.keroles.loadbalancerservice1;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Service1Controller {

    @Value("${server.port}")
    private String port;

    @GetMapping("/info")
    public String info() {
        return "Hello from LoadBalancerService1 running on port " + port;
    }
}
