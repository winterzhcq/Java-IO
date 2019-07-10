package cn.winter.demo.bio;

/**
 * BIO服务端代码
 * @author winter
 * @date 2019/6/19 14:12
 */

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {

    // 端口号
    private static int DEFAULT_PORT = 12345;

    //单例的ServerSocke
    private static ServerSocket server;

    // 线程池，实现伪异步IO，但是还是性能上有瓶颈
    private static ExecutorService executorService = Executors.newFixedThreadPool(60);

    //根据传入参数设置监听端口，如果没有参数调用以下方法并使用默认值
    public static void start(){
        // 使用默认值
        start(DEFAULT_PORT);
    }

    //这个方法不会被大量并发访问，不太需要考虑效率，直接进行方法同步就行了，服务器启动一次
    private  static synchronized void start(int port) {
        if (server != null)
            return;
        try {
            //通过构造函数创建ServerSocket
            //如果端口合法且空闲，服务端就监听成功
            server = new ServerSocket(port);
            System.out.println("服务器已经启动，端口号为"+port);
            Socket socket;
            //通过无线循环监听客户端连接
            //如果没有客户端接入，将阻塞在accept操作上,BIO的性能瓶颈之一
            while (true){
                 socket = server.accept();
                //当有新的客户端接入时，会执行下面的代码
                //然后创建一个新的线程处理这条Socket链路
                executorService.execute(new ServerHandler(socket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (server != null){
                    server.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                    start();
            }
        }).start();
    }

}

