package custom.LifeSystem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.ItemTemplate;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.model.variables.PlayerVariables;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.CreatureSay;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * 生活系統 - Life System
 * @author 黑普羅
 */
public class LifeSystem extends Script
{
	// ==================== 生活技能類型 ====================
	public enum LifeSkillType
	{
		GATHERING("採集", "LifeSystem_Gathering", 110001, "鐮刀"),
		MINING("挖礦", "LifeSystem_Mining", 110002, "鎬子"),
		LOGGING("伐木", "LifeSystem_Logging", 110003, "斧頭"),
		FISHING("釣魚", "LifeSystem_Fishing", 110004, "魚竿");
		
		private final String name;
		private final String varPrefix;
		private final int toolItemId;
		private final String toolName;
		
		LifeSkillType(String name, String varPrefix, int toolItemId, String toolName)
		{
			this.name = name;
			this.varPrefix = varPrefix;
			this.toolItemId = toolItemId;
			this.toolName = toolName;
		}
		
		public String getName()
		{
			return name;
		}
		
		public String getVarPrefix()
		{
			return varPrefix;
		}
		
		public int getToolItemId()
		{
			return toolItemId;
		}
		
		public String getToolName()
		{
			return toolName;
		}
	}

	// ==================== 資源物品配置 ====================
	public static class ResourceItem
	{
		int minLevel;        // 最低等級要求
		int itemId;          // 物品ID
		int minCount;        // 最小數量
		int maxCount;        // 最大數量
		int dropChance;      // 掉落機率 (0-100)
		int bonusExp;        // 額外獎勵經驗（獲得此物品時的額外經驗）

		public ResourceItem(int minLevel, int itemId, int minCount, int maxCount, int dropChance, int bonusExp)
		{
			this.minLevel = minLevel;
			this.itemId = itemId;
			this.minCount = minCount;
			this.maxCount = maxCount;
			this.dropChance = dropChance;
			this.bonusExp = bonusExp;
		}
	}

	// ==================== 資源NPC配置 ====================
	public static class ResourceNpc
	{
		int npcId;
		String npcName;
		LifeSkillType skillType;
		int cooldownSeconds;
		int minBaseExp;          // 新增：最小基礎經驗
		int maxBaseExp;          // 新增：最大基礎經驗
		List<ResourceItem> items;

		public ResourceNpc(int npcId, String npcName, LifeSkillType skillType, int cooldownSeconds, int minBaseExp, int maxBaseExp)
		{
			this.npcId = npcId;
			this.npcName = npcName;
			this.skillType = skillType;
			this.cooldownSeconds = cooldownSeconds;
			this.minBaseExp = minBaseExp;
			this.maxBaseExp = maxBaseExp;
			this.items = new ArrayList<>();
		}

		public void addItem(int minLevel, int itemId, int minCount, int maxCount, int dropChance, int bonusExp)
		{
			items.add(new ResourceItem(minLevel, itemId, minCount, maxCount, dropChance, bonusExp));
		}
	}
	
	// ==================== 常數配置 ====================
	private static final String HTML_PATH = "data/scripts/custom/LifeSystem/";
	private static final int MAX_LEVEL = 100;
	
	// 導師NPC ID
	private static final int GATHERING_MASTER = 62000;  // 採集導師
	private static final int MINING_MASTER = 62001;     // 挖礦導師
	private static final int LOGGING_MASTER = 62002;    // 伐木導師
	private static final int FISHING_MASTER = 62003;    // 釣魚導師
	
	// 等級經驗表
	private static final Map<Integer, Long> EXP_TABLE = new HashMap<>();
	
	// 資源NPC映射表
	private static final Map<Integer, ResourceNpc> RESOURCE_NPCS = new HashMap<>();
	
	// 導師NPC映射表
	private static final Map<Integer, LifeSkillType> MASTER_NPCS = new HashMap<>();

	static
	{
		// ==================== 生成經驗表 ====================
		for (int level = 1; level <= MAX_LEVEL; level++)
		{
			long requiredExp = (long) (10 * Math.pow(1.04, level - 1));
			EXP_TABLE.put(level, requiredExp);
		}

		// ==================== 註冊導師NPC ====================
		MASTER_NPCS.put(GATHERING_MASTER, LifeSkillType.GATHERING);
		MASTER_NPCS.put(MINING_MASTER, LifeSkillType.MINING);
		MASTER_NPCS.put(LOGGING_MASTER, LifeSkillType.LOGGING);
		MASTER_NPCS.put(FISHING_MASTER, LifeSkillType.FISHING);

		// ==================== 採集資源配置 ====================

		// 野花 (採集) - 基礎經驗 1~10
		ResourceNpc wildFlower = new ResourceNpc(62100, "野花", LifeSkillType.GATHERING, 30, 1, 10);
		wildFlower.addItem(1, 57, 1000, 2000, 100, 5);          // 金幣 + 額外5經驗
		wildFlower.addItem(1, 108000, 1, 2, 50, 3);             // 力量精華 + 額外3經驗
		wildFlower.addItem(10, 108001, 1, 1, 30, 0);            // Lv10+ 智慧精華
		wildFlower.addItem(20, 108002, 1, 2, 20, 0);            // Lv20+ 防護精華
		wildFlower.addItem(40, 97145, 1, 1, 10, 0);             // Lv40+ 稀有材料
		RESOURCE_NPCS.put(920001, wildFlower);

		// 藥草 (採集) - 基礎經驗 10~25
		ResourceNpc herbs = new ResourceNpc(62101, "藥草", LifeSkillType.GATHERING, 35, 10, 25);
		herbs.addItem(1, 57, 1500, 2500, 100, 8);
		herbs.addItem(5, 108003, 1, 2, 45, 5);
		herbs.addItem(15, 108004, 1, 1, 30, 0);
		herbs.addItem(30, 108005, 1, 2, 18, 0);
		herbs.addItem(50, 97145, 1, 2, 12, 0);
		RESOURCE_NPCS.put(920002, herbs);

		// 靈芝 (採集) - 基礎經驗 25~50
		ResourceNpc mushroom = new ResourceNpc(62103, "靈芝", LifeSkillType.GATHERING, 45, 25, 50);
		mushroom.addItem(1, 57, 2000, 3000, 100, 12);
		mushroom.addItem(10, 108006, 1, 2, 40, 8);
		mushroom.addItem(25, 108007, 1, 2, 25, 0);
		mushroom.addItem(45, 108008, 1, 1, 15, 0);
		mushroom.addItem(70, 105801, 1, 1, 5, 0);
		RESOURCE_NPCS.put(920003, mushroom);

		// ==================== 挖礦資源配置 ====================

		// 銅礦 (挖礦) - 基礎經驗 5~15
		ResourceNpc copperOre = new ResourceNpc(62104, "銅礦", LifeSkillType.MINING, 40, 5, 15);
		copperOre.addItem(1, 57, 2000, 3000, 100, 8);
		copperOre.addItem(1, 108003, 1, 2, 40, 5);
		copperOre.addItem(12, 108004, 1, 1, 25, 0);
		copperOre.addItem(25, 108005, 1, 2, 18, 0);
		copperOre.addItem(50, 97145, 1, 1, 10, 0);
		RESOURCE_NPCS.put(920011, copperOre);

		// 鐵礦 (挖礦) - 基礎經驗 15~30
		ResourceNpc ironOre = new ResourceNpc(62105, "鐵礦", LifeSkillType.MINING, 50, 15, 30);
		ironOre.addItem(1, 57, 2500, 4000, 100, 12);
		ironOre.addItem(10, 108006, 1, 2, 35, 8);
		ironOre.addItem(30, 108007, 1, 2, 22, 0);
		ironOre.addItem(55, 108008, 1, 1, 15, 0);
		ironOre.addItem(80, 105801, 1, 2, 8, 0);
		RESOURCE_NPCS.put(920012, ironOre);

		// 秘銀礦 (挖礦) - 基礎經驗 30~60
		ResourceNpc mithrilOre = new ResourceNpc(62106, "秘銀礦", LifeSkillType.MINING, 60, 30, 60);
		mithrilOre.addItem(1, 57, 3000, 5000, 100, 18);
		mithrilOre.addItem(20, 108000, 2, 3, 40, 12);
		mithrilOre.addItem(45, 108001, 1, 2, 28, 0);
		mithrilOre.addItem(65, 97145, 2, 3, 18, 0);
		mithrilOre.addItem(90, 105801, 1, 2, 10, 0);
		RESOURCE_NPCS.put(920013, mithrilOre);

		// ==================== 伐木資源配置 ====================

		// 白樺樹 (伐木) - 基礎經驗 3~12
		ResourceNpc birchTree = new ResourceNpc(62107, "白樺樹", LifeSkillType.LOGGING, 38, 3, 12);
		birchTree.addItem(1, 57, 1500, 2500, 100, 6);
		birchTree.addItem(1, 108000, 1, 2, 35, 4);
		birchTree.addItem(15, 108001, 1, 1, 28, 0);
		birchTree.addItem(28, 108002, 1, 2, 20, 0);
		birchTree.addItem(48, 97145, 1, 1, 12, 0);
		RESOURCE_NPCS.put(920021, birchTree);

		// 楓木 (伐木) - 基礎經驗 12~28
		ResourceNpc mapleTree = new ResourceNpc(62108, "楓木", LifeSkillType.LOGGING, 45, 12, 28);
		mapleTree.addItem(1, 57, 2000, 3500, 100, 10);
		mapleTree.addItem(8, 108003, 1, 2, 38, 6);
		mapleTree.addItem(22, 108004, 1, 2, 26, 0);
		mapleTree.addItem(42, 108005, 1, 1, 18, 0);
		mapleTree.addItem(68, 105801, 1, 1, 8, 0);
		RESOURCE_NPCS.put(920022, mapleTree);

		// 千年古木 (伐木) - 基礎經驗 28~55
		ResourceNpc ancientTree = new ResourceNpc(62109, "千年古木", LifeSkillType.LOGGING, 55, 28, 55);
		ancientTree.addItem(1, 57, 3000, 5000, 100, 15);
		ancientTree.addItem(18, 108006, 1, 2, 35, 10);
		ancientTree.addItem(38, 108007, 1, 2, 24, 0);
		ancientTree.addItem(60, 108008, 1, 1, 16, 0);
		ancientTree.addItem(85, 105801, 2, 3, 12, 0);
		RESOURCE_NPCS.put(920023, ancientTree);

		// ==================== 釣魚資源配置 ====================

		// 溪流 (釣魚) - 基礎經驗 8~20
		ResourceNpc stream = new ResourceNpc(62110, "溪流", LifeSkillType.FISHING, 45, 8, 20);
		stream.addItem(1, 57, 2000, 3500, 100, 8);
		stream.addItem(1, 108000, 1, 3, 50, 5);
		stream.addItem(12, 108001, 1, 2, 35, 0);
		stream.addItem(26, 108002, 1, 2, 24, 0);
		stream.addItem(46, 97145, 1, 1, 14, 0);
		RESOURCE_NPCS.put(920031, stream);

		// 湖泊 (釣魚) - 基礎經驗 20~40
		ResourceNpc lake = new ResourceNpc(62111, "湖泊", LifeSkillType.FISHING, 55, 20, 40);
		lake.addItem(1, 57, 2500, 4500, 100, 12);
		lake.addItem(8, 108003, 1, 2, 42, 8);
		lake.addItem(24, 108004, 1, 2, 30, 0);
		lake.addItem(44, 108005, 1, 2, 20, 0);
		lake.addItem(72, 105801, 1, 1, 10, 0);
		RESOURCE_NPCS.put(920032, lake);

		// 深海 (釣魚) - 基礎經驗 40~80
		ResourceNpc ocean = new ResourceNpc(62112, "深海", LifeSkillType.FISHING, 70, 40, 80);
		ocean.addItem(1, 57, 4000, 6000, 100, 20);
		ocean.addItem(15, 108006, 2, 3, 45, 15);
		ocean.addItem(35, 108007, 1, 2, 32, 0);
		ocean.addItem(58, 108008, 1, 2, 22, 0);
		ocean.addItem(88, 105801, 2, 3, 15, 0);
		RESOURCE_NPCS.put(920033, ocean);
	}
	
	public LifeSystem()
	{
		// 註冊所有導師NPC
		for (int masterId : MASTER_NPCS.keySet())
		{
			addStartNpc(masterId);
			addFirstTalkId(masterId);
			addTalkId(masterId);
		}
		
		// 註冊所有資源NPC
		for (ResourceNpc resource : RESOURCE_NPCS.values())
		{
			addStartNpc(resource.npcId);
			addFirstTalkId(resource.npcId);
			addTalkId(resource.npcId);
		}
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		int npcId = npc.getId();
		
		// 檢查是否為導師NPC
		if (MASTER_NPCS.containsKey(npcId))
		{
			LifeSkillType skillType = MASTER_NPCS.get(npcId);
			showMasterPage(player, npc, skillType);
			return null;
		}
		
		// 檢查是否為資源NPC
		if (RESOURCE_NPCS.containsKey(npcId))
		{
			ResourceNpc resource = RESOURCE_NPCS.get(npcId);
			showResourcePage(player, npc, resource);
			return null;
		}
		
		return null;
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (event.startsWith("collect_"))
		{
			int npcId = Integer.parseInt(event.substring(8));
			ResourceNpc resource = RESOURCE_NPCS.get(npcId);
			if (resource != null)
			{
				processCollection(player, npc, resource);
			}
		}
		
		return null;
	}
	
	// ==================== 顯示導師頁面 ====================
	private void showMasterPage(Player player, Npc npc, LifeSkillType skillType)
	{
		PlayerVariables pv = player.getVariables();
		
		int skillLevel = pv.getInt(skillType.getVarPrefix() + "_Level", 1);
		long currentExp = pv.getLong(skillType.getVarPrefix() + "_Exp", 0);
		long requiredExp = EXP_TABLE.getOrDefault(skillLevel, 1000L);
		
		NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
		html.setFile(player, HTML_PATH + "master.htm");
		
		html.replace("%skill_name%", skillType.getName());
		html.replace("%skill_level%", String.valueOf(skillLevel));
		html.replace("%current_exp%", formatNumber(currentExp));
		html.replace("%required_exp%", formatNumber(requiredExp));
		html.replace("%exp_percent%", String.format("%.1f", (currentExp * 100.0 / requiredExp)));
		html.replace("%tool_name%", skillType.getToolName());
		
		// 檢查工具
		boolean hasTool = player.getInventory().getItemByItemId(skillType.getToolItemId()) != null;
		String toolStatus = hasTool ? "<font color=\"00FF66\">✓ 已持有</font>" : "<font color=\"FF3333\">✗ 未持有</font>";
		html.replace("%tool_status%", toolStatus);
		
		player.sendPacket(html);
	}

	// ==================== 顯示資源頁面 ====================
	private void showResourcePage(Player player, Npc npc, ResourceNpc resource)
	{
		PlayerVariables pv = player.getVariables();

		int skillLevel = pv.getInt(resource.skillType.getVarPrefix() + "_Level", 1);
		long currentExp = pv.getLong(resource.skillType.getVarPrefix() + "_Exp", 0);
		long requiredExp = EXP_TABLE.getOrDefault(skillLevel, 1000L);

		// 檢查工具
		boolean hasTool = player.getInventory().getItemByItemId(resource.skillType.getToolItemId()) != null;

		// 檢查冷卻
		String cooldownKey = resource.skillType.getVarPrefix() + "_Cooldown_" + npc.getId();
		long lastCollectTime = pv.getLong(cooldownKey, 0);
		long currentTime = System.currentTimeMillis();
		long cooldownRemaining = ((lastCollectTime + (resource.cooldownSeconds * 1000L)) - currentTime) / 1000;

		NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
		html.setFile(player, HTML_PATH + "resource.htm");

		html.replace("%resource_name%", resource.npcName);
		html.replace("%skill_name%", resource.skillType.getName());
		html.replace("%skill_level%", String.valueOf(skillLevel));
		html.replace("%current_exp%", formatNumber(currentExp));
		html.replace("%required_exp%", formatNumber(requiredExp));
		html.replace("%exp_percent%", String.format("%.1f", (currentExp * 100.0 / requiredExp)));
		html.replace("%min_exp%", String.valueOf(resource.minBaseExp));  // 新增
		html.replace("%max_exp%", String.valueOf(resource.maxBaseExp));  // 新增

		// 工具狀態
		String toolStatus;
		if (hasTool)
		{
			toolStatus = "<font color=\"00FF66\">✓ " + resource.skillType.getToolName() + "</font>";
		}
		else
		{
			toolStatus = "<font color=\"FF3333\">✗ 需要" + resource.skillType.getToolName() + "</font>";
		}
		html.replace("%tool_status%", toolStatus);

		// 獎勵列表
		html.replace("%reward_list%", generateRewardList(resource, skillLevel));

		// 採集按鈕
		String collectButton;
		if (!hasTool)
		{
			collectButton = "<table width=\"280\"><tr><td align=center><font color=\"FF3333\">需要" + resource.skillType.getToolName() + "才能採集</font></td></tr></table>";
		}
		else if (cooldownRemaining > 0)
		{
			collectButton = "<table width=\"280\"><tr><td align=center><font color=\"FFFF00\">冷卻中: " + cooldownRemaining + " 秒</font></td></tr></table>";
		}
		else
		{
			collectButton = "<button action=\"bypass -h Quest LifeSystem collect_" + npc.getId() + "\" value=\"採集\" width=\"200\" height=\"31\" back=\"BranchSys3.icon2.ArmyTrainingInfo_down\" fore=\"BranchSys3.icon2.ArmyTrainingInfo\">";
		}
		html.replace("%collect_button%", collectButton);

		player.sendPacket(html);
	}

	// ==================== 處理採集 ====================
	private void processCollection(Player player, Npc npc, ResourceNpc resource)
	{
		PlayerVariables pv = player.getVariables();

		// 檢查工具
		if (player.getInventory().getItemByItemId(resource.skillType.getToolItemId()) == null)
		{
			player.sendMessage("你需要" + resource.skillType.getToolName() + "才能進行採集！");
			showResourcePage(player, npc, resource);
			return;
		}

		// 檢查冷卻
		String cooldownKey = resource.skillType.getVarPrefix() + "_Cooldown_" + npc.getId();
		long lastCollectTime = pv.getLong(cooldownKey, 0);
		long currentTime = System.currentTimeMillis();
		long cooldownMs = resource.cooldownSeconds * 1000L;

		if ((currentTime - lastCollectTime) < cooldownMs)
		{
			long cooldownRemaining = ((lastCollectTime + cooldownMs) - currentTime) / 1000;
			player.sendMessage("請等待 " + cooldownRemaining + " 秒後再次採集。");
			showResourcePage(player, npc, resource);
			return;
		}

		// 獲取技能等級
		int skillLevel = pv.getInt(resource.skillType.getVarPrefix() + "_Level", 1);
		long currentExp = pv.getLong(resource.skillType.getVarPrefix() + "_Exp", 0);

		// 處理獎勵
		List<String> obtainedItems = new ArrayList<>();

		// 基礎經驗（每次採集都會獲得）
		int baseExpGained = Rnd.get(resource.minBaseExp, resource.maxBaseExp);
		int totalExpGained = baseExpGained;

		for (ResourceItem item : resource.items)
		{
			// 等級檢查
			if (skillLevel < item.minLevel)
			{
				continue;
			}

			// 機率判定
			if (Rnd.get(100) < item.dropChance)
			{
				int count = Rnd.get(item.minCount, item.maxCount);
				player.addItem(ItemProcessType.NONE, item.itemId, count, npc, true);

				ItemTemplate itemTemplate = ItemData.getInstance().getTemplate(item.itemId);
				String itemName = itemTemplate != null ? itemTemplate.getName() : "物品#" + item.itemId;
				obtainedItems.add(itemName + " x" + count);

				// 額外經驗
				totalExpGained += item.bonusExp;
			}
		}

		// 更新冷卻
		pv.set(cooldownKey, currentTime);

		// 更新經驗
		long newExp = currentExp + totalExpGained;
		long requiredExp = EXP_TABLE.getOrDefault(skillLevel, 1000L);

		// 升級檢查
		boolean leveledUp = false;
		int oldLevel = skillLevel;
		while ((newExp >= requiredExp) && (skillLevel < MAX_LEVEL))
		{
			newExp -= requiredExp;
			skillLevel++;
			leveledUp = true;
			requiredExp = EXP_TABLE.getOrDefault(skillLevel, 1000L);
		}

		// 保存等級和經驗
		if (leveledUp)
		{
			pv.set(resource.skillType.getVarPrefix() + "_Level", skillLevel);
			player.sendPacket(new CreatureSay(null, ChatType.BATTLEFIELD, "生活系統",
					"恭喜！你的【" + resource.skillType.getName() + "】從 Lv." + oldLevel + " 提升至 Lv." + skillLevel + "！"));
		}
		pv.set(resource.skillType.getVarPrefix() + "_Exp", newExp);

		// 發送結果
		player.sendMessage("========================================");
		player.sendMessage("採集完成！");
		if (!obtainedItems.isEmpty())
		{
			player.sendMessage("獲得：");
			for (String item : obtainedItems)
			{
				player.sendMessage("  • " + item);
			}
		}
		else
		{
			player.sendMessage("什麼都沒有獲得...");
		}
		player.sendMessage("【" + resource.skillType.getName() + "】經驗 +" + totalExpGained);
		player.sendMessage("  (基礎經驗: " + baseExpGained + ")");
		player.sendMessage("========================================");
		npc.doDie(player);

	}
	
	// ==================== 生成獎勵列表 ====================
	private String generateRewardList(ResourceNpc resource, int playerLevel)
	{
		StringBuilder sb = new StringBuilder();
		
		for (ResourceItem item : resource.items)
		{
			ItemTemplate itemTemplate = ItemData.getInstance().getTemplate(item.itemId);
			String itemName = itemTemplate != null ? itemTemplate.getName() : "物品#" + item.itemId;
			
			String levelReq = item.minLevel > 1 ? " (Lv." + item.minLevel + "+)" : "";
			String chanceColor = item.dropChance >= 50 ? "00FF66" : (item.dropChance >= 20 ? "FFFF00" : "FF9900");
			
			boolean canObtain = playerLevel >= item.minLevel;
			String textColor = canObtain ? "FFFFFF" : "808080";
			
			sb.append("<tr bgcolor=\"222222\">");
			sb.append("<td width=\"140\"><font color=\"").append(textColor).append("\">").append(itemName).append(levelReq).append("</font></td>");
			sb.append("<td width=\"70\" align=\"center\"><font color=\"").append(chanceColor).append("\">").append(item.dropChance).append("%</font></td>");
			sb.append("<td width=\"70\" align=\"center\"><font color=\"").append(textColor).append("\">").append(item.minCount);
			if (item.maxCount > item.minCount)
			{
				sb.append("-").append(item.maxCount);
			}
			sb.append("</font></td>");
			sb.append("</tr>");
		}
		
		return sb.toString();
	}
	
	// ==================== 輔助方法 ====================
	private String formatNumber(long number)
	{
		return String.format("%,d", number);
	}
	
	public static void main(String[] args)
	{
		new LifeSystem();
		System.out.println("【系統】生活系統載入完畢！");
	}
}