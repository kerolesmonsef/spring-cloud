package com.keroles.soapserver.user.request;

import com.keroles.soapserver.user.wsdl.UserXml;

import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Data;

@Data
@XmlRootElement(name = "createUserRequest")
public class CreateUserRequest {
    private String name;
    private String email;
}
