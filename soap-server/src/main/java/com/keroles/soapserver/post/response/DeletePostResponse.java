package com.keroles.soapserver.post.response;

import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Data;

@Data
@XmlRootElement(name = "deletePostResponse")
public class DeletePostResponse {
    private boolean success;
}
