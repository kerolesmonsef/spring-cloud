package com.keroles.soapserver.user.response;

import com.keroles.soapserver.user.wsdl.UserXml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Data;

@Data
@XmlRootElement(name = "getUserResponse")
@XmlAccessorType(XmlAccessType.FIELD)
public class GetUserResponse {
    @XmlElement(name = "user")
    private UserXml user;
}
