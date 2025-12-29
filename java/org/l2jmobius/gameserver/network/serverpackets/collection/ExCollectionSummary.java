package org.l2jmobius.gameserver.network.serverpackets.collection;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.holders.player.PlayerCollectionData;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * @author Mobius
 */
public class ExCollectionSummary extends ServerPacket
{
	private final Player _player;

	public ExCollectionSummary(Player player)
	{
		_player = player;
	}

	@Override
	protected void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_COLLECTION_SUMMARY.writeId(this, buffer);

		// ⭐⭐⭐ 改為發送玩家已完成的收藏品數量 ⭐⭐⭐
		buffer.writeInt(_player.getCollections().size());

		// 發送每個已完成的收藏品 ID
		for (PlayerCollectionData collection : _player.getCollections())
		{
			buffer.writeShort(collection.getCollectionId());
			buffer.writeInt(0); // remainingTime
		}
	}
}