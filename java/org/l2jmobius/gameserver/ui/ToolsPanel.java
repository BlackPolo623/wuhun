package org.l2jmobius.gameserver.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

public class ToolsPanel extends JPanel
{
    private static final Color PANEL_BG = new Color(45, 45, 45);
    private static final Color FG_COLOR = new Color(240, 240, 240);
    private static final Color BORDER_COLOR = new Color(100, 100, 100);

    // 統一淡藍色系
    private static final Color BUTTON_LIGHT_BLUE = new Color(135, 175, 215);      // 淡藍色
    private static final Color BUTTON_LIGHT_BLUE_TOP = new Color(155, 190, 225); // 淡藍色頂部（漸層用）
    private static final Color BUTTON_DARK_BLUE = new Color(85, 125, 165);       // 深藍色（hover）
    private static final Color BUTTON_DARK_BLUE_TOP = new Color(105, 145, 185);  // 深藍色頂部（hover漸層）
    private static final Color BUTTON_PRESSED = new Color(65, 105, 145);         // 按下時的顏色
    private static final Color BUTTON_TEXT = new Color(30, 30, 30);              // 深色文字
    private static final Color BUTTON_BORDER = new Color(90, 130, 170);          // 邊框顏色

    public ToolsPanel()
    {
        setBackground(PANEL_BG);
        setBounds(500, 170, 284, 260);
        setBorder(new LineBorder(BORDER_COLOR, 1, false));
        setOpaque(true);
        setLayout(null);

        final JLabel lblTitle = new JLabel("工具面板");
        lblTitle.setFont(new Font("微軟正黑體", Font.BOLD, 16));
        lblTitle.setForeground(FG_COLOR);
        lblTitle.setBounds(10, 5, 264, 25);
        add(lblTitle);

        // NPC編輯器按鈕
        final GlossyButton btnNpcEditor = new GlossyButton("開啟 NPC 編輯器");
        btnNpcEditor.setBounds(10, 35, 264, 45);
        btnNpcEditor.setToolTipText("點擊開啟 NPC 編輯器");
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
        final GlossyButton btnPromotionManager = new GlossyButton("推廣獎勵管理器");
        btnPromotionManager.setBounds(10, 85, 264, 45);
        btnPromotionManager.setToolTipText("點擊開啟推廣獎勵管理器");
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
        final GlossyButton btnDonationReward = new GlossyButton("累積贊助滿額禮");
        btnDonationReward.setBounds(10, 135, 264, 45);
        btnDonationReward.setToolTipText("點擊開啟累積贊助滿額禮管理器");
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

        // 禮包發送系統按鈕
        final GlossyButton btnGiftPackage = new GlossyButton("禮包發送系統");
        btnGiftPackage.setBounds(10, 185, 264, 45);
        btnGiftPackage.setToolTipText("點擊開啟禮包發送系統");
        btnGiftPackage.addActionListener(_ -> {
            try
            {
                tools.giftpackage.GiftPackageManagerLauncher.launch();
            }
            catch (Exception e)
            {
                JOptionPane.showMessageDialog(this,
                        "無法啟動禮包發送系統: " + e.getMessage(),
                        "錯誤",
                        JOptionPane.ERROR_MESSAGE);
            }
        });
        add(btnGiftPackage);
    }

    /**
     * 自定義光亮按鈕類 - 統一淡藍色漸層效果（改進版）
     */
    private static class GlossyButton extends JButton
    {
        private boolean isHovered = false;
        private boolean isPressed = false;

        public GlossyButton(String text)
        {
            super(text);
            setFont(new Font("微軟正黑體", Font.BOLD, 13));
            setForeground(BUTTON_TEXT);
            setFocusPainted(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setOpaque(false);

            // 設定內邊距
            setBorder(new CompoundBorder(
                    new LineBorder(BUTTON_BORDER, 2, true),
                    new EmptyBorder(5, 15, 5, 15)
            ));

            // 滑鼠事件
            addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseEntered(java.awt.event.MouseEvent evt) {
                    isHovered = true;
                    repaint();
                }

                @Override
                public void mouseExited(java.awt.event.MouseEvent evt) {
                    isHovered = false;
                    repaint();
                }

                @Override
                public void mousePressed(java.awt.event.MouseEvent evt) {
                    isPressed = true;
                    repaint();
                }

                @Override
                public void mouseReleased(java.awt.event.MouseEvent evt) {
                    isPressed = false;
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g)
        {
            Graphics2D g2d = (Graphics2D) g.create();

            // 啟用抗鋸齒
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            int width = getWidth();
            int height = getHeight();

            // 選擇顏色
            Color topColor;
            Color bottomColor;

            if (isPressed)
            {
                // 按下時的顏色（稍微變暗）
                topColor = new Color(75, 115, 155);
                bottomColor = new Color(55, 95, 135);
            }
            else if (isHovered)
            {
                // 懸停時的顏色（明顯變深）
                topColor = BUTTON_DARK_BLUE_TOP;
                bottomColor = BUTTON_DARK_BLUE;
            }
            else
            {
                // 正常狀態（淡藍色）
                topColor = BUTTON_LIGHT_BLUE_TOP;
                bottomColor = BUTTON_LIGHT_BLUE;
            }

            // 繪製主要漸層背景（從上到下）
            GradientPaint gradient = new GradientPaint(
                    0, 0, topColor,
                    0, height, bottomColor
            );
            g2d.setPaint(gradient);
            g2d.fillRoundRect(0, 0, width - 1, height - 1, 10, 10);

            // ===== 改進：繪製更柔和的高光效果 =====
            if (!isPressed)
            {
                // 方法1: 微妙的頂部高光（非常淡，只在頂部一小部分）
                GradientPaint subtleHighlight = new GradientPaint(
                        0, 0, new Color(255, 255, 255, 25),  // 降低透明度
                        0, height / 3, new Color(255, 255, 255, 0)  // 範圍縮小到1/3
                );
                g2d.setPaint(subtleHighlight);
                g2d.fillRoundRect(3, 3, width - 7, height / 3, 8, 8);
            }

            // 繪製外邊框
            g2d.setColor(BUTTON_BORDER);
            g2d.drawRoundRect(0, 0, width - 1, height - 1, 10, 10);

            // 繪製內部細邊框（增加質感）
            if (!isPressed)
            {
                g2d.setColor(new Color(255, 255, 255, 30));  // 非常淡的白色
                g2d.drawRoundRect(1, 1, width - 3, height - 3, 9, 9);
            }
            else
            {
                // 按下時顯示內陰影
                g2d.setColor(new Color(0, 0, 0, 50));
                g2d.drawRoundRect(1, 1, width - 3, height - 3, 9, 9);
            }

            g2d.dispose();

            // 繪製文字
            super.paintComponent(g);
        }
    }
}