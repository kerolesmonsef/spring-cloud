package com.keroles.openfeign;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;


public class PostClientConfig {

    @Value("${posts.api.url}")
    private String postsApiUrl;


    public PostsClient userClient() {
        RestClient restClient = RestClient.builder()
                .baseUrl(postsApiUrl)
                .build();

        HttpServiceProxyFactory factory = HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(restClient))
                .build();

        return factory.createClient(PostsClient.class);
    }
}
