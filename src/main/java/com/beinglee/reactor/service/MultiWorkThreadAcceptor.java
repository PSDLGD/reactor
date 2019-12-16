package com.beinglee.reactor.service;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

/**
 * @author zhanglu
 * @date 2019/12/16 18:23
 */
public class MultiWorkThreadAcceptor implements Runnable {

    private Selector selector;
    private ServerSocketChannel serverSocket;
    private final int workCount = 3;
    private SubReactor[] workThreadHandlers = new SubReactor[workCount];
    private volatile int nextHandler = 0;


    public MultiWorkThreadAcceptor(int port) throws IOException {
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


    @Override
    public void run() {
        try {
            while (true) {
                selector.select();
                SocketChannel sc = serverSocket.accept();
                if (sc != null) {
                    synchronized (sc) {
                        SubReactor work = workThreadHandlers[nextHandler];
                        work.registerChannel(sc);
                        nextHandler++;
                        if (nextHandler >= workThreadHandlers.length) {
                            nextHandler = 0;
                        }
                    }
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class SubReactor implements Runnable {

        private final Selector selector;

        public SubReactor() throws IOException {
            selector = Selector.open();
        }

        public void registerChannel(SocketChannel sc) throws IOException {
            sc.register(this.selector, SelectionKey.OP_READ | SelectionKey.OP_CONNECT);
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
                        iterator.remove();
                        if (key.isReadable()) {
                            doRead();
                        } else if (key.isWritable()) {
                            doWrite();
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void doRead() {

        }

        private void doWrite() {

        }
    }
}
