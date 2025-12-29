package tools.promotionmanager;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

import org.l2jmobius.commons.database.DatabaseFactory;

public class PromotionManagerFrame extends JFrame
{
    private static final Color BG_COLOR = new Color(30, 30, 30);
    private static final Color FG_COLOR = new Color(240, 240, 240);
    private static final Color PANEL_BG = new Color(45, 45, 45);
    private static final Color BUTTON_BG = new Color(60, 60, 60);
    private static final Color BUTTON_HOVER = new Color(80, 80, 80);
    private static final Color BUTTON_TEXT = new Color(240, 240, 240);

    private JTextField txtAccount;
    private JTextField txtDate;
    private JTextField txtCoins;
    private JTextField txtNote;
    private JTextField txtSearchAccount;
    private JTable table;
    private DefaultTableModel tableModel;

    public PromotionManagerFrame()
    {
        // 設置基本外觀
        try
        {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

            // ===== 加入以下這段 =====
            // 對話框背景色
            UIManager.put("OptionPane.background", new Color(45, 45, 45));
            UIManager.put("Panel.background", new Color(45, 45, 45));

            // 對話框文字色
            UIManager.put("OptionPane.messageForeground", FG_COLOR);
            UIManager.put("Label.foreground", FG_COLOR);
            UIManager.put("TextField.foreground", FG_COLOR);

            // 對話框按鈕樣式（重點！）
            UIManager.put("Button.background", new Color(40, 40, 40));
            UIManager.put("Button.foreground", Color.WHITE);
            UIManager.put("Button.select", new Color(60, 60, 60));
            UIManager.put("Button.focus", new Color(60, 60, 60));

            // 輸入框樣式
            UIManager.put("TextField.background", new Color(55, 55, 55));
            UIManager.put("TextField.foreground", FG_COLOR);
            UIManager.put("TextField.caretForeground", FG_COLOR);

            // TextArea 樣式（批量登記用）
            UIManager.put("TextArea.background", new Color(55, 55, 55));
            UIManager.put("TextArea.foreground", FG_COLOR);
            UIManager.put("TextArea.caretForeground", FG_COLOR);
            // ===== 加到這裡 =====
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        setTitle("推廣獎勵管理器");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG_COLOR);
        getContentPane().setLayout(null);

        // 標題
        JLabel lblTitle = new JLabel("推廣獎勵管理系統");
        lblTitle.setFont(new Font("微軟正黑體", Font.BOLD, 20));
        lblTitle.setForeground(FG_COLOR);
        lblTitle.setBounds(20, 10, 300, 30);
        add(lblTitle);

        // 新增獎勵面板
        JLabel lblAdd = new JLabel("新增獎勵");
        lblAdd.setFont(new Font("微軟正黑體", Font.BOLD, 16));
        lblAdd.setForeground(FG_COLOR);
        lblAdd.setBounds(20, 50, 100, 25);
        add(lblAdd);

        JLabel lblAccount = new JLabel("帳號:");
        lblAccount.setForeground(FG_COLOR);
        lblAccount.setBounds(20, 85, 60, 25);
        add(lblAccount);

        txtAccount = new JTextField();
        txtAccount.setBounds(80, 85, 150, 25);
        txtAccount.setBackground(new Color(55, 55, 55));
        txtAccount.setForeground(FG_COLOR);
        txtAccount.setCaretColor(FG_COLOR);
        add(txtAccount);

        JLabel lblDate = new JLabel("日期:");
        lblDate.setForeground(FG_COLOR);
        lblDate.setBounds(250, 85, 60, 25);
        add(lblDate);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        txtDate = new JTextField(sdf.format(new Date()));
        txtDate.setBounds(310, 85, 120, 25);
        txtDate.setBackground(new Color(55, 55, 55));
        txtDate.setForeground(FG_COLOR);
        txtDate.setCaretColor(FG_COLOR);
        add(txtDate);

        JLabel lblCoins = new JLabel("硬幣:");
        lblCoins.setForeground(FG_COLOR);
        lblCoins.setBounds(450, 85, 60, 25);
        add(lblCoins);

        txtCoins = new JTextField("1");
        txtCoins.setBounds(510, 85, 60, 25);
        txtCoins.setBackground(new Color(55, 55, 55));
        txtCoins.setForeground(FG_COLOR);
        txtCoins.setCaretColor(FG_COLOR);
        add(txtCoins);

        JLabel lblNote = new JLabel("備註:");
        lblNote.setForeground(FG_COLOR);
        lblNote.setBounds(20, 120, 60, 25);
        add(lblNote);

        txtNote = new JTextField();
        txtNote.setBounds(80, 120, 350, 25);
        txtNote.setBackground(new Color(55, 55, 55));
        txtNote.setForeground(FG_COLOR);
        txtNote.setCaretColor(FG_COLOR);
        add(txtNote);

        JButton btnAdd = createButton("登記獎勵", 450, 120, 120, 25);
        btnAdd.addActionListener(e -> addReward());
        add(btnAdd);

        JButton btnBatch = createButton("批量登記", 590, 120, 120, 25);
        btnBatch.addActionListener(e -> batchAdd());
        add(btnBatch);

        // 查詢面板
        JLabel lblSearch = new JLabel("查詢帳號");
        lblSearch.setFont(new Font("微軟正黑體", Font.BOLD, 16));
        lblSearch.setForeground(FG_COLOR);
        lblSearch.setBounds(20, 160, 100, 25);
        add(lblSearch);

        JLabel lblSearchAccount = new JLabel("帳號:");
        lblSearchAccount.setForeground(FG_COLOR);
        lblSearchAccount.setBounds(20, 195, 60, 25);
        add(lblSearchAccount);

        txtSearchAccount = new JTextField();
        txtSearchAccount.setBounds(80, 195, 150, 25);
        txtSearchAccount.setBackground(new Color(55, 55, 55));
        txtSearchAccount.setForeground(FG_COLOR);
        txtSearchAccount.setCaretColor(FG_COLOR);
        add(txtSearchAccount);

        JButton btnSearch = createButton("查詢", 250, 195, 100, 25);
        btnSearch.addActionListener(e -> searchRewards());
        add(btnSearch);

        JButton btnRefresh = createButton("刷新", 370, 195, 100, 25);
        btnRefresh.addActionListener(e -> loadAllRewards());
        add(btnRefresh);

        // 表格
        String[] columns = {"ID", "帳號", "日期", "硬幣", "已領取", "領取時間", "領取角色", "備註"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(tableModel);
        table.setBackground(PANEL_BG);
        table.setForeground(FG_COLOR);
        table.setSelectionBackground(new Color(70, 130, 180));
        table.setSelectionForeground(Color.WHITE);
        table.setGridColor(new Color(100, 100, 100));
        table.setRowHeight(25);
        table.setFont(new Font("微軟正黑體", Font.PLAIN, 12));

        // 設置表格標題樣式
        JTableHeader header = table.getTableHeader();
        header.setBackground(new Color(50, 50, 50));
        header.setForeground(FG_COLOR);
        header.setFont(new Font("微軟正黑體", Font.BOLD, 12));
        header.setOpaque(true);

        // 強制設置標題渲染器
        DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer();
        headerRenderer.setBackground(new Color(50, 50, 50));
        headerRenderer.setForeground(FG_COLOR);
        headerRenderer.setFont(new Font("微軟正黑體", Font.BOLD, 12));
        headerRenderer.setOpaque(true);

        for (int i = 0; i < table.getColumnModel().getColumnCount(); i++)
        {
            table.getColumnModel().getColumn(i).setHeaderRenderer(new DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(JTable table, Object value,
                                                               boolean isSelected, boolean hasFocus, int row, int column)
                {
                    JLabel label = new JLabel(value.toString());
                    label.setBackground(new Color(50, 50, 50));
                    label.setForeground(FG_COLOR);
                    label.setFont(new Font("微軟正黑體", Font.BOLD, 12));
                    label.setOpaque(true);
                    label.setBorder(new LineBorder(new Color(100, 100, 100), 1));
                    return label;
                }
            });
        }

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBounds(20, 235, 750, 280);
        scrollPane.getViewport().setBackground(PANEL_BG);
        scrollPane.setBorder(new LineBorder(new Color(100, 100, 100), 1));
        add(scrollPane);

        // 操作按鈕
        JButton btnDelete = createButton("刪除選中", 20, 525, 120, 30);
        btnDelete.addActionListener(e -> deleteSelected());
        add(btnDelete);

        // 載入所有記錄
        loadAllRewards();
    }

    private JButton createButton(String text, int x, int y, int width, int height)
    {
        JButton btn = new JButton(text);

        btn.setFont(new Font("微軟正黑體", Font.PLAIN, 12));
        btn.setBounds(x, y, width, height);

        // 強制設定為黑底白字
        btn.setBackground(new Color(40, 40, 40));  // 深黑色背景
        btn.setForeground(Color.WHITE);            // 白色文字

        // 這些設定很重要！
        btn.setOpaque(true);
        btn.setContentAreaFilled(true);
        btn.setBorderPainted(false);  // 移除邊框
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

    private void addReward()
    {
        String account = txtAccount.getText().trim();
        String date = txtDate.getText().trim();
        String coinsStr = txtCoins.getText().trim();
        String note = txtNote.getText().trim();

        if (account.isEmpty())
        {
            JOptionPane.showMessageDialog(this, "請輸入帳號!");
            return;
        }

        int coins;
        try
        {
            coins = Integer.parseInt(coinsStr);
        }
        catch (NumberFormatException e)
        {
            JOptionPane.showMessageDialog(this, "硬幣數量必須是數字!");
            return;
        }

        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement ps = con.prepareStatement("INSERT INTO promotion_rewards (account_name, reward_date, coin_amount, note) VALUES (?, ?, ?, ?)"))
        {
            ps.setString(1, account);
            ps.setString(2, date);
            ps.setInt(3, coins);
            ps.setString(4, note.isEmpty() ? null : note);
            ps.executeUpdate();

            JOptionPane.showMessageDialog(this, "登記成功!");
            txtAccount.setText("");
            txtNote.setText("");
            loadAllRewards();
        }
        catch (Exception e)
        {
            JOptionPane.showMessageDialog(this, "登記失敗: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void batchAdd()
    {
        // ===== 改用 JTextArea =====
        javax.swing.JTextArea textArea = new javax.swing.JTextArea(10, 40);  // 10行高，40字符寬
        textArea.setBackground(new Color(55, 55, 55));
        textArea.setForeground(FG_COLOR);
        textArea.setCaretColor(FG_COLOR);
        textArea.setFont(new Font("微軟正黑體", Font.PLAIN, 12));
        textArea.setLineWrap(true);  // 自動換行
        textArea.setWrapStyleWord(true);

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new java.awt.Dimension(400, 200));

        int result = JOptionPane.showConfirmDialog(this, scrollPane,
                "請輸入帳號列表(每行一個或用分號;隔開)",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);

        if (result != JOptionPane.OK_OPTION)
        {
            return;
        }

        String input = textArea.getText();
        // ===== 改到這裡 =====

        if (input == null || input.trim().isEmpty())
        {
            return;
        }

        String date = txtDate.getText().trim();
        String coinsStr = txtCoins.getText().trim();
        String note = txtNote.getText().trim();

        int coins;
        try
        {
            coins = Integer.parseInt(coinsStr);
        }
        catch (NumberFormatException e)
        {
            JOptionPane.showMessageDialog(this, "硬幣數量必須是數字!");
            return;
        }

        // 先把分號替換成換行，然後統一用換行分割
        input = input.replace(";", "\n");
        String[] accounts = input.split("\n");

        int success = 0;
        int failed = 0;

        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement ps = con.prepareStatement("INSERT INTO promotion_rewards (account_name, reward_date, coin_amount, note) VALUES (?, ?, ?, ?)"))
        {
            for (String account : accounts)
            {
                account = account.trim();
                if (account.isEmpty())
                {
                    continue;
                }

                try
                {
                    ps.setString(1, account);
                    ps.setString(2, date);
                    ps.setInt(3, coins);
                    ps.setString(4, note.isEmpty() ? null : note);
                    ps.executeUpdate();
                    success++;
                }
                catch (Exception e)
                {
                    failed++;
                }
            }

            JOptionPane.showMessageDialog(this, "批量登記完成!\n成功: " + success + "\n失敗: " + failed);
            loadAllRewards();
        }
        catch (Exception e)
        {
            JOptionPane.showMessageDialog(this, "批量登記失敗: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void searchRewards()
    {
        String account = txtSearchAccount.getText().trim();
        if (account.isEmpty())
        {
            JOptionPane.showMessageDialog(this, "請輸入要查詢的帳號!");
            return;
        }

        tableModel.setRowCount(0);

        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT * FROM promotion_rewards WHERE account_name=? ORDER BY reward_date DESC"))
        {
            ps.setString(1, account);
            try (ResultSet rs = ps.executeQuery())
            {
                while (rs.next())
                {
                    Object[] row = {
                            rs.getInt("id"),
                            rs.getString("account_name"),
                            rs.getString("reward_date"),
                            rs.getInt("coin_amount"),
                            rs.getBoolean("claimed") ? "是" : "否",
                            rs.getString("claim_time"),
                            rs.getString("char_name"),
                            rs.getString("note")
                    };
                    tableModel.addRow(row);
                }
            }
        }
        catch (Exception e)
        {
            JOptionPane.showMessageDialog(this, "查詢失敗: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadAllRewards()
    {
        tableModel.setRowCount(0);

        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT * FROM promotion_rewards ORDER BY id DESC LIMIT 100"))
        {
            try (ResultSet rs = ps.executeQuery())
            {
                while (rs.next())
                {
                    Object[] row = {
                            rs.getInt("id"),
                            rs.getString("account_name"),
                            rs.getString("reward_date"),
                            rs.getInt("coin_amount"),
                            rs.getBoolean("claimed") ? "是" : "否",
                            rs.getString("claim_time"),
                            rs.getString("char_name"),
                            rs.getString("note")
                    };
                    tableModel.addRow(row);
                }
            }
        }
        catch (Exception e)
        {
            JOptionPane.showMessageDialog(this, "載入失敗: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void deleteSelected()
    {
        int row = table.getSelectedRow();
        if (row == -1)
        {
            JOptionPane.showMessageDialog(this, "請選擇要刪除的記錄!");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this, "確定要刪除這筆記錄嗎?", "確認", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION)
        {
            return;
        }

        int id = (int) tableModel.getValueAt(row, 0);

        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM promotion_rewards WHERE id=?"))
        {
            ps.setInt(1, id);
            ps.executeUpdate();

            JOptionPane.showMessageDialog(this, "刪除成功!");
            loadAllRewards();
        }
        catch (Exception e)
        {
            JOptionPane.showMessageDialog(this, "刪除失敗: " + e.getMessage());
            e.printStackTrace();
        }
    }
}