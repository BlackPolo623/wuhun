package org.l2jmobius.gameserver.data.holders;

/**
 * 未領取寵物數據
 * @author Custom
 */
public class UnclaimedPetData
{
	public final int id;
	public final int playerId;
	public final int petItemId;
	public final int tier;
	public final long hatchTime;
	public final boolean eventFired;

	public UnclaimedPetData(int id, int playerId, int petItemId, int tier, long hatchTime, boolean eventFired)
	{
		this.id = id;
		this.playerId = playerId;
		this.petItemId = petItemId;
		this.tier = tier;
		this.hatchTime = hatchTime;
		this.eventFired = eventFired;
	}
}
