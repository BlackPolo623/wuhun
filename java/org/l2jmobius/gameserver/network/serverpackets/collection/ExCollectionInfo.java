package org.l2jmobius.gameserver.network.serverpackets.collection;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.data.holders.CollectionDataHolder;
import org.l2jmobius.gameserver.data.xml.CollectionData;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.holders.player.PlayerCollectionData;
import org.l2jmobius.gameserver.model.item.holders.ItemEnchantHolder;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * @author Mobius
 */
public class ExCollectionInfo extends ServerPacket
{
	final Player _player;
	final int _category;
	final Set<Integer> _collectionIds = new HashSet<>();
	final List<Integer> _favoriteIds;
	final List<CollectionHolder> _collectionHolders = new LinkedList<>();

	public ExCollectionInfo(Player player, int category)
	{
		_player = player;
		_category = category;

		// 收集該分類下玩家已完成的收藏品
		for (PlayerCollectionData collection : player.getCollections())
		{
			final CollectionDataHolder collectionData = CollectionData.getInstance().getCollection(collection.getCollectionId());
			if ((collectionData != null) && (collectionData.getCategory() == category))
			{
				_collectionIds.add(collection.getCollectionId());
			}
		}

		_favoriteIds = player.getCollectionFavorites();

		// ⭐ 只生成第一個槽位的數據
		for (int collectionId : _collectionIds)
		{
			final CollectionDataHolder collectionData = CollectionData.getInstance().getCollection(collectionId);
			if (collectionData == null)
			{
				continue;
			}

			final CollectionHolder holder = new CollectionHolder(collectionId);
			final List<ItemEnchantHolder> items = collectionData.getItems();

			final ItemEnchantHolder displayItem = items.isEmpty() ?
					new ItemEnchantHolder(105802, 1, 0) : items.get(0);

			// 只添加第一個槽位
			holder.addCollectionData(0, displayItem.getId(), displayItem.getEnchantLevel());

			_collectionHolders.add(holder);
		}
	}

	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_COLLECTION_INFO.writeId(this, buffer);

		// Write collectionHolders data
		buffer.writeInt(_collectionHolders.size()); // size
		for (CollectionHolder holder : _collectionHolders)
		{
			final List<CollectionItemData> collectionDataList = holder.getCollectionData();
			buffer.writeInt(collectionDataList.size());
			for (CollectionItemData dataHolder : collectionDataList)
			{
				buffer.writeByte(dataHolder.getIndex());
				buffer.writeInt(dataHolder.getItemId());
				buffer.writeByte(dataHolder.getEnchantLevel());
				buffer.writeByte(0); // bless
				buffer.writeByte(0); // bless Condition
				buffer.writeInt(1); // amount
			}

			buffer.writeShort(holder.getCollectionId());
		}

		// favoriteList
		buffer.writeInt(_favoriteIds.size());
		for (int id : _favoriteIds)
		{
			buffer.writeShort(id);
		}

		// rewardList
		buffer.writeInt(0);

		buffer.writeByte(_category);
		buffer.writeShort(0);
	}

	// ⭐ 內部類：持有收藏品的虛擬數據
	private class CollectionHolder
	{
		private final int _collectionId;
		private final List<CollectionItemData> _collectionData;

		public CollectionHolder(int collectionId)
		{
			_collectionId = collectionId;
			_collectionData = new LinkedList<>();
		}

		public int getCollectionId()
		{
			return _collectionId;
		}

		public List<CollectionItemData> getCollectionData()
		{
			return _collectionData;
		}

		public void addCollectionData(int index, int itemId, int enchantLevel)
		{
			_collectionData.add(new CollectionItemData(index, itemId, enchantLevel));
		}
	}

	// ⭐ 內部類：持有單個槽位的虛擬數據
	private class CollectionItemData
	{
		private final int _index;
		private final int _itemId;
		private final int _enchantLevel;

		public CollectionItemData(int index, int itemId, int enchantLevel)
		{
			_index = index;
			_itemId = itemId;
			_enchantLevel = enchantLevel;
		}

		public int getIndex()
		{
			return _index;
		}

		public int getItemId()
		{
			return _itemId;
		}

		public int getEnchantLevel()
		{
			return _enchantLevel;
		}
	}
}