package com.abbiya.broadr.retrofit;

import com.abbiya.broadr.api.CommentResponseMap;
import com.abbiya.broadr.api.MessageResponseMap;

import retrofit.Callback;
import retrofit.http.GET;
import retrofit.http.Header;
import retrofit.http.Path;

/**
 * Created by seshachalam on 10/9/14.
 */
public interface ApiService {

    @GET("/api/{messageUUID}/comments")
    CommentResponseMap getComments(@Header("X-Auth") String GCMREGID, @Path("messageUUID") String messageUUID);

    @GET("/api/{messageUUID}/comments")
    void getComments(@Header("X-Auth") String GCMREGID, @Path("messageUUID") String messageUUID, Callback<CommentResponseMap> cb);

    @GET("/api/{boardID}/messages")
    MessageResponseMap getMessages(@Header("X-Auth") String GCMREGID, @Path("boardID") String boardID);

    @GET("/api/{boardID}/messages")
    void getMessages(@Header("X-Auth") String GCMREGID, @Path("boardID") String boardID, Callback<MessageResponseMap> cb);

    @GET("/api/{boardID}/m_delta/{lastMessageUri}/{direction}")
    MessageResponseMap getDeltaMessages(@Header("X-Auth") String GCMREGID, @Path("boardID") String boardID, @Path("lastMessageUri") String lastMessageUri, @Path("direction") int direction);

    @GET("/api/{boardID}/m_delta/{lastMessageUri}/{direction}")
    void getDeltaMessages(@Header("X-Auth") String GCMREGID, @Path("boardID") String boardID, @Path("lastMessageUri") String lastMessageUri, @Path("direction") int direction, Callback<MessageResponseMap> cb);

    @GET("/api/{messageUri}/c_delta/{lastCommentUri}/{direction}")
    CommentResponseMap getDeltaComments(@Header("X-Auth") String GCMREGID, @Path("messageUri") String messageUri, @Path("lastCommentUri") String lastCommentUri, @Path("direction") int direction);

    @GET("/api/{messageUri}/c_delta/{lastCommentUri}/{direction}")
    void getDeltaComments(@Header("X-Auth") String GCMREGID, @Path("messageUri") String messageUri, @Path("lastCommentUri") String lastCommentUri, @Path("direction") int direction, Callback<CommentResponseMap> cb);

}
