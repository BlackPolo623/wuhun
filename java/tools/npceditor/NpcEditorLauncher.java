package tools.npceditor;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * NPC編輯器啟動器
 * 可以獨立運行或從GameServer啟動
 */
public class NpcEditorLauncher
{
	private static NpcEditorFrame instance = null;

	/**
	 * 啟動NPC編輯器視窗
	 */
	public static void launch()
	{
		// 檢查是否已經打開
		if (instance != null && instance.isVisible())
		{
			// 編輯器已打開，顯示提示並將窗口置於前台
			SwingUtilities.invokeLater(() -> {
				JOptionPane.showMessageDialog(instance,
						"NPC編輯器已開啟，請勿重複開啟！",
						"提示",
						JOptionPane.INFORMATION_MESSAGE);
				instance.toFront();
				instance.requestFocus();
			});
			return;
		}

		SwingUtilities.invokeLater(() -> {
			try
			{
				// 設置系統外觀
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

				// 創建並顯示視窗
				instance = new NpcEditorFrame();

				// 添加窗口關閉監聽器，關閉時清空實例
				instance.addWindowListener(new java.awt.event.WindowAdapter() {
					@Override
					public void windowClosed(java.awt.event.WindowEvent e) {
						instance = null;
					}
				});

				instance.setVisible(true);

				System.out.println("[NPC編輯器] 已啟動");
			}
			catch (Exception e)
			{
				System.err.println("[NPC編輯器] 啟動失敗: " + e.getMessage());
				e.printStackTrace();
				instance = null;
			}
		});
	}

	/**
	 * 檢查編輯器是否已打開
	 */
	public static boolean isRunning()
	{
		return instance != null && instance.isVisible();
	}

	/**
	 * 獨立運行時的主方法
	 */
	public static void main(String[] args)
	{
		System.out.println("======================================");
		System.out.println("   L2J Mobius - NPC編輯器");
		System.out.println("======================================");

		launch();
	}
}