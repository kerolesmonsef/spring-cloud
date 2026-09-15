package com.keroles.soapserver.user.wsdl;

import jakarta.xml.bind.annotation.XmlType;
import lombok.Data;

@Data
@XmlType(name = "user")
public class UserXml {
    private long id;
    private String name;
    private String email;
}
