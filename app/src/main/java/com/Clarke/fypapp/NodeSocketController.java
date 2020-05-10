package com.Clarke.fypapp;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;

public class NodeSocketController extends WebSocketClient {
    String messages = "";

    public NodeSocketController(URI serverUri, Draft draft) {
        super(serverUri, draft);
    }

    public NodeSocketController(URI serverURI) {
        super(serverURI);
        System.out.println("started");
    }

    /**
     * In this implementation this socket only expects text responses, though this could be expanded to include files or specific message types
     *
     * @param message
     */
    @Override
    public void onMessage(String message) {
        messages = messages + "\n" + message;
    }

    @Override
    public void onError(Exception ex) {
        System.err.println("an error occurred:" + ex);
    }

    /**
     * allows the orchestratorSocket to access the received messages
     *
     * @return
     */
    public String getMessages() {

        return messages;
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        System.out.println("new connection opened");
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("closed with exit code " + code + " additional info: " + reason);
    }

}
