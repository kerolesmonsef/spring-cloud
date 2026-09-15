package com.keroles.soapserver.post.response;

import com.keroles.soapserver.post.wsdl.PostXml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Data;

@Data
@XmlRootElement(name = "createPostResponse")
@XmlAccessorType(XmlAccessType.FIELD)
public class CreatePostResponse {
    @XmlElement(name = "post")
    private PostXml post;
}
