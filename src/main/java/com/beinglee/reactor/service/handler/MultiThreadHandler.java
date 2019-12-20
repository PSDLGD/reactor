package com.beinglee.reactor.service.handler;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 *
 */
@Slf4j
public class MultiThreadHandler extends BaseHandler {

    private ExecutorService executorService = Executors.newFixedThreadPool(1);

    public MultiThreadHandler(Selector selector, SocketChannel sc) throws IOException {
        super(selector, sc);
    }

    private final Object lock = new Object();


    @Override
    public void run() {
        synchronized (lock) {
            executorService.execute(this::handle);
        }
    }
}
