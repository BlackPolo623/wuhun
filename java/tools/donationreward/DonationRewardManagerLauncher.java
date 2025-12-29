package tools.donationreward;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class DonationRewardManagerLauncher
{
    private static DonationRewardManagerFrame instance = null;
    
    public static void launch()
    {
        if (instance != null && instance.isVisible())
        {
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(instance, "累積贊助滿額禮管理器已開啟!", "提示", JOptionPane.INFORMATION_MESSAGE);
                instance.toFront();
                instance.requestFocus();
            });
            return;
        }
        
        SwingUtilities.invokeLater(() -> {
            try
            {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                instance = new DonationRewardManagerFrame();
                instance.addWindowListener(new java.awt.event.WindowAdapter() {
                    @Override
                    public void windowClosed(java.awt.event.WindowEvent e) {
                        instance = null;
                    }
                });
                instance.setVisible(true);
                System.out.println("[累積贊助滿額禮管理器] 已啟動");
            }
            catch (Exception e)
            {
                System.err.println("[累積贊助滿額禮管理器] 啟動失敗: " + e.getMessage());
                e.printStackTrace();
                instance = null;
            }
        });
    }
    
    public static void main(String[] args)
    {
        launch();
    }
}
