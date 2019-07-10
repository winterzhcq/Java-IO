package cn.winter.demo.nio;

import com.sun.org.apache.bcel.internal.generic.Select;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;

/**
 *     来自 https://github.com/anxpp/Java-IO
 *     创建NIO服务端的主要步骤如下：
 *
 *     打开ServerSocketChannel，监听客户端连接
 *     绑定监听端口，设置连接为非阻塞模式
 *     创建Reactor线程，创建多路复用器并启动线程
 *     将ServerSocketChannel注册到Reactor线程中的Selector上，监听ACCEPT事件
 *     Selector轮询准备就绪的key
 *     Selector监听到新的客户端接入，处理新的接入请求，完成TCP三次握手，简历物理链路
 *     设置客户端链路为非阻塞模式
 *     将新接入的客户端连接注册到Reactor线程的Selector上，监听读操作，读取客户端发送的网络消息
 *     异步读取客户端消息到缓冲区
 *     对Buffer编解码，处理半包消息，将解码成功的消息封装成Task
 *     将应答消息编码为Buffer，调用SocketChannel的write将消息异步发送给客户端
 * @author winter
 * @date 2019/6/19 17:56
 */
public class ServerHandler implements Runnable {

    private Selector selector = null;
    private ServerSocketChannel serverChannel = null;
    private boolean stop;

    /**
     * 初始化多路复用器，绑定监听端口
     *
     * @param port 指定监听的端口
     */
    public ServerHandler(int port) {
        try {
            // 创建select和channel
            // 首先，通过 Selector.open() 创建一个 Selector 相当于调度员的角色
            selector = Selector.open();
            // 然后，创建一个 ServerSocketChannel，并且向Selector注册，
            // 通过指定SelectionKey.OP_ACCEPT，告诉调度员它关注的是新的请求
            serverChannel = ServerSocketChannel.open();
            // 指定是非阻塞模式，如果是阻塞模式，注册操作不允许，会抛出IllegalBlockingModeException异常
            serverChannel.configureBlocking(false);
            // 绑定端口 backlog设为1024
            serverChannel.socket().bind(new InetSocketAddress(port), 1024);
            // 注册到 Selector，并说明关注点，监听客户端连接请求
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);
            System.out.println("服务器监听" + port);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }


    }

    public void stop() {
        this.stop = true;
    }

    public void run() {
        // 循环遍历selector
        while (!stop) {
            try {
                // 阻塞等待就绪的 Channel，这是关键点之一
                selector.select(1000);
                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                Iterator<SelectionKey> it = selectionKeys.iterator();
                SelectionKey key = null;
                while (it.hasNext()) {
                    key = it.next();
                    it.remove();
                    try {
                        // 生产系统中一般会额外进行就绪状态检查
                        handleInput(key);
                    } catch (IOException e) {
                        if (key != null) {
                            key.cancel();
                            if (key.channel() != null)
                                key.channel().close();
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        if (selector != null) {
            try {
                selector.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 处理事件
     * @param key
     * @throws IOException
     */
    private void handleInput(SelectionKey key) throws IOException {
        if (key.isValid()) {
            // 处理新接入的消息
            if (key.isAcceptable()) {
                ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
                // 通过ServerSocketChannel的accept创建SocketChannel实例
                // 完成该操作意味着完成TCP三次握手，TCP物理链路正式建立
                SocketChannel sc = ssc.accept();
                // 设置为非阻塞的
                sc.configureBlocking(false);
                // 注册为读
                sc.register(selector, SelectionKey.OP_READ);
            }
            // 读消息
            if (key.isReadable()) {
                SocketChannel sc = (SocketChannel) key.channel();
                // 创建ByteBuffer，并开辟一个1M的缓冲区
                ByteBuffer readBuff = ByteBuffer.allocate(1024);
                //非阻塞的读取请求码流，返回读取到的字节数
                int read = sc.read(readBuff);
                // 读取到字节，对字节进行编解码
                if (read > 0) {
                    // 将缓冲区当前的limit设置为position=0，用于后续对缓冲区的读取操作
                    readBuff.flip();
                    // 根据缓冲区可读字节数创建字节数组
                    byte[] bytes = new byte[readBuff.remaining()];
                    // 将缓冲区可读字节数组复制到新建的数组中
                    readBuff.get(bytes);
                    String body = new String(bytes, "utf-8");
                    System.out.println("服务收到消息：" + body);
                    String currentTime = new Date(System.currentTimeMillis()).toString();
                    // 发送应答消息
                    doWrite(sc, currentTime);
                } else if (read < 0) {
                    key.cancel();
                    sc.close();
                } else {
                }
            }
        }
    }

    /**
     * 异步发送应答消息
     * @param sc
     * @param content
     * @throws IOException
     */
    private void doWrite(SocketChannel sc, String content) throws IOException {
        if (content != null && content.trim().length() > 0) {
            // 将消息编码为字节数组
            byte[] bytes = content.getBytes();
            // 根据数组容量创建ByteBuffer
            ByteBuffer byteBuffer = ByteBuffer.allocate(bytes.length);
            // 将字节数组复制到缓冲区
            byteBuffer.put(bytes);
            // flip操作
            byteBuffer.flip();
            // 发送缓冲区的字节数组
            sc.write(byteBuffer);
        }
    }
}