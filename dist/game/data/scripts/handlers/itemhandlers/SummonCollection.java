package handlers.itemhandlers;

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.data.holders.CollectionDataHolder;
import org.l2jmobius.gameserver.data.xml.CollectionData;
import org.l2jmobius.gameserver.data.xml.OptionData;
import org.l2jmobius.gameserver.handler.IItemHandler;
import org.l2jmobius.gameserver.model.actor.Playable;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.holders.player.PlayerCollectionData;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.holders.ItemEnchantHolder;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.options.Options;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.CreatureSay;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.network.serverpackets.collection.ExCollectionComplete;

public class SummonCollection implements IItemHandler
{
	@Override
	public boolean onItemUse(Playable playable, Item item, boolean forceUse)
	{
		if (!playable.isPlayer())
		{
			playable.sendPacket(SystemMessageId.THIS_ITEM_CANNOT_BE_TRANSFERRED_TO_A_GUARDIAN);
			return false;
		}
		
		final Player player = playable.asPlayer();
		processRandomCollection(player);
		
		player.destroyItem(ItemProcessType.NONE, item.getObjectId(), 1, null, false);
		player.sendPacket(ActionFailed.STATIC_PACKET);
		return true;
	}
	
	private void processRandomCollection(Player player)
	{
		final List<CollectionDataHolder> list = new ArrayList<>(CollectionData.getInstance().getCollections());
		if (list.isEmpty())
		{
			return;
		}
		
		final CollectionDataHolder random = list.get(Rnd.get(list.size()));
		
		final int needCount = random.getCompleteCount();
		final List<ItemEnchantHolder> items = random.getItems();
		
		if ((items == null) || items.isEmpty())
		{
			return;
		}
		
		int haveCount = 0;
		for (PlayerCollectionData pc : player.getCollections())
		{
			if (pc.getCollectionId() == random.getCollectionId())
			{
				haveCount++;
			}
		}
		
		if (haveCount >= needCount)
		{
			player.sendPacket(new CreatureSay(null, ChatType.GENERAL, "收藏激活", "已激活成功：您之前已完成該收藏！"));
			return;
		}
		
		for (int i = haveCount; i < needCount; i++)
		{
			player.getCollections().add(new PlayerCollectionData(random.getCollectionId()));
		}
		
		player.sendPacket(new ExCollectionComplete(random.getCollectionId()));
		player.sendPacket(new SystemMessage(SystemMessageId.S1_COLLECTION_IS_COMPLETE).addString("隨機收藏"));
		
		final Options opt = OptionData.getInstance().getOptions(random.getOptionId());
		if (opt != null)
		{
			opt.apply(player);
		}
	}
}
