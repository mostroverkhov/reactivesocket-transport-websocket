package com.github.mostroverkhov.server;

import com.github.mostroverkhov.WebSocketDuplexConnection;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.logging.LogLevel;
import io.reactivesocket.Frame;
import io.reactivesocket.transport.TransportServer;
import io.reactivex.netty.protocol.http.server.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.Observer;
import rx.RxReactiveStreams;
import rx.Subscription;
import rx.subscriptions.Subscriptions;

import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA.
 * Author: mostroverkhov
 */
@SuppressWarnings("Duplicates")
public class WsTransportServer implements TransportServer {

    private static final Logger logger = LoggerFactory.getLogger(Frame.class);

    private final HttpServer<ByteBuf, ByteBuf> server;

    public WsTransportServer(int port) {
        server = HttpServer.newServer(port);

    }

    public static WsTransportServer create(int port) {
        return new WsTransportServer(port);
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
                            Observable<Void> connTerminate = RxReactiveStreams.toObservable(acceptor.apply(duplexConn));

                            return Observable.create(subscriber -> {
                                if (!subscriber.isUnsubscribed()) {
                                    Subscription subscription = connTerminate.subscribe(new Observer<Void>() {
                                        @Override
                                        public void onCompleted() {
                                            if (!subscriber.isUnsubscribed()) {
                                                subscriber.onCompleted();
                                            }
                                        }

                                        @Override
                                        public void onError(Throwable e) {

                                        }

                                        @Override
                                        public void onNext(Void aVoid) {
                                        }
                                    });
                                    subscriber.add(Subscriptions.create(subscription::unsubscribe));
                                }
                            });
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
