package instances.MirrorInstance;

import java.util.Calendar;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.config.custom.MirrorInstanceConfig;
import org.l2jmobius.gameserver.data.xml.NpcData;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.Spawn;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.holders.npc.FakePlayerHolder;
import org.l2jmobius.gameserver.model.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.model.instancezone.Instance;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.itemcontainer.Inventory;
import org.l2jmobius.gameserver.model.script.InstanceScript;
import org.l2jmobius.gameserver.model.stats.Stat;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * 鏡像副本系統 - Mirror Instance System 玩家挑戰自己的鏡像，鏡像使用玩家的外觀、裝備和屬性（按倍率放大） 成功擊敗鏡像後獲得永久的 FINAL_DAMAGE_REDUCE 屬性加成 修復：NpcTemplate 為所有相同 NPC ID 共用的單例，直接修改會污染其他副本。 改用 NPC 實例的 getVariables() 存儲每個副本獨立的倍率與創建者資訊， 並以 npc 實例層級的 addFixedValue 取代模板修改。
 */
public class MirrorInstance extends InstanceScript
{
	// 玩家變量名稱
	private static final String VAR_DAILY_COUNT = "鏡像副本次數";
	private static final String VAR_RESET_TIME = "鏡像副本重置時間";
	private static final String VAR_SELECTED_MULTIPLIER = "鏡像副本倍率";
	private static final String VAR_FDR_BONUS = "MIRROR_FDR_BONUS";

	// NPC 實例變量：綁定在每個 mirror npc 上，避免跨副本污染
	private static final String NPC_VAR_MULTIPLIER = "MIRROR_NPC_MULTIPLIER";
	private static final String NPC_VAR_OWNER_ID = "MIRROR_NPC_OWNER_ID";

	// 鏡像怪物刷新位置
	private static final Location MIRROR_SPAWN_LOC = new Location(149741, 46724, -3438);

	// 追蹤副本中的鏡像NPC：key = instanceId
	private static final Map<Integer, Npc> instanceMirrors = new ConcurrentHashMap<>();

	public MirrorInstance()
	{
		super(MirrorInstanceConfig.MIRROR_INSTANCE_TEMPLATE_ID);

		if (!MirrorInstanceConfig.MIRROR_INSTANCE_ENABLED)
		{
			return;
		}

		addStartNpc(MirrorInstanceConfig.MIRROR_INSTANCE_NPC_ID);
		addFirstTalkId(MirrorInstanceConfig.MIRROR_INSTANCE_NPC_ID);
		addTalkId(MirrorInstanceConfig.MIRROR_INSTANCE_NPC_ID);
		addKillId(MirrorInstanceConfig.MIRROR_FAKE_PLAYER_NPC_ID);
		addInstanceCreatedId(MirrorInstanceConfig.MIRROR_INSTANCE_TEMPLATE_ID);
		addInstanceLeaveId(MirrorInstanceConfig.MIRROR_INSTANCE_TEMPLATE_ID);
	}

	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (!MirrorInstanceConfig.MIRROR_INSTANCE_ENABLED)
		{
			player.sendMessage("鏡像副本系統已停用");
			return null;
		}

		switch (event)
		{
			case "showMain":
			{
				return showMainHtml(player);
			}
			case "showMultiplierSelect":
			{
				return showMultiplierSelectHtml(player);
			}
		}

		// 處理倍率選擇並進入副本
		if (event.startsWith("enterInstance_"))
		{
			try
			{
				int multiplier = Integer.parseInt(event.substring(14));
				return handleEnterInstance(player, multiplier);
			}
			catch (NumberFormatException e)
			{
				player.sendMessage("無效的倍率選擇");
				return null;
			}
		}

		return null;
	}

	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return showMainHtml(player);
	}

	/**
	 * 顯示主選單HTML
	 */
	private String showMainHtml(Player player)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(0, 1);
		html.setFile(player, "data/scripts/instances/MirrorInstance/MirrorInstance.htm");

		int dailyCount = getDailyCount(player);
		int remaining = MirrorInstanceConfig.MIRROR_DAILY_LIMIT - dailyCount;
		double currentFdr = player.getVariables().getDouble(VAR_FDR_BONUS, 0.0);

		html.replace("%daily_remaining%", String.valueOf(Math.max(0, remaining)));
		html.replace("%daily_limit%", String.valueOf(MirrorInstanceConfig.MIRROR_DAILY_LIMIT));
		html.replace("%current_fdr%", String.format("%.2f%%", currentFdr));
		html.replace("%max_fdr%", String.format("%.2f%%", MirrorInstanceConfig.MIRROR_MAX_FINAL_DAMAGE_REDUCE));
		html.replace("%entry_cost%", String.valueOf(MirrorInstanceConfig.MIRROR_ENTRY_ITEM_COUNT));

		player.sendPacket(html);
		return null;
	}

	/**
	 * 顯示倍率選擇HTML
	 */
	private String showMultiplierSelectHtml(Player player)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(0, 1);
		StringBuilder sb = new StringBuilder();

		sb.append("<html><title>鏡像副本 - 選擇難度</title><head><body scroll=\"no\">");
		sb.append("<table border=0 cellpadding=0 cellspacing=0 width=292 height=358>");
		sb.append("<tr><td valign=\"top\" align=\"center\">");
		sb.append("<table border=0 cellpadding=0 cellspacing=0 width=\"272\">");
		sb.append("<tr><td align=\"center\" height=\"40\"><font color=\"LEVEL\">選擇挑戰倍率</font></td></tr>");
		sb.append("<tr><td height=\"10\"></td></tr>");
		sb.append("<tr><td align=\"center\" height=\"25\"><font color=\"FFFFFF\">倍率越高，鏡像越強</font></td></tr>");
		sb.append("<tr><td height=\"15\"></td></tr>");

		for (int multiplier : MirrorInstanceConfig.MIRROR_AVAILABLE_MULTIPLIERS)
		{
			sb.append("<tr><td align=\"center\">");
			sb.append("<button value=\"").append(multiplier).append(" 倍挑戰\" ");
			sb.append("action=\"bypass -h Quest MirrorInstance enterInstance_").append(multiplier).append("\" ");
			sb.append("width=150 height=30 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
			sb.append("</td></tr>");
			sb.append("<tr><td height=\"5\"></td></tr>");
		}

		sb.append("<tr><td height=\"10\"></td></tr>");
		sb.append("<tr><td align=\"center\">");
		sb.append("<button value=\"返回\" action=\"bypass -h Quest MirrorInstance showMain\" ");
		sb.append("width=100 height=25 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
		sb.append("</td></tr>");
		sb.append("</table></td></tr></table>");
		sb.append("</body></html>");

		html.setHtml(sb.toString());
		player.sendPacket(html);
		return null;
	}

	/**
	 * 處理進入副本
	 */
	private String handleEnterInstance(Player player, int multiplier)
	{
		boolean validMultiplier = false;
		for (int m : MirrorInstanceConfig.MIRROR_AVAILABLE_MULTIPLIERS)
		{
			if (m == multiplier)
			{
				validMultiplier = true;
				break;
			}
		}
		if (!validMultiplier)
		{
			player.sendMessage("無效的倍率選擇");
			return null;
		}

		if (player.getInstanceWorld() != null)
		{
			player.sendPacket(new ExShowScreenMessage("你已經在副本中！", 3000));
			return null;
		}

		if (!checkAndUpdateDailyLimit(player))
		{
			player.sendPacket(new ExShowScreenMessage("今日挑戰次數已用完！", 3000));
			return null;
		}

		if (!player.destroyItemByItemId(null, MirrorInstanceConfig.MIRROR_ENTRY_ITEM_ID, MirrorInstanceConfig.MIRROR_ENTRY_ITEM_COUNT, player, true))
		{
			player.sendPacket(new ExShowScreenMessage("入場道具不足！", 3000));
			return null;
		}

		// 將倍率暫存於玩家變量，待 onInstanceCreated 讀取後立即清除
		player.getVariables().set(VAR_SELECTED_MULTIPLIER, multiplier);

		enterInstance(player, null, MirrorInstanceConfig.MIRROR_INSTANCE_TEMPLATE_ID);

		return null;
	}

	@Override
	public void onInstanceCreated(Instance instance, Player player)
	{
		// 讀取並立即移除玩家變量，避免後續進入的玩家誤讀到上一次設定的倍率
		int multiplier = player.getVariables().getInt(VAR_SELECTED_MULTIPLIER, 2);
		player.getVariables().remove(VAR_SELECTED_MULTIPLIER);

		// 刷新鏡像怪物，並將倍率與創建者資訊綁定在 NPC 實例上
		spawnMirrorMonster(player, instance, multiplier);

		player.sendPacket(new ExShowScreenMessage("鏡像副本已開啟！擊敗你的鏡像！", 5000));
	}

	/**
	 * 刷新鏡像怪物 修復重點： 1. 不再呼叫 mirror.getTemplate().setXxx()，因為 NpcTemplate 是所有同 ID NPC 共用的單例，修改後會污染其他已存在的鏡像副本。 2. 改用 FakePlayerHolder 攜帶名稱/稱號/外觀資訊，僅影響此 NPC 實例。 3. 每次刷新前先清除舊的 fixedValue，避免多次 addFixedValue 造成屬性累加。 4. 倍率與創建者 ObjectId 綁定於 npc.getVariables()，onKill 從 NPC 讀取， 不再依賴 killer
	 * 的玩家變量，徹底避免後進入玩家覆蓋倍率的問題。
	 */
	private void spawnMirrorMonster(Player player, Instance instance, int multiplier)
	{
		try
		{
			NpcTemplate template = NpcData.getInstance().getTemplate(MirrorInstanceConfig.MIRROR_FAKE_PLAYER_NPC_ID);
			if (template == null)
			{
				LOGGER.warning("鏡像副本：找不到NPC模板 ID=" + MirrorInstanceConfig.MIRROR_FAKE_PLAYER_NPC_ID);
				return;
			}

			Spawn spawn = new Spawn(template);
			spawn.setLocation(MIRROR_SPAWN_LOC);
			spawn.setInstanceId(instance.getId());

			Npc mirror = spawn.doSpawn();
			if (mirror == null)
			{
				LOGGER.warning("鏡像副本：無法刷新鏡像NPC");
				return;
			}

			// ── 外觀設定（僅影響此 NPC 實例，不觸碰共用 template）──
			FakePlayerHolder fakeHolder = createMirrorAppearance(player, multiplier);
			mirror.setCustomFakePlayerHolder(fakeHolder);

			// ── 將倍率與創建者 ObjectId 存入 NPC 實例變量 ──
			// onKill 將從此處讀取，與其他副本完全隔離
			mirror.getVariables().set(NPC_VAR_MULTIPLIER, multiplier);
			mirror.getVariables().set(NPC_VAR_OWNER_ID, player.getObjectId());

			// ── 屬性設定（每次都是全新 NPC 實例，無需清除舊值）──
			long mirrorHp = (player.getMaxHp() + MirrorInstanceConfig.MIRROR_BASE_HP) * multiplier;
			long mirrorPAtk = (player.getPAtk() + MirrorInstanceConfig.MIRROR_BASE_ATTACK) * multiplier;
			long mirrorPDef = (player.getPDef() + MirrorInstanceConfig.MIRROR_BASE_DEFENSE) * multiplier;
			long mirrorMAtk = (player.getMAtk() + MirrorInstanceConfig.MIRROR_BASE_ATTACK) * multiplier;
			long mirrorMDef = (player.getMDef() + MirrorInstanceConfig.MIRROR_BASE_DEFENSE) * multiplier;

			mirror.getStat().addFixedValue(Stat.MAX_HP, (double) mirrorHp);
			mirror.getStat().addFixedValue(Stat.PHYSICAL_ATTACK, (double) mirrorPAtk);
			mirror.getStat().addFixedValue(Stat.PHYSICAL_DEFENCE, (double) mirrorPDef);
			mirror.getStat().addFixedValue(Stat.MAGIC_ATTACK, (double) mirrorMAtk);
			mirror.getStat().addFixedValue(Stat.MAGICAL_DEFENCE, (double) mirrorMDef);
			mirror.getStat().addFixedValue(Stat.PHYSICAL_ATTACK_SPEED, (double) (player.getPAtkSpd() * multiplier));
			mirror.getStat().addFixedValue(Stat.MAGIC_ATTACK_SPEED, (double) (player.getMAtkSpd() * multiplier));

			mirror.setCurrentHp(mirrorHp);
			mirror.setTalkable(false);
			mirror.setTargetable(true);

			mirror.broadcastInfo();

			instanceMirrors.put(instance.getId(), mirror);

			LOGGER.info("鏡像副本：為玩家 " + player.getName() + " 刷新鏡像 (instanceId=" + instance.getId() + ", 倍率=" + multiplier + ", HP=" + mirrorHp + ", P.Atk=" + mirrorPAtk + ", P.Def=" + mirrorPDef + ", M.Atk=" + mirrorMAtk + ", M.Def=" + mirrorMDef + ")");
		}
		catch (Exception e)
		{
			LOGGER.warning("鏡像副本：刷新鏡像時發生錯誤: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * 根據玩家創建鏡像外觀 名稱與稱號透過 FakePlayerHolder 傳遞，不修改共用的 NpcTemplate。
	 */
	private FakePlayerHolder createMirrorAppearance(Player player, int multiplier)
	{
		StatSet set = new StatSet();

		set.set("name", player.getName());
		set.set("title", "鏡像 x" + multiplier); // 稱號放在 FakePlayerHolder 內
		set.set("classId", player.getPlayerClass().getId());
		set.set("hair", player.getAppearance().getHairStyle());
		set.set("hairColor", player.getAppearance().getHairColor());
		set.set("face", player.getAppearance().getFace());
		set.set("nameColor", player.getAppearance().getNameColor());
		set.set("titleColor", player.getAppearance().getTitleColor());

		Inventory inv = player.getInventory();
		set.set("equipHead", getItemDisplayId(inv.getPaperdollItem(Inventory.PAPERDOLL_HEAD)));
		set.set("equipRHand", getItemDisplayId(inv.getPaperdollItem(Inventory.PAPERDOLL_RHAND)));
		set.set("equipLHand", getItemDisplayId(inv.getPaperdollItem(Inventory.PAPERDOLL_LHAND)));
		set.set("equipGloves", getItemDisplayId(inv.getPaperdollItem(Inventory.PAPERDOLL_GLOVES)));
		set.set("equipChest", getItemDisplayId(inv.getPaperdollItem(Inventory.PAPERDOLL_CHEST)));
		set.set("equipLegs", getItemDisplayId(inv.getPaperdollItem(Inventory.PAPERDOLL_LEGS)));
		set.set("equipFeet", getItemDisplayId(inv.getPaperdollItem(Inventory.PAPERDOLL_FEET)));
		set.set("equipCloak", getItemDisplayId(inv.getPaperdollItem(Inventory.PAPERDOLL_CLOAK)));
		set.set("equipHair", getItemDisplayId(inv.getPaperdollItem(Inventory.PAPERDOLL_HAIR)));
		set.set("equipHair2", getItemDisplayId(inv.getPaperdollItem(Inventory.PAPERDOLL_HAIR2)));

		Item weapon = inv.getPaperdollItem(Inventory.PAPERDOLL_RHAND);
		set.set("weaponEnchantLevel", weapon != null ? weapon.getEnchantLevel() : 0);

		Item chest = inv.getPaperdollItem(Inventory.PAPERDOLL_CHEST);
		set.set("armorEnchantLevel", chest != null ? chest.getEnchantLevel() : 0);

		set.set("recommends", 0);
		set.set("nobleLevel", player.isNoble() ? 1 : 0);
		set.set("hero", player.isHero());
		set.set("clanId", player.getClanId());
		set.set("pledgeStatus", 0);
		set.set("sitting", false);
		set.set("privateStoreType", 0);
		set.set("privateStoreMessage", "");
		set.set("fakePlayerTalkable", false);
		set.set("fishing", false);

		return new FakePlayerHolder(set, false);
	}

	/**
	 * 獲取物品顯示ID
	 */
	private int getItemDisplayId(Item item)
	{
		return item != null ? item.getDisplayId() : 0;
	}

	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		if (npc.getId() != MirrorInstanceConfig.MIRROR_FAKE_PLAYER_NPC_ID)
		{
			return;
		}

		Instance instance = killer.getInstanceWorld();
		if (instance == null)
		{
			return;
		}

		// 移除記錄
		instanceMirrors.remove(instance.getId());

		// ── 修復：從 NPC 實例變量讀取倍率，而非從 killer 的玩家變量讀取 ──
		// 原始寫法 killer.getVariables().getInt(VAR_SELECTED_MULTIPLIER) 存在問題：
		// 若第二個玩家進入副本後覆蓋了該變量，先進入的玩家擊殺鏡像時會讀到錯誤的倍率。
		// 現在倍率已在 spawnMirrorMonster 時綁定於 NPC 實例，每個副本各自獨立。
		int multiplier = npc.getVariables().getInt(NPC_VAR_MULTIPLIER, 1);

		giveReward(killer, multiplier);

		killer.sendPacket(new ExShowScreenMessage("挑戰成功！5秒後傳送離開", 5000));
		ThreadPool.schedule(() ->
		{
			if ((killer != null) && killer.isOnline())
			{
				final Instance inst = killer.getInstanceWorld();
				if (inst != null)
				{
					inst.ejectPlayer(killer);
				}
			}
		}, 5000);
	}

	/**
	 * 給予挑戰成功獎勵
	 */
	private void giveReward(Player player, int multiplier)
	{
		double currentFdr = player.getVariables().getDouble(VAR_FDR_BONUS, 0.0);

		if (currentFdr >= MirrorInstanceConfig.MIRROR_MAX_FINAL_DAMAGE_REDUCE)
		{
			player.sendMessage("你的無視最終減傷已達上限！");
			return;
		}

		double rewardAmount = MirrorInstanceConfig.MIRROR_REWARD_FDR_PER_RUN * multiplier;
		double newFdr = Math.min(currentFdr + rewardAmount, MirrorInstanceConfig.MIRROR_MAX_FINAL_DAMAGE_REDUCE);
		player.getVariables().set(VAR_FDR_BONUS, newFdr);

		player.sendMessage("獲得永久無視最終減傷 +" + String.format("%.2f", rewardAmount) + "% (倍率 x" + multiplier + ")");
		player.sendMessage("當前累積無視最終減傷: " + String.format("%.2f", newFdr) + "%");

		if ((MirrorInstanceConfig.MIRROR_REWARD_ITEM_ID > 0) && (MirrorInstanceConfig.MIRROR_REWARD_ITEM_COUNT > 0))
		{
			player.addItem(null, MirrorInstanceConfig.MIRROR_REWARD_ITEM_ID, MirrorInstanceConfig.MIRROR_REWARD_ITEM_COUNT, player, true);
		}
	}

	@Override
	public void onInstanceLeave(Player player, Instance instance)
	{
		Npc mirror = instanceMirrors.remove(instance.getId());
		if ((mirror != null) && !mirror.isDead())
		{
			mirror.deleteMe();
		}

		// VAR_SELECTED_MULTIPLIER 已在 onInstanceCreated 讀取後立即清除，此處保留以防萬一
		player.getVariables().remove(VAR_SELECTED_MULTIPLIER);
	}

	/**
	 * 檢查並更新每日次數
	 */
	private boolean checkAndUpdateDailyLimit(Player player)
	{
		long resetTime = player.getVariables().getLong(VAR_RESET_TIME, 0);
		long now = System.currentTimeMillis();

		if (resetTime < now)
		{
			player.getVariables().set(VAR_DAILY_COUNT, 0);

			Calendar cal = Calendar.getInstance();
			cal.set(Calendar.HOUR_OF_DAY, MirrorInstanceConfig.MIRROR_RESET_HOUR);
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.SECOND, 0);
			cal.set(Calendar.MILLISECOND, 0);

			if (cal.getTimeInMillis() <= now)
			{
				cal.add(Calendar.DAY_OF_MONTH, 1);
			}

			player.getVariables().set(VAR_RESET_TIME, cal.getTimeInMillis());
		}

		int dailyCount = getDailyCount(player);
		if (dailyCount >= MirrorInstanceConfig.MIRROR_DAILY_LIMIT)
		{
			return false;
		}

		player.getVariables().set(VAR_DAILY_COUNT, dailyCount + 1);
		return true;
	}

	/**
	 * 獲取今日已使用次數
	 */
	private int getDailyCount(Player player)
	{
		return player.getVariables().getInt(VAR_DAILY_COUNT, 0);
	}

	public static void main(String[] args)
	{
		new MirrorInstance();
		System.out.println("鏡像副本系統載入完畢！");
	}
}
