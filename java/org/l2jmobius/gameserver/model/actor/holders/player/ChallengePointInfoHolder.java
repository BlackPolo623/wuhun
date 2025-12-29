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
package org.l2jmobius.gameserver.model.actor.holders.player;

/**
 * @author Serenitty, MacuK
 */
public class ChallengePointInfoHolder
{
	private final int _pointGroupId;
	private int _challengePoint;
	private final int _ticketPointOpt1;
	private final int _ticketPointOpt2;
	private final int _ticketPointOpt3;
	private final int _ticketPointOpt4;
	private final int _ticketPointOpt5;
	private final int _ticketPointOpt6;
	
	public ChallengePointInfoHolder(int pointGroupId, int challengePoint, int ticketPointOpt1, int ticketPointOpt2, int ticketPointOpt3, int ticketPointOpt4, int ticketPointOpt5, int ticketPointOpt6)
	{
		_pointGroupId = pointGroupId;
		_challengePoint = challengePoint;
		_ticketPointOpt1 = ticketPointOpt1;
		_ticketPointOpt2 = ticketPointOpt2;
		_ticketPointOpt3 = ticketPointOpt3;
		_ticketPointOpt4 = ticketPointOpt4;
		_ticketPointOpt5 = ticketPointOpt5;
		_ticketPointOpt6 = ticketPointOpt6;
	}
	
	public int getPointGroupId()
	{
		return _pointGroupId;
	}
	
	public int getChallengePoint()
	{
		return _challengePoint;
	}
	
	public int getTicketPointOpt1()
	{
		return _ticketPointOpt1;
	}
	
	public int getTicketPointOpt2()
	{
		return _ticketPointOpt2;
	}
	
	public int getTicketPointOpt3()
	{
		return _ticketPointOpt3;
	}
	
	public int getTicketPointOpt4()
	{
		return _ticketPointOpt4;
	}
	
	public int getTicketPointOpt5()
	{
		return _ticketPointOpt5;
	}
	
	public int getTicketPointOpt6()
	{
		return _ticketPointOpt6;
	}
	
	public void addPoints(int points)
	{
		_challengePoint += points;
	}
}
