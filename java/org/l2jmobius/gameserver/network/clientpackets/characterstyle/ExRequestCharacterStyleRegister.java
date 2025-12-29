package org.l2jmobius.gameserver.network.clientpackets.characterstyle;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.l2jmobius.gameserver.data.enums.CharacterStyleCategoryType;
import org.l2jmobius.gameserver.data.holders.CharacterStyleDataHolder;
import org.l2jmobius.gameserver.data.xml.CharacterStylesData;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;
import org.l2jmobius.gameserver.model.variables.PlayerVariables;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.network.serverpackets.characterstyle.ExCharacterStyleList;
import org.l2jmobius.gameserver.network.serverpackets.characterstyle.ExCharacterStyleRegister;

import static org.l2jmobius.commons.util.IXmlReader.LOGGER;

/**
 * @author Brado
 */
public class ExRequestCharacterStyleRegister extends ClientPacket
{
	private int _styleType;
	private int _styleId;
	private int _costItemId;

	@Override
	protected void readImpl()
	{
		_styleType = readInt();
		_styleId = readInt();
		_costItemId = readInt();
	}

	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}

		// 獲取造型類型
		final CharacterStyleCategoryType category = CharacterStyleCategoryType.getByClientId(_styleType);
		if (category == null)
		{
			LOGGER.warning("未知的造型類型: " + _styleType + " (玩家: " + player.getName() + ")");
			player.sendPacket(ExCharacterStyleRegister.STATIC_PACKET_FAIL);
			return;
		}

		// 查找造型數據
		final CharacterStyleDataHolder style = CharacterStylesData.getInstance().getSpecificStyleByCategoryAndId(category, _styleId);
		if (style == null)
		{
			LOGGER.warning("找不到造型配置 - 類型: " + category.name() + ", ID: " + _styleId + " (玩家: " + player.getName() + ")");
			player.sendPacket(ExCharacterStyleRegister.STATIC_PACKET_FAIL);
			player.sendMessage("此造型尚未配置");
			return;
		}

		// 檢查是否已擁有
		final List<Integer> availableStyles = player.getAvailableCharacterStyles(category);
		if (availableStyles.contains(_styleId))
		{
			player.sendMessage("你已經擁有這個造型了!");
			player.sendPacket(ExCharacterStyleRegister.STATIC_PACKET_FAIL);
			return;
		}

		// 檢查道具需求
		if (style._cost == null || style._cost.isEmpty())
		{
			LOGGER.warning("造型沒有設定費用配置: " + style._name);
			player.sendPacket(ExCharacterStyleRegister.STATIC_PACKET_FAIL);
			return;
		}

		// 驗證並扣除道具
		for (ItemHolder price : style._cost)
		{
			if (player.getInventory().getInventoryItemCount(price.getId(), -1) < price.getCount())
			{
				player.sendPacket(new SystemMessage(SystemMessageId.YOU_DO_NOT_HAVE_ENOUGH_REQUIRED_ITEMS));
				player.sendPacket(ExCharacterStyleRegister.STATIC_PACKET_FAIL);
				return;
			}
		}

		for (ItemHolder price : style._cost)
		{
			if (player.getInventory().destroyItemByItemId(ItemProcessType.DESTROY, price.getId(), price.getCount(), player, null) == null)
			{
				player.sendPacket(new SystemMessage(SystemMessageId.YOU_DO_NOT_HAVE_ENOUGH_REQUIRED_ITEMS));
				player.sendPacket(ExCharacterStyleRegister.STATIC_PACKET_FAIL);
				return;
			}
			player.sendItemList();
		}

		// 註冊造型
		player.modifyCharacterStyle(category, _styleId, false, true);

		// 發送成功封包並更新列表
		player.sendPacket(ExCharacterStyleRegister.STATIC_PACKET_SUCCESS);
		sendUpdatedStyleList(player, category);
		player.sendMessage("成功解鎖造型: " + style._name);
	}

	private void sendUpdatedStyleList(Player player, CharacterStyleCategoryType category)
	{
		ItemHolder swapCost = CharacterStylesData.getInstance().getSwapCostItemByCategory(category);
		if (swapCost == null)
		{
			swapCost = new ItemHolder(57, 0);
		}

		final List<Integer> unlockedStyles = player.getAvailableCharacterStyles(category);
		final List<Integer> favoriteStyles = player.getVariables().getIntegerList(
				PlayerVariables.FAVORITE_CHARACTER_STYLES + category
		);

		final Map<Integer, Integer> activeStyles = new HashMap<>();
		if (category == CharacterStyleCategoryType.APPEARANCE_WEAPON)
		{
			for (int slot = 0; slot < 4; slot++)
			{
				final int activeId = player.getActiveCharacterStyleId(category, slot);
				if (activeId > 0)
				{
					activeStyles.put(slot, activeId);
				}
			}
		}
		else
		{
			final int activeId = player.getActiveCharacterStyleId(category);
			if (activeId > 0)
			{
				activeStyles.put(0, activeId);
			}
		}

		player.sendPacket(new ExCharacterStyleList(
				category,
				swapCost,
				unlockedStyles,
				favoriteStyles,
				activeStyles
		));
	}
}