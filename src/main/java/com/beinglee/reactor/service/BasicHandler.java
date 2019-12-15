package com.beinglee.reactor.service;

import lombok.extern.slf4j.Slf4j;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

@Slf4j
public class BasicHandler implements Runnable {

    private static final int MAX_IN = 1024;
    private static final int MAX_OUT = 1024;

    private ByteBuffer input = ByteBuffer.allocate(MAX_IN);
    private ByteBuffer output = ByteBuffer.allocate(MAX_OUT);


    private SocketChannel socket;

    private SelectionKey sk;

    private static final int READING = 0, SENDING = 1, CLOSED = 2;

    private int state = READING;

    private StringBuilder request = new StringBuilder();

    public BasicHandler(Selector selector, SocketChannel sc) throws IOException {

        this.socket = sc;
        socket.configureBlocking(false);
        sk = socket.register(selector, READING);
        sk.interestOps(SelectionKey.OP_READ);
        sk.attach(this);

        selector.wakeup();
    }


    @Override
    public void run() {
        try {
            if (state == READING) {
                doRead();
            } else if (state == SENDING) {
                doSend();
            }
        } catch (IOException e) {
            try {
                sk.channel().close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }


    }

    private void doRead() throws IOException {
        input.clear();
        int read = socket.read(input);
        if (inputIsComplete(read)) {
            process();
            sk.interestOps(SelectionKey.OP_WRITE);
        }
    }

    private void process() throws EOFException {
        if (state == SENDING) {
            String requestContent = request.toString();
            byte[] response = requestContent.getBytes(StandardCharsets.UTF_8);
            output.put(response);
        } else if (state == CLOSED) {
            throw new EOFException();
        }
    }

    private Boolean inputIsComplete(int bytes) {
        if (bytes > 0) {
            input.flip();
            while (input.hasRemaining()) {
                byte ch = input.get();
                if (ch == 3) {
                    state = CLOSED;
                    return true;
                } else if (ch == '\r') {

                } else if (ch == '\n') {
                    state = SENDING;
                    return true;
                } else {
                    request.append((char) ch);
                }
            }
        }
        return false;

    }

    private boolean outputIsComplete(int written) {
        if (written <= 0) {
            return true;
        }
        output.clear();
        request.delete(0, request.length());
        return false;
    }

    private void doSend() throws IOException {
        int written = -1;
        output.flip();
        if (output.hasRemaining()) {
            written = socket.write(output);
        }
        if (outputIsComplete(written)) {
            sk.channel().close();
        } else {
            state = READING;
            socket.write(ByteBuffer.wrap("\r\nreactor> ".getBytes()));
            sk.interestOps(SelectionKey.OP_READ);
        }

    }
}
