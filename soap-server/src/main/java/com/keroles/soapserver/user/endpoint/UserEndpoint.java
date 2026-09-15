package com.keroles.soapserver.user.endpoint;

import com.keroles.soapserver.user.model.User;
import com.keroles.soapserver.user.service.UserService;
import com.keroles.soapserver.user.request.*;
import com.keroles.soapserver.user.response.*;
import com.keroles.soapserver.user.wsdl.UserXml;

import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

@Endpoint
public class UserEndpoint {
    private static final String NAMESPACE = "http://keroles.com/soapserver/users";

    private final UserService userService;

    public UserEndpoint(UserService userService) {
        this.userService = userService;
    }

    @PayloadRoot(namespace = NAMESPACE, localPart = "getUserRequest")
    @ResponsePayload
    public GetUserResponse getUser(@RequestPayload GetUserRequest request) {
        GetUserResponse response = new GetUserResponse();
        userService.findById(request.getId()).ifPresent(user -> response.setUser(toXml(user)));
        return response;
    }

    @PayloadRoot(namespace = NAMESPACE, localPart = "listUsersRequest")
    @ResponsePayload
    public ListUsersResponse listUsers(@RequestPayload ListUsersRequest request) {
        ListUsersResponse response = new ListUsersResponse();
        userService.findAll().forEach(user -> response.getUser().add(toXml(user)));
        return response;
    }

    @PayloadRoot(namespace = NAMESPACE, localPart = "createUserRequest")
    @ResponsePayload
    public CreateUserResponse createUser(@RequestPayload CreateUserRequest request) {
        CreateUserResponse response = new CreateUserResponse();
        response.setUser(toXml(userService.create(request.getName(), request.getEmail())));
        return response;
    }

    @PayloadRoot(namespace = NAMESPACE, localPart = "updateUserRequest")
    @ResponsePayload
    public UpdateUserResponse updateUser(@RequestPayload UpdateUserRequest request) {
        UpdateUserResponse response = new UpdateUserResponse();
        response.setSuccess(userService.update(request.getId(), request.getName(), request.getEmail()));
        return response;
    }

    @PayloadRoot(namespace = NAMESPACE, localPart = "deleteUserRequest")
    @ResponsePayload
    public DeleteUserResponse deleteUser(@RequestPayload DeleteUserRequest request) {
        DeleteUserResponse response = new DeleteUserResponse();
        response.setSuccess(userService.delete(request.getId()));
        return response;
    }

    private UserXml toXml(User user) {
        UserXml xml = new UserXml();
        xml.setId(user.getId());
        xml.setName(user.getName());
        xml.setEmail(user.getEmail());
        return xml;
    }
}
