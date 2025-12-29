package custom.PromotionReward;

import java.util.List;

import org.l2jmobius.gameserver.data.sql.PromotionRewardDAO;
import org.l2jmobius.gameserver.data.xml.MultisellData;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;


public class PromotionRewardNpc extends Script
{
    private static final int NPC_ID = 900012;
    private static final int PROMOTION_COIN = 105600;
    
    private PromotionRewardNpc()
    {
        addStartNpc(NPC_ID);
        addFirstTalkId(NPC_ID);
        addTalkId(NPC_ID);
    }
    
    @Override
    public String onEvent(String event, Npc npc, Player player)
    {
        if (event.equals("claim"))
        {
            return claimRewards(player);
        }
        else if (event.equals("history"))
        {
            return showHistory(player);
        }
        else if (event.equals("back"))
        {
            return onFirstTalk(npc,player);
        } else if (event.equals("shop"))
        {
            MultisellData.getInstance().separateAndSend(9000121, player, npc, false);
            return null;
        }
        return showMainHtml(player);
    }
    
    @Override
    public String onFirstTalk(Npc npc, Player player)
    {
        return showMainHtml(player);
    }
    
    private String showMainHtml(Player player)
    {
        String accountName = player.getAccountName();
        int unclaimedCount = PromotionRewardDAO.getUnclaimedCount(accountName);
        int totalCoins = PromotionRewardDAO.getUnclaimedTotalCoins(accountName);
        
        NpcHtmlMessage html = new NpcHtmlMessage();
        html.setFile(player, "data/scripts/custom/PromotionReward/PromotionReward.htm");
        html.replace("%unclaimed%", String.valueOf(unclaimedCount));
        html.replace("%coins%", String.valueOf(totalCoins));
        player.sendPacket(html);
        return null;
    }
    
    private String claimRewards(Player player)
    {
        String accountName = player.getAccountName();
        int totalCoins = PromotionRewardDAO.getUnclaimedTotalCoins(accountName);
        
        if (totalCoins <= 0)
        {
            player.sendMessage("您目前沒有可領取的推廣獎勵!");
            return showMainHtml(player);
        }
        
        // 給予推廣硬幣
        player.addItem(ItemProcessType.NONE, PROMOTION_COIN, totalCoins, player, true);
        
        // 更新資料庫
        boolean success = PromotionRewardDAO.claimAllRewards(accountName, player.getName());
        
        if (success)
        {
            player.sendMessage("成功領取 " + totalCoins + " 個推廣硬幣!");
        }
        else
        {
            player.sendMessage("領取失敗,請聯繫管理員!");
        }
        
        return showMainHtml(player);
    }

    private String showHistory(Player player)
    {
        String accountName = player.getAccountName();
        List<PromotionRewardDAO.RewardRecord> records = PromotionRewardDAO.getRewardRecords(accountName);

        NpcHtmlMessage html = new NpcHtmlMessage();
        html.setFile(player, "data/scripts/custom/PromotionReward/PromotionRewardHistory.htm");

        StringBuilder sb = new StringBuilder();
        for (PromotionRewardDAO.RewardRecord record : records)
        {
            sb.append("<tr>");
            sb.append("<td width=80 align=\"center\">").append(record.rewardDate).append("</td>");
            sb.append("<td width=50 align=\"center\">").append(record.coinAmount).append("</td>");
            sb.append("<td width=50 align=\"center\">").append(record.claimed ? "<font color=00FF00>已領</font>" : "<font color=FF0000>未領</font>").append("</td>");
            sb.append("<td width=90 align=\"center\">").append(record.note != null ? record.note : "-").append("</td>");
            sb.append("</tr>");
        }

        html.replace("%records%", sb.toString());
        player.sendPacket(html);
        return null;
    }
    
    public static void main(String[] args)
    {
        new PromotionRewardNpc();
    }
}