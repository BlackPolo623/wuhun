package custom.ScratchCard;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.data.xml.ScratchCardData;
import org.l2jmobius.gameserver.data.xml.ScratchCardData.JackpotReward;
import org.l2jmobius.gameserver.data.xml.ScratchCardData.RewardItem;
import org.l2jmobius.gameserver.data.xml.ScratchCardData.ScratchCardConfig;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.ItemTemplate;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.model.variables.GlobalVariables;
import org.l2jmobius.gameserver.model.variables.PlayerVariables;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.CreatureSay;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.util.Broadcast;

import custom.ScratchCard.ScratchCardDAO.ScratchCardState;

/**
 * 多類型刮刮樂系統
 * @author 黑普羅
 */
public class ScratchCard extends Script
{
    private static final int NPC_ID = 900023;
    private static final String HTML_PATH = "data/scripts/custom/ScratchCard/";
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

            case "selectType":
                showTypeSelection(player, npc);
                break;

            case "show":
                showScratchBoard(player, npc);
                break;

            default:
                if (event.startsWith("buy_"))
                {
                    // 購買指定類型: buy_類型ID
                    try
                    {
                        int cardType = Integer.parseInt(event.substring(4));
                        handleBuyCard(player, npc, cardType);
                    }
                    catch (NumberFormatException e)
                    {
                        player.sendMessage("無效的刮刮樂類型!");
                    }
                }
                else if (event.startsWith("rewards_"))
                {
                    // 查看指定類型獎勵: rewards_類型ID
                    try
                    {
                        int cardType = Integer.parseInt(event.substring(8));
                        showRewardList(player, npc, cardType);
                    }
                    catch (NumberFormatException e)
                    {
                        player.sendMessage("無效的刮刮樂類型!");
                    }
                }
                else if (event.startsWith("scratch_"))
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
        // 檢查是否有進行中的刮刮樂
        ScratchCardState state = ScratchCardDAO.loadState(player.getObjectId());
        boolean hasActiveCard = (state != null);

        NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
        html.setFile(player, HTML_PATH + "main.htm");

        if (hasActiveCard)
        {
            ScratchCardConfig config = ScratchCardData.getInstance().getConfig(state.cardType);
            String cardName = config != null ? config.getName() : "未知類型";

            html.replace("%status%", "<font color=\"00FF66\">進行中</font>");
            html.replace("%current_card%", cardName);
            html.replace("%main_button%",
                    "<button action=\"bypass -h Quest ScratchCard show\" value=\"繼續刮開\" " +
                            "width=\"200\" height=\"31\" back=\"BranchSys3.icon2.ArmyTrainingInfo_down\" " +
                            "fore=\"BranchSys3.icon2.ArmyTrainingInfo\">");
        }
        else
        {
            html.replace("%status%", "<font color=\"808080\">無</font>");
            html.replace("%current_card%", "-");
            html.replace("%main_button%",
                    "<button action=\"bypass -h Quest ScratchCard selectType\" value=\"選擇刮刮樂類型\" " +
                            "width=\"200\" height=\"31\" back=\"BranchSys3.icon2.ArmyTrainingInfo_down\" " +
                            "fore=\"BranchSys3.icon2.ArmyTrainingInfo\">");
        }

        player.sendPacket(html);
    }

    /**
     * 顯示類型選擇頁面
     */
    private void showTypeSelection(Player player, Npc npc)
    {
        // 檢查是否已有進行中的刮刮樂
        ScratchCardState state = ScratchCardDAO.loadState(player.getObjectId());
        if (state != null)
        {
            player.sendMessage("你已經有一張刮刮樂了!請先完成它。");
            showMainMenu(player, npc);
            return;
        }

        Map<Integer, ScratchCardConfig> configs = ScratchCardData.getInstance().getAllConfigs();

        NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
        html.setFile(player, HTML_PATH + "select_type.htm");

        // 生成類型列表
        StringBuilder typeList = new StringBuilder();
        for (ScratchCardConfig config : configs.values())
        {
            PlayerVariables pv = player.getVariables();
            int todayCount = pv.getInt(PV_PREFIX + "daily_count_" + config.getId(), 0);
            int dailyLimit = config.getDailyLimit();
            boolean canBuy = (dailyLimit == 0 || todayCount < dailyLimit);

            typeList.append("<tr bgcolor=\"222222\" height=\"35\">");
            typeList.append("<td width=\"110\" >");
            typeList.append("<font color=\"LEVEL\">").append(config.getName()).append("</font><br1>");
            typeList.append("<font color=\"808080\" size=\"1\">格子數:").append(config.getGridSize()).append("</font>");
            typeList.append("</td>");

            typeList.append("<td width=\"110\" >");
            typeList.append("<font color=\"FFD700\">").append(formatNumber(config.getPriceCount())).append("</font><br1>");
            if (dailyLimit > 0)
            {
                String limitColor = todayCount >= dailyLimit ? "FF3333" : "00FF66";
                typeList.append("<font color=\"").append(limitColor).append("\" size=\"1\">");
                typeList.append(todayCount).append("/").append(dailyLimit).append("</font>");
            }
            else
            {
                typeList.append("<font color=\"808080\" size=\"1\">無限制</font>");
            }
            typeList.append("</td>");

            typeList.append("<td width=\"50\">");
            if (canBuy)
            {
                typeList.append("<button action=\"bypass -h Quest ScratchCard buy_").append(config.getId());
                typeList.append("\" value=\"購買\" width=\"45\" height=\"20\" ");
                typeList.append("back=\"L2UI_CT1.Button_DF\" fore=\"L2UI_CT1.Button_DF\">");
            }
            else
            {
                typeList.append("<font color=\"FF3333\" size=\"1\">已滿</font>");
            }
            typeList.append("</td>");

            typeList.append("</tr>");

            // 添加查看獎勵按鈕
            typeList.append("<tr bgcolor=\"222222\">");
            typeList.append("<td colspan=\"3\" align=\"center\" height=\"20\">");
            typeList.append("<button action=\"bypass -h Quest ScratchCard rewards_").append(config.getId());
            typeList.append("\" value=\"查看獎勵內容\" width=\"110\" height=\"18\" ");
            typeList.append("back=\"L2UI_CT1.Button_DF\" fore=\"L2UI_CT1.Button_DF\">");
            typeList.append("</td>");
            typeList.append("</tr>");

            typeList.append("<tr><td colspan=\"3\" height=\"5\"></td></tr>");
        }

        html.replace("%type_list%", typeList.toString());
        player.sendPacket(html);
    }

    /**
     * 處理購買刮刮樂
     */
    private void handleBuyCard(Player player, Npc npc, int cardType)
    {
        // 檢查配置是否存在
        ScratchCardConfig config = ScratchCardData.getInstance().getConfig(cardType);
        if (config == null)
        {
            player.sendMessage("無效的刮刮樂類型!");
            showTypeSelection(player, npc);
            return;
        }

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
            int todayCount = pv.getInt(PV_PREFIX + "daily_count_" + cardType, 0);
            if (todayCount >= dailyLimit)
            {
                player.sendMessage("今日購買次數已達上限(" + dailyLimit + "次)!");
                showTypeSelection(player, npc);
                return;
            }
        }

        // 檢查金幣
        long playerGold = player.getInventory().getInventoryItemCount(config.getPriceItemId(), 0);
        if (playerGold < config.getPriceCount())
        {
            player.sendMessage("金幣不足!需要 " + formatNumber(config.getPriceCount()) + " 金幣。");
            showTypeSelection(player, npc);
            return;
        }

        // 扣除金幣
        player.destroyItemByItemId(null, config.getPriceItemId(), config.getPriceCount(), npc, true);

        long contribution = (long) (config.getPriceCount() * 0.2);
        GlobalVariables gv = GlobalVariables.getInstance();
        long currentTotal = gv.getLong("ScratchCard_TotalContribution", 0L);
        gv.set("ScratchCard_TotalContribution", currentTotal + contribution);

        // 生成刮刮樂盤面
        String boardState = generateBoardState(config);

        // 儲存到資料庫
        if (!ScratchCardDAO.createNew(player.getObjectId(), cardType, boardState))
        {
            player.sendMessage("系統錯誤,請聯繫管理員!");
            // 退還金幣
            player.addItem(null, config.getPriceItemId(), config.getPriceCount(), npc, true);
            return;
        }

        // 更新今日購買次數
        int todayCount = pv.getInt(PV_PREFIX + "daily_count_" + cardType, 0);
        pv.set(PV_PREFIX + "daily_count_" + cardType, todayCount + 1);

        player.sendMessage("========================================");
        player.sendMessage("成功購買【" + config.getName() + "】!");
        player.sendMessage("消耗: " + formatNumber(config.getPriceCount()) + " 金幣");
        player.sendMessage("========================================");

        // 直接顯示刮刮樂界面
        showScratchBoard(player, npc);
    }

    /**
     * 生成隨機盤面
     */
    private String generateBoardState(ScratchCardConfig config)
    {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < config.getGridSize(); i++)
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

        // 獲取配置
        ScratchCardConfig config = ScratchCardData.getInstance().getConfig(state.cardType);
        if (config == null)
        {
            player.sendMessage("刮刮樂配置錯誤!");
            showMainMenu(player, npc);
            return;
        }

        NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
        html.setFile(player, HTML_PATH + "board.htm");

        // 解析盤面
        String[] symbols = state.boardState.split(",");
        Set<Integer> openedSet = parseOpenedPositions(state.openedPositions);

        // 替換變數
        html.replace("%card_name%", config.getName());
        html.replace("%opened_count%", String.valueOf(state.openedCount));
        html.replace("%total_count%", String.valueOf(symbols.length));
        html.replace("%accumulated%", String.valueOf(state.accumulatedCount));
        html.replace("%board%", generateBoardHtml(symbols, openedSet));

        player.sendPacket(html);
    }

    /**
     * 生成盤面HTML
     */
    private String generateBoardHtml(String[] symbols, Set<Integer> openedSet)
    {
        StringBuilder sb = new StringBuilder();
        int cols = 4;
        int totalSymbols = symbols.length;
        int rows = (int) Math.ceil((double) totalSymbols / cols);

        for (int row = 0; row < rows; row++)
        {
            sb.append("<tr>");
            for (int col = 0; col < cols; col++)
            {
                int position = row * cols + col;

                if (position >= totalSymbols)
                {
                    sb.append("<td width=\"32\" height=\"32\"></td>");
                    continue;
                }

                if (openedSet.contains(position))
                {
                    sb.append("<td align=\"center\" width=\"32\" height=\"32\">");
                    int symbolType = Integer.parseInt(symbols[position]);
                    String iconName = getSymbolIcon(symbolType);
                    sb.append("<img src=\"").append(iconName).append("\" width=\"32\" height=\"32\">");
                    sb.append("</td>");
                }
                else
                {
                    sb.append("<td align=\"center\" width=\"32\" height=\"32\">");
                    sb.append("<button action=\"bypass -h Quest ScratchCard scratch_").append(position);
                    sb.append("\" value=\"?\" width=\"32\" height=\"32\" ");
                    sb.append("back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
                    sb.append("</td>");
                }
            }
            sb.append("</tr>");

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
            case 0: return "icon.card_devil_black";
            case 1: return "icon.card_event_dragon_pocket";
            case 2: return "icon.ev_211020_vip_token_i02";
            default: return "icon.card_devil_black";
        }
    }

    /**
     * 處理刮開格子
     */
    private void handleScratch(Player player, Npc npc, int position)
    {
        ScratchCardState state = ScratchCardDAO.loadState(player.getObjectId());
        if (state == null)
        {
            player.sendMessage("你目前沒有刮刮樂!");
            showMainMenu(player, npc);
            return;
        }

        ScratchCardConfig config = ScratchCardData.getInstance().getConfig(state.cardType);
        if (config == null)
        {
            player.sendMessage("刮刮樂配置錯誤!");
            return;
        }

        Set<Integer> openedSet = parseOpenedPositions(state.openedPositions);
        if (openedSet.contains(position))
        {
            player.sendMessage("這個格子已經刮開了!");
            showScratchBoard(player, npc);
            return;
        }

        String[] symbols = state.boardState.split(",");
        int symbolType = Integer.parseInt(symbols[position]);

        openedSet.add(position);
        state.openedPositions = buildOpenedPositions(openedSet);
        state.openedCount++;

        boolean isComplete = false;
        switch (symbolType)
        {
            case 0:
                player.sendMessage("很遺憾,什麼都沒有...");
                break;
            case 1:
                state.accumulatedCount++;
                player.sendMessage("恭喜!累積 +1 (當前:" + state.accumulatedCount + ")");
                break;
            case 2:
                player.sendMessage("★★★ 恭喜中神秘大獎! ★★★");
                giveJackpotReward(player, npc, config);
                isComplete = true;
                break;
        }

        ScratchCardDAO.updateState(player.getObjectId(), state.openedPositions,
                state.openedCount, state.accumulatedCount);

        if (state.openedCount >= symbols.length || isComplete)
        {
            completeCard(player, npc, state, config, isComplete);
        }
        else
        {
            showScratchBoard(player, npc);
        }
    }

    /**
     * 發放神秘大獎
     */
    private void giveJackpotReward(Player player, Npc npc, ScratchCardConfig config)
    {
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

        Broadcast.toAllOnlinePlayers(new CreatureSay(null, ChatType.BATTLEFIELD, "刮刮樂系統",
                "恭喜玩家 " + player.getName() + " 在【" + config.getName() + "】中刮中神秘大獎!"));
    }

    /**
     * 完成刮刮樂
     */
    private void completeCard(Player player, Npc npc, ScratchCardState state,
                              ScratchCardConfig config, boolean isJackpot)
    {
        if (!isJackpot && state.accumulatedCount > 0)
        {
            giveAccumulateReward(player, npc, config, state.accumulatedCount);
        }

        ScratchCardDAO.deleteState(player.getObjectId());

        player.sendMessage("========================================");
        player.sendMessage("【" + config.getName() + "】已完成!");
        player.sendMessage("========================================");

        showMainMenu(player, npc);
    }

    /**
     * 發放累積獎勵
     */
    private void giveAccumulateReward(Player player, Npc npc, ScratchCardConfig config, int count)
    {
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
    private void showRewardList(Player player, Npc npc, int cardType)
    {
        ScratchCardConfig config = ScratchCardData.getInstance().getConfig(cardType);
        if (config == null)
        {
            player.sendMessage("無效的刮刮樂類型!");
            return;
        }

        NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
        html.setFile(player, HTML_PATH + "rewards.htm");

        html.replace("%card_name%", config.getName());

        // 生成累積獎勵列表
        StringBuilder accRewards = new StringBuilder();
        List<Integer> sortedCounts = new ArrayList<>(config.getAccumulateRewards().keySet());
        sortedCounts.sort(Integer::compareTo);

        for (int count : sortedCounts)
        {
            List<RewardItem> items = config.getAccumulateRewards().get(count);
            accRewards.append("<tr bgcolor=\"222222\"><td colspan=\"2\" height=\"25\">");
            accRewards.append("<font color=\"FFCC33\">累積 ").append(count).append(" 個:</font>");
            accRewards.append("</td></tr>");

            for (RewardItem item : items)
            {
                ItemTemplate template = ItemData.getInstance().getTemplate(item.getItemId());
                String itemName = template != null ? template.getName() : "未知道具";
                accRewards.append("<tr bgcolor=\"222222\"><td width=\"70\"><font color=\"00FF66\">")
                        .append(itemName).append("</font></td>");
                accRewards.append("<td width=\"180\" ><font color=\"FFFF00\">x")
                        .append(formatNumber(item.getAmount())).append("</font></td></tr>");
            }
        }

        html.replace("%accumulate_rewards%", accRewards.toString());
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
            if (!first) sb.append(",");
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
        System.out.println("【系統】多類型刮刮樂系統載入完畢!");
    }
}