package com.github.mostroverkhov.server;

import com.github.mostroverkhov.WebSocketDuplexConnection;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.logging.LogLevel;
import io.reactivesocket.transport.TransportServer;
import io.reactivex.netty.protocol.http.server.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.RxReactiveStreams;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA.
 * Author: mostroverkhov
 */
@SuppressWarnings("Duplicates")
public class WsTransportServer implements TransportServer {

    private static final Logger logger = LoggerFactory.getLogger(WsTransportServer.class);

    private final HttpServer<ByteBuf, ByteBuf> server;

    private WsTransportServer(SocketAddress socketAddress) {
        this.server = HttpServer.newServer(socketAddress);
    }

    public static WsTransportServer create(SocketAddress address) {
        return new WsTransportServer(address);
    }

    public static WsTransportServer create(String host, int port) {
        return new WsTransportServer(new InetSocketAddress(host, port));
    }

    @Override
    public StartedServer start(ConnectionAcceptor acceptor) {

        server.enableWireLogging("msg-server", LogLevel.DEBUG)
                .start((req, resp) -> {
                    logger.info("Server http request: " + req);
                    if (req.isWebSocketUpgradeRequested()) {
                        return resp.acceptWebSocketUpgrade(wsConn -> {
                            logger.info("Ws handler ready");
                            WebSocketDuplexConnection duplexConn = new WebSocketDuplexConnection(wsConn);
                            return RxReactiveStreams.toObservable(acceptor.apply(duplexConn));
                        });
                    } else {
                        return resp.setStatus(HttpResponseStatus.NOT_FOUND);
                    }
                });

        return new StartedServer() {
            @Override
            public SocketAddress getServerAddress() {
                return server.getServerAddress();
            }

            @Override
            public int getServerPort() {
                return server.getServerPort();
            }

            @Override
            public void awaitShutdown() {
                server.awaitShutdown();
            }

            @Override
            public void awaitShutdown(long duration, TimeUnit durationUnit) {
                server.awaitShutdown(duration, durationUnit);
            }

            @Override
            public void shutdown() {
                server.shutdown();
            }
        };
    }
}
