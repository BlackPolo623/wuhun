package org.l2jmobius.gameserver.ui;

import java.awt.Color;
import java.awt.Font;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.LineBorder;

public class ToolsPanel extends JPanel
{
    private static final Color PANEL_BG = new Color(45, 45, 45);
    private static final Color FG_COLOR = new Color(240, 240, 240);
    private static final Color BORDER_COLOR = new Color(100, 100, 100);
    private static final Color BUTTON_BLUE = new Color(70, 130, 180);
    private static final Color BUTTON_BLUE_HOVER = new Color(90, 150, 200);
    private static final Color BUTTON_BORDER = new Color(50, 110, 160);
    private static final Color BUTTON_GREEN = new Color(60, 150, 90);
    private static final Color BUTTON_GREEN_HOVER = new Color(80, 170, 110);
    private static final Color BUTTON_GREEN_BORDER = new Color(40, 130, 70);
    private static final Color BUTTON_ORANGE = new Color(200, 120, 50);
    private static final Color BUTTON_ORANGE_HOVER = new Color(220, 140, 70);
    private static final Color BUTTON_ORANGE_BORDER = new Color(180, 100, 30);

    public ToolsPanel()
    {
       setBackground(PANEL_BG);
       setBounds(500, 170, 284, 220);  // 高度改為220以容納三個按鈕
       setBorder(new LineBorder(BORDER_COLOR, 1, false));
       setOpaque(true);
       setLayout(null);

       final JLabel lblTitle = new JLabel("工具面板");
       lblTitle.setFont(new Font("微軟正黑體", Font.BOLD, 16));
       lblTitle.setForeground(FG_COLOR);
       lblTitle.setBounds(10, 5, 264, 25);
       add(lblTitle);

       // NPC編輯器按鈕
       final JButton btnNpcEditor = new JButton("開啟 NPC 編輯器");
       btnNpcEditor.setFont(new Font("微軟正黑體", Font.PLAIN, 14));
       btnNpcEditor.setBounds(10, 35, 264, 55);
       btnNpcEditor.setToolTipText("點擊開啟 NPC 編輯器");
       btnNpcEditor.setBackground(BUTTON_BLUE);
       btnNpcEditor.setForeground(Color.WHITE);
       btnNpcEditor.setFocusPainted(false);
       btnNpcEditor.setBorder(new LineBorder(BUTTON_BORDER, 2, true));

       btnNpcEditor.addMouseListener(new java.awt.event.MouseAdapter() {
          @Override
          public void mouseEntered(java.awt.event.MouseEvent evt) {
             btnNpcEditor.setBackground(BUTTON_BLUE_HOVER);
          }

          @Override
          public void mouseExited(java.awt.event.MouseEvent evt) {
             btnNpcEditor.setBackground(BUTTON_BLUE);
          }
       });

       btnNpcEditor.addActionListener(_ -> {
          try
          {
             tools.npceditor.NpcEditorLauncher.launch();
          }
          catch (Exception e)
          {
             JOptionPane.showMessageDialog(this,
                   "無法啟動NPC編輯器: " + e.getMessage(),
                   "錯誤",
                   JOptionPane.ERROR_MESSAGE);
          }
       });

       add(btnNpcEditor);

       // 推廣獎勵管理器按鈕
       final JButton btnPromotionManager = new JButton("推廣獎勵管理器");
       btnPromotionManager.setFont(new Font("微軟正黑體", Font.PLAIN, 14));
       btnPromotionManager.setBounds(10, 95, 264, 55);
       btnPromotionManager.setToolTipText("點擊開啟推廣獎勵管理器");
       btnPromotionManager.setBackground(BUTTON_GREEN);
       btnPromotionManager.setForeground(Color.WHITE);
       btnPromotionManager.setFocusPainted(false);
       btnPromotionManager.setBorder(new LineBorder(BUTTON_GREEN_BORDER, 2, true));

       btnPromotionManager.addMouseListener(new java.awt.event.MouseAdapter() {
          @Override
          public void mouseEntered(java.awt.event.MouseEvent evt) {
             btnPromotionManager.setBackground(BUTTON_GREEN_HOVER);
          }

          @Override
          public void mouseExited(java.awt.event.MouseEvent evt) {
             btnPromotionManager.setBackground(BUTTON_GREEN);
          }
       });

       btnPromotionManager.addActionListener(_ -> {
          try
          {
             tools.promotionmanager.PromotionManagerLauncher.launch();
          }
          catch (Exception e)
          {
             JOptionPane.showMessageDialog(this,
                   "無法啟動推廣獎勵管理器: " + e.getMessage(),
                   "錯誤",
                   JOptionPane.ERROR_MESSAGE);
          }
       });

       add(btnPromotionManager);

       // 累積贊助滿額禮管理器按鈕
       final JButton btnDonationReward = new JButton("累積贊助滿額禮");
       btnDonationReward.setFont(new Font("微軟正黑體", Font.PLAIN, 14));
       btnDonationReward.setBounds(10, 155, 264, 55);
       btnDonationReward.setToolTipText("點擊開啟累積贊助滿額禮管理器");
       btnDonationReward.setBackground(BUTTON_ORANGE);
       btnDonationReward.setForeground(Color.WHITE);
       btnDonationReward.setFocusPainted(false);
       btnDonationReward.setBorder(new LineBorder(BUTTON_ORANGE_BORDER, 2, true));

       btnDonationReward.addMouseListener(new java.awt.event.MouseAdapter() {
          @Override
          public void mouseEntered(java.awt.event.MouseEvent evt) {
             btnDonationReward.setBackground(BUTTON_ORANGE_HOVER);
          }

          @Override
          public void mouseExited(java.awt.event.MouseEvent evt) {
             btnDonationReward.setBackground(BUTTON_ORANGE);
          }
       });

       btnDonationReward.addActionListener(_ -> {
          try
          {
             tools.donationreward.DonationRewardManagerLauncher.launch();
          }
          catch (Exception e)
          {
             JOptionPane.showMessageDialog(this,
                   "無法啟動累積贊助滿額禮管理器: " + e.getMessage(),
                   "錯誤",
                   JOptionPane.ERROR_MESSAGE);
          }
       });

       add(btnDonationReward);
    }
}
