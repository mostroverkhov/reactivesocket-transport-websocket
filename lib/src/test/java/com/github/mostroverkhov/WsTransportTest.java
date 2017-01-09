package com.github.mostroverkhov;

import com.github.mostroverkhov.client.WsTransportClient;
import com.github.mostroverkhov.server.WsTransportServer;
import io.reactivesocket.AbstractReactiveSocket;
import io.reactivesocket.ConnectionSetupPayload;
import io.reactivesocket.Payload;
import io.reactivesocket.ReactiveSocket;
import io.reactivesocket.client.ReactiveSocketClient;
import io.reactivesocket.frame.ByteBufferUtil;
import io.reactivesocket.lease.DisabledLeaseAcceptingSocket;
import io.reactivesocket.lease.LeaseEnforcingSocket;
import io.reactivesocket.server.ReactiveSocketServer;
import io.reactivesocket.transport.TransportServer;
import io.reactivesocket.util.PayloadImpl;
import io.reactivex.Flowable;
import org.junit.Test;
import org.reactivestreams.Publisher;

import java.net.SocketAddress;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static io.reactivesocket.client.KeepAliveProvider.never;
import static io.reactivesocket.client.SetupProvider.keepAlive;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * Created with IntelliJ IDEA.
 * Author: mostroverkhov
 */
public class WsTransportTest {
    @Test
    public void clientServer() throws Exception {
        WsTransportServer wsTransportServer = WsTransportServer.create(8025);
        TransportServer.StartedServer server = ReactiveSocketServer.create(wsTransportServer)
                .start(new SocketAcceptorImpl());

        SocketAddress address = server.getServerAddress();
        ReactiveSocket client = Flowable.fromPublisher(ReactiveSocketClient.create(WsTransportClient.create(address),
                keepAlive(never()).disableLease())
                .connect())
                .blockingFirst();

        List<String> responses = Flowable.fromPublisher(client.requestStream(new PayloadImpl("Hello")))
                .map(Payload::getData)
                .map(ByteBufferUtil::toUtf8String)
                .doOnNext(System.out::println)
                .take(10)
                .concatWith(Flowable.fromPublisher(client.close()).cast(String.class))
                .toList().blockingGet();

        assertEquals(10, responses.size());
        responses.forEach(r -> assertFalse(r.isEmpty()));
    }

    private static class SocketAcceptorImpl implements ReactiveSocketServer.SocketAcceptor {
        @Override
        public LeaseEnforcingSocket accept(ConnectionSetupPayload setupPayload, ReactiveSocket reactiveSocket) {
            return new DisabledLeaseAcceptingSocket(new AbstractReactiveSocket() {
                @Override
                public Publisher<Payload> requestStream(Payload payload) {
                    return Flowable.interval(100, TimeUnit.MILLISECONDS)
                            .map(n -> new PayloadImpl("Interval: " + n));
                }
            });
        }
    }
}
