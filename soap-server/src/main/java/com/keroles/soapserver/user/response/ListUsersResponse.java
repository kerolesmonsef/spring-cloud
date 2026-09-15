package com.keroles.soapserver.user.response;

import com.keroles.soapserver.user.wsdl.UserXml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
@XmlRootElement(name = "listUsersResponse")
@XmlAccessorType(XmlAccessType.FIELD)
public class ListUsersResponse {
    @XmlElement(name = "user")
    private List<UserXml> user = new ArrayList<>();
}
