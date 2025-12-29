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
 * @author Sdw, Atronic
 */
public class ExMagicAttackInfo extends ServerPacket
{
	public static final int OVERHIT = 1;
	public static final int EVADED = 2;
	public static final int BLOCKED = 3;
	public static final int RESISTED = 4;
	public static final int IMMUNE = 5;
	public static final int IMMUNE2 = 6;
	public static final int CRITICAL = 7;
	public static final int CRITICAL_HEAL = 8;
	public static final int PERFECTION = 9;
	public static final int P_CRITICAL = 10;
	public static final int M_CRITICAL = 11;
	
	private final int _caster;
	private final int _target;
	private final int _type;
	
	public ExMagicAttackInfo(int caster, int target, int type)
	{
		_caster = caster;
		_target = target;
		_type = type;
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_MAGIC_ATTACK_INFO.writeId(this, buffer);
		buffer.writeInt(_caster);
		buffer.writeInt(_target);
		buffer.writeInt(_type);
	}
}
