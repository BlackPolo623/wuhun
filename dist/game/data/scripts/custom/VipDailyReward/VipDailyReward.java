/*
 * Copyright (c) 2013 L2jMobius
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package custom.VipDailyReward;

import org.l2jmobius.gameserver.config.VipSystemConfig;
import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.ItemTemplate;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.script.Script;


import java.util.HashMap;
import java.util.Map;

/**
 * VIP每日獎勵NPC
 * @author 黑普羅
 */
public class VipDailyReward extends Script
{
    // NPC ID
    private static final int VIP_REWARD_NPC = 900008;
    
    // PlayerVariables key
    private static final String LAST_REWARD_TIME = "VIP_DAILY_REWARD_TIME";
    
    // 每日重置時間（毫秒）- 24小時
    private static final long REWARD_COOLDOWN = 24 * 60 * 60 * 1000;
    
    // VIP等級獎勵配置
    // 格式: VIP等級 -> {道具ID, 數量}
    private static final Map<Integer, RewardData[]> VIP_REWARDS = new HashMap<>();
    
    static
    {
        
        // VIP 1級獎勵
        VIP_REWARDS.put(1, new RewardData[]
        {
            new RewardData(71177, 1),
        });
        
        // VIP 2級獎勵
        VIP_REWARDS.put(2, new RewardData[]
        {
            new RewardData(71178, 1),
        });
        
        // VIP 3級獎勵
        VIP_REWARDS.put(3, new RewardData[]
        {
                new RewardData(71179, 1),
        });
        
        // VIP 4級獎勵
        VIP_REWARDS.put(4, new RewardData[]
        {
                new RewardData(71180, 1),
        });
        
        // VIP 5級獎勵
        VIP_REWARDS.put(5, new RewardData[]
        {
                new RewardData(71181, 1),
        });
        
        // VIP 6級獎勵
        VIP_REWARDS.put(6, new RewardData[]
        {
                new RewardData(71182, 1),
        });
        
        // VIP 7級獎勵
        VIP_REWARDS.put(7, new RewardData[]
        {
                new RewardData(71183, 1),
        });
        
        // VIP 8級獎勵
        VIP_REWARDS.put(8, new RewardData[]
        {
                new RewardData(99027, 1),
        });
        
        // VIP 9級獎勵
        VIP_REWARDS.put(9, new RewardData[]
        {
                new RewardData(99028, 1),
        });
        
        // VIP 10級獎勵
        VIP_REWARDS.put(10, new RewardData[]
        {
                new RewardData(99029, 1),
        });
    }
    
    private VipDailyReward()
    {
        addStartNpc(VIP_REWARD_NPC);
        addTalkId(VIP_REWARD_NPC);
        addFirstTalkId(VIP_REWARD_NPC);
    }
    
    @Override
    public String onEvent(String event, Npc npc, Player player)
    {
        if (player == null)
        {
            return null;
        }
        
        String htmltext = null;
        
        switch (event)
        {
            case "claim_reward":
            {
                if (!VipSystemConfig.VIP_SYSTEM_ENABLED)
                {
                    player.sendMessage("VIP系統目前未開啟。");
                    return null;
                }
                
                // 檢查冷卻時間
                final long lastRewardTime = player.getVariables().getLong(LAST_REWARD_TIME, 0);
                final long currentTime = System.currentTimeMillis();
                
                if ((currentTime - lastRewardTime) < REWARD_COOLDOWN)
                {
                    final long remainingTime = REWARD_COOLDOWN - (currentTime - lastRewardTime);
                    final long hours = remainingTime / (60 * 60 * 1000);
                    final long minutes = (remainingTime % (60 * 60 * 1000)) / (60 * 1000);
                    
                    player.sendMessage("════════════════════════════════");
                    player.sendMessage("你今天已經領取過獎勵了！");
                    player.sendMessage("剩餘冷卻時間: " + hours + " 小時 " + minutes + " 分鐘");
                    player.sendMessage("════════════════════════════════");
                    htmltext = getHtm(player, "data/scripts/custom/VipDailyReward/VipDailyReward_claimed.htm");
                    htmltext = htmltext.replace("%hours%", String.valueOf(hours));
                    htmltext = htmltext.replace("%minutes%", String.valueOf(minutes));
                    return htmltext;
                }
                
                // 獲取玩家VIP等級
                final byte vipTier = player.getVipTier();
                
                // 獲取對應等級的獎勵
                final RewardData[] rewards = VIP_REWARDS.get((int) vipTier);
                
                if (rewards == null || rewards.length == 0)
                {
                    player.sendMessage("你的VIP等級沒有可領取的獎勵。");
                    return null;
                }
                
                // 檢查背包空間
                int requiredSlots = 0;
                for (RewardData reward : rewards)
                {
                    final ItemTemplate item = ItemData.getInstance().getTemplate(reward.itemId);
                    if (item != null && !item.isStackable())
                    {
                        requiredSlots += reward.count;
                    }
                    else
                    {
                        requiredSlots++;
                    }
                }
                
                if (player.getInventory().getSize() + requiredSlots > player.getInventoryLimit())
                {
                    player.sendMessage("背包空間不足，請整理後再來領取。");
                    return null;
                }
                
                // 發放獎勵
                player.sendMessage("════════════════════════════════");
                player.sendMessage("VIP " + vipTier + " 級每日獎勵");
                player.sendMessage("════════════════════════════════");
                
                for (RewardData reward : rewards)
                {
                    player.addItem(ItemProcessType.NONE, reward.itemId, reward.count, npc, true);
                    final ItemTemplate item = ItemData.getInstance().getTemplate(reward.itemId);
                    if (item != null)
                    {
                        player.sendMessage("獲得: " + item.getName() + " x" + reward.count);
                    }
                }
                
                player.sendMessage("════════════════════════════════");
                player.sendMessage("獎勵已發放完畢！");
                player.sendMessage("明天再來領取更多獎勵吧！");
                player.sendMessage("════════════════════════════════");
                
                // 更新領取時間
                player.getVariables().set(LAST_REWARD_TIME, currentTime);
                
                htmltext = getHtm(player, "data/scripts/custom/VipDailyReward/VipDailyReward_success.htm");
                htmltext = htmltext.replace("%vipTier%", String.valueOf(vipTier));
                break;
            }
        }
        
        return htmltext;
    }
    
    @Override
    public String onFirstTalk(Npc npc, Player player)
    {
        if (!VipSystemConfig.VIP_SYSTEM_ENABLED)
        {
            player.sendMessage("VIP系統目前未開啟。");
            return null;
        }
        
        String htmltext = getHtm(player, "data/scripts/custom/VipDailyReward/VipDailyReward.htm");
        
        final byte vipTier = player.getVipTier();
        final long vipPoints = player.getVipPoints();
        
        // 檢查今天是否已領取
        final long lastRewardTime = player.getVariables().getLong(LAST_REWARD_TIME, 0);
        final long currentTime = System.currentTimeMillis();
        final boolean canClaim = (currentTime - lastRewardTime) >= REWARD_COOLDOWN;
        
        htmltext = htmltext.replace("%vipTier%", String.valueOf(vipTier));
        htmltext = htmltext.replace("%vipPoints%", String.valueOf(vipPoints));
        htmltext = htmltext.replace("%canClaim%", canClaim ? "可以領取" : "已領取");
        htmltext = htmltext.replace("%claimStatus%", canClaim ?
                "<button value=\"領取今日獎勵\" action=\"bypass -h Quest VipDailyReward claim_reward\" width=\"250\" height=\"31\" back=\"BranchSys3.icon2.ArmyTrainingInfo_down\" fore=\"BranchSys3.icon2.ArmyTrainingInfo\">" :
            "<font color=\"999999\">今天已經領取過了</font>");
        
        return htmltext;
    }
    
    /**
     * 獎勵數據類
     */
    private static class RewardData
    {
        public final int itemId;
        public final long count;
        
        public RewardData(int itemId, long count)
        {
            this.itemId = itemId;
            this.count = count;
        }
    }
    
    public static void main(String[] args)
    {
        new VipDailyReward();
    }
}