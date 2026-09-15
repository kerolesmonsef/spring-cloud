package com.keroles.soapserver.post.request;

import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Data;

@Data
@XmlRootElement(name = "getPostRequest")
public class GetPostRequest {
    private long id;
}
