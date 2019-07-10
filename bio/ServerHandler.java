package cn.winter.demo.bio;

/**
 * 处理客户端发过来的消息
 * @author winter
 * @date 2019/6/19 14:13
 */
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ServerHandler implements Runnable{
    private Socket socket;

    public ServerHandler(Socket socket) {
        this.socket = socket;
    }

    public void run() {
        BufferedReader in = null;
        PrintWriter out = null;
        String msg = null;
        try {
             // 要求双方都建立正常的tcp链接，如果有一方关闭连接会报错
            // 接收客户端的消息
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            // 给予客户端回应
            out = new PrintWriter(socket.getOutputStream(),true);
            while ((msg = in.readLine()) != null && msg.length()!=0 ) {
                //当连接成功后在此等待接收消息（挂起，进入阻塞状态）
                System.out.println("server received : " + msg);
                out.print("received~\n");
                out.flush();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (in != null)
                    in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                if (out != null)
                    out.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
