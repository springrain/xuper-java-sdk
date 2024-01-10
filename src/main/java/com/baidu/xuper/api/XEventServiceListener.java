package com.baidu.xuper.api;

import com.baidu.xuper.pb.EventServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;


public class XEventServiceListener {
    private final ManagedChannel channel;
    private final EventServiceGrpc.EventServiceStub eventServiceStub;

    public XEventServiceListener(String target) {
        this.channel = ManagedChannelBuilder.forTarget(target)
                .usePlaintext()
                .directExecutor()
                .build();
        eventServiceStub = EventServiceGrpc.newStub(channel);
    }

    public void close() {
        channel.shutdownNow();
    }
    public EventServiceGrpc.EventServiceStub getEventServiceStub() {
        return eventServiceStub;
    }
}
