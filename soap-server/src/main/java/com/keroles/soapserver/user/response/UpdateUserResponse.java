package com.keroles.soapserver.user.response;

import com.keroles.soapserver.user.wsdl.UserXml;

import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Data;

@Data
@XmlRootElement(name = "updateUserResponse")
public class UpdateUserResponse {
    private boolean success;
}
