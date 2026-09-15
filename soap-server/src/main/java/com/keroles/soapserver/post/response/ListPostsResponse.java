package com.keroles.soapserver.post.response;

import com.keroles.soapserver.post.wsdl.PostXml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
@XmlRootElement(name = "listPostsResponse")
@XmlAccessorType(XmlAccessType.FIELD)
public class ListPostsResponse {
    @XmlElement(name = "post")
    private List<PostXml> post = new ArrayList<>();
}
