package com.beinglee.reactor.service;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

/**
 * 主从多线程模型
 */
@Slf4j
public class MultiWorkThreadReactor implements Runnable {

    private Selector selector;
    private ServerSocketChannel serverSocket;
    private final int workCount = 3;
    private SubReactor[] workThreadHandlers = new SubReactor[workCount];
    private volatile int nextHandler = 0;


    public MultiWorkThreadReactor(int port) throws IOException {
        selector = Selector.open();
        serverSocket = ServerSocketChannel.open();
        serverSocket.socket().bind(new InetSocketAddress(port));
        serverSocket.configureBlocking(false);
        serverSocket.register(selector, SelectionKey.OP_ACCEPT);
        this.init();
    }

    public void init() {
        nextHandler = 0;
        for (int i = 0; i < workThreadHandlers.length; i++) {
            try {
                workThreadHandlers[i] = new SubReactor();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    private synchronized void dispatch(SocketChannel sc) throws IOException {
        SubReactor work = workThreadHandlers[nextHandler];
        work.registerChannel(sc);
        nextHandler++;
        if (nextHandler >= workThreadHandlers.length) {
            nextHandler = 0;
        }
        Thread t = new Thread(work);
        t.start();
    }


    @Override
    public void run() {
        try {
            while (!Thread.interrupted()) {
                selector.select();
                SocketChannel sc = serverSocket.accept();
                if (sc != null) {
                    this.dispatch(sc);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    class SubReactor implements Runnable {

        private final Selector selector;

        public SubReactor() throws IOException {
            selector = Selector.open();
        }

        public void registerChannel(SocketChannel sc) throws IOException {
            new BasicHandler(selector, sc);
        }

        @Override
        public void run() {
            while (!Thread.interrupted()) {
                try {
                    selector.select();
                    Set<SelectionKey> keys = selector.selectedKeys();
                    Iterator<SelectionKey> iterator = keys.iterator();
                    while (iterator.hasNext()) {
                        SelectionKey key = iterator.next();
                        Runnable r = (Runnable) key.attachment();
                        r.run();
                        iterator.remove();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
