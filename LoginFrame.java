package window1;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class LoginFrame extends JFrame {
    private JPasswordField passwordText;
    private JButton loginButton;

    public LoginFrame() {
        setTitle("ATM");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent we) {
                NetworkManager.getInstance().closeConnection();
                System.exit(0);
            }
        });

        JPanel panel = new JPanel();
        JPanel topPanel = new JPanel();
        JPanel bottomPanel = new JPanel();

        JLabel passwordLabel = new JLabel("Enter your password here:");
        passwordText = new JPasswordField(20);

        loginButton = new JButton("Login");
        loginButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                NetworkManager networkManager = NetworkManager.getInstance();
                String password = new String(passwordText.getPassword());
//                if (password.equals("")){
//                    JOptionPane.showMessageDialog(null, "密码不能为空，请重试！", "错误", JOptionPane.ERROR_MESSAGE);
//                    return;
//                }
                networkManager.getInstance().sendMessage("PASS " + password);
                String response = networkManager.receiveMessage();
                if (response.startsWith("525")) {
                    JOptionPane.showMessageDialog(null, "登录成功！");
                    new ServeTable();
                    dispose();
                } else if(response.startsWith("401")){
                    JOptionPane.showMessageDialog(null, "密码错误！", "错误", JOptionPane.ERROR_MESSAGE);
                    passwordText.setText(""); // 清空密码文本框，允许用户重新输入
                }
            }
        });

        panel.setLayout(new GridLayout(3, 2, 5, 5));
        panel.add(passwordLabel);
        panel.add(passwordText);

        topPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        JLabel welcomeLabel = new JLabel("Welcome to our bank!", JLabel.CENTER);
        topPanel.add(welcomeLabel);

        bottomPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        bottomPanel.add(loginButton);

        add(topPanel, BorderLayout.NORTH);
        add(panel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        setSize(400, 240);
        setResizable(false);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    public static void main(String[] args) {
        new LoginFrame();
    }
}