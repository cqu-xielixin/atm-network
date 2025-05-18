package window1;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class CardInsertionFrame extends JFrame {
    private JTextField  usernameText;

    public CardInsertionFrame() {
        setTitle("ATM");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent we) {
                NetworkManager.getInstance().closeConnection();
                System.exit(0);
            }
        });

        JPanel topPanel = new JPanel();
        JPanel centerPanel = new JPanel();
        JPanel bottomPanel = new JPanel();

        JLabel welcomeLabel = new JLabel("Welcome to our bank!", JLabel.CENTER);
        topPanel.add(welcomeLabel);

        JLabel usernameLabel = new JLabel("Enter your username here:");
        usernameText = new JTextField (20);

        centerPanel.setLayout(new GridLayout(3, 2, 5, 5));
        centerPanel.add(usernameLabel);
        centerPanel.add(usernameText);

        JButton okButton = new JButton("OK");
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                NetworkManager networkManager = NetworkManager.getInstance();
                String username = usernameText.getText();
                if (username.equals("")){
                    JOptionPane.showMessageDialog(null, "账号不能为空，请重试！", "错误", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                networkManager.getInstance().sendMessage("HELO " + username);
                System.out.println("HELO " + username);
                String response = networkManager.receiveMessage();
                if (response.startsWith("500")) {
                    new LoginFrame();
                    dispose();
                } else {
                    JOptionPane.showMessageDialog(null, "账号错误，请重试！", "错误", JOptionPane.ERROR_MESSAGE);
                    usernameText.setText(""); // 清空用户名文本框，允许用户重新输入
                }
            }
        });
        bottomPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        bottomPanel.add(okButton);

        add(topPanel, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        setSize(400, 240);
        setResizable(false);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    public static void main(String[] args) {
        try {
            // 设置 Nimbus 外观
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (Exception e) {
            e.printStackTrace();
        }
        new CardInsertionFrame();
    }
}