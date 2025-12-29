/*
 * Copyright (c) 2013 L2jMobius
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
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
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.network.serverpackets.collection.ExCollectionComplete;
import org.l2jmobius.gameserver.network.serverpackets.collection.ExCollectionList;
import org.l2jmobius.gameserver.network.serverpackets.collection.ExCollectionRegister;
import org.l2jmobius.gameserver.network.serverpackets.collection.ExCollectionSummary;

/**
 * 隨機完成收藏品道具處理器
 * 使用後隨機選擇一個未完成的收藏品並自動完成它
 *
 * @author 黑普羅
 */
public class RandomCollectionComplete implements IItemHandler {
    @Override
    public boolean onItemUse(Playable playable, Item item, boolean forceUse) {
        if (!playable.isPlayer()) {
            playable.sendPacket(SystemMessageId.THIS_ITEM_CANNOT_BE_TRANSFERRED_TO_A_GUARDIAN);
            return false;
        }

        final Player player = playable.asPlayer();

        // 獲取未完成的收藏品列表
        final List<CollectionDataHolder> uncompletedCollections = new ArrayList<>();
        for (CollectionDataHolder collection : CollectionData.getInstance().getCollections()) {
            if (!isCollectionComplete(player, collection)) {
                uncompletedCollections.add(collection);
            }
        }

        if (uncompletedCollections.isEmpty()) {
            player.sendMessage("恭喜!你已完成所有收藏品!");
            return false;
        }

        final CollectionDataHolder selectedCollection = uncompletedCollections.get(Rnd.get(uncompletedCollections.size()));

// 銷毀使用的道具
        if (!player.destroyItem(ItemProcessType.FEE, item, 1, player, true)) {
            return false;
        }

// 完成選中的收藏品
        completeCollection(player, selectedCollection);

        return true;
    }

    /**
     * 檢查玩家是否已完成指定收藏品
     *
     * @param player     玩家
     * @param collection 收藏品數據
     * @return 是否已完成
     */
    private boolean isCollectionComplete(Player player, CollectionDataHolder collection) {
        for (PlayerCollectionData data : player.getCollections()) {
            if (data.getCollectionId() == collection.getCollectionId()) {
                return true; // 簡單判斷：存在即完成
            }
        }
        return false;
    }

    private void completeCollection(Player player, CollectionDataHolder collection) {
        final int collectionId = collection.getCollectionId();
        final List<ItemEnchantHolder> items = collection.getItems();

        // 檢查是否已完成
        for (PlayerCollectionData data : player.getCollections()) {
            if (data.getCollectionId() == collectionId) {
                player.sendMessage("你已經完成過這個收藏品了！");
                return;
            }
        }

        // 添加到收藏品列表
        player.getCollections().add(new PlayerCollectionData(collectionId));

        // ⭐ 只發送第一個槽位（讓客戶端知道這個收藏品存在）
        final ItemEnchantHolder displayItem = items.isEmpty() ?
                new ItemEnchantHolder(105802, 1, 0) : items.get(0);

        player.sendPacket(new ExCollectionRegister(true, collectionId, 0, displayItem));

        // ⭐⭐⭐ 立即發送完成封包 - 這會讓整個收藏品框變色 ⭐⭐⭐
        player.sendPacket(new ExCollectionComplete(collectionId));

        player.sendPacket(new SystemMessage(SystemMessageId.S1_COLLECTION_IS_COMPLETE)
                .addString("收藏品 #" + collectionId));

        // 應用 Options
        final Options options = OptionData.getInstance().getOptions(collection.getOptionId());
        if (options != null) {
            options.apply(player);
        }

        // 刷新界面
        player.sendPacket(new ExCollectionList(collection.getCategory()));
        player.sendPacket(new ExCollectionSummary(player));
        player.sendSkillList();
        player.getStat().recalculateStats(true);
        player.broadcastUserInfo();
        player.updateUserInfo();

        // 保存到數據庫
        player.storeCollections();

        player.sendMessage("恭喜！你已完成收藏品 #" + collectionId);
    }

}
