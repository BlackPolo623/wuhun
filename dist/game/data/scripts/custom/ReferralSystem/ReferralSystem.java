package custom.ReferralSystem;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

public class ReferralSystem extends Script
{
	private static final int NPC_ID = 50007;
	private static final int REFERRAL_SKILL_ID = 90001;
	private static final int MAX_SKILL_LEVEL = 50;
	private static final int CLEAR_OFFLINE_DAYS = 7;
	
	// ================= 等級獎勵配置 =================
	private static final Map<Integer, Reward> LEVEL_REWARDS = new HashMap<>();
	static
	{
		LEVEL_REWARDS.put(40, new Reward(57, 50_000));
		LEVEL_REWARDS.put(60, new Reward(57, 100_000));
		LEVEL_REWARDS.put(80, new Reward(57, 300_000));
		LEVEL_REWARDS.put(85, new Reward(3470, 5)); // 自訂道具示例
	}
	
	public ReferralSystem()
	{
		addStartNpc(NPC_ID);
		addTalkId(NPC_ID);
		addFirstTalkId(NPC_ID);
		
		ThreadPool.scheduleAtFixedRate(this::clearOfflineRelations, 10_000, 10_000);
		
	}
	
	/* ================= 綁定 ================= */
	
	public boolean bind(Player master, Player slave)
	{
		if ((master == null) || (slave == null) || (master == slave))
		{
			return false;
		}
		
		// 已存在關係
		if (getSlaves(master).contains(slave.getObjectId()))
		{
			master.sendMessage("該玩家已被綁定");
			return false;
		}
		
		// IP / HWID 限制（只限制這一對）
		if (!checkUnique(master, slave))
		{
			master.sendMessage("IP 或 HWID 不允許綁定");
			return false;
		}
		
		addSlave(master, slave.getObjectId());
		slave.getVariables().set("REFERRAL_MASTER", master.getObjectId());
		
		updateReferralSkill(master);
		updateReferralSkill(slave);
		
		checkLevelRewards(master);
		checkLevelRewards(slave);
		
		master.sendMessage("成功綁定玩家：" + slave.getName());
		return true;
	}
	
	/* ================= 技能控制 ================= */
	
	private void updateReferralSkill(Player player)
	{
		int level = getReferralCount(player);
		level = Math.min(level, MAX_SKILL_LEVEL);
		
		removeReferralSkill(player);
		
		if (level > 0)
		{
			Skill skill = SkillData.getInstance().getSkill(REFERRAL_SKILL_ID, level);
			if (skill != null)
			{
				player.addSkill(skill, true);
			}
		}
	}
	
	private void removeReferralSkill(Player player)
	{
		Skill old = player.getKnownSkill(REFERRAL_SKILL_ID);
		if (old != null)
		{
			player.removeSkill(old, true);
		}
	}
	
	/**
	 * 計算玩家綁定關係數 - master 的 slaves 數量 - slave 的 master 數量（1 或 0）
	 */
	private int getReferralCount(Player player)
	{
		int count = getSlaves(player).size();
		if (player.getVariables().getInt("REFERRAL_MASTER", 0) > 0)
		{
			count++;
		}
		return count;
	}
	
	/* ================= 等級獎勵 ================= */
	
	private void checkLevelRewards(Player player)
	{
		if (getReferralCount(player) <= 0)
		{
			return; // 無關係不給
		}
		
		for (Map.Entry<Integer, Reward> entry : LEVEL_REWARDS.entrySet())
		{
			int level = entry.getKey();
			Reward reward = entry.getValue();
			
			String varKey = "REFERRAL_REWARD_LV_" + level;
			
			if ((player.getLevel() >= level) && !player.getVariables().getBoolean(varKey, false))
			{
				player.addItem(null, reward.itemId, reward.count, player, true);
				player.getVariables().set(varKey, true);
				player.sendMessage("🎉 拉新等級獎勵已領取：Lv." + level);
			}
		}
	}
	
	/* ================= 7 天離線清理 ================= */
	
	private void clearOfflineRelations()
	{
		for (Player master : World.getInstance().getPlayers())
		{
			Set<Integer> slaves = getSlaves(master);
			Iterator<Integer> it = slaves.iterator();
			
			while (it.hasNext())
			{
				Player slave = World.getInstance().getPlayer(it.next());
				if (slave == null)
				{
					it.remove();
					continue;
				}
				
				if ((System.currentTimeMillis() - slave.getLastAccess()) > TimeUnit.DAYS.toMillis(CLEAR_OFFLINE_DAYS))
				{
					slave.getVariables().remove("REFERRAL_MASTER");
					updateReferralSkill(slave);
					it.remove();
				}
			}
			checkLevelRewards(master);
			saveSlaves(master, slaves);
			updateReferralSkill(master);
		}
	}
	
	/* ================= 工具 ================= */
	
	private boolean checkUnique(Player master, Player slave)
	{
		if (Objects.equals(master.getIPAddress(), slave.getIPAddress()))
		{
			return false;
		}
		
		String hwid1 = master.getAccountVariables().getString("HWID", "");
		String hwid2 = slave.getAccountVariables().getString("HWID", "");
		return hwid1.isEmpty() || !hwid1.equals(hwid2);
	}
	
	private void addSlave(Player master, int objId)
	{
		Set<Integer> set = getSlaves(master);
		set.add(objId);
		saveSlaves(master, set);
	}
	
	private void saveSlaves(Player master, Set<Integer> set)
	{
		master.getVariables().set("REFERRAL_SLAVES", set.toString());
	}
	
	private Set<Integer> getSlaves(Player master)
	{
		Set<Integer> set = new HashSet<>();
		String data = master.getVariables().getString("REFERRAL_SLAVES", "");
		
		for (String s : data.replace("[", "").replace("]", "").split(","))
		{
			if (!s.isBlank())
			{
				set.add(Integer.parseInt(s.trim()));
			}
		}
		return set;
	}
	
	/* ================= HTML ================= */
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setFile(player, "data/scripts/custom/ReferralSystem/ReferralSystem.htm");
		player.sendPacket(html);
		return null;
	}
	
	/* ================= 獎勵結構 ================= */
	
	private static class Reward
	{
		final int itemId;
		final long count;
		
		Reward(int itemId, long count)
		{
			this.itemId = itemId;
			this.count = count;
		}
	}
	
	public static void main(String[] args)
	{
		new ReferralSystem();
		System.out.println("ReferralSystem 多人綁定 + 等級獎勵版 已載入");
	}
}
