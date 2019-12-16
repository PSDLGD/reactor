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

    protected ByteBuffer input = ByteBuffer.allocate(MAX_IN);
    protected ByteBuffer output = ByteBuffer.allocate(MAX_OUT);

    protected SocketChannel socket;

    protected SelectionKey sk;

    protected static final int READING = 0, WRITING = 1, CLOSED = 2;

    public static int EXIT = 3;

    protected int state = READING;

    protected StringBuilder request = new StringBuilder();

    public BasicHandler(Selector selector, SocketChannel sc) throws IOException {
        this.socket = sc;
        socket.configureBlocking(false);
        sk = socket.register(selector, SelectionKey.OP_READ, this);
        selector.wakeup();
    }


    @Override
    public void run() {
        try {
            if (state == READING) {
                doRead();
            } else if (state == WRITING) {
                doWrite();
            }
        } catch (IOException e) {
            close();
        }
    }

    protected void doRead() throws IOException {
        input.clear();
        int read = socket.read(input);
        if (inputIsComplete(read)) {
            process();
            sk.interestOps(SelectionKey.OP_WRITE);
        }
    }

    private void doWrite() throws IOException {
        int written = -1;
        output.flip();
        if (output.hasRemaining()) {
            written = socket.write(output);
        }
        if (outputIsComplete(written)) {
            close();
        } else {
            state = READING;
            socket.write(ByteBuffer.wrap("\r\nreactor> ".getBytes()));
            sk.interestOps(SelectionKey.OP_READ);
        }
    }

    protected void process() throws EOFException {
        if (state == WRITING) {
            String requestContent = request.toString();
            byte[] response = requestContent.getBytes(StandardCharsets.UTF_8);
            output.put(response);
        } else if (state == CLOSED) {
            throw new EOFException();
        }
    }

    /**
     * 按ctrl+c客户端退出
     */
    protected Boolean inputIsComplete(int bytes) {
        if (bytes > 0) {
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

    private boolean outputIsComplete(int written) {
        if (written <= 0) {
            return true;
        }
        output.clear();
        request.delete(0, request.length());
        return false;
    }

    protected void close() {
        try {
            sk.channel().close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
