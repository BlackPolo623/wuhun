package handlers.itemhandlers;

import org.l2jmobius.gameserver.handler.IItemHandler;
import org.l2jmobius.gameserver.model.actor.Playable;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.instance.Pet;
import org.l2jmobius.gameserver.model.item.EtcItem;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.instance.Item;

/**
 * 寵物經驗書道具處理器
 * 使用後對當前召喚的寵物增加固定經驗值。
 * 經驗值由道具 XML 的 <set name="exp_amount" val="..."/> 設定。
 */
public class PetExpBook implements IItemHandler
{
	@Override
	public boolean onItemUse(Playable playable, Item item, boolean forceUse)
	{
		if (!playable.isPlayer())
		{
			return false;
		}

		final Player player = playable.asPlayer();
		final Pet pet = player.getPet();

		if (pet == null)
		{
			player.sendMessage("請先召喚你的寵物，才能使用此道具！");
			return false;
		}

		final long expAmount = ((EtcItem) item.getTemplate()).getExpAmount();
		if (expAmount <= 0)
		{
			return false;
		}

		player.destroyItem(ItemProcessType.DESTROY, item, 1, player, true);
		pet.getStat().addExp(expAmount);
		player.sendMessage("成功為寵物增加了 " + String.format("%,d", expAmount) + " 點經驗值！");
		return true;
	}
}
