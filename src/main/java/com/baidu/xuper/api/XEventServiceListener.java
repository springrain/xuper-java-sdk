package com.baidu.xuper.api;

import com.baidu.xuper.pb.EventServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.concurrent.TimeUnit;


public class XEventServiceListener {
    private final ManagedChannel channel;
    private final EventServiceGrpc.EventServiceStub eventServiceStub;

    public XEventServiceListener(String target,Integer maxInboundMessageSize) {
        this.channel = ManagedChannelBuilder.forTarget(target)
                .usePlaintext()
                .maxInboundMessageSize(maxInboundMessageSize)
                .directExecutor()
                .enableRetry()
                .defaultLoadBalancingPolicy("round_robin")
                .keepAliveTime(10, TimeUnit.SECONDS)
                .keepAliveTimeout(20, TimeUnit.SECONDS)
                .build();
        eventServiceStub = EventServiceGrpc.newStub(channel);
    }

    public void close() {
        channel.shutdownNow();
    }
     EventServiceGrpc.EventServiceStub getEventServiceStub() {
        return eventServiceStub;
    }

    public void subscribe(com.baidu.xuper.pb.EventOuterClass.SubscribeRequest request,
                          io.grpc.stub.StreamObserver<com.baidu.xuper.pb.EventOuterClass.Event> responseObserver) {
        getEventServiceStub().subscribe(request,responseObserver);
    }
}
