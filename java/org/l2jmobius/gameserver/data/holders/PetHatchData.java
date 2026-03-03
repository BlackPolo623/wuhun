package org.l2jmobius.gameserver.data.holders;

/**
 * 寵物孵化資料 Holder
 * @author Custom
 */
public class PetHatchData
{
	public final int playerId;
	public final int slotIndex;
	public final int eggItemId;
	public final int eggTier;
	public final long startTime;
	public final int hatchDuration;
	public final int feedConsumed;
	public final int upgradeChance;

	public PetHatchData(int playerId, int slotIndex, int eggItemId, int eggTier, long startTime, int hatchDuration, int feedConsumed, int upgradeChance)
	{
		this.playerId = playerId;
		this.slotIndex = slotIndex;
		this.eggItemId = eggItemId;
		this.eggTier = eggTier;
		this.startTime = startTime;
		this.hatchDuration = hatchDuration;
		this.feedConsumed = feedConsumed;
		this.upgradeChance = upgradeChance;
	}
}
