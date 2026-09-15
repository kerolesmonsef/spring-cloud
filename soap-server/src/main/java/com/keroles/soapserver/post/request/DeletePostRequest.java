package com.keroles.soapserver.post.request;

import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Data;

@Data
@XmlRootElement(name = "deletePostRequest")
public class DeletePostRequest {
    private long id;
}
