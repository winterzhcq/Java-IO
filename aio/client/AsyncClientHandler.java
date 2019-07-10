package cn.winter.demo.aio.client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.CountDownLatch;

/**
 * @author winter
 * @date 2019/7/10 14:11
 */
public class AsyncClientHandler implements CompletionHandler<Void, AsyncClientHandler>,Runnable {
    private AsynchronousSocketChannel clientChannel;
    private String host;
    private int port;
    private CountDownLatch latch;
    public AsyncClientHandler(String ip, int port) {
       this.host = ip;
       this.port = port;
        try {
            clientChannel = AsynchronousSocketChannel.open();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        //创建CountDownLatch等待
        latch = new CountDownLatch(1);
        //发起异步连接操作，回调参数就是这个类本身，如果连接成功会回调completed方法
        clientChannel.connect(new InetSocketAddress(host, port), this,this);
        try {
            latch.await();
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }
        try {
            clientChannel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMsg(String msg) {
        byte[] req = msg.getBytes();
        ByteBuffer writeBuffer = ByteBuffer.allocate(req.length);
        writeBuffer.put(req);
        writeBuffer.flip();
        //异步写
        clientChannel.write(writeBuffer, writeBuffer,new WriteHandler(clientChannel, latch));
    }

    // 连接服务器成功
    // 意味着TCP三次握手完成
    @Override
    public void completed(Void result, AsyncClientHandler attachment) {
        System.out.println("客户端成功连接到服务器...");
    }

    @Override
    public void failed(Throwable exc, AsyncClientHandler attachment) {
        System.err.println("连接服务器失败...");
        exc.printStackTrace();
        try {
            clientChannel.close();
            latch.countDown();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
