package window1;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Client {
    public static void main(String[] args) {
        try {
            // 创建Socket对象，指定服务端的IP地址和端口号
            Socket socket = new Socket("192.168.43.232", 2525);
            System.out.println("程序开始运行");

            // 获取输入流和输出流 输入流和输出流是通过socket对象来进行数据传输的。
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            // 从控制台读取用户输入
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            String message;

            while (true) {
                message = reader.readLine();

                if (message.equalsIgnoreCase("exit")) {
                    out.println("exit");
                    break;
                }

                out.println(message);

                String response = in.readLine();
                System.out.println("服务端响应：" + response);
            }

            // 关闭连接
            socket.close();
        } catch (IOException e) {
            System.err.println("连接失败：" + e.getMessage());
            e.printStackTrace();
        }
    }
}