package com.keroles.soapserver.post.endpoint;

import com.keroles.soapserver.post.model.Post;
import com.keroles.soapserver.post.service.PostService;
import com.keroles.soapserver.post.request.*;
import com.keroles.soapserver.post.response.*;
import com.keroles.soapserver.post.wsdl.PostXml;

import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

@Endpoint
public class PostEndpoint {
    private static final String NAMESPACE = "http://keroles.com/soapserver/posts";

    private final PostService postService;

    public PostEndpoint(PostService postService) {
        this.postService = postService;
    }

    @PayloadRoot(namespace = NAMESPACE, localPart = "getPostRequest")
    @ResponsePayload
    public GetPostResponse getPost(@RequestPayload GetPostRequest request) {
        GetPostResponse response = new GetPostResponse();
        postService.findById(request.getId()).ifPresent(post -> response.setPost(toXml(post)));
        return response;
    }

    @PayloadRoot(namespace = NAMESPACE, localPart = "listPostsRequest")
    @ResponsePayload
    public ListPostsResponse listPosts(@RequestPayload ListPostsRequest request) {
        ListPostsResponse response = new ListPostsResponse();
        postService.findAll().forEach(post -> response.getPost().add(toXml(post)));
        return response;
    }

    @PayloadRoot(namespace = NAMESPACE, localPart = "createPostRequest")
    @ResponsePayload
    public CreatePostResponse createPost(@RequestPayload CreatePostRequest request) {
        CreatePostResponse response = new CreatePostResponse();
        response.setPost(toXml(postService.create(request.getTitle(), request.getDescription(), request.getUserId())));
        return response;
    }

    @PayloadRoot(namespace = NAMESPACE, localPart = "updatePostRequest")
    @ResponsePayload
    public UpdatePostResponse updatePost(@RequestPayload UpdatePostRequest request) {
        UpdatePostResponse response = new UpdatePostResponse();
        response.setSuccess(postService.update(request.getId(), request.getTitle(), request.getDescription()));
        return response;
    }

    @PayloadRoot(namespace = NAMESPACE, localPart = "deletePostRequest")
    @ResponsePayload
    public DeletePostResponse deletePost(@RequestPayload DeletePostRequest request) {
        DeletePostResponse response = new DeletePostResponse();
        response.setSuccess(postService.delete(request.getId()));
        return response;
    }

    private PostXml toXml(Post post) {
        PostXml xml = new PostXml();
        xml.setId(post.getId());
        xml.setTitle(post.getTitle());
        xml.setDescription(post.getDescription());
        xml.setCreatedAt(post.getCreatedAt());
        xml.setUserId(post.getUserId());
        return xml;
    }
}
