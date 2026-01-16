// ScratchCard.java
package custom.ScratchCard;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.data.xml.ScratchCardData;
import org.l2jmobius.gameserver.data.xml.ScratchCardData.JackpotReward;
import org.l2jmobius.gameserver.data.xml.ScratchCardData.RewardItem;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.ItemTemplate;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.model.variables.PlayerVariables;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.CreatureSay;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.util.Broadcast;

import custom.ScratchCard.ScratchCardDAO.ScratchCardState;

/**
 * 新春刮刮樂系統
 * @author 黑普羅
 */
public class ScratchCard extends Script
{
    // NPC ID
    private static final int NPC_ID = 900023;
    
    // HTML 路徑
    private static final String HTML_PATH = "data/scripts/custom/ScratchCard/";
    
    // PlayerVariables 前綴
    private static final String PV_PREFIX = "ScratchCard_";
    
    public ScratchCard()
    {
        addStartNpc(NPC_ID);
        addTalkId(NPC_ID);
        addFirstTalkId(NPC_ID);
    }
    
    @Override
    public String onFirstTalk(Npc npc, Player player)
    {
        showMainMenu(player, npc);
        return null;
    }
    
    @Override
    public String onEvent(String event, Npc npc, Player player)
    {
        if (event == null || player == null)
        {
            return null;
        }
        
        switch (event)
        {
            case "main":
                showMainMenu(player, npc);
                break;
                
            case "buy":
                handleBuyCard(player, npc);
                break;
                
            case "show":
                showScratchBoard(player, npc);
                break;
                
            case "rewards":
                showRewardList(player, npc);
                break;
                
            default:
                if (event.startsWith("scratch_"))
                {
                    // 刮開格子: scratch_位置
                    try
                    {
                        int position = Integer.parseInt(event.substring(8));
                        handleScratch(player, npc, position);
                    }
                    catch (NumberFormatException e)
                    {
                        player.sendMessage("無效的操作!");
                    }
                }
                break;
        }
        
        return null;
    }
    
    /**
     * 顯示主選單
     */
    private void showMainMenu(Player player, Npc npc)
    {
        ScratchCardData config = ScratchCardData.getInstance();
        PlayerVariables pv = player.getVariables();
        
        // 檢查是否有進行中的刮刮樂
        ScratchCardState state = ScratchCardDAO.loadState(player.getObjectId());
        boolean hasActiveCard = (state != null);
        
        // 檢查今日購買次數
        int dailyLimit = config.getDailyLimit();
        int todayCount = pv.getInt(PV_PREFIX + "daily_count", 0);
        
        NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
        html.setFile(player, HTML_PATH + "main.htm");
        
        // 替換變數
        html.replace("%has_card%", hasActiveCard ? "是" : "否");
        html.replace("%today_count%", String.valueOf(todayCount));
        html.replace("%daily_limit%", dailyLimit > 0 ? String.valueOf(dailyLimit) : "無限制");
        html.replace("%price%", formatNumber(config.getPriceCount()));
        
        // 根據狀態顯示不同按鈕
        if (hasActiveCard)
        {
            html.replace("%main_button%", 
                "<button action=\"bypass -h Quest ScratchCard show\" value=\"繼續刮開\" " +
                "width=\"200\" height=\"31\" back=\"BranchSys3.icon2.ArmyTrainingInfo_down\" " +
                "fore=\"BranchSys3.icon2.ArmyTrainingInfo\">");
        }
        else
        {
            boolean canBuy = (dailyLimit == 0 || todayCount < dailyLimit);
            if (canBuy)
            {
                html.replace("%main_button%", 
                    "<button action=\"bypass -h Quest ScratchCard buy\" value=\"購買刮刮樂\" " +
                    "width=\"200\" height=\"31\" back=\"BranchSys3.icon2.ArmyTrainingInfo_down\" " +
                    "fore=\"BranchSys3.icon2.ArmyTrainingInfo\">");
            }
            else
            {
                html.replace("%main_button%", 
                    "<table width=\"200\"><tr><td align=center><font color=\"FF0000\">今日次數已用完</font></td></tr></table>");
            }
        }
        
        player.sendPacket(html);
    }
    
    /**
     * 處理購買刮刮樂
     */
    private void handleBuyCard(Player player, Npc npc)
    {
        ScratchCardData config = ScratchCardData.getInstance();
        PlayerVariables pv = player.getVariables();
        
        // 檢查是否已有進行中的刮刮樂
        ScratchCardState state = ScratchCardDAO.loadState(player.getObjectId());
        if (state != null)
        {
            player.sendMessage("你已經有一張刮刮樂了!請先完成它。");
            showMainMenu(player, npc);
            return;
        }
        
        // 檢查今日購買次數
        int dailyLimit = config.getDailyLimit();
        if (dailyLimit > 0)
        {
            int todayCount = pv.getInt(PV_PREFIX + "daily_count", 0);
            if (todayCount >= dailyLimit)
            {
                player.sendMessage("今日購買次數已達上限(" + dailyLimit + "次)!");
                showMainMenu(player, npc);
                return;
            }
        }
        
        // 檢查金幣
        long playerGold = player.getInventory().getInventoryItemCount(config.getPriceItemId(), 0);
        if (playerGold < config.getPriceCount())
        {
            player.sendMessage("金幣不足!需要 " + formatNumber(config.getPriceCount()) + " 金幣。");
            showMainMenu(player, npc);
            return;
        }
        
        // 扣除金幣
        player.destroyItemByItemId(null, config.getPriceItemId(), config.getPriceCount(), npc, true);
        
        // 生成刮刮樂盤面
        String boardState = generateBoardState(config.getGridSize());
        
        // 儲存到資料庫
        if (!ScratchCardDAO.createNew(player.getObjectId(), boardState))
        {
            player.sendMessage("系統錯誤,請聯繫管理員!");
            // 退還金幣
            player.addItem(null, config.getPriceItemId(), config.getPriceCount(), npc, true);
            return;
        }
        
        // 更新今日購買次數
        int todayCount = pv.getInt(PV_PREFIX + "daily_count", 0);
        pv.set(PV_PREFIX + "daily_count", todayCount + 1);
        
        player.sendMessage("========================================");
        player.sendMessage("成功購買刮刮樂!");
        player.sendMessage("消耗: " + formatNumber(config.getPriceCount()) + " 金幣");
        player.sendMessage("========================================");
        
        // 直接顯示刮刮樂界面
        showScratchBoard(player, npc);
    }
    
    /**
     * 生成隨機盤面
     */
    private String generateBoardState(int gridSize)
    {
        ScratchCardData config = ScratchCardData.getInstance();
        StringBuilder sb = new StringBuilder();
        
        for (int i = 0; i < gridSize; i++)
        {
            if (i > 0)
            {
                sb.append(",");
            }
            sb.append(config.generateRandomSymbol());
        }
        
        return sb.toString();
    }
    
    /**
     * 顯示刮刮樂盤面
     */
    private void showScratchBoard(Player player, Npc npc)
    {
        // 載入狀態
        ScratchCardState state = ScratchCardDAO.loadState(player.getObjectId());
        if (state == null)
        {
            player.sendMessage("你目前沒有刮刮樂!");
            showMainMenu(player, npc);
            return;
        }
        
        NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
        html.setFile(player, HTML_PATH + "board.htm");
        
        // 解析盤面
        String[] symbols = state.boardState.split(",");
        Set<Integer> openedSet = parseOpenedPositions(state.openedPositions);
        
        // 替換變數
        html.replace("%opened_count%", String.valueOf(state.openedCount));
        html.replace("%total_count%", String.valueOf(symbols.length));
        html.replace("%accumulated%", String.valueOf(state.accumulatedCount));
        html.replace("%board%", generateBoardHtml(symbols, openedSet));
        
        player.sendPacket(html);
    }

    /**
     * 生成盤面HTML (4x6格子，簡單按鈕版本)
     */
    private String generateBoardHtml(String[] symbols, Set<Integer> openedSet)
    {
        StringBuilder sb = new StringBuilder();
        int cols = 4; // 4列
        int rows = 6; // 6行

        for (int row = 0; row < rows; row++)
        {
            sb.append("<tr>");
            for (int col = 0; col < cols; col++)
            {
                int position = row * cols + col;

                if (openedSet.contains(position))
                {
                    // 已刮開 - 顯示對應icon
                    sb.append("<td align=\"center\" width=\"32\" height=\"32\">");
                    int symbolType = Integer.parseInt(symbols[position]);
                    String iconName = getSymbolIcon(symbolType);
                    sb.append("<img src=\"").append(iconName).append("\" width=\"32\" height=\"32\">");
                    sb.append("</td>");
                }
                else
                {
                    // 未刮開 - 顯示按鈕
                    sb.append("<td align=\"center\" width=\"32\" height=\"32\">");
                    sb.append("<button action=\"bypass -h Quest ScratchCard scratch_").append(position)
                            .append("\" value=\"?\" width=\"32\" height=\"32\" ")
                            .append("back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
                    sb.append("</td>");
                }
            }
            sb.append("</tr>");

            // 每行之間加點間距
            if (row < rows - 1)
            {
                sb.append("<tr><td height=\"2\" colspan=\"4\"></td></tr>");
            }
        }

        return sb.toString();
    }
    
    /**
     * 根據符號類型返回對應icon
     */
    private String getSymbolIcon(int symbolType)
    {
        switch (symbolType)
        {
            case 0: // 空白
                return "icon.card_devil_black";
            case 1: // 累積
                return "icon.card_event_dragon_pocket";
            case 2: // 大獎
                return "icon.ev_211020_vip_token_i02";
            default:
                return "icon.card_devil_black";
        }
    }
    
    /**
     * 處理刮開格子
     */
    private void handleScratch(Player player, Npc npc, int position)
    {
        // 載入狀態
        ScratchCardState state = ScratchCardDAO.loadState(player.getObjectId());
        if (state == null)
        {
            player.sendMessage("你目前沒有刮刮樂!");
            showMainMenu(player, npc);
            return;
        }
        
        // 檢查位置是否已刮開
        Set<Integer> openedSet = parseOpenedPositions(state.openedPositions);
        if (openedSet.contains(position))
        {
            player.sendMessage("這個格子已經刮開了!");
            showScratchBoard(player, npc);
            return;
        }
        
        // 解析盤面
        String[] symbols = state.boardState.split(",");
        int symbolType = Integer.parseInt(symbols[position]);
        
        // 更新狀態
        openedSet.add(position);
        state.openedPositions = buildOpenedPositions(openedSet);
        state.openedCount++;
        
        // 處理不同符號
        boolean isComplete = false;
        switch (symbolType)
        {
            case 0: // 空白
                player.sendMessage("很遺憾,什麼都沒有...");
                break;
                
            case 1: // 累積
                state.accumulatedCount++;
                player.sendMessage("恭喜!累積 +1 (當前:" + state.accumulatedCount + ")");
                break;
                
            case 2: // 大獎!
                player.sendMessage("★★★ 恭喜中神秘大獎! ★★★");
                giveJackpotReward(player, npc);
                isComplete = true; // 中大獎直接結束
                break;
        }
        
        // 更新資料庫
        ScratchCardDAO.updateState(player.getObjectId(), state.openedPositions, 
                                    state.openedCount, state.accumulatedCount);
        
        // 檢查是否全部刮完
        if (state.openedCount >= symbols.length || isComplete)
        {
            completeCard(player, npc, state, isComplete);
        }
        else
        {
            showScratchBoard(player, npc);
        }
    }
    
    /**
     * 發放神秘大獎
     */
    private void giveJackpotReward(Player player, Npc npc)
    {
        ScratchCardData config = ScratchCardData.getInstance();
        JackpotReward jackpot = config.getRandomJackpot();
        
        player.sendMessage("========================================");
        player.sendMessage("神秘大獎內容:");
        
        for (RewardItem item : jackpot.getItems())
        {
            player.addItem(ItemProcessType.NONE, item.getItemId(), item.getAmount(), npc, true);
            ItemTemplate template = ItemData.getInstance().getTemplate(item.getItemId());
            String itemName = template != null ? template.getName() : "未知道具";
            player.sendMessage("- " + itemName + " x" + formatNumber(item.getAmount()));
        }
        
        player.sendMessage("========================================");
        
        // 全服公告
        Broadcast.toAllOnlinePlayers(new CreatureSay(null, ChatType.BATTLEFIELD, "刮刮樂系統",
            "恭喜玩家 " + player.getName() + " 刮中神秘大獎!"));
    }
    
    /**
     * 完成刮刮樂
     */
    private void completeCard(Player player, Npc npc, ScratchCardState state, boolean isJackpot)
    {
        if (!isJackpot && state.accumulatedCount > 0)
        {
            // 發放累積獎勵
            giveAccumulateReward(player, npc, state.accumulatedCount);
        }
        
        // 刪除資料庫記錄
        ScratchCardDAO.deleteState(player.getObjectId());
        
        player.sendMessage("========================================");
        player.sendMessage("刮刮樂已完成!");
        player.sendMessage("========================================");
        
        showMainMenu(player, npc);
    }
    
    /**
     * 發放累積獎勵
     */
    private void giveAccumulateReward(Player player, Npc npc, int count)
    {
        ScratchCardData config = ScratchCardData.getInstance();
        List<RewardItem> rewards = config.getAccumulateReward(count);
        
        if (rewards == null || rewards.isEmpty())
        {
            player.sendMessage("累積數量不足,沒有獎勵...");
            return;
        }
        
        player.sendMessage("========================================");
        player.sendMessage("累積獎勵 (累積:" + count + "):");
        
        for (RewardItem item : rewards)
        {
            player.addItem(ItemProcessType.NONE, item.getItemId(), item.getAmount(), npc, true);
            ItemTemplate template = ItemData.getInstance().getTemplate(item.getItemId());
            String itemName = template != null ? template.getName() : "未知道具";
            player.sendMessage("- " + itemName + " x" + formatNumber(item.getAmount()));
        }
        
        player.sendMessage("========================================");
    }
    
    /**
     * 顯示獎勵列表
     */
    private void showRewardList(Player player, Npc npc)
    {
        ScratchCardData config = ScratchCardData.getInstance();
        
        NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
        html.setFile(player, HTML_PATH + "rewards.htm");
        
        // 生成累積獎勵列表
        StringBuilder accRewards = new StringBuilder();
        List<Integer> sortedCounts = new ArrayList<>(config.getAccumulateRewards().keySet());
        sortedCounts.sort(Integer::compareTo);
        
        for (int count : sortedCounts)
        {
            List<RewardItem> items = config.getAccumulateRewards().get(count);
            accRewards.append("<tr bgcolor=\"222222\"><td colspan=\"2\" align=\"center\" height=\"25\">");
            accRewards.append("<font color=\"FFCC33\">累積 ").append(count).append(" 個:</font>");
            accRewards.append("</td></tr>");
            
            for (RewardItem item : items)
            {
                ItemTemplate template = ItemData.getInstance().getTemplate(item.getItemId());
                String itemName = template != null ? template.getName() : "未知道具";
                accRewards.append("<tr bgcolor=\"222222\"><td width=\"200\"><font color=\"00FF66\">")
                         .append(itemName).append("</font></td>");
                accRewards.append("<td width=\"80\" align=\"right\"><font color=\"FFFF00\">x")
                         .append(formatNumber(item.getAmount())).append("</font></td></tr>");
            }
        }
        
        html.replace("%accumulate_rewards%", accRewards.toString());
        
        // 機率資訊
        html.replace("%empty_rate%", String.format("%.1f", config.getEmptyChance() / 10.0));
        html.replace("%acc_rate%", String.format("%.1f", config.getAccumulateChance() / 10.0));
        html.replace("%jackpot_rate%", String.format("%.1f", config.getJackpotChance() / 10.0));
        
        player.sendPacket(html);
    }
    
    // ==================== 輔助方法 ====================
    
    private Set<Integer> parseOpenedPositions(String openedPositions)
    {
        Set<Integer> set = new HashSet<>();
        if (openedPositions == null || openedPositions.isEmpty())
        {
            return set;
        }
        
        String[] parts = openedPositions.split(",");
        for (String part : parts)
        {
            try
            {
                set.add(Integer.parseInt(part));
            }
            catch (NumberFormatException e)
            {
                // 忽略
            }
        }
        return set;
    }
    
    private String buildOpenedPositions(Set<Integer> openedSet)
    {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (int pos : openedSet)
        {
            if (!first)
            {
                sb.append(",");
            }
            sb.append(pos);
            first = false;
        }
        return sb.toString();
    }
    
    private String formatNumber(long number)
    {
        return String.format("%,d", number);
    }
    
    public static void main(String[] args)
    {
        new ScratchCard();
        System.out.println("【系統】新春刮刮樂系統載入完畢!");
    }
}