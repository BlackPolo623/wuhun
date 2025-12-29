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
package org.l2jmobius.gameserver.network;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

import org.l2jmobius.gameserver.config.DevelopmentConfig;
import org.l2jmobius.gameserver.network.clientpackets.*;
import org.l2jmobius.gameserver.network.clientpackets.enchant.RequestEnchantItem;
import org.l2jmobius.gameserver.network.clientpackets.friend.RequestAnswerFriendInvite;
import org.l2jmobius.gameserver.network.clientpackets.friend.RequestFriendDel;
import org.l2jmobius.gameserver.network.clientpackets.friend.RequestFriendInvite;
import org.l2jmobius.gameserver.network.clientpackets.friend.RequestFriendList;
import org.l2jmobius.gameserver.network.clientpackets.friend.RequestSendFriendMsg;
import org.l2jmobius.gameserver.network.clientpackets.pet.RequestChangePetName;
import org.l2jmobius.gameserver.network.clientpackets.pet.RequestGetItemFromPet;
import org.l2jmobius.gameserver.network.clientpackets.pet.RequestGiveItemToPet;
import org.l2jmobius.gameserver.network.clientpackets.pet.RequestPetGetItem;
import org.l2jmobius.gameserver.network.clientpackets.pet.RequestPetUseItem;

/**
 * @author Mobius
 */
public enum ClientPackets
{
	LOGOUT(0x00, Logout::new, ConnectionState.AUTHENTICATED, ConnectionState.IN_GAME),
	ATTACK(0x01, AttackRequest::new, ConnectionState.IN_GAME),
	MOVE_BACKWARD_TO_LOCATION(0x02, null, ConnectionState.IN_GAME),
	START_PLEDGE_WAR(0x03, RequestStartPledgeWar::new, ConnectionState.IN_GAME),
	REPLY_START_PLEDGE(0x04, RequestReplyStartPledgeWar::new, ConnectionState.IN_GAME),
	STOP_PLEDGE_WAR(0x05, RequestStopPledgeWar::new, ConnectionState.IN_GAME),
	REPLY_STOP_PLEDGE_WAR(0x06, RequestReplyStopPledgeWar::new, ConnectionState.IN_GAME),
	SURRENDER_PLEDGE_WAR(0x07, RequestSurrenderPledgeWar::new, ConnectionState.IN_GAME),
	REPLY_SURRENDER_PLEDGE_WAR(0x08, RequestReplySurrenderPledgeWar::new, ConnectionState.IN_GAME),
	SET_PLEDGE_CREST(0x09, RequestSetPledgeCrest::new, ConnectionState.IN_GAME),
	NOT_USE_14(0x0A, null, ConnectionState.IN_GAME),
	GIVE_NICKNAME(0x0B, RequestGiveNickName::new, ConnectionState.IN_GAME),
	CHARACTER_CREATE(0x0C, CharacterCreate::new, ConnectionState.AUTHENTICATED),
	CHARACTER_DELETE(0x0D, CharacterDelete::new, ConnectionState.AUTHENTICATED),
	VERSION(0x0E, ProtocolVersion::new, ConnectionState.CONNECTED),
	MOVE_TO_LOCATION(0x0F, MoveToLocation::new, ConnectionState.IN_GAME),
	NOT_USE_34(0x10, null, ConnectionState.IN_GAME),
	ENTER_WORLD(0x11, EnterWorld::new, ConnectionState.ENTERING),
	CHARACTER_SELECT(0x12, CharacterSelect::new, ConnectionState.AUTHENTICATED),
	NEW_CHARACTER(0x13, NewCharacter::new, ConnectionState.AUTHENTICATED),
	ITEMLIST(0x14, RequestItemList::new, ConnectionState.IN_GAME),
	NOT_USE_1(0x15, null, ConnectionState.IN_GAME),
	UNEQUIP_ITEM(0x16, RequestUnEquipItem::new, ConnectionState.IN_GAME),
	DROP_ITEM(0x17, RequestDropItem::new, ConnectionState.IN_GAME),
	GET_ITEM(0x18, null, ConnectionState.IN_GAME),
	USE_ITEM(0x19, UseItem::new, ConnectionState.IN_GAME),
	TRADE_REQUEST(0x1A, TradeRequest::new, ConnectionState.IN_GAME),
	TRADE_ADD(0x1B, AddTradeItem::new, ConnectionState.IN_GAME),
	TRADE_DONE(0x1C, TradeDone::new, ConnectionState.IN_GAME),
	NOT_USE_35(0x1D, null, ConnectionState.IN_GAME),
	NOT_USE_36(0x1E, null, ConnectionState.IN_GAME),
	ACTION(0x1F, Action::new, ConnectionState.IN_GAME),
	NOT_USE_37(0x20, null, ConnectionState.IN_GAME),
	NOT_USE_38(0x21, null, ConnectionState.IN_GAME),
	LINK_HTML(0x22, RequestLinkHtml::new, ConnectionState.IN_GAME),
	PASS_CMD_TO_SERVER(0x23, RequestBypassToServer::new, ConnectionState.IN_GAME),
	WRITE_BBS(0x24, RequestBBSwrite::new, ConnectionState.IN_GAME),
	JOIN_PLEDGE(0x26, RequestJoinPledge::new, ConnectionState.IN_GAME),
	ANSWER_JOIN_PLEDGE(0x27, RequestAnswerJoinPledge::new, ConnectionState.IN_GAME),
	WITHDRAWAL_PLEDGE(0x28, RequestWithdrawalPledge::new, ConnectionState.IN_GAME),
	OUST_PLEDGE_MEMBER(0x29, RequestOustPledgeMember::new, ConnectionState.IN_GAME),
	NOT_USE_40(0x2A, null, ConnectionState.IN_GAME),
	LOGIN(0x2B, AuthLogin::new, ConnectionState.CONNECTED),
	GET_ITEM_FROM_PET(0x2C, RequestGetItemFromPet::new, ConnectionState.IN_GAME),
	NOT_USE_22(0x2D, null, ConnectionState.IN_GAME),
	ALLIANCE_INFO(0x2E, RequestAllyInfo::new, ConnectionState.IN_GAME),
	CRYSTALLIZE_ITEM(0x2F, RequestCrystallizeItem::new, ConnectionState.IN_GAME),
	PRIVATE_STORE_MANAGE_SELL(0x30, RequestPrivateStoreManageSell::new, ConnectionState.IN_GAME),
	PRIVATE_STORE_LIST_SET(0x31, SetPrivateStoreListSell::new, ConnectionState.IN_GAME),
	PRIVATE_STORE_MANAGE_CANCEL(0x32, null, ConnectionState.IN_GAME),
	STOP_MOVE_TOWARD(0x33, StopMoveToward::new, ConnectionState.IN_GAME),
	SOCIAL_ACTION(0x34, null, ConnectionState.IN_GAME),
	CHANGE_MOVE_TYPE(0x35, null, ConnectionState.IN_GAME),
	CHANGE_WAIT_TYPE(0x36, null, ConnectionState.IN_GAME),
	SELL_LIST(0x37, RequestSellItem::new, ConnectionState.IN_GAME),
	MAGIC_SKILL_LIST(0x38, RequestMagicSkillList::new, ConnectionState.IN_GAME),
	MAGIC_SKILL_USE(0x39, RequestMagicSkillUse::new, ConnectionState.IN_GAME),
	APPEARING(0x3A, Appearing::new, ConnectionState.IN_GAME),
	WAREHOUSE_DEPOSIT_LIST(0x3B, SendWareHouseDepositList::new, ConnectionState.IN_GAME),
	WAREHOUSE_WITHDRAW_LIST(0x3C, SendWareHouseWithDrawList::new, ConnectionState.IN_GAME),
	SHORTCUT_REG(0x3D, RequestShortcutReg::new, ConnectionState.IN_GAME),
	NOT_USE_3(0x3E, null, ConnectionState.IN_GAME),
	DEL_SHORTCUT(0x3F, RequestShortcutDel::new, ConnectionState.IN_GAME),
	BUY_LIST(0x40, RequestBuyItem::new, ConnectionState.IN_GAME),
	NOT_USE_2(0x41, null, ConnectionState.IN_GAME),
	JOIN_PARTY(0x42, RequestJoinParty::new, ConnectionState.IN_GAME),
	ANSWER_JOIN_PARTY(0x43, RequestAnswerJoinParty::new, ConnectionState.IN_GAME),
	WITHDRAWAL_PARTY(0x44, RequestWithDrawalParty::new, ConnectionState.IN_GAME),
	OUST_PARTY_MEMBER(0x45, RequestOustPartyMember::new, ConnectionState.IN_GAME),
	DISMISS_PARTY(0x46, null, ConnectionState.IN_GAME),
	CAN_NOT_MOVE_ANYMORE(0x47, CannotMoveAnymore::new, ConnectionState.IN_GAME),
	TARGET_UNSELECTED(0x48, RequestTargetCanceld::new, ConnectionState.IN_GAME),
	SAY2(0x49, Say2::new, ConnectionState.IN_GAME),
	MOVE_TOWARD(0x4A, MoveToward::new, ConnectionState.IN_GAME),
	NOT_USE_4(0x4B, null, ConnectionState.IN_GAME),
	NOT_USE_5(0x4C, null, ConnectionState.IN_GAME),
	PLEDGE_REQ_SHOW_MEMBER_LIST_OPEN(0x4D, RequestPledgeMemberList::new, ConnectionState.IN_GAME),
	NOT_USE_6(0x4E, null, ConnectionState.IN_GAME),
	MAGIC_LIST(0x4F, null, ConnectionState.IN_GAME),
	SKILL_LIST(0x50, RequestSkillList::new, ConnectionState.IN_GAME),
	MOVE_WITH_DELTA(0x52, MoveWithDelta::new, ConnectionState.IN_GAME),
	GETON_VEHICLE(0x53, RequestGetOnVehicle::new, ConnectionState.IN_GAME),
	GETOFF_VEHICLE(0x54, RequestGetOffVehicle::new, ConnectionState.IN_GAME),
	TRADE_START(0x55, AnswerTradeRequest::new, ConnectionState.IN_GAME),
	ICON_ACTION(0x56, RequestActionUse::new, ConnectionState.IN_GAME),
	RESTART(0x57, RequestRestart::new, ConnectionState.IN_GAME),
	NOT_USE_9(0x58, null, ConnectionState.IN_GAME),
	VALIDATE_POSITION(0x59, ValidatePosition::new, ConnectionState.IN_GAME),
	START_ROTATING(0x5B, StartRotating::new, ConnectionState.IN_GAME),
	FINISH_ROTATING(0x5C, FinishRotating::new, ConnectionState.IN_GAME),
	NOT_USE_15(0x5D, null, ConnectionState.IN_GAME),
	SHOW_BOARD(0x5E, RequestShowBoard::new, ConnectionState.IN_GAME),
	REQUEST_ENCHANT_ITEM(0x5F, RequestEnchantItem::new, ConnectionState.IN_GAME),
	DESTROY_ITEM(0x60, RequestDestroyItem::new, ConnectionState.IN_GAME),
	TARGET_USER_FROM_MENU(0x61, null, ConnectionState.IN_GAME),
	QUESTLIST(0x62, RequestQuestList::new, ConnectionState.IN_GAME),
	DESTROY_QUEST(0x63, RequestQuestAbort::new, ConnectionState.IN_GAME),
	NOT_USE_16(0x64, null, ConnectionState.IN_GAME),
	PLEDGE_INFO(0x65, RequestPledgeInfo::new, ConnectionState.IN_GAME),
	PLEDGE_EXTENDED_INFO(0x66, RequestPledgeExtendedInfo::new, ConnectionState.IN_GAME),
	PLEDGE_CREST(0x67, RequestPledgeCrest::new, ConnectionState.IN_GAME),
	NOT_USE_17(0x68, null, ConnectionState.IN_GAME),
	NOT_USE_18(0x69, null, ConnectionState.IN_GAME),
	L2_FRIEND_LIST(0x6A, null, ConnectionState.IN_GAME),
	L2_FRIEND_SAY(0x6B, RequestSendFriendMsg::new, ConnectionState.IN_GAME),
	OPEN_MINIMAP(0x6C, RequestShowMiniMap::new, ConnectionState.IN_GAME),
	MSN_CHAT_LOG(0x6D, null, ConnectionState.IN_GAME),
	RELOAD(0x6E, null, ConnectionState.IN_GAME),
	HENNA_EQUIP(0x6F, null, ConnectionState.IN_GAME),
	HENNA_UNEQUIP_LIST(0x70, RequestHennaRemoveList::new, ConnectionState.IN_GAME),
	HENNA_UNEQUIP_INFO(0x71, RequestHennaItemRemoveInfo::new, ConnectionState.IN_GAME),
	HENNA_UNEQUIP(0x72, null, ConnectionState.IN_GAME),
	ACQUIRE_SKILL_INFO(0x73, RequestAcquireSkillInfo::new, ConnectionState.IN_GAME),
	SYS_CMD_2(0x74, SendBypassBuildCmd::new, ConnectionState.IN_GAME),
	MOVE_TO_LOCATION_IN_VEHICLE(0x75, RequestMoveToLocationInVehicle::new, ConnectionState.IN_GAME),
	CAN_NOT_MOVE_ANYMORE_IN_VEHICLE(0x76, CannotMoveAnymoreInVehicle::new, ConnectionState.IN_GAME),
	FRIEND_ADD_REQUEST(0x77, RequestFriendInvite::new, ConnectionState.IN_GAME),
	FRIEND_ADD_REPLY(0x78, RequestAnswerFriendInvite::new, ConnectionState.IN_GAME),
	FRIEND_LIST(0x79, RequestFriendList::new, ConnectionState.IN_GAME),
	FRIEND_REMOVE(0x7A, RequestFriendDel::new, ConnectionState.IN_GAME),
	RESTORE_CHARACTER(0x7B, CharacterRestore::new, ConnectionState.AUTHENTICATED),
	REQ_ACQUIRE_SKILL(0x7C, RequestAcquireSkill::new, ConnectionState.IN_GAME),
	RESTART_POINT(0x7D, RequestRestartPoint::new, ConnectionState.IN_GAME),
	GM_COMMAND_TYPE(0x7E, RequestGMCommand::new, ConnectionState.IN_GAME),
	LIST_PARTY_WAITING(0x7F, RequestPartyMatchConfig::new, ConnectionState.IN_GAME),
	MANAGE_PARTY_ROOM(0x80, RequestPartyMatchList::new, ConnectionState.IN_GAME),
	JOIN_PARTY_ROOM(0x81, RequestPartyMatchDetail::new, ConnectionState.IN_GAME),
	NOT_USE_20(0x82, null, ConnectionState.IN_GAME),
	PRIVATE_STORE_BUY_LIST_SEND(0x83, RequestPrivateStoreBuy::new, ConnectionState.IN_GAME),
	NOT_USE_21(0x84, null, ConnectionState.IN_GAME),
	TUTORIAL_LINK_HTML(0x85, RequestTutorialLinkHtml::new, ConnectionState.IN_GAME),
	TUTORIAL_PASS_CMD_TO_SERVER(0x86, RequestTutorialPassCmdToServer::new, ConnectionState.IN_GAME),
	TUTORIAL_MARK_PRESSED(0x87, RequestTutorialQuestionMark::new, ConnectionState.IN_GAME),
	TUTORIAL_CLIENT_EVENT(0x88, RequestTutorialClientEvent::new, ConnectionState.IN_GAME),
	PETITION(0x89, RequestPetition::new, ConnectionState.IN_GAME),
	PETITION_CANCEL(0x8A, RequestPetitionCancel::new, ConnectionState.IN_GAME),
	GMLIST(0x8B, RequestGmList::new, ConnectionState.IN_GAME),
	JOIN_ALLIANCE(0x8C, RequestJoinAlly::new, ConnectionState.IN_GAME),
	ANSWER_JOIN_ALLIANCE(0x8D, RequestAnswerJoinAlly::new, ConnectionState.IN_GAME),
	WITHDRAW_ALLIANCE(0x8E, AllyLeave::new, ConnectionState.IN_GAME),
	OUST_ALLIANCE_MEMBER_PLEDGE(0x8F, AllyDismiss::new, ConnectionState.IN_GAME),
	DISMISS_ALLIANCE(0x90, RequestDismissAlly::new, ConnectionState.IN_GAME),
	SET_ALLIANCE_CREST(0x91, RequestSetAllyCrest::new, ConnectionState.IN_GAME),
	ALLIANCE_CREST(0x92, RequestAllyCrest::new, ConnectionState.IN_GAME),
	CHANGE_PET_NAME(0x93, RequestChangePetName::new, ConnectionState.IN_GAME),
	PET_USE_ITEM(0x94, RequestPetUseItem::new, ConnectionState.IN_GAME),
	GIVE_ITEM_TO_PET(0x95, RequestGiveItemToPet::new, ConnectionState.IN_GAME),
	PRIVATE_STORE_QUIT(0x96, RequestPrivateStoreQuitSell::new, ConnectionState.IN_GAME),
	PRIVATE_STORE_SET_MSG(0x97, SetPrivateStoreMsgSell::new, ConnectionState.IN_GAME),
	PET_GET_ITEM(0x98, RequestPetGetItem::new, ConnectionState.IN_GAME),
	PRIVATE_STORE_MANAGE_BUY(0x99, RequestPrivateStoreManageBuy::new, ConnectionState.IN_GAME),
	PRIVATE_STORE_BUY_LIST_SET(0x9A, SetPrivateStoreListBuy::new, ConnectionState.IN_GAME),
	PRIVATE_STORE_BUY_MANAGE_CANCEL(0x9B, null, ConnectionState.IN_GAME),
	PRIVATE_STORE_BUY_QUIT(0x9C, RequestPrivateStoreQuitBuy::new, ConnectionState.IN_GAME),
	PRIVATE_STORE_BUY_SET_MSG(0x9D, SetPrivateStoreMsgBuy::new, ConnectionState.IN_GAME),
	NOT_USE_24(0xAE, null, ConnectionState.IN_GAME),
	PRIVATE_STORE_BUY_BUY_LIST_SEND(0x9F, RequestPrivateStoreSell::new, ConnectionState.IN_GAME),
	SEND_TIME_CHECK_PACKET(0xA0, null, ConnectionState.IN_GAME),
	NOT_USE_26(0xA1, null, ConnectionState.IN_GAME),
	NOT_USE_27(0xA2, null, ConnectionState.IN_GAME),
	NOT_USE_28(0xA3, null, ConnectionState.IN_GAME),
	NOT_USE_29(0xA4, null, ConnectionState.IN_GAME),
	NOT_USE_30(0xA5, null, ConnectionState.IN_GAME),
	REQUEST_SKILL_COOL_TIME(0xA6, RequestSkillCoolTime::new, ConnectionState.IN_GAME),
	REQUEST_PACKAGE_SENDABLE_ITEM_LIST(0xA7, RequestPackageSendableItemList::new, ConnectionState.IN_GAME),
	REQUEST_PACKAGE_SEND(0xA8, RequestPackageSend::new, ConnectionState.IN_GAME),
	BLOCK_PACKET(0xA9, RequestBlock::new, ConnectionState.IN_GAME),
	CASTLE_SIEGE_INFO(0xAA, RequestSiegeInfo::new, ConnectionState.IN_GAME),
	CASTLE_SIEGE_ATTACKER_LIST(0xAB, RequestSiegeAttackerList::new, ConnectionState.IN_GAME),
	CASTLE_SIEGE_DEFENDER_LIST(0xAC, RequestSiegeDefenderList::new, ConnectionState.IN_GAME),
	JOIN_CASTLE_SIEGE(0xAD, RequestJoinSiege::new, ConnectionState.IN_GAME),
	CONFIRM_CASTLE_SIEGE_WAITING_LIST(0xAE, RequestConfirmSiegeWaitingList::new, ConnectionState.IN_GAME),
	SET_CASTLE_SIEGE_TIME(0xAF, RequestSetCastleSiegeTime::new, ConnectionState.IN_GAME),
	MULTI_SELL_CHOOSE(0xB0, MultiSellChoose::new, ConnectionState.IN_GAME),
	NET_PING(0xB1, RequestNetPing::new, ConnectionState.IN_GAME),
	REMAIN_TIME(0xB2, null, ConnectionState.IN_GAME),
	USER_CMD_BYPASS(0xB3, BypassUserCmd::new, ConnectionState.IN_GAME),
	SNOOP_QUIT(0xB4, SnoopQuit::new, ConnectionState.IN_GAME),
	RECIPE_BOOK_OPEN(0xB5, RequestRecipeBookOpen::new, ConnectionState.IN_GAME),
	RECIPE_ITEM_DELETE(0xB6, RequestRecipeBookDestroy::new, ConnectionState.IN_GAME),
	RECIPE_ITEM_MAKE_INFO(0xB7, RequestRecipeItemMakeInfo::new, ConnectionState.IN_GAME),
	RECIPE_ITEM_MAKE_SELF(0xB8, RequestRecipeItemMakeSelf::new, ConnectionState.IN_GAME),
	RECIPE_SHOP_MANAGE_LIST(0xB9, null, ConnectionState.IN_GAME),
	RECIPE_SHOP_MESSAGE_SET(0xBA, RequestRecipeShopMessageSet::new, ConnectionState.IN_GAME),
	RECIPE_SHOP_LIST_SET(0xBB, RequestRecipeShopListSet::new, ConnectionState.IN_GAME),
	RECIPE_SHOP_MANAGE_QUIT(0xBC, RequestRecipeShopManageQuit::new, ConnectionState.IN_GAME),
	RECIPE_SHOP_MANAGE_CANCEL(0xBD, null, ConnectionState.IN_GAME),
	RECIPE_SHOP_MAKE_INFO(0xBE, RequestRecipeShopMakeInfo::new, ConnectionState.IN_GAME),
	RECIPE_SHOP_MAKE_DO(0xBF, RequestRecipeShopMakeItem::new, ConnectionState.IN_GAME),
	RECIPE_SHOP_SELL_LIST(0xC0, RequestRecipeShopManagePrev::new, ConnectionState.IN_GAME),
	OBSERVER_END(0xC1, ObserverReturn::new, ConnectionState.IN_GAME),
	VOTE_SOCIALITY(0xC2, null, ConnectionState.IN_GAME),
	HENNA_ITEM_LIST(0xC3, RequestHennaItemList::new, ConnectionState.IN_GAME),
	HENNA_ITEM_INFO(0xC4, RequestHennaItemInfo::new, ConnectionState.IN_GAME),
	BUY_SEED(0xC5, RequestBuySeed::new, ConnectionState.IN_GAME),
	CONFIRM_DLG(0xC6, DlgAnswer::new, ConnectionState.IN_GAME),
	BUY_PREVIEW_LIST(0xC7, RequestPreviewItem::new, ConnectionState.IN_GAME),
	SSQ_STATUS(0xC8, null, ConnectionState.IN_GAME),
	PETITION_VOTE(0xC9, RequestPetitionFeedback::new, ConnectionState.IN_GAME),
	NOT_USE_33(0xCA, null, ConnectionState.IN_GAME),
	GAMEGUARD_REPLY(0xCB, GameGuardReply::new, ConnectionState.IN_GAME),
	MANAGE_PLEDGE_POWER(0xCC, RequestPledgePower::new, ConnectionState.IN_GAME),
	MAKE_MACRO(0xCD, RequestMakeMacro::new, ConnectionState.IN_GAME),
	DELETE_MACRO(0xCE, RequestDeleteMacro::new, ConnectionState.IN_GAME),
	BUY_PROCURE(0xCF, null, ConnectionState.IN_GAME),
	EX_PACKET(0xD0, null, ConnectionState.values()); // This packet has its own connection state checking so we allow all of them.
	
	public static final ClientPackets[] PACKET_ARRAY;
	static
	{
		final int maxPacketId = Arrays.stream(values()).mapToInt(ClientPackets::getPacketId).max().orElse(0);
		PACKET_ARRAY = new ClientPackets[maxPacketId + 1];
		for (ClientPackets packet : values())
		{
			PACKET_ARRAY[packet.getPacketId()] = packet;
		}
	}
	
	private final int _packetId;
	private final Supplier<ClientPacket> _packetSupplier;
	private final Set<ConnectionState> _connectionStates;
	
	ClientPackets(int packetId, Supplier<ClientPacket> packetSupplier, ConnectionState... connectionStates)
	{
		// Packet id is an unsigned byte.
		if (packetId > 0xFF)
		{
			throw new IllegalArgumentException("Packet id must not be bigger than 0xFF");
		}
		
		_packetId = packetId;
		_packetSupplier = packetSupplier != null ? packetSupplier : () -> null;
		_connectionStates = new HashSet<>(Arrays.asList(connectionStates));
	}
	
	public int getPacketId()
	{
		return _packetId;
	}
	
	public ClientPacket newPacket()
	{
		final ClientPacket packet = _packetSupplier.get();
		if (DevelopmentConfig.DEBUG_CLIENT_PACKETS)
		{
			if (packet != null)
			{
				final String name = packet.getClass().getSimpleName();
				if (!DevelopmentConfig.EXCLUDED_DEBUG_PACKETS.contains(name))
				{
					PacketLogger.info("[C] " + name);
				}
			}
			else if (DevelopmentConfig.DEBUG_UNKNOWN_PACKETS)
			{
				PacketLogger.info("[C] 0x" + Integer.toHexString(_packetId).toUpperCase());
			}
		}
		
		return packet;
	}
	
	public Set<ConnectionState> getConnectionStates()
	{
		return _connectionStates;
	}
}
