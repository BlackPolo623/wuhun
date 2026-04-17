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
package org.l2jmobius.gameserver.network.serverpackets.ranking;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.config.ServerConfig;
import org.l2jmobius.gameserver.managers.RankManager;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * @author NviX, Mobius
 */
public class ExRankingCharRankers extends ServerPacket
{
	private final Player _player;
	private final int _group;
	private final int _scope;
	private final int _race;
	private final int _class;
	private final Map<Integer, StatSet> _playerList;
	private final Map<Integer, StatSet> _snapshotList;
	private final Map<Integer, Integer> _snapshotCharIdToRank;

	public ExRankingCharRankers(Player player, int group, int scope, int race, int baseclass)
	{
		_player = player;
		_group = group;
		_scope = scope;
		_race = race;
		_class = baseclass;
		_playerList = RankManager.getInstance().getRankList();
		_snapshotList = RankManager.getInstance().getSnapshotList();
		_snapshotCharIdToRank = RankManager.getInstance().getSnapshotCharIdToRank();
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_RANKING_CHAR_RANKERS.writeId(this, buffer);
		buffer.writeByte(_group);
		buffer.writeByte(_scope);
		buffer.writeInt(_race);
		buffer.writeInt(_player.getPlayerClass().getId());
		if (!_playerList.isEmpty())
		{
			switch (_group)
			{
				case 0: // all
				{
					if (_scope == 0) // all
					{
						final int count = _playerList.size() > 150 ? 150 : _playerList.size();
						buffer.writeInt(count);
						for (Integer id : _playerList.keySet())
						{
							final StatSet player = _playerList.get(id);
							buffer.writeSizedString(player.getString("name"));
							buffer.writeSizedString(player.getString("clanName"));
							buffer.writeInt(ServerConfig.SERVER_ID);
							buffer.writeInt(player.getInt("soulring"));
							buffer.writeInt(player.getInt("classId"));
							buffer.writeInt(player.getInt("race"));
							buffer.writeInt(id); // server rank
							final Integer snapshotRank0 = _snapshotCharIdToRank.get(player.getInt("charId"));
							if (snapshotRank0 != null)
							{
								final StatSet snapshot = _snapshotList.get(snapshotRank0);
								buffer.writeInt(snapshotRank0); // server rank snapshot
								buffer.writeInt(snapshot != null ? snapshot.getInt("raceRank", 0) : 0); // race rank snapshot
								buffer.writeInt(snapshot != null ? snapshot.getInt("classRank", 0) : 0); // nClassRank_Snapshot
							}
							else
							{
								buffer.writeInt(id);
								buffer.writeInt(0);
								buffer.writeInt(0);
							}
						}
					}
					else
					{
						boolean found = false;
						for (Integer id : _playerList.keySet())
						{
							final StatSet player = _playerList.get(id);
							if (player.getInt("charId") == _player.getObjectId())
							{
								found = true;
								final int first = id > 10 ? (id - 9) : 1;
								final int last = _playerList.size() >= (id + 10) ? id + 10 : id + (_playerList.size() - id);
								if (first == 1)
								{
									buffer.writeInt(last - (first - 1));
								}
								else
								{
									buffer.writeInt(last - first);
								}

								for (int id2 = first; id2 <= last; id2++)
								{
									final StatSet plr = _playerList.get(id2);
									buffer.writeSizedString(plr.getString("name"));
									buffer.writeSizedString(plr.getString("clanName"));
									buffer.writeInt(ServerConfig.SERVER_ID);
									buffer.writeInt(plr.getInt("soulring"));
									buffer.writeInt(plr.getInt("classId"));
									buffer.writeInt(plr.getInt("race"));
									buffer.writeInt(id2); // server rank
									final Integer snapshotRank1 = _snapshotCharIdToRank.get(player.getInt("charId"));
									if (snapshotRank1 != null)
									{
										final StatSet snapshot = _snapshotList.get(snapshotRank1);
										buffer.writeInt(snapshotRank1); // server rank snapshot
										buffer.writeInt(snapshot != null ? snapshot.getInt("raceRank", 0) : 0);
										buffer.writeInt(snapshot != null ? snapshot.getInt("classRank", 0) : 0); // nClassRank_Snapshot
									}
								}
							}
						}
						
						if (!found)
						{
							buffer.writeInt(0);
						}
					}
					break;
				}
				case 1: // race
				{
					if (_scope == 0) // all
					{
						int count = 0;
						for (int i = 1; i <= _playerList.size(); i++)
						{
							final StatSet player = _playerList.get(i);
							if (_race == player.getInt("race"))
							{
								count++;
							}
						}
						
						buffer.writeInt(count > 100 ? 100 : count);
						int i = 1;
						for (Integer id : _playerList.keySet())
						{
							final StatSet player = _playerList.get(id);
							if (_race == player.getInt("race"))
							{
								buffer.writeSizedString(player.getString("name"));
								buffer.writeSizedString(player.getString("clanName"));
								buffer.writeInt(ServerConfig.SERVER_ID);
								buffer.writeInt(player.getInt("soulring"));
								buffer.writeInt(player.getInt("classId"));
								buffer.writeInt(player.getInt("race"));
								buffer.writeInt(i); // server rank
								final Integer snapshotRank2 = _snapshotCharIdToRank.get(player.getInt("charId"));
								if (snapshotRank2 != null)
								{
									final StatSet snapshot = _snapshotList.get(snapshotRank2);
									buffer.writeInt(snapshotRank2); // server rank snapshot
									buffer.writeInt(snapshot != null ? snapshot.getInt("raceRank", 0) : 0); // race rank snapshot
									buffer.writeInt(snapshot != null ? snapshot.getInt("classRank", 0) : 0); // nClassRank_Snapshot
								}
								else
								{
									buffer.writeInt(i);
									buffer.writeInt(i);
									buffer.writeInt(i); // nClassRank_Snapshot
								}

								i++;
							}
						}
					}
					else
					{
						boolean found = false;
						final Map<Integer, StatSet> raceList = new ConcurrentHashMap<>();
						int i = 1;
						for (Integer id : _playerList.keySet())
						{
							final StatSet set = _playerList.get(id);
							if (_player.getRace().ordinal() == set.getInt("race"))
							{
								raceList.put(i, _playerList.get(id));
								i++;
							}
						}
						
						for (Integer id : raceList.keySet())
						{
							final StatSet player = raceList.get(id);
							if (player.getInt("charId") == _player.getObjectId())
							{
								found = true;
								final int first = id > 10 ? (id - 9) : 1;
								final int last = raceList.size() >= (id + 10) ? id + 10 : id + (raceList.size() - id);
								if (first == 1)
								{
									buffer.writeInt(last - (first - 1));
								}
								else
								{
									buffer.writeInt(last - first);
								}
								
								for (int id2 = first; id2 <= last; id2++)
								{
									final StatSet plr = raceList.get(id2);
									buffer.writeSizedString(plr.getString("name"));
									buffer.writeSizedString(plr.getString("clanName"));
									buffer.writeInt(ServerConfig.SERVER_ID);
									buffer.writeInt(plr.getInt("soulring"));
									buffer.writeInt(plr.getInt("classId"));
									buffer.writeInt(plr.getInt("race"));
									buffer.writeInt(id2); // server rank
									buffer.writeInt(id2);
									buffer.writeInt(id2);
									buffer.writeInt(id2); // nClassRank_Snapshot
								}
							}
						}

						if (!found)
						{
							buffer.writeInt(0);
						}
					}
					break;
				}
				case 2: // clan
				{
					final Clan clan = _player.getClan();
					if (clan != null)
					{
						final Map<Integer, StatSet> clanList = new ConcurrentHashMap<>();
						int i = 1;
						for (Integer id : _playerList.keySet())
						{
							final StatSet set = _playerList.get(id);
							if (clan.getName().equals(set.getString("clanName")))
							{
								clanList.put(i, _playerList.get(id));
								i++;
							}
						}
						
						buffer.writeInt(clanList.size());
						for (Integer id : clanList.keySet())
						{
							final StatSet player = clanList.get(id);
							buffer.writeSizedString(player.getString("name"));
							buffer.writeSizedString(player.getString("clanName"));
							buffer.writeInt(ServerConfig.SERVER_ID);
							buffer.writeInt(player.getInt("soulring"));
							buffer.writeInt(player.getInt("classId"));
							buffer.writeInt(player.getInt("race"));
							buffer.writeInt(id); // clan rank
							final Integer snapshotRank3 = _snapshotCharIdToRank.get(player.getInt("charId"));
							if (snapshotRank3 != null)
							{
								final StatSet snapshot = _snapshotList.get(snapshotRank3);
								buffer.writeInt(snapshotRank3); // server rank snapshot
								buffer.writeInt(snapshot != null ? snapshot.getInt("raceRank", 0) : 0); // race rank snapshot
								buffer.writeInt(snapshot != null ? snapshot.getInt("classRank", 0) : 0); // nClassRank_Snapshot
							}
							else
							{
								buffer.writeInt(id);
								buffer.writeInt(0);
								buffer.writeInt(0);
							}
						}
					}
					else
					{
						buffer.writeInt(0);
					}
					break;
				}
				case 3: // friend
				{
					if (!_player.getFriendList().isEmpty())
					{
						final Set<Integer> friendList = ConcurrentHashMap.newKeySet();
						int count = 1;
						for (int id : _player.getFriendList())
						{
							for (Integer id2 : _playerList.keySet())
							{
								final StatSet temp = _playerList.get(id2);
								if (temp.getInt("charId") == id)
								{
									friendList.add(temp.getInt("charId"));
									count++;
								}
							}
						}
						
						friendList.add(_player.getObjectId());
						buffer.writeInt(count);
						for (int id : _playerList.keySet())
						{
							final StatSet player = _playerList.get(id);
							if (friendList.contains(player.getInt("charId")))
							{
								buffer.writeSizedString(player.getString("name"));
								buffer.writeSizedString(player.getString("clanName"));
								buffer.writeInt(ServerConfig.SERVER_ID);
								buffer.writeInt(player.getInt("soulring"));
								buffer.writeInt(player.getInt("classId"));
								buffer.writeInt(player.getInt("race"));
								buffer.writeInt(id); // friend rank
								final Integer snapshotRank4 = _snapshotCharIdToRank.get(player.getInt("charId"));
								if (snapshotRank4 != null)
								{
									final StatSet snapshot = _snapshotList.get(snapshotRank4);
									buffer.writeInt(snapshotRank4); // server rank snapshot
									buffer.writeInt(snapshot != null ? snapshot.getInt("raceRank", 0) : 0); // race rank snapshot
									buffer.writeInt(snapshot != null ? snapshot.getInt("classRank", 0) : 0); // nClassRank_Snapshot
								}
								else
								{
									buffer.writeInt(id);
									buffer.writeInt(0);
									buffer.writeInt(0);
								}
							}
						}
					}
					else
					{
						buffer.writeInt(1);
						buffer.writeSizedString(_player.getName());
						final Clan clan = _player.getClan();
						if (clan != null)
						{
							buffer.writeSizedString(clan.getName());
						}
						else
						{
							buffer.writeSizedString("");
						}
						
						buffer.writeInt(ServerConfig.SERVER_ID);
						buffer.writeInt(_player.getVariables().getInt("魂環", 0));
						buffer.writeInt(_player.getBaseClass());
						buffer.writeInt(_player.getRace().ordinal());
						buffer.writeInt(1); // clan rank
						final Integer snapshotRank5 = _snapshotCharIdToRank.get(_player.getObjectId());
						if (snapshotRank5 != null)
						{
							final StatSet snapshot = _snapshotList.get(snapshotRank5);
							buffer.writeInt(snapshotRank5); // server rank snapshot
							buffer.writeInt(snapshot != null ? snapshot.getInt("raceRank", 0) : 0); // race rank snapshot
							buffer.writeInt(snapshot != null ? snapshot.getInt("classRank", 0) : 0); // nClassRank_Snapshot
						}
						else
						{
							buffer.writeInt(0);
							buffer.writeInt(0);
							buffer.writeInt(0);
						}
					}
					break;
				}
				case 4: // class
				{
					if (_scope == 0) // all
					{
						int count = 0;
						for (int i = 1; i <= _playerList.size(); i++)
						{
							final StatSet player = _playerList.get(i);
							if (_class == player.getInt("classId"))
							{
								count++;
							}
						}
						
						buffer.writeInt(count > 100 ? 100 : count);
						int i = 1;
						for (Integer id : _playerList.keySet())
						{
							final StatSet player = _playerList.get(id);
							if (_class == player.getInt("classId"))
							{
								buffer.writeSizedString(player.getString("name"));
								buffer.writeSizedString(player.getString("clanName"));
								buffer.writeInt(ServerConfig.SERVER_ID);
								buffer.writeInt(player.getInt("soulring"));
								buffer.writeInt(player.getInt("classId"));
								buffer.writeInt(player.getInt("race"));
								buffer.writeInt(i); // server rank
								final Integer snapshotRank6 = _snapshotCharIdToRank.get(player.getInt("charId"));
								if (snapshotRank6 != null)
								{
									final StatSet snapshot = _snapshotList.get(snapshotRank6);
									buffer.writeInt(snapshotRank6); // server rank snapshot
									buffer.writeInt(snapshot != null ? snapshot.getInt("raceRank", 0) : 0); // race rank snapshot
									buffer.writeInt(snapshot != null ? snapshot.getInt("classRank", 0) : 0); // nClassRank_Snapshot
								}
								else
								{
									buffer.writeInt(i);
									buffer.writeInt(i);
									buffer.writeInt(i); // nClassRank_Snapshot?
								}

								i++;
							}
						}
					}
					else
					{
						boolean found = false;
						
						final Map<Integer, StatSet> classList = new ConcurrentHashMap<>();
						int i = 1;
						for (Integer id : _playerList.keySet())
						{
							final StatSet set = _playerList.get(id);
							if (_player.getBaseClass() == set.getInt("classId"))
							{
								classList.put(i, _playerList.get(id));
								i++;
							}
						}
						
						for (Integer id : classList.keySet())
						{
							final StatSet player = classList.get(id);
							if (player.getInt("charId") == _player.getObjectId())
							{
								found = true;
								final int first = id > 10 ? (id - 9) : 1;
								final int last = classList.size() >= (id + 10) ? id + 10 : id + (classList.size() - id);
								if (first == 1)
								{
									buffer.writeInt(last - (first - 1));
								}
								else
								{
									buffer.writeInt(last - first);
								}
								
								for (int id2 = first; id2 <= last; id2++)
								{
									final StatSet plr = classList.get(id2);
									buffer.writeSizedString(plr.getString("name"));
									buffer.writeSizedString(plr.getString("clanName"));
									buffer.writeInt(ServerConfig.SERVER_ID);
									buffer.writeInt(plr.getInt("soulring"));
									buffer.writeInt(plr.getInt("classId"));
									buffer.writeInt(plr.getInt("race"));
									buffer.writeInt(id2); // server rank
									buffer.writeInt(id2);
									buffer.writeInt(id2);
									buffer.writeInt(id2); // nClassRank_Snapshot?
								}
							}
						}
						
						if (!found)
						{
							buffer.writeInt(0);
						}
					}
					break;
				}
			}
		}
		else
		{
			buffer.writeInt(0);
		}
	}
}
