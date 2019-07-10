package cn.winter.demo.aio.server;

/**
 * AIO的服务端
 * @author winter
 * @date 2019/7/10 13:10
 */
public class Server {
    private static int DEFAULT_PORT = 12345;
    private static AsyncServerHandler serverHandler;
    public volatile static long clientCount;
    public static void start(){
        start(DEFAULT_PORT);
    }

    private static synchronized void start(int port) {
        if (serverHandler != null)
            return;
        serverHandler = new AsyncServerHandler(port);
        new Thread(serverHandler,"Server").start();
    }

    public static void main(String[] args) {
        start();
    }
}
