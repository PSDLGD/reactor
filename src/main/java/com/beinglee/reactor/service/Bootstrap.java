package com.beinglee.reactor.service;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
public class Bootstrap {

    public static void main(String[] args) {
        try {
            Reactor reactor = new Reactor(10393);
            Thread t = new Thread(reactor);
            t.start();
            log.info("Server start.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
