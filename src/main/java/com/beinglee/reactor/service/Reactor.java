package com.beinglee.reactor.service;

import com.beinglee.reactor.service.handler.BaseHandler;
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
 * 单一Reactor模式
 * 单线程Reactor(BaseHandler):即整个过程只有一个Reactor线程负责全部的Dispatcher和Handler的工作.
 * 多线程Reactor(MultiWorkThreadReactor):一个Reactor线程负责Dispatcher,Handler操作交给线程池处理.
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

    /**
     * 1 若是连接事件 获取的是Accepter
     * 2 若是IO读写事件 获取的是Handler
     */
    private void dispatch(SelectionKey key) {
        Runnable r = (Runnable) key.attachment();
        if (r != null) {
            r.run();
        }
    }

    /**
     * 连接事件就绪,处理连接事件。
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
                    // 单线程
                    new BaseHandler(selector, sc);
                    // 多线程
//                    new MultiThreadHandler(selector, sc);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
