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
package org.l2jmobius.gameserver.network.serverpackets.enchant.challengepoint;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.holders.player.ChallengePointInfoHolder;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * @author Serenitty
 */
public class ExEnchantChallengePointInfo extends ServerPacket
{
	private final ChallengePointInfoHolder[] _challengeinfo;
	
	public ExEnchantChallengePointInfo(Player player)
	{
		_challengeinfo = player.getChallengeInfo().initializeChallengePoints();
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_ENCHANT_CHALLENGE_POINT_INFO.writeId(this, buffer);
		buffer.writeInt(_challengeinfo.length); // vCurrentPointInfo
		for (ChallengePointInfoHolder info : _challengeinfo)
		{
			buffer.writeInt(info.getPointGroupId()); // nPointGroupId
			buffer.writeInt(info.getChallengePoint()); // nChallengePoint
			buffer.writeInt(info.getTicketPointOpt1()); // nTicketPointOpt1
			buffer.writeInt(info.getTicketPointOpt2()); // nTicketPointOpt2
			buffer.writeInt(info.getTicketPointOpt3()); // nTicketPointOpt3
			buffer.writeInt(info.getTicketPointOpt4()); // nTicketPointOpt4
			buffer.writeInt(info.getTicketPointOpt5()); // nTicketPointOpt5
			buffer.writeInt(info.getTicketPointOpt6()); // nTicketPointOpt6
		}
	}
}
