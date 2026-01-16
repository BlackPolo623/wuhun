package tools.giftpackage;

import java.awt.Color;
import java.awt.Font;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.LineBorder;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.managers.MailManager;
import org.l2jmobius.gameserver.model.Message;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.itemcontainer.Mail;
import org.l2jmobius.gameserver.network.enums.MailType;

public class GiftPackageManagerFrame extends JFrame
{
    private static final long serialVersionUID = 1L;
    
    // 顏色配置
    private static final Color BG_COLOR = new Color(30, 30, 30);
    private static final Color FG_COLOR = new Color(50, 240, 240);
    private static final Color PANEL_BG = new Color(45, 45, 45);
    private static final Color INPUT_BG = new Color(55, 55, 55);
    private static final Color BORDER_COLOR = new Color(100, 100, 100);
    
    // UI元件
    private JTextField txtCharacterName;
    private JComboBox<GiftPackage> cmbPackage;
    private JSpinner spnQuantity;
    private JTextArea txtLog;
    private JButton btnSend;
    private JButton btnRefresh;
    
    // ==================== 禮包配置類 ====================
    public static class GiftPackage
    {
        private final String packageId;
        private final String packageName;
        private final String description;
        private final List<RewardItem> rewards;
        
        public GiftPackage(String packageId, String packageName, String description)
        {
            this.packageId = packageId;
            this.packageName = packageName;
            this.description = description;
            this.rewards = new ArrayList<>();
        }
        
        public void addReward(int itemId, long count)
        {
            rewards.add(new RewardItem(itemId, count));
        }
        
        public String getPackageId()
        {
            return packageId;
        }
        
        public String getPackageName()
        {
            return packageName;
        }
        
        public String getDescription()
        {
            return description;
        }
        
        public List<RewardItem> getRewards()
        {
            return rewards;
        }
        
        @Override
        public String toString()
        {
            return packageName + " (" + description + ")";
        }
    }
    
    // ==================== 獎勵物品類 ====================
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
    
    // ==================== 禮包配置（可依月份調整）====================
    private static final Map<String, GiftPackage> GIFT_PACKAGES = initializePackages();
    
    private static Map<String, GiftPackage> initializePackages()
    {
        Map<String, GiftPackage> packages = new LinkedHashMap<>();
        
        // ========== 2025年1月禮包 ==========
        
        // 基礎禮包
        GiftPackage Donate_202602_3000 = new GiftPackage(
            "Donate_202602_3000",
            "2026年2月禮包_3000元",
            "3000元"
        );
        Donate_202602_3000.addReward(57, 10000);      // 古幣 x10000
        Donate_202602_3000.addReward(105801, 10);        // 武魂魂魄 x10
        Donate_202602_3000.addReward(105802, 5);         // 收藏品隨機開啟券 x5
        packages.put(Donate_202602_3000.getPackageId(), Donate_202602_3000);
        

        
        // TODO: 新月份禮包在這裡添加
        // 例如：2025年2月禮包
        /*
        GiftPackage feb2025Basic = new GiftPackage(
            "2025_02_basic",
            "2025年2月基礎禮包",
            "春節特惠"
        );
        feb2025Basic.addReward(97145, 15000);
        // ... 添加更多獎勵
        packages.put(feb2025Basic.getPackageId(), feb2025Basic);
        */
        
        return packages;
    }
    
    // ==================== 建構函數 ====================
    public GiftPackageManagerFrame()
    {
        initializeDatabase();
        initComponents();
    }
    
    // ==================== 初始化資料庫 ====================
    private void initializeDatabase()
    {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement ps = con.prepareStatement(
                 "CREATE TABLE IF NOT EXISTS gift_package_history (" +
                 "id INT AUTO_INCREMENT PRIMARY KEY, " +
                 "package_id VARCHAR(50) NOT NULL, " +
                 "package_name VARCHAR(100) NOT NULL, " +
                 "account_name VARCHAR(45) NOT NULL, " +
                 "char_name VARCHAR(35) NOT NULL, " +
                 "quantity INT NOT NULL, " +
                 "send_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                 "INDEX idx_account (account_name), " +
                 "INDEX idx_package (package_id), " +
                 "INDEX idx_send_date (send_date)" +
                 ")"))
        {
            ps.execute();
            System.out.println("[禮包發送系統] 資料表檢查完成");
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
    }
    
    // ==================== 初始化UI元件 ====================
    private void initComponents()
    {
        setTitle("禮包發送系統");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setBounds(100, 100, 600, 550);
        setResizable(false);
        
        JPanel contentPane = new JPanel();
        contentPane.setBackground(BG_COLOR);
        contentPane.setBorder(new LineBorder(BORDER_COLOR, 1));
        contentPane.setLayout(null);
        setContentPane(contentPane);
        
        // 標題
        JLabel lblTitle = new JLabel("禮包發送系統");
        lblTitle.setFont(new Font("微軟正黑體", Font.BOLD, 18));
        lblTitle.setForeground(FG_COLOR);
        lblTitle.setBounds(20, 10, 560, 30);
        contentPane.add(lblTitle);
        
        // 禮包選擇標籤
        JLabel lblPackage = new JLabel("選擇禮包:");
        lblPackage.setFont(new Font("微軟正黑體", Font.PLAIN, 14));
        lblPackage.setForeground(FG_COLOR);
        lblPackage.setBounds(20, 50, 100, 25);
        contentPane.add(lblPackage);
        
        // 禮包下拉選單
        cmbPackage = new JComboBox<>();
        cmbPackage.setModel(new DefaultComboBoxModel<>(GIFT_PACKAGES.values().toArray(new GiftPackage[0])));
        cmbPackage.setFont(new Font("微軟正黑體", Font.PLAIN, 13));
        cmbPackage.setBackground(INPUT_BG);
        cmbPackage.setForeground(FG_COLOR);
        cmbPackage.setBounds(120, 50, 460, 25);
        cmbPackage.setOpaque(true);
        cmbPackage.setBorder(new LineBorder(BORDER_COLOR, 1));
        
        // 設定下拉選單的 UI 樣式
        cmbPackage.setUI(new javax.swing.plaf.basic.BasicComboBoxUI() {
            @Override
            protected JButton createArrowButton() {
                JButton btn = new JButton("▼");
                btn.setBackground(new Color(40, 40, 40));
                btn.setForeground(FG_COLOR);
                btn.setBorder(new LineBorder(BORDER_COLOR, 0));
                return btn;
            }
        });
        
        // 設定下拉列表的渲染器
        cmbPackage.setRenderer(new javax.swing.DefaultListCellRenderer() {
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
        
        contentPane.add(cmbPackage);
        
        // 角色名稱標籤
        JLabel lblCharName = new JLabel("角色名稱:");
        lblCharName.setFont(new Font("微軟正黑體", Font.PLAIN, 14));
        lblCharName.setForeground(FG_COLOR);
        lblCharName.setBounds(20, 85, 100, 25);
        contentPane.add(lblCharName);
        
        // 角色名稱輸入框
        txtCharacterName = new JTextField();
        txtCharacterName.setFont(new Font("微軟正黑體", Font.PLAIN, 14));
        txtCharacterName.setBackground(INPUT_BG);
        txtCharacterName.setForeground(FG_COLOR);
        txtCharacterName.setCaretColor(FG_COLOR);
        txtCharacterName.setBorder(new LineBorder(BORDER_COLOR, 1));
        txtCharacterName.setBounds(120, 85, 200, 25);
        contentPane.add(txtCharacterName);
        
        // 發送數量標籤
        JLabel lblQuantity = new JLabel("發送數量:");
        lblQuantity.setFont(new Font("微軟正黑體", Font.PLAIN, 14));
        lblQuantity.setForeground(FG_COLOR);
        lblQuantity.setBounds(330, 85, 80, 25);
        contentPane.add(lblQuantity);
        
        // 發送數量選擇器
        SpinnerNumberModel spinnerModel = new SpinnerNumberModel(1, 1, 99, 1);
        spnQuantity = new JSpinner(spinnerModel);
        spnQuantity.setFont(new Font("微軟正黑體", Font.PLAIN, 14));
        spnQuantity.setBounds(410, 85, 60, 25);
        
        // 設定 Spinner 樣式
        JSpinner.DefaultEditor editor = (JSpinner.DefaultEditor) spnQuantity.getEditor();
        editor.getTextField().setBackground(INPUT_BG);
        editor.getTextField().setForeground(FG_COLOR);
        editor.getTextField().setCaretColor(FG_COLOR);
        spnQuantity.setBorder(new LineBorder(BORDER_COLOR, 1));
        
        contentPane.add(spnQuantity);
        
        // 數量說明
        JLabel lblQtyNote = new JLabel("(1-99)");
        lblQtyNote.setFont(new Font("微軟正黑體", Font.PLAIN, 11));
        lblQtyNote.setForeground(new Color(150, 150, 150));
        lblQtyNote.setBounds(475, 85, 60, 25);
        contentPane.add(lblQtyNote);
        
        // 發送按鈕
        btnSend = createButton("發送禮包", 120, 120, 150, 35);
        btnSend.addActionListener(e -> sendGiftPackage());
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
        scrollPane.setBounds(20, 195, 560, 310);
        scrollPane.setBorder(new LineBorder(BORDER_COLOR, 1));
        contentPane.add(scrollPane);
        
        // 初始加載記錄
        refreshLog();
    }
    
    // ==================== 創建按鈕 ====================
    private JButton createButton(String text, int x, int y, int width, int height)
    {
        JButton btn = new JButton(text);
        btn.setFont(new Font("微軟正黑體", Font.PLAIN, 12));
        btn.setBounds(x, y, width, height);
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
    
    // ==================== 發送禮包 ====================
    private void sendGiftPackage()
    {
        String charName = txtCharacterName.getText().trim();

        if (charName.isEmpty())
        {
            JOptionPane.showMessageDialog(this, "請輸入角色名稱!", "錯誤", JOptionPane.ERROR_MESSAGE);
            return;
        }

        GiftPackage selectedPackage = (GiftPackage) cmbPackage.getSelectedItem();
        if (selectedPackage == null)
        {
            JOptionPane.showMessageDialog(this, "請選擇禮包!", "錯誤", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int quantity = (Integer) spnQuantity.getValue();

        // ==================== 改進：檢查角色是否存在（支援離線玩家）====================
        int charObjectId = getCharacterObjectId(charName);
        if (charObjectId == -1)
        {
            JOptionPane.showMessageDialog(this,
                    "角色 '" + charName + "' 不存在!\n請確認角色名稱是否正確。",
                    "錯誤",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        String accountName = getAccountName(charObjectId);
        if (accountName == null)
        {
            JOptionPane.showMessageDialog(this,
                    "無法取得角色的帳號資訊!",
                    "錯誤",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 發送郵件
        try
        {
            for (int i = 0; i < quantity; i++)
            {
                sendRewardMailToCharacter(charObjectId, charName, selectedPackage, i + 1, quantity);
            }

            // 記錄到資料庫
            recordSend(accountName, charName, selectedPackage, quantity);

            JOptionPane.showMessageDialog(this,
                    "成功發送 " + quantity + " 個 " + selectedPackage.getPackageName() + " 給 " + charName + "!\n" +
                            (isCharacterOnline(charName) ? "玩家目前在線，可立即查收。" : "玩家目前離線，上線後可收到郵件。"),
                    "成功",
                    JOptionPane.INFORMATION_MESSAGE);

            // 刷新記錄
            refreshLog();

            // 清空輸入
            txtCharacterName.setText("");
            spnQuantity.setValue(1);
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

    private int getCharacterObjectId(String charName)
    {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT charId FROM characters WHERE char_name = ?"))
        {
            ps.setString(1, charName);
            try (ResultSet rs = ps.executeQuery())
            {
                if (rs.next())
                {
                    return rs.getInt("charId");
                }
            }
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
        return -1;
    }

    /**
     * 從資料庫查詢角色的帳號名稱
     * @param charObjectId 角色ObjectId
     * @return 帳號名稱，如果查詢失敗則返回null
     */
    private String getAccountName(int charObjectId)
    {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT account_name FROM characters WHERE charId = ?"))
        {
            ps.setInt(1, charObjectId);
            try (ResultSet rs = ps.executeQuery())
            {
                if (rs.next())
                {
                    return rs.getString("account_name");
                }
            }
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 檢查角色是否在線
     * @param charName 角色名稱
     * @return true=在線，false=離線
     */
    private boolean isCharacterOnline(String charName)
    {
        return World.getInstance().getPlayer(charName) != null;
    }

    /**
     * 發送獎勵郵件給指定角色（支援離線玩家）
     */
    private void sendRewardMailToCharacter(int charObjectId, String charName, GiftPackage giftPackage,
                                           int currentNum, int totalNum)
    {
        String subject = giftPackage.getPackageName() + (totalNum > 1 ? " (" + currentNum + "/" + totalNum + ")" : "");
        String content = "親愛的玩家您好:\n\n" +
                "感謝您購買 " + giftPackage.getPackageName() + "!\n\n" +
                giftPackage.getDescription() + "\n\n" +
                (totalNum > 1 ? "這是您購買的第 " + currentNum + " 個禮包。\n\n" : "") +
                "請查收附件中的獎勵道具。\n\n" +
                "祝您遊戲愉快!\n" +
                "武魂天堂2 營運團隊";

        Message msg = new Message(charObjectId, subject, content, MailType.NEWS_INFORMER);
        Mail attachments = msg.createAttachments();

        // 添加所有獎勵物品
        for (RewardItem reward : giftPackage.getRewards())
        {
            attachments.addItem(ItemProcessType.REWARD, reward.getItemId(), reward.getCount(), null, null);
        }

        MailManager.getInstance().sendMessage(msg);

        System.out.println(String.format("[禮包發送系統] 已發送郵件給 %s (CharId: %d) - %s (%d/%d)",
                charName, charObjectId, giftPackage.getPackageName(), currentNum, totalNum));
    }
    
    // ==================== 記錄發送歷史 ====================
    private void recordSend(String accountName, String charName, GiftPackage giftPackage, int quantity)
    {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement ps = con.prepareStatement(
                 "INSERT INTO gift_package_history (package_id, package_name, account_name, char_name, quantity) " +
                 "VALUES (?, ?, ?, ?, ?)"))
        {
            ps.setString(1, giftPackage.getPackageId());
            ps.setString(2, giftPackage.getPackageName());
            ps.setString(3, accountName);
            ps.setString(4, charName);
            ps.setInt(5, quantity);
            ps.executeUpdate();
            
            System.out.println(String.format("[禮包發送系統] 記錄發送: %s / %s / %s x%d", 
                accountName, charName, giftPackage.getPackageName(), quantity));
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
    }
    
    // ==================== 發送獎勵郵件 ====================
    private void sendRewardMail(Player player, GiftPackage giftPackage, int currentNum, int totalNum)
    {
        String subject = giftPackage.getPackageName() + (totalNum > 1 ? " (" + currentNum + "/" + totalNum + ")" : "");
        String content = "親愛的玩家您好:\n\n" +
                        "感謝您購買 " + giftPackage.getPackageName() + "!\n\n" +
                        giftPackage.getDescription() + "\n\n" +
                        (totalNum > 1 ? "這是您購買的第 " + currentNum + " 個禮包。\n\n" : "") +
                        "請查收附件中的獎勵道具。\n\n" +
                        "祝您遊戲愉快!\n" +
                        "武魂天堂2 營運團隊";
        
        Message msg = new Message(player.getObjectId(), subject, content, MailType.NEWS_INFORMER);
        Mail attachments = msg.createAttachments();
        
        // 添加所有獎勵物品
        for (RewardItem reward : giftPackage.getRewards())
        {
            attachments.addItem(ItemProcessType.REWARD, reward.getItemId(), reward.getCount(), null, null);
        }
        
        MailManager.getInstance().sendMessage(msg);
        
        System.out.println(String.format("[禮包發送系統] 已發送郵件給 %s (帳號: %s) - %s (%d/%d)", 
            player.getName(), player.getAccountName(), giftPackage.getPackageName(), currentNum, totalNum));
    }
    
    // ==================== 刷新記錄 ====================
    private void refreshLog()
    {
        StringBuilder sb = new StringBuilder();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement ps = con.prepareStatement(
                 "SELECT package_name, account_name, char_name, quantity, send_date " +
                 "FROM gift_package_history ORDER BY send_date DESC LIMIT 100"))
        {
            try (ResultSet rs = ps.executeQuery())
            {
                sb.append("=== 最近 100 筆發送記錄 ===\n\n");
                
                int count = 0;
                while (rs.next())
                {
                    count++;
                    String packageName = rs.getString("package_name");
                    String accountName = rs.getString("account_name");
                    String charName = rs.getString("char_name");
                    int quantity = rs.getInt("quantity");
                    Date sendDate = rs.getTimestamp("send_date");
                    
                    sb.append(String.format("[%d] %s | 帳號: %s | 角色: %s\n    禮包: %s | 數量: %d\n\n", 
                        count, sdf.format(sendDate), accountName, charName, packageName, quantity));
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