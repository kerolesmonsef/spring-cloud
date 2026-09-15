package com.keroles.soapserver.post.request;

import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Data;

@Data
@XmlRootElement(name = "createPostRequest")
public class CreatePostRequest {
    private String title;
    private String description;
    private long userId;
}
