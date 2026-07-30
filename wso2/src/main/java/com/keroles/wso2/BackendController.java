package com.keroles.wso2;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class BackendController {

    @GetMapping("/hi")
    public Map<String, String> hello() {
        return Map.of("message", "hello from wso2 java backend , port 8092");
    }

    @GetMapping("/items/{id}")
    public Map<String, Object> item(@PathVariable long id) {
        return Map.of("id", id, "name", "item-" + id);
    }
}
