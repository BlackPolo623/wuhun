package handlers.itemhandlers;

import org.l2jmobius.gameserver.handler.IItemHandler;
import org.l2jmobius.gameserver.model.actor.Playable;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.instance.Item;

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
}