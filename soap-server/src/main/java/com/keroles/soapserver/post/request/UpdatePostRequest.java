package com.keroles.soapserver.post.request;

import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Data;

@Data
@XmlRootElement(name = "updatePostRequest")
public class UpdatePostRequest {
    private long id;
    private String title;
    private String description;
}
