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
 * 鏡像副本系統 - Mirror Instance System 玩家挑戰自己的鏡像，鏡像使用玩家的外觀、裝備和屬性（按倍率放大） 成功擊敗鏡像後獲得永久的 FINAL_DAMAGE_REDUCE 屬性加成
 */
public class MirrorInstance extends InstanceScript
{
	// 玩家變量名稱
	private static final String VAR_DAILY_COUNT = "鏡像副本次數";
	private static final String VAR_RESET_TIME = "鏡像副本重置時間";
	private static final String VAR_SELECTED_MULTIPLIER = "鏡像副本倍率";
	private static final String VAR_FDR_BONUS = "MIRROR_FDR_BONUS";

	// ===== BUG FIX =====
	// 副本級別變量（保存在 Instance 上，不受後進入玩家影響）
	// 修復問題：玩家A進入後打怪，玩家B後進入會覆蓋怪物屬性和結算倍率
	private static final String INST_VAR_MULTIPLIER = "inst_mirror_multiplier";
	private static final String INST_VAR_INITIALIZED = "inst_mirror_initialized";
	// ===================

	// 鏡像怪物刷新位置
	private static final Location MIRROR_SPAWN_LOC = new Location(149741, 46724, -3438);

	// 追蹤副本中的鏡像NPC
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

		// 替換變量
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

		// 生成倍率按鈕
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
		// 檢查倍率是否有效
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

		// 檢查是否已在副本中
		if (player.getInstanceWorld() != null)
		{
			player.sendPacket(new ExShowScreenMessage("你已經在副本中！", 3000));
			return null;
		}

		// 檢查每日次數
		if (!checkAndUpdateDailyLimit(player))
		{
			player.sendPacket(new ExShowScreenMessage("今日挑戰次數已用完！", 3000));
			return null;
		}

		// 檢查並扣除道具
		if (!player.destroyItemByItemId(null, MirrorInstanceConfig.MIRROR_ENTRY_ITEM_ID, MirrorInstanceConfig.MIRROR_ENTRY_ITEM_COUNT, player, true))
		{
			player.sendPacket(new ExShowScreenMessage("入場道具不足！", 3000));
			return null;
		}

		// 保存選擇的倍率到玩家臨時變量（onInstanceCreated 讀取後立即轉存至副本變量）
		player.getVariables().set(VAR_SELECTED_MULTIPLIER, multiplier);

		// 進入副本
		enterInstance(player, null, MirrorInstanceConfig.MIRROR_INSTANCE_TEMPLATE_ID);

		return null;
	}

	@Override
	public void onInstanceCreated(Instance instance, Player player)
	{
		// ===== BUG FIX =====
		// 防止副本被多次初始化：只有第一個進入（創建者）才執行初始化
		// 原Bug：每次有新玩家進入都會重新刷新怪物屬性並覆蓋倍率
		if (instance.getParameters().getInt(INST_VAR_INITIALIZED, 0) == 1)
		{
			return;
		}

		// 從創建者身上讀取倍率，轉存至副本變量（後進入的玩家不會影響此值）
		int multiplier = player.getVariables().getInt(VAR_SELECTED_MULTIPLIER, 2);
		instance.getParameters().set(INST_VAR_MULTIPLIER, multiplier);
		instance.getParameters().set(INST_VAR_INITIALIZED, 1);

		// 清除創建者身上的臨時倍率變量（已安全存入副本）
		player.getVariables().remove(VAR_SELECTED_MULTIPLIER);
		// ===================

		// 用創建者屬性刷新鏡像怪物（固定一次，後進入玩家不會觸發此處）
		spawnMirrorMonster(player, instance, multiplier);

		player.sendPacket(new ExShowScreenMessage("鏡像副本已開啟！擊敗你的鏡像！", 5000));
	}

	/**
	 * 刷新鏡像怪物
	 */
	private void spawnMirrorMonster(Player player, Instance instance, int multiplier)
	{
		try
		{
			// 獲取鏡像NPC模板
			NpcTemplate template = NpcData.getInstance().getTemplate(MirrorInstanceConfig.MIRROR_FAKE_PLAYER_NPC_ID);
			if (template == null)
			{
				LOGGER.warning("鏡像副本：找不到NPC模板 ID=" + MirrorInstanceConfig.MIRROR_FAKE_PLAYER_NPC_ID);
				return;
			}

			// 創建刷新點
			Spawn spawn = new Spawn(template);
			spawn.setLocation(MIRROR_SPAWN_LOC);
			spawn.setInstanceId(instance.getId());

			// 刷新NPC
			Npc mirror = spawn.doSpawn();
			if (mirror == null)
			{
				LOGGER.warning("鏡像副本：無法刷新鏡像NPC");
				return;
			}

			// 構建玩家外觀數據（race/sex/name/title 全部存入 holder，不再修改共享的 NpcTemplate）
			// 修復：多個副本同時運行時，修改 getTemplate() 會影響所有副本的同 ID NPC 外觀
			FakePlayerHolder fakeHolder = createMirrorAppearance(player, multiplier);
			mirror.setCustomFakePlayerHolder(fakeHolder);

			// 設置鏡像屬性（玩家屬性 + 基礎值）* 倍率
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

			// 設置攻擊速度和施法速度（與玩家相同，不乘倍率）
			mirror.getStat().addFixedValue(Stat.PHYSICAL_ATTACK_SPEED, (double) (player.getPAtkSpd()* multiplier));
			mirror.getStat().addFixedValue(Stat.MAGIC_ATTACK_SPEED, (double) (player.getMAtkSpd()* multiplier));

			// 設置當前HP為最大值
			mirror.setCurrentHp(mirrorHp);

			// 確保 NPC 可以被攻擊（設置為怪物行為）
			mirror.setTalkable(false);
			mirror.setTargetable(true);

			// 廣播更新（確保客戶端看到正確的名字、稱號和外觀）
			mirror.broadcastInfo();

			// 記錄鏡像NPC
			instanceMirrors.put(instance.getId(), mirror);

			LOGGER.info("鏡像副本：為玩家 " + player.getName() + " 刷新鏡像 (倍率=" + multiplier + ", HP=" + mirrorHp + ", P.Atk=" + mirrorPAtk + ", P.Def=" + mirrorPDef + ", M.Atk=" + mirrorMAtk + ", M.Def=" + mirrorMDef + ")");
		}
		catch (Exception e)
		{
			LOGGER.warning("鏡像副本：刷新鏡像時發生錯誤: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * 根據玩家創建鏡像外觀
	 * 將 race/sex/name/title 全部存入 FakePlayerHolder，不依賴共享的 NpcTemplate
	 */
	private FakePlayerHolder createMirrorAppearance(Player player, int multiplier)
	{
		StatSet set = new StatSet();

		// 基本外觀（包含 race/sex/name/title，不再修改共享 NpcTemplate）
		set.set("name", player.getName());
		set.set("title", "鏡像 x" + multiplier);
		set.set("race", player.getRace());
		set.set("sex", player.getAppearance().getSexType());
		set.set("classId", player.getPlayerClass().getId());
		set.set("hair", player.getAppearance().getHairStyle());
		set.set("hairColor", player.getAppearance().getHairColor());
		set.set("face", player.getAppearance().getFace());
		set.set("nameColor", player.getAppearance().getNameColor());
		set.set("titleColor", player.getAppearance().getTitleColor());

		// 裝備外觀
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

		// 附魔等級
		Item weapon = inv.getPaperdollItem(Inventory.PAPERDOLL_RHAND);
		set.set("weaponEnchantLevel", weapon != null ? weapon.getEnchantLevel() : 0);

		Item chest = inv.getPaperdollItem(Inventory.PAPERDOLL_CHEST);
		set.set("armorEnchantLevel", chest != null ? chest.getEnchantLevel() : 0);

		// 其他屬性
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

		// 創建FakePlayerHolder（不註冊到全局FakePlayerData）
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

		// ===== BUG FIX =====
		// 從副本變量讀取倍率，而非從擊殺者的玩家變量讀取
		// 原Bug：結算時讀取 killer.getVariables() 的倍率，若後進入的玩家擊殺則按其倍率結算
		int multiplier = instance.getParameters().getInt(INST_VAR_MULTIPLIER, 1);
		// ===================

		// 給予獎勵（按副本創建時記錄的倍率結算）
		giveReward(killer, multiplier);

		// 5秒後傳送出副本
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
	 * @param player 玩家
	 * @param multiplier 挑戰倍率
	 */
	private void giveReward(Player player, int multiplier)
	{
		// 獲取當前累積的FDR
		double currentFdr = player.getVariables().getDouble(VAR_FDR_BONUS, 0.0);

		// 檢查是否已達上限
		if (currentFdr >= MirrorInstanceConfig.MIRROR_MAX_FINAL_DAMAGE_REDUCE)
		{
			player.sendMessage("你的無視最終減傷已達上限！");
			return;
		}

		// 根據倍率計算獎勵：基礎獎勵 × 倍率
		double rewardAmount = MirrorInstanceConfig.MIRROR_REWARD_FDR_PER_RUN * multiplier;

		// 增加FDR
		double newFdr = Math.min(currentFdr + rewardAmount, MirrorInstanceConfig.MIRROR_MAX_FINAL_DAMAGE_REDUCE);
		player.getVariables().set(VAR_FDR_BONUS, newFdr);

		player.sendMessage("獲得永久無視最終減傷 +" + String.format("%.2f", rewardAmount) + "% (倍率 x" + multiplier + ")");
		player.sendMessage("當前累積無視最終減傷: " + String.format("%.2f", newFdr) + "%");

		// 給予道具獎勵（如果配置了）
		if ((MirrorInstanceConfig.MIRROR_REWARD_ITEM_ID > 0) && (MirrorInstanceConfig.MIRROR_REWARD_ITEM_COUNT > 0))
		{
			player.addItem(null, MirrorInstanceConfig.MIRROR_REWARD_ITEM_ID, MirrorInstanceConfig.MIRROR_REWARD_ITEM_COUNT, player, true);
		}
	}

	@Override
	public void onInstanceLeave(Player player, Instance instance)
	{
		// ===== BUG FIX =====
		// 只有副本中已無任何玩家時才清理鏡像NPC
		// 原Bug：任何玩家離開都會刪除怪物，導致還在打的玩家怪物消失
		if (instance.getPlayers().isEmpty())
		{
			Npc mirror = instanceMirrors.remove(instance.getId());
			if ((mirror != null) && !mirror.isDead())
			{
				mirror.deleteMe();
			}
		}
		// ===================

		// 安全清理玩家臨時倍率變量（防止殘留）
		player.getVariables().remove(VAR_SELECTED_MULTIPLIER);
	}

	/**
	 * 檢查並更新每日次數
	 */
	private boolean checkAndUpdateDailyLimit(Player player)
	{
		long resetTime = player.getVariables().getLong(VAR_RESET_TIME, 0);
		long now = System.currentTimeMillis();

		// 檢查是否需要重置
		if (resetTime < now)
		{
			player.getVariables().set(VAR_DAILY_COUNT, 0);

			// 計算下次重置時間
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

		// 增加次數
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
