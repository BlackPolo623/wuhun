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
package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.model.VariationInstance;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.holders.player.Shortcut;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * @author Mobius
 */
public class ShortcutRegister extends ServerPacket
{
	private final Player _player;
	private final Shortcut _shortcut;
	
	/**
	 * Register new skill shortcut
	 * @param shortcut
	 * @param player
	 */
	public ShortcutRegister(Shortcut shortcut, Player player)
	{
		_player = player;
		_shortcut = shortcut;
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.SHORTCUT_REG.writeId(this, buffer);
		buffer.writeInt(_shortcut.getType().ordinal());
		buffer.writeInt(_shortcut.getSlot() + (_shortcut.getPage() * 12)); // C4 Client
		buffer.writeByte(_shortcut.isAutoUse()); // 228
		switch (_shortcut.getType())
		{
			case ITEM:
			{
				buffer.writeInt(_shortcut.getId());
				buffer.writeInt(_shortcut.getCharacterType());
				buffer.writeInt(_shortcut.getSharedReuseGroup());
				buffer.writeInt(0); // unknown
				buffer.writeInt(0); // unknown
				final Item item = _player.getInventory().getItemByObjectId(_shortcut.getId());
				if (item != null)
				{
					final VariationInstance augment = item.getAugmentation();
					if (augment != null)
					{
						buffer.writeInt(augment.getOption1Id());
						buffer.writeInt(augment.getOption2Id());
						buffer.writeInt(augment.getOption3Id());
					}
					else
					{
						buffer.writeInt(0);
						buffer.writeInt(0);
						buffer.writeInt(0);
					}
					
					buffer.writeInt(item.getVisualId());
				}
				else
				{
					buffer.writeInt(0);
					buffer.writeInt(0);
					buffer.writeInt(0);
				}
				
				break;
			}
			case SKILL:
			{
				buffer.writeInt(_shortcut.getId());
				buffer.writeShort(_shortcut.getLevel());
				buffer.writeShort(_shortcut.getSubLevel());
				buffer.writeInt(_shortcut.getSharedReuseGroup());
				buffer.writeByte(0); // C5
				buffer.writeInt(_shortcut.getCharacterType());
				buffer.writeInt(0); // if 1 - cannot use
				buffer.writeInt(0); // reuse delay ?
				break;
			}
			case ACTION:
			case MACRO:
			case RECIPE:
			case BOOKMARK:
			{
				buffer.writeInt(_shortcut.getId());
				buffer.writeInt(_shortcut.getCharacterType());
				break;
			}
		}
	}
}
