package org.l2jmobius.gameserver.network.clientpackets.collection;

import org.l2jmobius.gameserver.data.holders.CollectionDataHolder;
import org.l2jmobius.gameserver.data.xml.CollectionData;
import org.l2jmobius.gameserver.data.xml.OptionData;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.holders.player.PlayerCollectionData;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.holders.ItemEnchantHolder;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.options.Options;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.serverpackets.ConfirmDlg;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.network.serverpackets.collection.ExCollectionComplete;
import org.l2jmobius.gameserver.network.serverpackets.collection.ExCollectionList;
import org.l2jmobius.gameserver.network.serverpackets.collection.ExCollectionRegister;
import org.l2jmobius.gameserver.network.serverpackets.collection.ExCollectionSummary;

/**
 * @author Berezkin Nikolay, Mobius
 */
public class RequestCollectionRegister extends ClientPacket
{
	private int _collectionId;
	private int _index;
	private int _itemObjId;

	@Override
	protected void readImpl()
	{
		_collectionId = readShort();
		_index = readInt();
		_itemObjId = readInt();
	}

	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}

		final Item item = player.getInventory().getItemByObjectId(_itemObjId);
		if (item == null)
		{
			player.sendMessage("Item not found.");
			return;
		}

		final CollectionDataHolder collection = CollectionData.getInstance().getCollection(_collectionId);
		if (collection == null)
		{
			player.sendMessage("Could not find collection.");
			return;
		}

		// 檢查物品是否符合收藏品要求
		long count = 0;
		for (ItemEnchantHolder data : collection.getItems())
		{
			if ((data.getId() == item.getId()) && ((data.getEnchantLevel() == 0) || (data.getEnchantLevel() == item.getEnchantLevel())))
			{
				count = data.getCount();
				break;
			}
		}

		if ((count == 0) || (item.getCount() < count) || item.isEquipped())
		{
			player.sendMessage("Incorrect item count.");
			return;
		}

		// ⭐ 新邏輯：檢查收藏品是否已完成
		boolean isComplete = false;
		for (PlayerCollectionData coll : player.getCollections())
		{
			if (coll.getCollectionId() == _collectionId)
			{
				isComplete = true;
				break;
			}
		}

		if (isComplete)
		{
			player.sendPacket(new ExCollectionRegister(false, _collectionId, _index, new ItemEnchantHolder(item.getId(), count, item.getEnchantLevel())));
			player.sendPacket(SystemMessageId.THIS_ITEM_CANNOT_BE_ADDED_TO_YOUR_COLLECTION);
			player.sendPacket(new ConfirmDlg("Collection already completed."));
			return;
		}

		// ⭐ 新邏輯：一次性完成整個收藏品
		// 銷毀物品
		player.destroyItem(ItemProcessType.FEE, item, count, player, true);

		// 添加到收藏品列表（只存 collectionId）
		player.getCollections().add(new PlayerCollectionData(_collectionId));

		// 動態生成所有槽位的註冊封包
		final int completeCount = collection.getCompleteCount();
		for (int i = 0; i < completeCount && i < collection.getItems().size(); i++)
		{
			final ItemEnchantHolder itemData = collection.getItems().get(i);
			player.sendPacket(new ExCollectionRegister(true, _collectionId, i, itemData));
		}

		// 發送完成封包
		player.sendPacket(new ExCollectionComplete(_collectionId));
		player.sendPacket(new SystemMessage(SystemMessageId.S1_COLLECTION_IS_COMPLETE).addString("收藏品 #" + _collectionId));

		// 應用 Options
		final Options options = OptionData.getInstance().getOptions(collection.getOptionId());
		if (options != null)
		{
			options.apply(player);
		}

		// 刷新界面
		player.sendPacket(new ExCollectionList(collection.getCategory()));
		player.sendPacket(new ExCollectionSummary(player));
		player.sendSkillList();
		player.getStat().recalculateStats(true);
		player.broadcastUserInfo();

		// 保存到數據庫
		player.storeCollections();

		player.sendMessage("恭喜！你已完成收藏品 #" + _collectionId);
	}
}