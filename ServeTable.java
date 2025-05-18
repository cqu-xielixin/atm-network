package window1;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class ServeTable extends JFrame {
    public ServeTable(){
        setTitle("ATM");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        addWindowListener(new WindowAdapter(){
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });

        //创建gui
        JPanel westPanel = new JPanel();
        JPanel eastPanel = new JPanel();

        //westPanel作为功能切换板块
        JButton balance = new JButton("Balance");
        JButton withdraw = new JButton("Withdraw");
        JButton exit = new JButton("Exit");
        JButton confirm = new JButton("Confirm");
        //eastPanel作为显示面板
        JLabel welcomeLabel = new JLabel("Login successfully！", JLabel.LEFT);
        JLabel guidingLabel = new JLabel("What do you want to do?",JLabel.LEFT);

        //设置label位置
        eastPanel.setLayout(new GridLayout(2,1,5,5));
        eastPanel.add(welcomeLabel);
        eastPanel.add(guidingLabel);
        //设置按钮位置
        westPanel.setLayout(new GridLayout(3,1,5,5));
        westPanel.add(balance);
        westPanel.add(withdraw);
        westPanel.add(exit);

        add(westPanel,BorderLayout.WEST);
        add(eastPanel,BorderLayout.EAST);
        //设置窗口大小并居中
        setSize(400, 240);
        setResizable(false);
        setLocationRelativeTo(null);
        setVisible(true);

        //功能代码

        //读取数据


        //添加事件监听器
        balance.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                NetworkManager networkManager = NetworkManager.getInstance();
                networkManager.sendMessage("BALA");
                String response = networkManager.receiveMessage();

                eastPanel.removeAll();
                JLabel balanceLabel = new JLabel(response, JLabel.LEFT);
                eastPanel.add(balanceLabel);

                // 重新布局
                eastPanel.revalidate();
                eastPanel.repaint();
                add(eastPanel,BorderLayout.CENTER);
            }
        });
        withdraw.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                //创建布局
                eastPanel.removeAll();
                JTextField withdrawText;
                JLabel withdrawLabel = new JLabel("Enter your withdraw amount here:");
                withdrawText = new JTextField(20);

                eastPanel.setLayout(new GridLayout(3,1,5,5));
                eastPanel.add(withdrawLabel);
                eastPanel.add(withdrawText);
                eastPanel.add(confirm);

                // 重新布局
                eastPanel.revalidate();
                eastPanel.repaint();
                add(eastPanel,BorderLayout.CENTER);

                confirm.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent ae) {
                        NetworkManager networkManager = NetworkManager.getInstance();
                        String withdraw = withdrawText.getText();
                        networkManager.getInstance().sendMessage("WDRA " + withdraw);
                        System.out.println("WDRA " + withdraw);
                        String response = networkManager.receiveMessage();
                        if (response.startsWith("525")) {
                            JOptionPane.showMessageDialog(null, "取款成功！");
                        }
                        else if(response.startsWith("401")){
                            JOptionPane.showMessageDialog(null, "余额不足！", "错误", JOptionPane.ERROR_MESSAGE);
                        }
                        else{
                            JOptionPane.showMessageDialog(null, "取款失败！请联系服务人员！", "错误", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                });


            }
        });
        exit.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                NetworkManager networkManager = NetworkManager.getInstance();
                networkManager.sendMessage("BYE");
                dispose();
            }
        });

    }

    public static void main(String[] args) {
        new ServeTable();
    }
}