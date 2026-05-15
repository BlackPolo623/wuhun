package handlers.itemhandlers;

import org.l2jmobius.gameserver.handler.IItemHandler;
import org.l2jmobius.gameserver.model.actor.Playable;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.skill.Skill;

public class SkillPermission implements IItemHandler
{
	// 權限道具配置 {道具ID, 技能ID, 技能等級, "技能名稱"}
	private static final Object[][] PERMISSIONS = {
			{109001, 101001, 1, "武器强制轉換-初級"},
			{109001, 101005, 1, "武器强制轉換法師-初級"},
			{109002, 101002, 1, "武器强制轉換-中級"},
			{109002, 101006, 1, "武器强制轉換法師-中級"},
			{109003, 101003, 1, "武器强制轉換-高級"},
			{109003, 101007, 1, "武器强制轉換法師-中級"},
			{109004, 101004, 1, "武器强制轉換-頂級"},
			{109004, 101008, 1, "武器强制轉換法師-頂級"},
	};

	private static final int[][] SLAVE_SKILLS = {
			{101001, 101005},
			{101002, 101006},
			{101003, 101007},
			{101004, 101008},
	};

	// 技能等級分組 {master, slave}，索引越大等級越高
	// 索引 0 初級、1 中級、2 高級、3 頂級
	private static final int[][] TIER_GROUPS = {
			{101001, 101005}, // 初級
			{101002, 101006}, // 中級
			{101003, 101007}, // 高級
			{101004, 101008}, // 頂級
	};
	private static final String[] TIER_NAMES = {"初級", "中級", "高級", "頂級"};

	private static final int CONTROLLER_ITEM_ID = 105806;

	@Override
	public boolean onItemUse(Playable playable, Item item, boolean forceUse)
	{
		if (!playable.isPlayer())
		{
			return false;
		}

		Player player = playable.asPlayer();
		int itemId = item.getId();

		Object[] config = null;
		for (Object[] cfg : PERMISSIONS)
		{
			if ((int) cfg[0] == itemId)
			{
				config = cfg;
				break;
			}
		}

		if (config == null)
		{
			return false;
		}

		int skillId = (int) config[1];
		String skillName = (String) config[3];
		String varName = "SkillPerm_" + skillId;

		if (player.getVariables().getBoolean(varName, false))
		{
			player.sendMessage("您已經擁有【" + skillName + "】的權限了！");
			return false;
		}

		player.destroyItem(ItemProcessType.NONE, item, 1, null, true);
		player.getVariables().set(varName, true);

		if (player.getInventory().getItemByItemId(CONTROLLER_ITEM_ID) == null)
		{
			player.addItem(ItemProcessType.NONE, CONTROLLER_ITEM_ID, 1, player, true);
		}

		player.sendMessage("獲得【" + skillName + "】權限！請使用「技能控制器」開啟技能");

		return true;
	}
	// 新增方法
	public static int getSlaveSkill(int masterSkillId)
	{
		for (int[] pair : SLAVE_SKILLS)
		{
			if (pair[0] == masterSkillId)
				return pair[1];
		}
		return 0;
	}

	// 新增方法：檢查是否為從屬技能
	public static boolean isSlaveSkill(int skillId)
	{
		for (int[] pair : SLAVE_SKILLS)
		{
			if (pair[1] == skillId)
				return true;
		}
		return false;
	}

	public static Object[][] getAllSkills()
	{
		return PERMISSIONS;
	}

	/**
	 * 檢查並強制只保留玩家擁有的最高等級技能組。
	 * 規則：
	 * - 同時擁有「初+中」→ 留中、移除初
	 * - 同時擁有「初+中+高」→ 留高、移除初與中
	 * - 多餘等級的 master 與 slave 都會從玩家身上移除
	 * - 同時清除多餘等級對應的權限變數（SkillPerm_）
	 *
	 * @param player 要檢查的玩家
	 * @return 被移除的等級名稱列表（用於通知玩家），無動作時回傳 null
	 */
	public static String enforceHighestTierOnly(Player player)
	{
		if (player == null)
		{
			return null;
		}

		// 找出玩家實際持有技能的最高等級索引
		int highestTier = -1;
		for (int i = TIER_GROUPS.length - 1; i >= 0; i--)
		{
			if (player.getKnownSkill(TIER_GROUPS[i][0]) != null
				|| player.getKnownSkill(TIER_GROUPS[i][1]) != null)
			{
				highestTier = i;
				break;
			}
		}

		if (highestTier <= 0)
		{
			return null; // 無技能或只有最低等級，不需處理
		}

		final StringBuilder removed = new StringBuilder();
		boolean any = false;

		// 移除所有低於最高等級的技能與權限
		for (int i = 0; i < highestTier; i++)
		{
			boolean tierTouched = false;
			for (int skillId : TIER_GROUPS[i])
			{
				final Skill known = player.getKnownSkill(skillId);
				if (known != null)
				{
					player.removeSkill(known, true, true);
					tierTouched = true;
				}
				// 一律移除權限變數，避免日後再次出現多組
				player.getVariables().remove("SkillPerm_" + skillId);
			}
			if (tierTouched)
			{
				if (removed.length() > 0) removed.append("、");
				removed.append(TIER_NAMES[i]);
				any = true;
			}
		}

		if (!any)
		{
			return null;
		}

		player.sendSkillList();
		player.broadcastUserInfo();
		return removed.toString();
	}
}