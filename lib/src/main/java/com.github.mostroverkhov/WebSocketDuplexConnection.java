package com.github.mostroverkhov;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.util.ReferenceCountUtil;
import io.reactivesocket.DuplexConnection;
import io.reactivesocket.Frame;
import io.reactivex.netty.protocol.http.ws.WebSocketConnection;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

import static rx.RxReactiveStreams.toObservable;
import static rx.RxReactiveStreams.toPublisher;

/**
 * Created with IntelliJ IDEA.
 * Author: mostroverkhov
 */
public class WebSocketDuplexConnection implements DuplexConnection {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketDuplexConnection.class);

    private final WebSocketConnection wsConn;
    private final MutableDirectByteBuf buffer = new MutableDirectByteBuf(Unpooled.buffer(0));
    private final Frame frame = Frame.allocate(buffer);

    public WebSocketDuplexConnection(WebSocketConnection wsConn) {
        this.wsConn = wsConn;
    }

    @Override
    public double availability() {
        return 1L;
    }

    @Override
    public Publisher<Void> send(Publisher<Frame> frameO) {
        return toPublisher(wsConn.writeAndFlushOnEach(toObservable(frameO)
                .map(frame -> {
                    logger.info("Sending RS Frame" + frame);
                    ByteBuffer data = frame.getByteBuffer();
                    ByteBuf byteBuf = Unpooled.wrappedBuffer(data);
                    return new BinaryWebSocketFrame(byteBuf);
                })));
    }

    @Override
    public Publisher<Frame> receive() {

        return toPublisher(wsConn.getInput().map(wsFrame -> {
            ByteBuf content = wsFrame.content();
            try {

                buffer.wrap(content);
                frame.wrap(buffer, 0);
                logger.info("Received RS Frame: " + frame);
                return frame;

            } catch (Exception e) {
                logger.error("Receive error" + e);
                throw e;
            }

        }));
    }

    @Override
    public Publisher<Void> close() {
        return toPublisher(wsConn.close());
    }

    @Override
    public Publisher<Void> onClose() {
        return toPublisher(wsConn.closeListener());
    }
}
