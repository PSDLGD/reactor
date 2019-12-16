package com.beinglee.reactor.service;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Set;

/**
 * 1 客户端发起请求并建立连接，注册Accept。
 */
@Slf4j
public class Reactor implements Runnable {

    private Selector selector;

    private ServerSocketChannel serverSocket;


    public Reactor(int port) throws IOException {
        this.selector = Selector.open();
        serverSocket = ServerSocketChannel.open();
        serverSocket.socket().bind(new InetSocketAddress(port));
        serverSocket.configureBlocking(false);
        serverSocket.register(selector, SelectionKey.OP_ACCEPT, new Accepter());
        log.info("Listening on port:{}", port);
    }


    @Override
    public void run() {
        while (!Thread.interrupted()) {
            try {
                this.selector.select();
                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                for (SelectionKey key : selectionKeys) {
                    this.dispatch(key);
                }
                selectionKeys.clear();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void dispatch(SelectionKey key) {
        Runnable r = (Runnable) key.attachment();
        r.run();
    }

    /**
     * 连接事件就绪,处理连接事件
     */
    class Accepter implements Runnable {
        @Override
        public void run() {
            SocketChannel sc;
            try {
                sc = serverSocket.accept();
                if (sc != null) {
                    sc.write(ByteBuffer.wrap("Implementation of Ractor Design Pattern by BeingLee\r\nreactor>".getBytes()));
                    log.info("Accept and handler -{}", sc.socket().getLocalSocketAddress());
                    new BasicHandler(selector, sc);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
