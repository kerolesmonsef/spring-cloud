package com.keroles.soapserver.user.request;

import com.keroles.soapserver.user.wsdl.UserXml;

import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Data;

@Data
@XmlRootElement(name = "deleteUserRequest")
public class DeleteUserRequest {
    private long id;
}
