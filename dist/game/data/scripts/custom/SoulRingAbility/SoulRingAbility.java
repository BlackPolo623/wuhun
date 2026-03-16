/**
 * 魂環能力分配系統
 */
package custom.SoulRingAbility;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.CreatureSay;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * 魂環能力分配系統
 * @author 黑普羅
 */
public class SoulRingAbility extends Script
{
	private static final int NPC_ID = 900013;
	private static final String SOUL_RING_VAR = "魂環";
	private static final double PERCENT_PER_POINT = 0.3;
	private static final int MAX_POINTS_PER_STAT = 350;
	private static final String SYSTEM_NAME = "魂環能力";

	public enum SoulRingStat
	{
		// PVE 一般怪物
		PVE_PHYS_ATK("PVE物理攻擊傷害", "SoulRing_PvePhysAtk"),
		PVE_PHYS_SKILL("PVE物理技能傷害", "SoulRing_PvePhysSkill"),
		PVE_MAGIC_SKILL("PVE魔法技能傷害", "SoulRing_PveMagicSkill"),
		PVE_PHYS_DEF("PVE物理攻擊防禦", "SoulRing_PvePhysDef"),
		PVE_PHYS_SKILL_DEF("PVE物理技能防禦", "SoulRing_PvePhysSkillDef"),
		PVE_MAGIC_DEF("PVE魔法技能防禦", "SoulRing_PveMagicDef"),

		// PVE 首領怪物
		PVE_RAID_PHYS_ATK("首領物理攻擊傷害", "SoulRing_RaidPhysAtk"),
		PVE_RAID_PHYS_SKILL("首領物理技能傷害", "SoulRing_RaidPhysSkill"),
		PVE_RAID_MAGIC_SKILL("首領魔法技能傷害", "SoulRing_RaidMagicSkill"),
		PVE_RAID_PHYS_DEF("首領物理攻擊防禦", "SoulRing_RaidPhysDef"),
		PVE_RAID_PHYS_SKILL_DEF("首領物理技能防禦", "SoulRing_RaidPhysSkillDef"),
		PVE_RAID_MAGIC_DEF("首領魔法技能防禦", "SoulRing_RaidMagicDef"),

		// PVP 對玩家
		PVP_PHYS_ATK("PVP物理攻擊傷害", "SoulRing_PvpPhysAtk"),
		PVP_PHYS_SKILL("PVP物理技能傷害", "SoulRing_PvpPhysSkill"),
		PVP_MAGIC_SKILL("PVP魔法技能傷害", "SoulRing_PvpMagicSkill"),
		PVP_PHYS_DEF("PVP物理攻擊防禦", "SoulRing_PvpPhysDef"),
		PVP_PHYS_SKILL_DEF("PVP物理技能防禦", "SoulRing_PvpPhysSkillDef"),
		PVP_MAGIC_DEF("PVP魔法技能防禦", "SoulRing_PvpMagicDef");

		private final String displayName;
		private final String varName;

		SoulRingStat(String displayName, String varName)
		{
			this.displayName = displayName;
			this.varName = varName;
		}

		public String getDisplayName()
		{
			return displayName;
		}

		public String getVarName()
		{
			return varName;
		}
	}

	private SoulRingAbility()
	{
		addStartNpc(NPC_ID);
		addTalkId(NPC_ID);
		addFirstTalkId(NPC_ID);
	}

	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		showIndexPage(player, npc);
		return null;
	}

	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (event.equals("index"))
		{
			showIndexPage(player, npc);
		}
		else if (event.equals("show_pve_normal"))
		{
			showPveNormalPage(player, npc);
		}
		else if (event.equals("show_pve_raid"))
		{
			showPveRaidPage(player, npc);
		}
		else if (event.equals("show_pvp"))
		{
			showPvpPage(player, npc);
		}
		else if (event.startsWith("addN_") || event.startsWith("subN_"))
		{
			final boolean isAdd = event.startsWith("addN_");
			// 格式：addN_STAT_NAME $amount（空格分隔輸入值）
			final String[] spaceParts = event.split(" ", 2);
			// spaceParts[0] = "addN_PVE_PHYS_ATK"，statName = "PVE_PHYS_ATK"
			final String statName = spaceParts[0].substring(5);

			int amount = 0;
			if (spaceParts.length >= 2 && !spaceParts[1].trim().isEmpty())
			{
				try
				{
					amount = Integer.parseInt(spaceParts[1].trim());
				}
				catch (NumberFormatException e)
				{
					player.sendMessage("請輸入有效的整數數字");
					if (statName.startsWith("PVP"))
						showPvpPage(player, npc);
					else if (statName.contains("RAID"))
						showPveRaidPage(player, npc);
					else
						showPveNormalPage(player, npc);
					return null;
				}
			}

			if (amount <= 0)
			{
				player.sendMessage("請先輸入要加點的數字");
				if (statName.startsWith("PVP"))
					showPvpPage(player, npc);
				else if (statName.contains("RAID"))
					showPveRaidPage(player, npc);
				else
					showPveNormalPage(player, npc);
				return null;
			}

			try
			{
				final SoulRingStat soulStat = SoulRingStat.valueOf(statName);
				if (isAdd)
					addPoints(player, soulStat, amount);
				else
					removePoints(player, soulStat, amount);
			}
			catch (IllegalArgumentException e)
			{
				player.sendMessage("系統錯誤：未知的能力類型");
			}

			// 根據能力類型返回對應頁面
			if (statName.startsWith("PVP"))
				showPvpPage(player, npc);
			else if (statName.contains("RAID"))
				showPveRaidPage(player, npc);
			else
				showPveNormalPage(player, npc);
		}
		else if (event.equals("reset"))
		{
			showResetConfirm(player, npc);
		}
		else if (event.equals("reset_confirm"))
		{
			resetAllPoints(player);
			player.sendPacket(new CreatureSay(null, ChatType.GENERAL, SYSTEM_NAME, "已重置所有能力點數"));
			showIndexPage(player, npc);
		}

		return null;
	}

	private void showIndexPage(Player player, Npc npc)
	{
		final int soulRingLevel = player.getVariables().getInt(SOUL_RING_VAR, 0);
		final int totalPoints = soulRingLevel;
		final int usedPoints = getTotalUsedPoints(player);
		final int remainingPoints = totalPoints - usedPoints;

		NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
		html.setFile(player, "data/scripts/custom/SoulRingAbility/index.htm");

		html.replace("%soul_ring_level%", String.valueOf(soulRingLevel));
		html.replace("%used_points%", String.valueOf(usedPoints));
		html.replace("%total_points%", String.valueOf(totalPoints));
		html.replace("%remaining_points%", String.valueOf(remainingPoints));

		player.sendPacket(html);
	}

	private void showPveNormalPage(Player player, Npc npc)
	{
		final int soulRingLevel = player.getVariables().getInt(SOUL_RING_VAR, 0);
		final int totalPoints = soulRingLevel;
		final int usedPoints = getTotalUsedPoints(player);
		final int remainingPoints = totalPoints - usedPoints;

		NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
		html.setFile(player, "data/scripts/custom/SoulRingAbility/pve_normal.htm");

		html.replace("%soul_ring_level%", String.valueOf(soulRingLevel));
		html.replace("%used_points%", String.valueOf(usedPoints));
		html.replace("%total_points%", String.valueOf(totalPoints));
		html.replace("%remaining_points%", String.valueOf(remainingPoints));
		html.replace("%pve_normal_list%", buildStatList(player, 0, 6));

		player.sendPacket(html);
	}

	private void showPveRaidPage(Player player, Npc npc)
	{
		final int soulRingLevel = player.getVariables().getInt(SOUL_RING_VAR, 0);
		final int totalPoints = soulRingLevel;
		final int usedPoints = getTotalUsedPoints(player);
		final int remainingPoints = totalPoints - usedPoints;

		NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
		html.setFile(player, "data/scripts/custom/SoulRingAbility/pve_raid.htm");

		html.replace("%soul_ring_level%", String.valueOf(soulRingLevel));
		html.replace("%used_points%", String.valueOf(usedPoints));
		html.replace("%total_points%", String.valueOf(totalPoints));
		html.replace("%remaining_points%", String.valueOf(remainingPoints));
		html.replace("%pve_raid_list%", buildStatList(player, 6, 12));

		player.sendPacket(html);
	}

	private void showPvpPage(Player player, Npc npc)
	{
		final int soulRingLevel = player.getVariables().getInt(SOUL_RING_VAR, 0);
		final int totalPoints = soulRingLevel;
		final int usedPoints = getTotalUsedPoints(player);
		final int remainingPoints = totalPoints - usedPoints;

		NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
		html.setFile(player, "data/scripts/custom/SoulRingAbility/pvp.htm");

		html.replace("%soul_ring_level%", String.valueOf(soulRingLevel));
		html.replace("%used_points%", String.valueOf(usedPoints));
		html.replace("%total_points%", String.valueOf(totalPoints));
		html.replace("%remaining_points%", String.valueOf(remainingPoints));
		html.replace("%pvp_list%", buildStatList(player, 12, 18));

		player.sendPacket(html);
	}

	private String buildStatList(Player player, int startIndex, int endIndex)
	{
		StringBuilder sb = new StringBuilder();

		for (int i = startIndex; i < endIndex; i++)
		{
			SoulRingStat soulStat = SoulRingStat.values()[i];
			final int points = player.getVariables().getInt(soulStat.getVarName(), 0);
			final double bonus = points * PERCENT_PER_POINT;
			final int totalUsed = getTotalUsedPoints(player);
			final int soulRingLevel = player.getVariables().getInt(SOUL_RING_VAR, 0);
			final String varName = "amt" + (i - startIndex);

			// 第一行：能力名稱與當前數值（獨立 table，不依賴外層欄位）
			sb.append("<table width=\"280\" border=\"0\"><tr>");
			sb.append("<td align=\"center\" height=\"22\">");
			sb.append("<font color=\"LEVEL\">").append(soulStat.getDisplayName()).append("</font>  ");
			sb.append("<font color=\"00FF00\">").append(points).append("</font>點");
			sb.append("  (<font color=\"FFFF00\">+").append(String.format("%.1f", bonus)).append("%</font>)");
			sb.append("</td>");
			sb.append("</tr></table>");

			// 第二行：[ - ]  輸入框  [ + ]（獨立 table，固定三欄）
			sb.append("<table width=\"280\" border=\"0\"><tr height=\"28\">");

			// 減少按鈕
			sb.append("<td width=\"55\" align=\"center\">");
			if (points > 0)
				sb.append("<button value=\"-\" action=\"bypass -h Quest SoulRingAbility subN_").append(soulStat.name())
						.append(" $").append(varName).append("\" width=\"40\" height=\"25\" back=\"L2UI_CT1.Button_DF\" fore=\"L2UI_CT1.Button_DF\">");
			else
				sb.append("<button value=\"-\" action=\"\" width=\"40\" height=\"25\" back=\"L2UI_CT1.Button_DF\" fore=\"L2UI_CT1.Button_DF_Grayed\">");
			sb.append("</td>");

			// 輸入框
			sb.append("<td width=\"170\" align=\"center\">");
			sb.append("<edit var=\"").append(varName).append("\" width=\"140\" height=\"16\" maxlen=\"5\">");
			sb.append("</td>");

			// 增加按鈕
			sb.append("<td width=\"55\" align=\"center\">");
			if (points < MAX_POINTS_PER_STAT && totalUsed < soulRingLevel)
				sb.append("<button value=\"+\" action=\"bypass -h Quest SoulRingAbility addN_").append(soulStat.name())
						.append(" $").append(varName).append("\" width=\"40\" height=\"25\" back=\"L2UI_CT1.Button_DF\" fore=\"L2UI_CT1.Button_DF\">");
			else
				sb.append("<button value=\"+\" action=\"\" width=\"40\" height=\"25\" back=\"L2UI_CT1.Button_DF\" fore=\"L2UI_CT1.Button_DF_Grayed\">");
			sb.append("</td>");

			sb.append("</tr></table>");
			sb.append("<img src=\"L2UI.SquareGray\" width=\"280\" height=\"1\">");
		}

		return sb.toString();
	}

	private void showResetConfirm(Player player, Npc npc)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
		html.setFile(player, "data/scripts/custom/SoulRingAbility/reset.htm");
		player.sendPacket(html);
	}

	private void addPoints(Player player, SoulRingStat soulStat, int amount)
	{
		final int soulRingLevel = player.getVariables().getInt(SOUL_RING_VAR, 0);
		final int totalUsed = getTotalUsedPoints(player);
		final int currentPoints = player.getVariables().getInt(soulStat.getVarName(), 0);

		final int canAddByTotal = soulRingLevel - totalUsed;       // 剩餘總點數
		final int canAddByCap = MAX_POINTS_PER_STAT - currentPoints; // 距離單項上限
		final int canAdd = Math.min(canAddByTotal, canAddByCap);

		if (canAdd <= 0)
		{
			if (totalUsed >= soulRingLevel)
				player.sendPacket(new CreatureSay(null, ChatType.GENERAL, SYSTEM_NAME, "沒有剩餘點數可分配"));
			else
				player.sendPacket(new CreatureSay(null, ChatType.GENERAL, SYSTEM_NAME, "該能力已達到最大值"));
			return;
		}

		final int actualAdd = Math.min(amount, canAdd);
		player.getVariables().set(soulStat.getVarName(), currentPoints + actualAdd);

		if (actualAdd < amount)
			player.sendMessage("已加 " + actualAdd + " 點（超出可用點數或能力上限，已自動調整）");
	}

	private void removePoints(Player player, SoulRingStat soulStat, int amount)
	{
		final int currentPoints = player.getVariables().getInt(soulStat.getVarName(), 0);

		if (currentPoints <= 0)
		{
			player.sendPacket(new CreatureSay(null, ChatType.GENERAL, SYSTEM_NAME, "該能力點數已為0"));
			return;
		}

		final int actualRemove = Math.min(amount, currentPoints);
		player.getVariables().set(soulStat.getVarName(), currentPoints - actualRemove);

		if (actualRemove < amount)
			player.sendMessage("已扣 " + actualRemove + " 點（已到達0，無法再扣）");
	}

	private void resetAllPoints(Player player)
	{
		for (SoulRingStat soulStat : SoulRingStat.values())
		{
			player.getVariables().remove(soulStat.getVarName());
		}
	}

	private int getTotalUsedPoints(Player player)
	{
		int total = 0;
		for (SoulRingStat soulStat : SoulRingStat.values())
		{
			total += player.getVariables().getInt(soulStat.getVarName(), 0);
		}
		return total;
	}

	public static void main(String[] args)
	{
		new SoulRingAbility();
		System.out.println("【系統】魂環能力分配系統載入完畢！");
	}
}