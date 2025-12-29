package tools.donationreward;

import java.awt.Color;
import java.awt.Font;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.LineBorder;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.managers.MailManager;
import org.l2jmobius.gameserver.model.Message;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.itemcontainer.Mail;
import org.l2jmobius.gameserver.network.enums.MailType;

public class DonationRewardManagerFrame extends JFrame
{
    private static final long serialVersionUID = 1L;
    
    // 顏色配置
    private static final Color BG_COLOR = new Color(30, 30, 30);
    private static final Color FG_COLOR = new Color(50, 240, 240);
    private static final Color PANEL_BG = new Color(45, 45, 45);
    private static final Color INPUT_BG = new Color(55, 55, 55);
    private static final Color BORDER_COLOR = new Color(100, 100, 100);
    private static final Color BUTTON_BLUE = new Color(70, 130, 180);
    private static final Color BUTTON_BLUE_HOVER = new Color(90, 150, 200);
    private static final Color BUTTON_GREEN = new Color(60, 150, 90);
    private static final Color BUTTON_GREEN_HOVER = new Color(80, 170, 110);
    
    // UI元件
    private JTextField txtCharacterName;
    private JComboBox<DonationTier> cmbTier;
    private JTextArea txtLog;
    private JButton btnSend;
    private JButton btnRefresh;
    
    // 滿額禮設定類
    public static class DonationTier
    {
        private final int amount;
        private final String description;
        private final List<RewardItem> rewards;
        
        public DonationTier(int amount, String description)
        {
            this.amount = amount;
            this.description = description;
            this.rewards = new ArrayList<>();
        }
        
        public void addReward(int itemId, long count)
        {
            rewards.add(new RewardItem(itemId, count));
        }
        
        public int getAmount()
        {
            return amount;
        }
        
        public List<RewardItem> getRewards()
        {
            return rewards;
        }
        
        @Override
        public String toString()
        {
            return amount + " 元 - " + description;
        }
    }
    
    // 獎勵物品類
    public static class RewardItem
    {
        private final int itemId;
        private final long count;
        
        public RewardItem(int itemId, long count)
        {
            this.itemId = itemId;
            this.count = count;
        }
        
        public int getItemId()
        {
            return itemId;
        }
        
        public long getCount()
        {
            return count;
        }
    }
    
    // 滿額禮配置（可以根據需求調整）
    private static final DonationTier[] DONATION_TIERS = initializeTiers();

    private static DonationTier[] initializeTiers()
    {
        // 1000元檔
        DonationTier tier1000 = new DonationTier(1000, "1000滿額");
        tier1000.addReward(93448, 1);         // +16a級武器自選 x1
        tier1000.addReward(101254, 1);        // 英雄人偶卡 x1
        tier1000.addReward(105802, 10);       // 收藏品隨機開啟券 x10
        tier1000.addReward(97145, 50000);     // 古幣 x50000
        tier1000.addReward(105801, 50);       // 武魂魂魄 x50

        // 2000元檔
        DonationTier tier2000 = new DonationTier(2000, "2000滿額");
        tier2000.addReward(101264, 4);        // +10 a級防具交換券x4
        tier2000.addReward(101254, 1);        // 英雄人偶卡 x1
        tier2000.addReward(105802, 20);       // 收藏品隨機開啟券 x20
        tier2000.addReward(97145, 50000);     // 古幣 x50000
        tier2000.addReward(105801, 50);       // 武魂魂魄 x50

        // 3000元檔
        DonationTier tier3000 = new DonationTier(3000, "3000滿額");
        // 無限成長精華9種 x100
        for (int i = 108000; i <= 108008; i++)
        {
            tier3000.addReward(i, 100);
        }
        tier3000.addReward(101255, 1);        // 傳說人偶卡 x1
        tier3000.addReward(101254, 1);        // 英雄人偶卡 x1
        tier3000.addReward(105802, 30);       // 收藏品隨機開啟券 x30
        tier3000.addReward(97145, 50000);     // 古幣 x50000
        tier3000.addReward(105801, 50);       // 武魂魂魄 x50

        // 5000元檔
        DonationTier tier5000 = new DonationTier(5000, "5000滿額");
        // 無限成長精華9種 x200
        for (int i = 108000; i <= 108008; i++)
        {
            tier5000.addReward(i, 200);
        }
        tier5000.addReward(101255, 1);        // 傳說人偶卡 x1
        tier5000.addReward(101254, 1);        // 英雄人偶卡 x1
        tier5000.addReward(105802, 50);       // 收藏品隨機開啟券 x50
        tier5000.addReward(105618, 1);        // +15BOSS武器 x1
        tier5000.addReward(105801, 100);      // 武魂魂魄 x100
        tier5000.addReward(97597, 200);       // 寶石選擇交換券 x200
        tier5000.addReward(97145, 100000);    // 古幣 x100000
        tier5000.addReward(94383, 10);        // 稀有飾品捲 x10

        // 10000元檔
        DonationTier tier10000 = new DonationTier(10000, "10000滿額");
        // 無限成長精華9種 x500
        for (int i = 108000; i <= 108008; i++)
        {
            tier10000.addReward(i, 500);
        }
        tier10000.addReward(101256, 1);       // 神話人偶卡 x1
        tier10000.addReward(101255, 1);       // 傳說人偶卡 x1
        tier10000.addReward(101254, 1);       // 英雄人偶卡 x1
        tier10000.addReward(105802, 50);      // 收藏品隨機開啟券 x50
        tier10000.addReward(105618, 1);       // +15BOSS武器 x1
        tier10000.addReward(105801, 300);     // 武魂魂魄 x300
        tier10000.addReward(97597, 400);      // 寶石選擇交換券 x400
        tier10000.addReward(97145, 250000);   // 古幣 x250000
        tier10000.addReward(94383, 10);       // 稀有飾品捲 x10

        // 15000元檔
        DonationTier tier15000 = new DonationTier(15000, "15000滿額");
        // 無限成長精華9種 x500
        for (int i = 108000; i <= 108008; i++)
        {
            tier15000.addReward(i, 500);
        }
        tier15000.addReward(101256, 1);       // 神話人偶卡 x1
        tier15000.addReward(101255, 1);       // 傳說人偶卡 x1
        tier15000.addReward(101254, 1);       // 英雄人偶卡 x1
        tier15000.addReward(105802, 50);      // 收藏品隨機開啟券 x50
        tier15000.addReward(105618, 1);       // +15BOSS武器 x1
        tier15000.addReward(105801, 300);     // 武魂魂魄 x300
        tier15000.addReward(97597, 400);      // 寶石選擇交換券 x400
        tier15000.addReward(97145, 250000);   // 古幣 x250000
        tier15000.addReward(94383, 10);       // 稀有飾品捲 x10

        // 20000元檔
        DonationTier tier20000 = new DonationTier(20000, "20000滿額");
        // 無限成長精華9種 x500
        for (int i = 108000; i <= 108008; i++)
        {
            tier20000.addReward(i, 500);
        }
        tier20000.addReward(101256, 1);       // 神話人偶卡 x1
        tier20000.addReward(101255, 1);       // 傳說人偶卡 x1
        tier20000.addReward(101254, 1);       // 英雄人偶卡 x1
        tier20000.addReward(105802, 50);      // 收藏品隨機開啟券 x50
        tier20000.addReward(105618, 1);       // +15BOSS武器 x1
        tier20000.addReward(105801, 300);     // 武魂魂魄 x300
        tier20000.addReward(97597, 400);      // 寶石選擇交換券 x400
        tier20000.addReward(97145, 250000);   // 古幣 x250000
        tier20000.addReward(94383, 10);       // 稀有飾品捲 x10

        return new DonationTier[] { tier1000, tier2000, tier3000, tier5000, tier10000, tier15000, tier20000 };
    }
    
    public DonationRewardManagerFrame()
    {
        initializeDatabase();
        initComponents();
    }
    
    private void initializeDatabase()
    {
        // 創建資料表（如果不存在）
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement ps = con.prepareStatement(
                 "CREATE TABLE IF NOT EXISTS donation_rewards (" +
                 "id INT AUTO_INCREMENT PRIMARY KEY, " +
                 "account_name VARCHAR(45) NOT NULL, " +
                 "char_name VARCHAR(35) NOT NULL, " +
                 "donation_amount INT NOT NULL, " +
                 "claim_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                 "UNIQUE KEY unique_claim (account_name, donation_amount)" +
                 ")"))
        {
            ps.execute();
            System.out.println("[累積贊助滿額禮] 資料表檢查完成");
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
    }
    
    private void initComponents()
    {
        setTitle("累積贊助滿額禮管理器");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setBounds(100, 100, 600, 500);
        setResizable(false);
        
        JPanel contentPane = new JPanel();
        contentPane.setBackground(BG_COLOR);
        contentPane.setBorder(new LineBorder(BORDER_COLOR, 1));
        contentPane.setLayout(null);
        setContentPane(contentPane);
        
        // 標題
        JLabel lblTitle = new JLabel("累積贊助滿額禮發送系統");
        lblTitle.setFont(new Font("微軟正黑體", Font.BOLD, 18));
        lblTitle.setForeground(FG_COLOR);
        lblTitle.setBounds(20, 10, 560, 30);
        contentPane.add(lblTitle);
        
        // 角色名稱標籤
        JLabel lblCharName = new JLabel("角色名稱:");
        lblCharName.setFont(new Font("微軟正黑體", Font.PLAIN, 14));
        lblCharName.setForeground(FG_COLOR);
        lblCharName.setBounds(20, 50, 100, 25);
        contentPane.add(lblCharName);
        
        // 角色名稱輸入框
        txtCharacterName = new JTextField();
        txtCharacterName.setFont(new Font("微軟正黑體", Font.PLAIN, 14));
        txtCharacterName.setBackground(INPUT_BG);
        txtCharacterName.setForeground(FG_COLOR);
        txtCharacterName.setCaretColor(FG_COLOR);
        txtCharacterName.setBorder(new LineBorder(BORDER_COLOR, 1));
        txtCharacterName.setBounds(120, 50, 200, 25);
        contentPane.add(txtCharacterName);
        
        // 滿額禮檔次標籤
        JLabel lblTier = new JLabel("滿額禮檔次:");
        lblTier.setFont(new Font("微軟正黑體", Font.PLAIN, 14));
        lblTier.setForeground(FG_COLOR);
        lblTier.setBounds(20, 85, 100, 25);
        contentPane.add(lblTier);

        // 滿額禮下拉選單
        cmbTier = new JComboBox<>();
        cmbTier.setModel(new DefaultComboBoxModel<>(DONATION_TIERS));
        cmbTier.setFont(new Font("微軟正黑體", Font.PLAIN, 14));
        cmbTier.setBackground(INPUT_BG);
        cmbTier.setForeground(FG_COLOR);
        cmbTier.setBounds(120, 85, 300, 25);

// ===== 加入以下這段 =====
        cmbTier.setOpaque(true);  // 確保背景色生效
        cmbTier.setBorder(new LineBorder(BORDER_COLOR, 1));  // 加邊框

// 設定下拉選單的 UI 樣式
        cmbTier.setUI(new javax.swing.plaf.basic.BasicComboBoxUI() {
            @Override
            protected JButton createArrowButton() {
                JButton btn = new JButton("▼");
                btn.setBackground(new Color(40, 40, 40));
                btn.setForeground(FG_COLOR);
                btn.setBorder(new LineBorder(BORDER_COLOR, 0));
                return btn;
            }
        });

// 設定下拉列表的渲染器（確保下拉項目也是暗色）
        cmbTier.setRenderer(new javax.swing.DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(
                    javax.swing.JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {

                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

                setBackground(isSelected ? new Color(70, 130, 180) : INPUT_BG);
                setForeground(FG_COLOR);
                setBorder(new LineBorder(BORDER_COLOR, 1));

                return this;
            }
        });

        contentPane.add(cmbTier);
        contentPane.add(cmbTier);
        
        // 發送按鈕
        btnSend = createButton("發送滿額禮", 120, 120, 150, 35);
        btnSend.addActionListener(e -> sendDonationReward());
        contentPane.add(btnSend);
        
        // 刷新按鈕
        btnRefresh = createButton("刷新記錄", 280, 120, 140, 35);
        btnRefresh.addActionListener(e -> refreshLog());
        contentPane.add(btnRefresh);
        
        // 記錄區域標籤
        JLabel lblLog = new JLabel("發送記錄:");
        lblLog.setFont(new Font("微軟正黑體", Font.BOLD, 14));
        lblLog.setForeground(FG_COLOR);
        lblLog.setBounds(20, 165, 100, 25);
        contentPane.add(lblLog);
        
        // 記錄顯示區域
        txtLog = new JTextArea();
        txtLog.setFont(new Font("微軟正黑體", Font.PLAIN, 12));
        txtLog.setBackground(INPUT_BG);
        txtLog.setForeground(FG_COLOR);
        txtLog.setCaretColor(FG_COLOR);
        txtLog.setEditable(false);
        txtLog.setLineWrap(true);
        txtLog.setWrapStyleWord(true);
        
        JScrollPane scrollPane = new JScrollPane(txtLog);
        scrollPane.setBounds(20, 195, 560, 260);
        scrollPane.setBorder(new LineBorder(BORDER_COLOR, 1));
        contentPane.add(scrollPane);
        
        // 初始加載記錄
        refreshLog();
    }

    private JButton createButton(String text, int x, int y, int width, int height)
    {
        JButton btn = new JButton(text);
        btn.setFont(new Font("微軟正黑體", Font.PLAIN, 12));
        btn.setBounds(x, y, width, height);

        // 強制設定為黑底白字
        btn.setBackground(new Color(40, 40, 40));
        btn.setForeground(Color.WHITE);
        btn.setOpaque(true);
        btn.setContentAreaFilled(true);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);

        // 滑鼠懸停效果
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btn.setBackground(new Color(60, 60, 60));
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                btn.setBackground(new Color(40, 40, 40));
            }

            @Override
            public void mousePressed(java.awt.event.MouseEvent evt) {
                btn.setBackground(new Color(30, 30, 30));
            }

            @Override
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                btn.setBackground(new Color(60, 60, 60));
            }
        });

        return btn;
    }
    
    private void sendDonationReward()
    {
        String charName = txtCharacterName.getText().trim();
        
        if (charName.isEmpty())
        {
            JOptionPane.showMessageDialog(this, "請輸入角色名稱!", "錯誤", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        DonationTier selectedTier = (DonationTier) cmbTier.getSelectedItem();
        if (selectedTier == null)
        {
            JOptionPane.showMessageDialog(this, "請選擇滿額禮檔次!", "錯誤", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // 檢查玩家是否在線
        Player player = World.getInstance().getPlayer(charName);
        if (player == null)
        {
            JOptionPane.showMessageDialog(this, 
                "角色 '" + charName + "' 不在線上或不存在!\n請確認角色名稱是否正確。", 
                "錯誤", 
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        String accountName = player.getAccountName();
        
        // 檢查是否已領取過該檔次
        if (hasClaimedTier(accountName, selectedTier.getAmount()))
        {
            JOptionPane.showMessageDialog(this, 
                "帳號 '" + accountName + "' 已經領取過 " + selectedTier.getAmount() + " 元檔次的滿額禮!", 
                "重複領取", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // 發送郵件
        try
        {
            sendRewardMail(player, selectedTier);
            
            // 記錄到資料庫
            recordClaim(accountName, charName, selectedTier.getAmount());
            
            JOptionPane.showMessageDialog(this, 
                "成功發送 " + selectedTier + " 給 " + charName + "!", 
                "成功", 
                JOptionPane.INFORMATION_MESSAGE);
            
            // 刷新記錄
            refreshLog();
            
            // 清空輸入
            txtCharacterName.setText("");
        }
        catch (Exception e)
        {
            JOptionPane.showMessageDialog(this, 
                "發送失敗: " + e.getMessage(), 
                "錯誤", 
                JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
    
    private boolean hasClaimedTier(String accountName, int amount)
    {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement ps = con.prepareStatement(
                 "SELECT COUNT(*) FROM donation_rewards WHERE account_name = ? AND donation_amount = ?"))
        {
            ps.setString(1, accountName);
            ps.setInt(2, amount);
            
            try (ResultSet rs = ps.executeQuery())
            {
                if (rs.next())
                {
                    return rs.getInt(1) > 0;
                }
            }
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
        
        return false;
    }
    
    private void recordClaim(String accountName, String charName, int amount)
    {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement ps = con.prepareStatement(
                 "INSERT INTO donation_rewards (account_name, char_name, donation_amount) VALUES (?, ?, ?)"))
        {
            ps.setString(1, accountName);
            ps.setString(2, charName);
            ps.setInt(3, amount);
            ps.executeUpdate();
            
            System.out.println("[累積贊助滿額禮] 記錄領取: " + accountName + " / " + charName + " / " + amount + "元");
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
    }
    
    private void sendRewardMail(Player player, DonationTier tier)
    {
        String subject = "累積贊助滿額禮 - " + tier.getAmount() + "元檔";
        String content = "親愛的玩家您好:\n\n" +
                        "感謝您對本伺服器的支持與贊助!\n\n" +
                        "您已達成累積贊助 " + tier.getAmount() + " 元的里程碑,\n" +
                        "特別贈送 " + tier.description + " 以表謝意。\n\n" +
                        "請查收附件中的獎勵道具。\n\n" +
                        "祝您遊戲愉快!\n" +
                        "武魂天堂2 營運團隊";
        
        Message msg = new Message(player.getObjectId(), subject, content, MailType.NEWS_INFORMER);
        Mail attachments = msg.createAttachments();
        
        // 添加所有獎勵物品
        for (RewardItem reward : tier.getRewards())
        {
            attachments.addItem(ItemProcessType.REWARD, reward.getItemId(), reward.getCount(), null, null);
        }
        
        MailManager.getInstance().sendMessage(msg);
        
        System.out.println("[累積贊助滿額禮] 已發送郵件給 " + player.getName() + " (帳號: " + player.getAccountName() + ")");
    }
    
    private void refreshLog()
    {
        StringBuilder sb = new StringBuilder();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement ps = con.prepareStatement(
                 "SELECT account_name, char_name, donation_amount, claim_date " +
                 "FROM donation_rewards ORDER BY claim_date DESC LIMIT 50"))
        {
            try (ResultSet rs = ps.executeQuery())
            {
                sb.append("=== 最近 50 筆發送記錄 ===\n\n");
                
                int count = 0;
                while (rs.next())
                {
                    count++;
                    String accountName = rs.getString("account_name");
                    String charName = rs.getString("char_name");
                    int amount = rs.getInt("donation_amount");
                    Date claimDate = rs.getTimestamp("claim_date");
                    
                    sb.append(String.format("[%d] %s | 帳號: %s | 角色: %s | 檔次: %d 元\n", 
                        count, sdf.format(claimDate), accountName, charName, amount));
                }
                
                if (count == 0)
                {
                    sb.append("目前還沒有發送記錄。\n");
                }
            }
        }
        catch (SQLException e)
        {
            sb.append("讀取記錄時發生錯誤: ").append(e.getMessage()).append("\n");
            e.printStackTrace();
        }
        
        txtLog.setText(sb.toString());
        txtLog.setCaretPosition(0);
    }
}
