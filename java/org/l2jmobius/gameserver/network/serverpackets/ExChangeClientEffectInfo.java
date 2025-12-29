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
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * @author Mobius, Brado
 */
public class ExChangeClientEffectInfo extends ServerPacket
{
	public static final ExChangeClientEffectInfo STATIC_FREYA_DEFAULT = new ExChangeClientEffectInfo(0, 0, 1);
	public static final ExChangeClientEffectInfo STATIC_FREYA_DESTROYED = new ExChangeClientEffectInfo(0, 0, 2);
	public static final ExChangeClientEffectInfo GIRAN_NORMAL = new ExChangeClientEffectInfo(0, 0, 1);
	public static final ExChangeClientEffectInfo GIRAN_PETALS = new ExChangeClientEffectInfo(0, 0, 2);
	public static final ExChangeClientEffectInfo GIRAN_SNOW = new ExChangeClientEffectInfo(0, 0, 3);
	public static final ExChangeClientEffectInfo GIRAN_FLOWERS = new ExChangeClientEffectInfo(0, 0, 4);
	public static final ExChangeClientEffectInfo GIRAN_WATER = new ExChangeClientEffectInfo(0, 0, 5);
	public static final ExChangeClientEffectInfo GIRAN_AUTUMN = new ExChangeClientEffectInfo(0, 0, 6);
	public static final ExChangeClientEffectInfo TRANSCEDENT_DEFAULT = new ExChangeClientEffectInfo(0, 0, 1);
	public static final ExChangeClientEffectInfo TRANSCEDENT_GIRAN = new ExChangeClientEffectInfo(0, 0, 2);
	public static final ExChangeClientEffectInfo TRANSCEDENT_ORC = new ExChangeClientEffectInfo(0, 0, 3);
	public static final ExChangeClientEffectInfo TRANSCEDENT_DWARF = new ExChangeClientEffectInfo(0, 0, 4);
	public static final ExChangeClientEffectInfo TRANSCEDENT_DARK_ELF = new ExChangeClientEffectInfo(0, 0, 5);
	public static final ExChangeClientEffectInfo TRANSCEDENT_GRACIA_AIRSTRIP = new ExChangeClientEffectInfo(0, 0, 6);
	public static final ExChangeClientEffectInfo TRANSCEDENT_ELF = new ExChangeClientEffectInfo(0, 0, 7);
	public static final ExChangeClientEffectInfo TRANSCEDENT_ERTHEIA = new ExChangeClientEffectInfo(0, 0, 8);
	public static final ExChangeClientEffectInfo TRANSCEDENT_HIGH_ELF = new ExChangeClientEffectInfo(0, 0, 9);
	public static final ExChangeClientEffectInfo TRANSCEDENT_SKY_TOWER = new ExChangeClientEffectInfo(0, 0, 10);
	public static final ExChangeClientEffectInfo SEAL_OF_SHILLEN_DEFAULT = new ExChangeClientEffectInfo(0, 0, 1);
	public static final ExChangeClientEffectInfo SEAL_OF_SHILLEN_CORE = new ExChangeClientEffectInfo(0, 0, 2);
	public static final ExChangeClientEffectInfo SEAL_OF_SHILLEN_ORFEN = new ExChangeClientEffectInfo(0, 0, 3);
	public static final ExChangeClientEffectInfo SEAL_OF_SHILLEN_QUEEN_ANT = new ExChangeClientEffectInfo(0, 0, 4);
	public static final ExChangeClientEffectInfo SEAL_OF_SHILLEN_ZAKEN = new ExChangeClientEffectInfo(0, 0, 5);
	
	private final int _type;
	private final int _key;
	private final int _value;
	
	/**
	 * @param type
	 *            <ul>
	 *            <li>0 - ChangeZoneState</li>
	 *            <li>1 - SetL2Fog</li>
	 *            <li>2 - postEffectData</li>
	 *            </ul>
	 * @param key
	 * @param value
	 */
	public ExChangeClientEffectInfo(int type, int key, int value)
	{
		_type = type;
		_key = key;
		_value = value;
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_CLIENT_EFFECT_INFO.writeId(this, buffer);
		buffer.writeInt(_type);
		buffer.writeInt(_key);
		buffer.writeInt(_value);
	}
}
