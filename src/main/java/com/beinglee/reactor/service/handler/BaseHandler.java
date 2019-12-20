package com.beinglee.reactor.service.handler;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

/**
 * @author zhanglu
 * @date 2019/12/20 14:11
 */
@Slf4j
public class BaseHandler extends AbstractHandler implements Runnable {


    public BaseHandler(Selector selector, SocketChannel sc) throws IOException {
        super(selector, sc);
    }

    @Override
    protected void process() throws IOException {
        if (state == CLOSED) {
            sk.channel().close();
        } else if (state == WRITING) {
            String reqContent = request.toString();
            String response = "Received: " + reqContent + "\nreactor> ";
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            output.put(bytes);
        }
    }

    @Override
    public void run() {
        handle();
    }
}
