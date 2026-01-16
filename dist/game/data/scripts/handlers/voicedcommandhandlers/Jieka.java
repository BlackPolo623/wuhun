package handlers.voicedcommandhandlers;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.l2jmobius.gameserver.handler.IVoicedCommandHandler;
import org.l2jmobius.gameserver.managers.InstanceManager;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.instancezone.Instance;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.CreatureSay;

public class Jieka implements IVoicedCommandHandler
{
	private static final String[] VOICED_COMMANDS =
			{
					"QLFB",
					"解卡",
					"ex",
			};

	@Override
	public boolean onCommand(String command, Player activeChar, String params)
	{
		if (command.equals("QLFB") || command.equals("解卡") || command.equals("ex"))
		{
			// ===== 1. 清除副本進入時間限制（原有功能） =====
			final Map<Integer, Long> instanceTimes = InstanceManager.getInstance().getAllInstanceTimes(activeChar);
			for (Entry<Integer, Long> entry : instanceTimes.entrySet())
			{
				final int id = entry.getKey();
				InstanceManager.getInstance().deleteInstanceTime(activeChar, id);
			}
			InstanceManager.getInstance().restoreInstanceTimes();

			// ===== 2. 清除玩家的副本關聯狀態（新增） =====
			final Instance currentInstance = InstanceManager.getInstance().getPlayerInstance(activeChar, false);
			if (currentInstance != null)
			{
				// 如果玩家還在副本內，先傳送出去
				if (currentInstance.containsPlayer(activeChar))
				{
					currentInstance.ejectPlayer(activeChar);
					activeChar.sendPacket(new CreatureSay(null, ChatType.WORLD, "解卡功能", "已將您傳送出副本"));
				}

				// ===== 調用自定義的 removeAllowed 方法 =====
				if (removeAllowed(currentInstance, activeChar))
				{
					activeChar.sendPacket(new CreatureSay(null, ChatType.WORLD, "解卡功能", "已清除副本關聯狀態"));
				}
				else
				{
					activeChar.sendPacket(new CreatureSay(null, ChatType.WORLD, "解卡功能", "清除副本狀態時發生錯誤"));
				}
			}

			if (currentInstance != null)
			{
				activeChar.sendPacket(new CreatureSay(null, ChatType.WORLD, "解卡功能", "✓ 從副本 " + currentInstance.getTemplateId() + " 中移除"));
			}
			activeChar.sendPacket(new CreatureSay(null, ChatType.WORLD, "解卡功能", "您現在可以正常進入副本了！"));
		}
		return true;
	}

	// ===== 新增：獨立的 removeAllowed 函數 =====
	/**
	 * 從副本的允許列表中移除玩家
	 * @param instance 副本實例
	 * @param player 要移除的玩家
	 * @return 成功返回 true，失敗返回 false
	 */
	private boolean removeAllowed(Instance instance, Player player)
	{
		try
		{
			// 使用反射獲取 Instance 類的私有字段 _allowed
			Field allowedField = Instance.class.getDeclaredField("_allowed");

			// 設置為可訪問（繞過 private 限制）
			allowedField.setAccessible(true);

			// 獲取 _allowed 字段的值（這是一個 Set<Integer> 集合）
			@SuppressWarnings("unchecked")
			Set<Integer> allowedSet = (Set<Integer>) allowedField.get(instance);

			// 從集合中移除玩家的 ObjectId
			boolean removed = allowedSet.remove(player.getObjectId());

			return removed;
		}
		catch (NoSuchFieldException e)
		{
			System.err.println("【解卡錯誤】找不到 _allowed 字段: " + e.getMessage());
			e.printStackTrace();
			return false;
		}
		catch (IllegalAccessException e)
		{
			System.err.println("【解卡錯誤】無法訪問 _allowed 字段: " + e.getMessage());
			e.printStackTrace();
			return false;
		}
		catch (Exception e)
		{
			System.err.println("【解卡錯誤】移除 allowed 狀態時發生異常: " + e.getMessage());
			e.printStackTrace();
			return false;
		}
	}

	@Override
	public String[] getCommandList()
	{
		return VOICED_COMMANDS;
	}
}