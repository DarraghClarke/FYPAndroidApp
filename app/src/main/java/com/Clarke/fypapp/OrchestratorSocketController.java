package com.Clarke.fypapp;

import com.Clarke.fypapp.core.HostRequest;
import com.Clarke.fypapp.core.HostResponse;
import com.Clarke.fypapp.core.Message;
import com.Clarke.fypapp.core.NodeInfo;
import com.Clarke.fypapp.core.NodeInfoRequest;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft;
import org.java_websocket.handshake.ServerHandshake;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.typeadapters.RuntimeTypeAdapterFactory;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;

public class OrchestratorSocketController extends WebSocketClient {
    private UUID assignedUUID;
    NodeSocketController nodeSocketController;
    String desiredServiceName;

    public OrchestratorSocketController(URI serverUri, Draft draft) {
        super(serverUri, draft);
    }

    public OrchestratorSocketController(URI serverURI,String desiredServiceName) {
        super(serverURI);
        this.desiredServiceName = desiredServiceName;

    }

    /**
     * When the websocket library receives any messages they are routed to this method
     *
     * @param message   the message received
     */
    @Override
    public void onMessage(String message) {
        RuntimeTypeAdapterFactory<Message> adapter = RuntimeTypeAdapterFactory
                .of(Message.class, "type")
                .registerSubtype(NodeInfo.class, Message.MessageTypes.NODE_INFO)
                .registerSubtype(HostRequest.class, Message.MessageTypes.HOST_REQUEST)
                .registerSubtype(HostResponse.class, Message.MessageTypes.HOST_RESPONSE)
                .registerSubtype(NodeInfoRequest.class, Message.MessageTypes.NODE_INFO_REQUEST);


        Gson gson = new GsonBuilder().setPrettyPrinting().registerTypeAdapterFactory(adapter).create();
        Message messageObj = gson.fromJson(message, Message.class);


        //this routes inbound messages based on type and then moves them to other methods
        switch (messageObj.getType()) {
            case Message.MessageTypes.NODE_INFO_REQUEST:
                NodeInfoRequest infoRequest = (NodeInfoRequest) messageObj;
                assignedUUID = infoRequest.getAssignedUUID();
                sendHeartbeatResponse();
                hostRequest();
                break;
            case Message.MessageTypes.HOST_RESPONSE:
                HostResponse hostResponse = (HostResponse) messageObj;
                hostResponse.getServiceHostAddress();
                try {
                    nodeSocketController=new NodeSocketController(new URI("wss://"+hostResponse.getServiceHostAddress()));
                    nodeSocketController.run();
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }
                break;
        }
    }

    /**
     * Constructs and sends Heartbeat responses when called
     */
    public void sendHeartbeatResponse() {
        Gson gson = new Gson();
        NodeInfo nodeInfo = new NodeInfo(assignedUUID, null, "MobileUser");

        String jsonStr = gson.toJson(nodeInfo);
        send(jsonStr);
    }
    /**
     * Constructs and sends a message requesting a service host
     */
    public void hostRequest() {
        Gson gson = new Gson();

        HostRequest serviceRequest = new HostRequest(assignedUUID, desiredServiceName);
        String jsonStr = gson.toJson(serviceRequest);
        send(jsonStr);
    }

    @Override
    public void onError(Exception ex) {
        System.err.println("an error occurred:" + ex);
    }

    /**
     * Send a string message to the service host
     * @param message
     */
    public void sendMessageOnNodeSocket(String message){

        nodeSocketController.send(message);
    }
    /**
     * Send a File to the service host
     * @param file
     */
    public void sendFileOnNodeSocket(File file){
        Gson gson = new Gson();
        send(gson.toJson(file));


    }

    /**
     * this gets any output that the service node has gotten from the service
     * @return String of service output, returns empty string if nothing received
     */
    public String getNodeSocketInformation(){
        if (nodeSocketController==null){
            return "";
        }
    return nodeSocketController.getMessages();
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
