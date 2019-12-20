package com.beinglee.reactor.service.handler;

/**
 * @author zhanglu
 * @date 2019/12/20 11:50
 */
public interface Handler {


    void handle();

    boolean inputIsComplete(int bytes);

}
