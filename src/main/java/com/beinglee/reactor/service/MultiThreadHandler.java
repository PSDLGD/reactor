package com.beinglee.reactor.service;

import lombok.extern.slf4j.Slf4j;

import java.io.EOFException;
import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author zhanglu
 * @date 2019/12/16 14:01
 */
@Slf4j
public class MultiThreadHandler extends BasicHandler {

    private ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    public MultiThreadHandler(Selector selector, SocketChannel sc) throws IOException {
        super(selector, sc);
    }

    private final Object lock = new Object();

    private static final int PROCESSING = 4;

    @Override
    protected void doRead() throws IOException {
        synchronized (lock) {
            input.clear();
            int read = socket.read(input);
            if (inputIsComplete(read)) {
                executorService.submit(new Processor());
            }
        }
    }

    class Processor implements Runnable {
        @Override
        public void run() {
            processAndHandOff();
        }
    }

    private void processAndHandOff() {
        synchronized (lock) {
            try {
                process();
            } catch (EOFException e) {
                close();
            }
            state = WRITING;
            sk.interestOps(SelectionKey.OP_WRITE);
            // 这里需要唤醒 Selector，因为当把处理交给 workpool 时，Reactor 线程已经阻塞在 select() 方法了， 注意
            // 此时该通道感兴趣的事件还是 OP_READ，这里将通道感兴趣的事件改为 OP_WRITE，如果不唤醒的话，就只能在
            // 下次select 返回时才能有响应了，当然了也可以在 select 方法上设置超时。
            sk.selector().wakeup();
        }
    }
}
