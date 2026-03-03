package org.l2jmobius.gameserver.data.holders;

/**
 * 寵物收藏資料 Holder
 * @author Custom
 */
public class PetCollectionData
{
	public final int playerId;
	public final int petItemId;
	public final boolean stored;

	public PetCollectionData(int playerId, int petItemId, boolean stored)
	{
		this.playerId = playerId;
		this.petItemId = petItemId;
		this.stored = stored;
	}
}
