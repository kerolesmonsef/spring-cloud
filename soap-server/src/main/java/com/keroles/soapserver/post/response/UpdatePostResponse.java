package com.keroles.soapserver.post.response;

import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Data;

@Data
@XmlRootElement(name = "updatePostResponse")
public class UpdatePostResponse {
    private boolean success;
}
