package com.beinglee.reactor.service.handler;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

/**
 * @author zhanglu
 * @date 2019/12/20 11:58
 */
@Slf4j
public abstract class AbstractHandler implements Handler {

    protected static final int READING = 0, WRITING = 1, CLOSED = 2, EXIT = 3;

    protected ByteBuffer input = ByteBuffer.allocate(1024);
    protected ByteBuffer output = ByteBuffer.allocate(1024);

    protected int state = READING;

    protected StringBuilder request = new StringBuilder();

    protected Selector selector;
    protected SocketChannel socketChannel;
    protected SelectionKey sk;

    public AbstractHandler(Selector selector, SocketChannel sc) throws IOException {
        this.selector = selector;
        this.socketChannel = sc;
        socketChannel.configureBlocking(false);
        sk = socketChannel.register(selector, SelectionKey.OP_READ, this);
        this.selector.wakeup();
    }

    @Override
    public void handle() {
        try {
            if (state == READING) {
                doRead();
            } else if (state == WRITING) {
                doWrite();
            }
        } catch (IOException e) {
            log.error("Handler handle occur IOException", e);
        }

    }

    protected void doRead() throws IOException {
        input.clear();
        int read = socketChannel.read(input);
        if (inputIsComplete(read)) {
            process();
            if (state != CLOSED) {
                sk.interestOps(SelectionKey.OP_WRITE);
            }
        }
    }

    protected void doWrite() throws IOException {
        output.flip();
        if (output.hasRemaining()) {
            socketChannel.write(output);
        }
        output.clear();
        request.delete(0, request.length());
        state = READING;
        sk.interestOps(SelectionKey.OP_READ);
    }

    protected abstract void process() throws IOException;


    @Override
    public boolean inputIsComplete(int read) {
        if (read > 0) {
            input.flip();
            while (input.hasRemaining()) {
                byte ch = input.get();
                // ctrl + c
                if (ch == EXIT) {
                    state = CLOSED;
                    return true;
                } // Enter 输入完毕
                else if (ch == '\r') {
                    state = WRITING;
                    request.append((char) ch);
                    return true;
                }
                request.append((char) ch);
            }
        }
        return false;
    }


}
