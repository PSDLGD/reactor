package com.beinglee.reactor.service.handler;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 *
 */
@Slf4j
public class MultiThreadHandler extends BaseHandler {

    private Executor executor = Executors.newSingleThreadExecutor();

    public MultiThreadHandler(Selector selector, SocketChannel sc) throws IOException {
        super(selector, sc);
    }

    private final Object lock = new Object();


    @Override
    public void run() {
        synchronized (lock) {
            executor.execute(this::handle);
        }
    }
}
