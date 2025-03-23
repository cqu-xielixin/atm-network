import java.io.*;
import java.net.*;

public class ATMClient {
    private static final String SERVER_ADDRESS = "localhost"; // 服务器地址
    private static final int SERVER_PORT = 2525; // 服务器端口

    public static void main(String[] args) {
        try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            // 发送请求
            out.println("HELO sp <200000>");
            out.println("PASS sp <password>");

            // 接收服务器响应
            String response;
            while ((response = in.readLine()) != null) {
                System.out.println("Server: " + response);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}