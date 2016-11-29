package com.github.mostroverkhov.client;

import com.github.mostroverkhov.WebSocketDuplexConnection;
import io.netty.handler.logging.LogLevel;
import io.reactivesocket.DuplexConnection;
import io.reactivesocket.transport.TransportClient;
import io.reactivex.netty.protocol.http.client.HttpClient;
import io.reactivex.netty.protocol.http.ws.client.WebSocketResponse;
import org.reactivestreams.Publisher;
import rx.Observable;
import rx.RxReactiveStreams;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

/**
 * Created with IntelliJ IDEA.
 * Author: mostroverkhov
 */
public class WsTransportClient implements TransportClient {

    private final SocketAddress socketAddress;

    public WsTransportClient(final String host, int port) {
        this(new InetSocketAddress(host, port));
    }

    public WsTransportClient(SocketAddress address) {
        this.socketAddress = address;
    }

    public static WsTransportClient create(SocketAddress socketAddress) {
        return new WsTransportClient(socketAddress);
    }

    public static WsTransportClient create(final String host, int port) {
        return new WsTransportClient(host, port);
    }

    @Override
    public Publisher<DuplexConnection> connect() {

        Observable<DuplexConnection> wsConn = HttpClient.newClient(socketAddress)
                .enableWireLogging("msging-client", LogLevel.DEBUG)
                .createGet("")
                .requestWebSocketUpgrade()
                .flatMap(WebSocketResponse::getWebSocketConnection)
                .map(WebSocketDuplexConnection::new);

        return RxReactiveStreams.toPublisher(wsConn);
    }
}
