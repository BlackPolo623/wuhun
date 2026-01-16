package custom.PotentialCube;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.l2jmobius.gameserver.data.xml.PotentialDAO;
import org.l2jmobius.gameserver.data.xml.PotentialData;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.data.holders.PotentialSlotHolder;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.network.serverpackets.SkillList;

public class PotentialCube extends Script
{
    // NPC ID
    private static final int NPC_ID = 900020;

    // 道具ID
    private static final int ITEM_NORMAL = 111000;
    private static final int ITEM_LOCK = 111001;
    private static final int ITEM_COMBINE = 111002;
    private static final int ITEM_FREE = 111003;

    // HTML路徑
    private static final String HTML_PATH = "data/scripts/custom/PotentialCube/";

    // 待選擇類型
    private static final int PENDING_TYPE_COMBINE = 1;
    private static final int PENDING_TYPE_FREE = 2;

    public PotentialCube()
    {
        addStartNpc(NPC_ID);
        addFirstTalkId(NPC_ID);
        addTalkId(NPC_ID);
    }

    @Override
    public String onFirstTalk(Npc npc, Player player)
    {
        showMainPage(player);
        return null;
    }

    @Override
    public String onEvent(String event, Npc npc, Player player)
    {
        if (event.equals("main"))
        {
            showMainPage(player);
        }
        else if (event.equals("reroll_normal"))
        {
            processNormalReroll(player);
        }
        else if (event.equals("lock_select"))
        {
            showLockSelectPage(player);
        }
        else if (event.startsWith("lock_reroll_"))
        {
            int lockedSlot = Integer.parseInt(event.substring(12));
            processLockReroll(player, lockedSlot);
        }
        else if (event.equals("reroll_combine"))
        {
            processCombineReroll(player);
        }
        else if (event.startsWith("combine_apply_"))
        {
            String choice = event.substring(14);
            applyCombineResult(player, choice);
        }
        else if (event.equals("reroll_free"))
        {
            processFreeReroll(player);
        }
        else if (event.startsWith("free_select_"))
        {
            int skillId = Integer.parseInt(event.substring(12));
            toggleFreeSelection(player, skillId);
        }
        else if (event.equals("free_confirm"))
        {
            applyFreeSelection(player);
        }

        return null;
    }

    // ==================== 主頁面 ====================

    private void showMainPage(Player player)
    {
        // 檢查是否有未完成的選擇
        String[] pending = PotentialDAO.loadPendingChoice(player.getObjectId());
        if (pending != null)
        {
            int pendingType = Integer.parseInt(pending[0]);
            String oldData = pending[1];
            String newData = pending[2];

            if (pendingType == PENDING_TYPE_COMBINE)
            {
                // 恢復結合選擇頁面
                Map<Integer, Integer> oldPotentials = deserializePotentials(oldData);
                Map<Integer, Integer> newPotentials = deserializePotentials(newData);
                showCombineSelectPage(player, oldPotentials, newPotentials);
                return;
            }
            else if (pendingType == PENDING_TYPE_FREE)
            {
                // 恢復自由選擇頁面
                showFreeSelectPageFromDb(player, oldData, newData);
                return;
            }
        }

        Map<Integer, Integer> potentials = PotentialDAO.loadPotentials(player.getObjectId());

        // 如果沒有潛能，初始化
        if (potentials.isEmpty())
        {
            initializePotentials(player, potentials);
        }

        NpcHtmlMessage html = new NpcHtmlMessage(0, 1);
        html.setFile(player, HTML_PATH + "main.htm");
        html.replace("%current_potentials%", buildPotentialDisplay(potentials, false));
        player.sendPacket(html);
    }

    private void initializePotentials(Player player, Map<Integer, Integer> potentials)
    {
        for (PotentialSlotHolder slot : PotentialData.getInstance().getAllSlots())
        {
            int skillId = PotentialData.getInstance().getRandomSkillForSlot(slot.getSlotId());
            potentials.put(slot.getSlotId(), skillId);
            PotentialDAO.savePotential(player.getObjectId(), slot.getSlotId(), skillId);
        }
        applyPotentialSkills(player, potentials);
    }

    // ==================== 普通重骰 ====================

    private void processNormalReroll(Player player)
    {
        if (!checkAndConsumeItem(player, ITEM_NORMAL, 1))
        {
            player.sendMessage("你沒有普通潛能方塊！");
            showMainPage(player);
            return;
        }

        Map<Integer, Integer> newPotentials = new HashMap<>();

        // 移除舊技能
        Map<Integer, Integer> oldPotentials = PotentialDAO.loadPotentials(player.getObjectId());
        removePotentialSkills(player, oldPotentials);

        // 重新隨機所有潛能
        for (PotentialSlotHolder slot : PotentialData.getInstance().getAllSlots())
        {
            int skillId = PotentialData.getInstance().getRandomSkillForSlot(slot.getSlotId());
            newPotentials.put(slot.getSlotId(), skillId);
            PotentialDAO.savePotential(player.getObjectId(), slot.getSlotId(), skillId);
        }

        // 套用新技能
        applyPotentialSkills(player, newPotentials);

        player.sendMessage("========================================");
        player.sendMessage("成功重骰所有潛能！");
        player.sendMessage("========================================");

        showMainPage(player);
    }

    // ==================== 鎖定重骰 ====================

    private void showLockSelectPage(Player player)
    {
        if (!hasItem(player, ITEM_LOCK, 1))
        {
            player.sendMessage("你沒有鎖定潛能方塊！");
            showMainPage(player);
            return;
        }

        Map<Integer, Integer> potentials = PotentialDAO.loadPotentials(player.getObjectId());

        NpcHtmlMessage html = new NpcHtmlMessage(0, 1);
        html.setFile(player, HTML_PATH + "lock_select.htm");

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Integer, Integer> entry : potentials.entrySet())
        {
            int slot = entry.getKey();
            int skillId = entry.getValue();
            String skillName = getSkillName(skillId);
            String color = PotentialData.getInstance().getSkillColor(skillId);

            sb.append("<table width=\"280\">");
            sb.append("<tr>");
            sb.append("<td width=\"180\" align=\"left\"><font color=\"").append(color).append("\">")
                    .append("槽位").append(slot).append(": ").append(skillName).append("</font></td>");
            sb.append("<td width=\"100\" align=\"right\">");
            sb.append("<button action=\"bypass -h Quest PotentialCube lock_reroll_").append(slot)
                    .append("\" value=\"鎖定此潛能\" width=\"90\" height=\"20\" back=\"L2UI_CT1.Button_DF\" fore=\"L2UI_CT1.Button_DF\">");
            sb.append("</td>");
            sb.append("</tr>");
            sb.append("</table>");
            sb.append("<br>");
        }

        html.replace("%potential_list%", sb.toString());
        player.sendPacket(html);
    }

    private void processLockReroll(Player player, int lockedSlot)
    {
        if (!checkAndConsumeItem(player, ITEM_LOCK, 1))
        {
            player.sendMessage("你沒有鎖定潛能方塊！");
            showMainPage(player);
            return;
        }

        Map<Integer, Integer> potentials = PotentialDAO.loadPotentials(player.getObjectId());

        // 移除舊技能
        removePotentialSkills(player, potentials);

        // 重新隨機未鎖定的潛能
        for (PotentialSlotHolder slot : PotentialData.getInstance().getAllSlots())
        {
            if (slot.getSlotId() != lockedSlot)
            {
                int skillId = PotentialData.getInstance().getRandomSkillForSlot(slot.getSlotId());
                potentials.put(slot.getSlotId(), skillId);
                PotentialDAO.savePotential(player.getObjectId(), slot.getSlotId(), skillId);
            }
        }

        // 套用新技能
        applyPotentialSkills(player, potentials);

        player.sendMessage("========================================");
        player.sendMessage("成功重骰未鎖定的潛能！");
        player.sendMessage("槽位" + lockedSlot + "已保留");
        player.sendMessage("========================================");

        showMainPage(player);
    }

    // ==================== 結合重骰 ====================

    private void processCombineReroll(Player player)
    {
        // 先扣除道具
        if (!checkAndConsumeItem(player, ITEM_COMBINE, 1))
        {
            player.sendMessage("你沒有結合潛能方塊！");
            showMainPage(player);
            return;
        }

        // 保存舊的潛能
        Map<Integer, Integer> oldPotentials = PotentialDAO.loadPotentials(player.getObjectId());

        // 生成新的潛能
        Map<Integer, Integer> newPotentials = new HashMap<>();
        for (PotentialSlotHolder slot : PotentialData.getInstance().getAllSlots())
        {
            int skillId = PotentialData.getInstance().getRandomSkillForSlot(slot.getSlotId());
            newPotentials.put(slot.getSlotId(), skillId);
        }

        // 持久化到數據庫
        String oldData = serializePotentials(oldPotentials);
        String newData = serializePotentials(newPotentials);
        PotentialDAO.savePendingChoice(player.getObjectId(), PENDING_TYPE_COMBINE, oldData, newData);

        player.sendMessage("道具已消耗，請選擇要套用的結果");

        // 顯示選擇頁面
        showCombineSelectPage(player, oldPotentials, newPotentials);
    }

    private void showCombineSelectPage(Player player, Map<Integer, Integer> oldPotentials, Map<Integer, Integer> newPotentials)
    {
        NpcHtmlMessage html = new NpcHtmlMessage(0, 1);
        html.setFile(player, HTML_PATH + "combine_select.htm");
        html.replace("%old_potentials%", buildPotentialDisplay(oldPotentials, true));
        html.replace("%new_potentials%", buildPotentialDisplay(newPotentials, true));
        player.sendPacket(html);
    }

    private void applyCombineResult(Player player, String choice)
    {
        String[] pending = PotentialDAO.loadPendingChoice(player.getObjectId());
        if (pending == null)
        {
            player.sendMessage("未找到待選擇記錄！");
            showMainPage(player);
            return;
        }

        Map<Integer, Integer> oldPotentials = deserializePotentials(pending[1]);
        Map<Integer, Integer> newPotentials = deserializePotentials(pending[2]);
        Map<Integer, Integer> selectedPotentials;

        if ("old".equals(choice))
        {
            selectedPotentials = oldPotentials;
            player.sendMessage("已選擇保留原本的潛能");
        }
        else
        {
            selectedPotentials = newPotentials;
            player.sendMessage("已套用新的潛能");
        }

        // 移除舊技能
        Map<Integer, Integer> currentPotentials = PotentialDAO.loadPotentials(player.getObjectId());
        removePotentialSkills(player, currentPotentials);

        // 保存並套用選擇的潛能
        for (Map.Entry<Integer, Integer> entry : selectedPotentials.entrySet())
        {
            PotentialDAO.savePotential(player.getObjectId(), entry.getKey(), entry.getValue());
        }
        applyPotentialSkills(player, selectedPotentials);

        // 清理待選擇記錄
        PotentialDAO.deletePendingChoice(player.getObjectId());

        showMainPage(player);
    }

    // ==================== 自由重骰 ====================

    private void processFreeReroll(Player player)
    {
        // 先扣除道具
        if (!checkAndConsumeItem(player, ITEM_FREE, 1))
        {
            player.sendMessage("你沒有自由潛能方塊！");
            showMainPage(player);
            return;
        }

        // 生成8個隨機選項
        List<Integer> options = new ArrayList<>();
        for (int i = 0; i < 8; i++)
        {
            int slotIndex = (i % 4) + 1;
            int skillId = PotentialData.getInstance().getRandomSkillForSlot(slotIndex);
            options.add(skillId);
        }

        // 持久化到數據庫
        String oldData = serializeIntList(options);  // options作為oldData
        String newData = "";  // 空字符串表示尚未選擇
        PotentialDAO.savePendingChoice(player.getObjectId(), PENDING_TYPE_FREE, oldData, newData);

        player.sendMessage("道具已消耗，請選擇4個潛能");

        showFreeSelectPage(player, options, new ArrayList<>());
    }

    private void showFreeSelectPageFromDb(Player player, String oldData, String newData)
    {
        List<Integer> options = deserializeIntList(oldData);
        List<Integer> selected = newData.isEmpty() ? new ArrayList<>() : deserializeIntList(newData);
        showFreeSelectPage(player, options, selected);
    }

    private void showFreeSelectPage(Player player, List<Integer> options, List<Integer> selected)
    {
        NpcHtmlMessage html = new NpcHtmlMessage(0, 1);
        html.setFile(player, HTML_PATH + "free_select.htm");

        StringBuilder sb = new StringBuilder();
        sb.append("<table width=\"280\" bgcolor=\"111111\" border=\"0\" cellspacing=\"1\" cellpadding=\"3\">");

        for (int i = 0; i < options.size(); i++)
        {
            int skillId = options.get(i);
            String skillName = getSkillName(skillId);
            String color = PotentialData.getInstance().getSkillColor(skillId);
            boolean isSelected = selected.contains(skillId);

            sb.append("<tr bgcolor=\"222222\">");
            sb.append("<td width=\"180\" align=\"left\">");
            if (isSelected)
            {
                sb.append("<font color=\"00FF00\">✓ </font>");
            }
            sb.append("<font color=\"").append(color).append("\">").append(skillName).append("</font>");
            sb.append("</td>");
            sb.append("<td width=\"100\" align=\"right\">");

            if (isSelected)
            {
                sb.append("<button action=\"bypass -h Quest PotentialCube free_select_").append(skillId)
                        .append("\" value=\"取消選擇\" width=\"90\" height=\"20\" back=\"L2UI_CT1.Button_DF\" fore=\"L2UI_CT1.Button_DF\">");
            }
            else if (selected.size() < 4)
            {
                sb.append("<button action=\"bypass -h Quest PotentialCube free_select_").append(skillId)
                        .append("\" value=\"選擇\" width=\"90\" height=\"20\" back=\"L2UI_CT1.Button_DF\" fore=\"L2UI_CT1.Button_DF\">");
            }
            else
            {
                sb.append("<font color=\"808080\">已滿</font>");
            }

            sb.append("</td>");
            sb.append("</tr>");
        }

        sb.append("</table>");

        html.replace("%potential_list%", sb.toString());
        html.replace("%selected_count%", String.valueOf(selected.size()));

        // 確認按鈕
        if (selected.size() == 4)
        {
            html.replace("%confirm_button%", "<tr><td align=\"center\"><button action=\"bypass -h Quest PotentialCube free_confirm\" value=\"確認套用\" width=\"200\" height=\"25\" back=\"L2UI_CT1.Button_DF\" fore=\"L2UI_CT1.Button_DF\"></td></tr>");
        }
        else
        {
            html.replace("%confirm_button%", "");
        }

        player.sendPacket(html);
    }

    private void toggleFreeSelection(Player player, int skillId)
    {
        String[] pending = PotentialDAO.loadPendingChoice(player.getObjectId());
        if (pending == null || Integer.parseInt(pending[0]) != PENDING_TYPE_FREE)
        {
            player.sendMessage("未找到待選擇記錄！");
            showMainPage(player);
            return;
        }

        List<Integer> options = deserializeIntList(pending[1]);
        List<Integer> selected = pending[2].isEmpty() ? new ArrayList<>() : deserializeIntList(pending[2]);

        if (selected.contains(skillId))
        {
            selected.remove(Integer.valueOf(skillId));
        }
        else if (selected.size() < 4)
        {
            selected.add(skillId);
        }

        // 更新數據庫
        String newData = serializeIntList(selected);
        PotentialDAO.savePendingChoice(player.getObjectId(), PENDING_TYPE_FREE, pending[1], newData);

        showFreeSelectPage(player, options, selected);
    }

    private void applyFreeSelection(Player player)
    {
        String[] pending = PotentialDAO.loadPendingChoice(player.getObjectId());
        if (pending == null || Integer.parseInt(pending[0]) != PENDING_TYPE_FREE)
        {
            player.sendMessage("未找到待選擇記錄！");
            showMainPage(player);
            return;
        }

        List<Integer> selected = deserializeIntList(pending[2]);

        if (selected.size() != 4)
        {
            player.sendMessage("請選擇4個潛能！");
            List<Integer> options = deserializeIntList(pending[1]);
            showFreeSelectPage(player, options, selected);
            return;
        }

        // 移除舊技能
        Map<Integer, Integer> oldPotentials = PotentialDAO.loadPotentials(player.getObjectId());
        removePotentialSkills(player, oldPotentials);

        // 套用選擇的潛能
        Map<Integer, Integer> newPotentials = new HashMap<>();
        for (int i = 0; i < 4; i++)
        {
            int slotIndex = i + 1;
            int skillId = selected.get(i);
            newPotentials.put(slotIndex, skillId);
            PotentialDAO.savePotential(player.getObjectId(), slotIndex, skillId);
        }

        applyPotentialSkills(player, newPotentials);

        // 清理待選擇記錄
        PotentialDAO.deletePendingChoice(player.getObjectId());

        player.sendMessage("========================================");
        player.sendMessage("成功套用選擇的潛能！");
        player.sendMessage("========================================");

        showMainPage(player);
    }

    // ==================== 序列化/反序列化 ====================

    private String serializePotentials(Map<Integer, Integer> potentials)
    {
        // 格式: slot1:skillId1,slot2:skillId2,slot3:skillId3,slot4:skillId4
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 4; i++)
        {
            if (i > 1)
            {
                sb.append(",");
            }
            sb.append(i).append(":").append(potentials.get(i));
        }
        return sb.toString();
    }

    private Map<Integer, Integer> deserializePotentials(String data)
    {
        Map<Integer, Integer> result = new HashMap<>();
        if (data == null || data.isEmpty())
        {
            return result;
        }

        String[] pairs = data.split(",");
        for (String pair : pairs)
        {
            String[] parts = pair.split(":");
            int slot = Integer.parseInt(parts[0]);
            int skillId = Integer.parseInt(parts[1]);
            result.put(slot, skillId);
        }
        return result;
    }

    private String serializeIntList(List<Integer> list)
    {
        // 格式: skillId1,skillId2,skillId3,...
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++)
        {
            if (i > 0)
            {
                sb.append(",");
            }
            sb.append(list.get(i));
        }
        return sb.toString();
    }

    private List<Integer> deserializeIntList(String data)
    {
        List<Integer> result = new ArrayList<>();
        if (data == null || data.isEmpty())
        {
            return result;
        }

        String[] ids = data.split(",");
        for (String id : ids)
        {
            result.add(Integer.parseInt(id));
        }
        return result;
    }

    // ==================== 輔助方法 ====================

    private String buildPotentialDisplay(Map<Integer, Integer> potentials, boolean compact)
    {
        StringBuilder sb = new StringBuilder();

        for (int i = 1; i <= 4; i++)
        {
            Integer skillId = potentials.get(i);
            if (skillId == null)
            {
                continue;
            }

            String skillName = getSkillName(skillId);
            String color = PotentialData.getInstance().getSkillColor(skillId);

            sb.append("<tr bgcolor=\"222222\">");

            if (compact)
            {
                sb.append("<td align=\"left\" width=\"140\">槽位").append(i).append("</td>");
                sb.append("<td align=\"right\" width=\"140\"><font color=\"").append(color).append("\">")
                        .append(skillName).append("</font></td>");
            }
            else
            {
                sb.append("<td align=\"center\" colspan=\"2\">");
                sb.append("<font color=\"FFCC33\">槽位").append(i).append(": </font>");
                sb.append("<font color=\"").append(color).append("\">").append(skillName).append("</font>");
                sb.append("</td>");
            }

            sb.append("</tr>");
        }

        return sb.toString();
    }

    private String getSkillName(int skillId)
    {
        Skill skill = SkillData.getInstance().getSkill(skillId, 1);
        return skill != null ? skill.getName() : "未知技能";
    }

    private void applyPotentialSkills(Player player, Map<Integer, Integer> potentials)
    {
        for (int skillId : potentials.values())
        {
            Skill skill = SkillData.getInstance().getSkill(skillId, 1);
            if (skill != null)
            {
                player.addSkill(skill, true);
            }
        }
        player.sendPacket(new SkillList());
        player.broadcastUserInfo();
    }

    private void removePotentialSkills(Player player, Map<Integer, Integer> potentials)
    {
        for (int skillId : potentials.values())
        {
            player.removeSkill(skillId);
        }
        player.sendPacket(new SkillList());
    }

    private boolean hasItem(Player player, int itemId, long count)
    {
        return player.getInventory().getInventoryItemCount(itemId, -1) >= count;
    }

    private boolean checkAndConsumeItem(Player player, int itemId, long count)
    {
        if (!hasItem(player, itemId, count))
        {
            return false;
        }

        player.destroyItemByItemId(null, itemId, count, null, true);
        return true;
    }

    public static void main(String[] args)
    {
        new PotentialCube();
        System.out.println("【系統】潛能方塊系統載入完畢！");
    }
}