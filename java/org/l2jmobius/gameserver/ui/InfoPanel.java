package org.l2jmobius.gameserver.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.LineBorder;

/**
 * 靜態漸變信息面板 - 無動畫但有漸變背景
 * @author Mobius
 */
public class InfoPanel extends JPanel
{
	private static final Color PANEL_BG = new Color(45, 45, 45);
	private static final Color GRADIENT_START = new Color(70, 130, 180);   // 起始顏色
	private static final Color GRADIENT_END = new Color(139, 0, 255);       // 結束顏色
	private static final Color TEXT_COLOR = new Color(255, 255, 255);      // 白色文字

	private final JLabel lblInfo;

	public InfoPanel(String infoText)
	{
		setBackground(PANEL_BG);
		setBounds(220, 180, 284, 70);
		setOpaque(false);  // ✅ 透明以便顯示漸變
		setLayout(null);

		setBorder(new LineBorder(GRADIENT_START, 2, true));

		lblInfo = new JLabel("<html><center>" + infoText.replace("\n", "<br>") + "</center></html>");
		lblInfo.setFont(new Font("微軟正黑體", Font.BOLD, 14));
		lblInfo.setForeground(TEXT_COLOR);
		lblInfo.setHorizontalAlignment(SwingConstants.CENTER);
		lblInfo.setBounds(10, 5, 264, 60);
		add(lblInfo);
	}

	@Override
	protected void paintComponent(Graphics g)
	{
		super.paintComponent(g);

		// ✅ 繪製靜態漸變背景（只執行一次，不消耗資源）
		Graphics2D g2d = (Graphics2D) g;
		GradientPaint gradient = new GradientPaint(
				0, 0, GRADIENT_START,
				getWidth(), getHeight(), GRADIENT_END
		);
		g2d.setPaint(gradient);
		g2d.fillRect(0, 0, getWidth(), getHeight());
	}

	public void setText(String text)
	{
		lblInfo.setText("<html><center>" + text.replace("\n", "<br>") + "</center></html>");
	}
}