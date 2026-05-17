package com.keroles.loadbalancerservice2;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Service2Controller {

    @Value("${server.port}")
    private String port;

    @GetMapping("/info")
    public String info() {
        return "Hello from LoadBalancerService2 running on port " + port;
    }
}
