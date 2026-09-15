package com.keroles.soapserver.post.wsdl;

import jakarta.xml.bind.annotation.XmlType;
import lombok.Data;

import java.time.Instant;

@Data
@XmlType(name = "post")
public class PostXml {
    private long id;
    private String title;
    private String description;
    private Instant createdAt;
    private long userId;
}
