package com.baidu.xuper.api;

import com.baidu.xuper.pb.XendorserGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;


public class XendorserClient {
    private final ManagedChannel channel;
    private final XendorserGrpc.XendorserBlockingStub blockingClient;

    public XendorserClient(String target) {
        this.channel = ManagedChannelBuilder.forTarget(target)
                .usePlaintext()
                .directExecutor()
                .build();
        blockingClient = XendorserGrpc.newBlockingStub(channel);
    }

    public void close() {
        channel.shutdownNow();
    }

    XendorserGrpc.XendorserBlockingStub getBlockingClient() {
        return blockingClient;
    }
}
