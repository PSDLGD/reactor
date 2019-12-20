package com.beinglee.reactor.service.handler;

import com.beinglee.reactor.service.handler.BaseHandler;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.channels.SelectionKey;
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
    protected void doRead() throws IOException {
        synchronized (lock) {
            input.clear();
            int read = socketChannel.read(input);
            if (inputIsComplete(read)) {
                executorService.execute(new Processor());
            }
        }
    }

    class Processor implements Runnable {
        @Override
        public void run() {
            try {
                process();
                if (state != CLOSED) {
                    sk.interestOps(SelectionKey.OP_WRITE);
                }
            } catch (IOException e) {
                log.error("MultiThreadHandler process occur error.", e);
            }
        }
    }
}
