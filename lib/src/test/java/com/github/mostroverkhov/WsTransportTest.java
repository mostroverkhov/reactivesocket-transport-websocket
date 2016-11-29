package com.github.mostroverkhov;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.logging.LogLevel;
import io.reactivex.netty.protocol.http.server.HttpServer;
import org.junit.Ignore;
import org.junit.Test;
import rx.Observable;

/**
 * Created with IntelliJ IDEA.
 * Author: mostroverkhov
 */
public class WsTransportTest {

    @Ignore
    @Test
    public void ws() throws Exception {

        HttpServer<ByteBuf, ByteBuf> server;

        /*Starts a new HTTP server on an ephemeral port.*/
        server = HttpServer.newServer()
                .enableWireLogging("msg-server", LogLevel.DEBUG)
                .start((req, resp) -> {
                               /*If WebSocket upgrade is requested, then accept the request with an echo handler.*/
                    if (req.isWebSocketUpgradeRequested()) {
                        return resp.acceptWebSocketUpgrade(wsConn -> {
                            Observable<WebSocketFrame> in = wsConn.getInput()
                                    .filter(wsf -> wsf instanceof BinaryWebSocketFrame)
                                    .cast(BinaryWebSocketFrame.class)
                                    .map(f -> {
                                        ByteBuf data = f.content();
                                        data.setByte(data.readerIndex(), 1);
                                        return new MessageFrame(data);
                                    });
                            return wsConn.writeAndFlushOnEach(in);
                        });
                    } else {
                                   /*Else send a NOT FOUND response.*/
                        return resp.setStatus(HttpResponseStatus.NOT_FOUND);
                    }
                });

        server.awaitShutdown();

    }
}
