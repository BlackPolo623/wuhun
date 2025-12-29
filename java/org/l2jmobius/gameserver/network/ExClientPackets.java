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
import org.l2jmobius.gameserver.network.clientpackets.adenadistribution.RequestDivideAdena;
import org.l2jmobius.gameserver.network.clientpackets.adenadistribution.RequestDivideAdenaCancel;
import org.l2jmobius.gameserver.network.clientpackets.adenadistribution.RequestDivideAdenaStart;
import org.l2jmobius.gameserver.network.clientpackets.adenlab.RequestAdenLabBossInfo;
import org.l2jmobius.gameserver.network.clientpackets.adenlab.RequestAdenLabBossList;
import org.l2jmobius.gameserver.network.clientpackets.adenlab.RequestAdenLabBossUnlock;
import org.l2jmobius.gameserver.network.clientpackets.adenlab.RequestAdenLabNormalPlay;
import org.l2jmobius.gameserver.network.clientpackets.adenlab.RequestAdenLabNormalSlot;
import org.l2jmobius.gameserver.network.clientpackets.adenlab.RequestAdenLabSpecialFix;
import org.l2jmobius.gameserver.network.clientpackets.adenlab.RequestAdenLabSpecialPlay;
import org.l2jmobius.gameserver.network.clientpackets.adenlab.RequestAdenLabSpecialProbability;
import org.l2jmobius.gameserver.network.clientpackets.adenlab.RequestAdenLabSpecialSlot;
import org.l2jmobius.gameserver.network.clientpackets.adenlab.RequestAdenLabTranscendentEnchant;
import org.l2jmobius.gameserver.network.clientpackets.adenlab.RequestAdenLabTranscendentProbability;
import org.l2jmobius.gameserver.network.clientpackets.appearance.RequestExCancelShape_Shifting_Item;
import org.l2jmobius.gameserver.network.clientpackets.appearance.RequestExTryToPutShapeShiftingEnchantSupportItem;
import org.l2jmobius.gameserver.network.clientpackets.appearance.RequestExTryToPutShapeShiftingTargetItem;
import org.l2jmobius.gameserver.network.clientpackets.appearance.RequestShapeShiftingItem;
import org.l2jmobius.gameserver.network.clientpackets.attendance.RequestVipAttendanceCheck;
import org.l2jmobius.gameserver.network.clientpackets.attendance.RequestVipAttendanceItemList;
import org.l2jmobius.gameserver.network.clientpackets.attendance.RequestVipAttendanceItemReward;
import org.l2jmobius.gameserver.network.clientpackets.attributechange.RequestChangeAttributeCancel;
import org.l2jmobius.gameserver.network.clientpackets.attributechange.RequestChangeAttributeItem;
import org.l2jmobius.gameserver.network.clientpackets.attributechange.SendChangeAttributeTargetItem;
import org.l2jmobius.gameserver.network.clientpackets.autopeel.ExRequestItemAutoPeel;
import org.l2jmobius.gameserver.network.clientpackets.autopeel.ExRequestReadyItemAutoPeel;
import org.l2jmobius.gameserver.network.clientpackets.autopeel.ExRequestStopItemAutoPeel;
import org.l2jmobius.gameserver.network.clientpackets.autoplay.ExAutoPlaySetting;
import org.l2jmobius.gameserver.network.clientpackets.autoplay.ExRequestActivateAutoShortcut;
import org.l2jmobius.gameserver.network.clientpackets.balok.ExBalrogWarGetReward;
import org.l2jmobius.gameserver.network.clientpackets.balok.ExBalrogWarShowRanking;
import org.l2jmobius.gameserver.network.clientpackets.balok.ExBalrogWarShowUI;
import org.l2jmobius.gameserver.network.clientpackets.balok.ExBalrogWarTeleport;
import org.l2jmobius.gameserver.network.clientpackets.balthusevent.RequestEventBalthusToken;
import org.l2jmobius.gameserver.network.clientpackets.blackcoupon.RequestItemRestore;
import org.l2jmobius.gameserver.network.clientpackets.blackcoupon.RequestItemRestoreList;
import org.l2jmobius.gameserver.network.clientpackets.blessing.RequestBlessOptionCancel;
import org.l2jmobius.gameserver.network.clientpackets.blessing.RequestBlessOptionEnchant;
import org.l2jmobius.gameserver.network.clientpackets.blessing.RequestBlessOptionProbList;
import org.l2jmobius.gameserver.network.clientpackets.blessing.RequestBlessOptionPutItem;
import org.l2jmobius.gameserver.network.clientpackets.captcha.RequestCaptchaAnswer;
import org.l2jmobius.gameserver.network.clientpackets.captcha.RequestRefreshCaptcha;
import org.l2jmobius.gameserver.network.clientpackets.castlewar.ExCastleWarObserverStart;
import org.l2jmobius.gameserver.network.clientpackets.castlewar.ExMercenaryCastleWarCastleSiegeAttackerList;
import org.l2jmobius.gameserver.network.clientpackets.castlewar.ExMercenaryCastleWarCastleSiegeDefenderList;
import org.l2jmobius.gameserver.network.clientpackets.castlewar.ExMercenaryCastleWarCastleSiegeHudInfo;
import org.l2jmobius.gameserver.network.clientpackets.castlewar.ExMercenaryCastleWarCastleSiegeInfo;
import org.l2jmobius.gameserver.network.clientpackets.castlewar.ExPledgeMercenaryMemberJoin;
import org.l2jmobius.gameserver.network.clientpackets.castlewar.ExPledgeMercenaryMemberList;
import org.l2jmobius.gameserver.network.clientpackets.castlewar.ExPledgeMercenaryRecruitInfoSet;
import org.l2jmobius.gameserver.network.clientpackets.characterstyle.ExRequestCharacterStyleList;
import org.l2jmobius.gameserver.network.clientpackets.characterstyle.ExRequestCharacterStyleRegister;
import org.l2jmobius.gameserver.network.clientpackets.characterstyle.ExRequestCharacterStyleSelect;
import org.l2jmobius.gameserver.network.clientpackets.characterstyle.ExRequestCharacterStyleUpdateFavorite;
import org.l2jmobius.gameserver.network.clientpackets.classchange.ExRequestClassChange;
import org.l2jmobius.gameserver.network.clientpackets.classchange.ExRequestClassChangeVerifying;
import org.l2jmobius.gameserver.network.clientpackets.collection.RequestCollectionCloseUI;
import org.l2jmobius.gameserver.network.clientpackets.collection.RequestCollectionFavoriteList;
import org.l2jmobius.gameserver.network.clientpackets.collection.RequestCollectionReceiveReward;
import org.l2jmobius.gameserver.network.clientpackets.collection.RequestCollectionRegister;
import org.l2jmobius.gameserver.network.clientpackets.collection.RequestCollectionUpdateFavorite;
import org.l2jmobius.gameserver.network.clientpackets.collection.RequestExCollectionList;
import org.l2jmobius.gameserver.network.clientpackets.collection.RequestExCollectionOpenUI;
import org.l2jmobius.gameserver.network.clientpackets.collection.RequestExCollectionSummary;
import org.l2jmobius.gameserver.network.clientpackets.commission.RequestCommissionBuyInfo;
import org.l2jmobius.gameserver.network.clientpackets.commission.RequestCommissionBuyItem;
import org.l2jmobius.gameserver.network.clientpackets.commission.RequestCommissionCancel;
import org.l2jmobius.gameserver.network.clientpackets.commission.RequestCommissionDelete;
import org.l2jmobius.gameserver.network.clientpackets.commission.RequestCommissionInfo;
import org.l2jmobius.gameserver.network.clientpackets.commission.RequestCommissionList;
import org.l2jmobius.gameserver.network.clientpackets.commission.RequestCommissionRegister;
import org.l2jmobius.gameserver.network.clientpackets.commission.RequestCommissionRegisteredItem;
import org.l2jmobius.gameserver.network.clientpackets.commission.RequestCommissionRegistrableItemList;
import org.l2jmobius.gameserver.network.clientpackets.compound.RequestNewEnchantClose;
import org.l2jmobius.gameserver.network.clientpackets.compound.RequestNewEnchantPushOne;
import org.l2jmobius.gameserver.network.clientpackets.compound.RequestNewEnchantPushTwo;
import org.l2jmobius.gameserver.network.clientpackets.compound.RequestNewEnchantRemoveOne;
import org.l2jmobius.gameserver.network.clientpackets.compound.RequestNewEnchantRemoveTwo;
import org.l2jmobius.gameserver.network.clientpackets.compound.RequestNewEnchantRetryToPutItems;
import org.l2jmobius.gameserver.network.clientpackets.compound.RequestNewEnchantTry;
import org.l2jmobius.gameserver.network.clientpackets.crossevent.RequestCrossEventData;
import org.l2jmobius.gameserver.network.clientpackets.crossevent.RequestCrossEventInfo;
import org.l2jmobius.gameserver.network.clientpackets.crossevent.RequestCrossEventNormalReward;
import org.l2jmobius.gameserver.network.clientpackets.crossevent.RequestCrossEventRareReward;
import org.l2jmobius.gameserver.network.clientpackets.crossevent.RequestCrossEventReset;
import org.l2jmobius.gameserver.network.clientpackets.crystalization.RequestCrystallizeEstimate;
import org.l2jmobius.gameserver.network.clientpackets.crystalization.RequestCrystallizeItemCancel;
import org.l2jmobius.gameserver.network.clientpackets.dailymission.RequestMissionLevelJumpLevel;
import org.l2jmobius.gameserver.network.clientpackets.dailymission.RequestMissionLevelReceiveReward;
import org.l2jmobius.gameserver.network.clientpackets.dailymission.RequestMissionRewardList;
import org.l2jmobius.gameserver.network.clientpackets.dailymission.RequestOneDayRewardReceive;
import org.l2jmobius.gameserver.network.clientpackets.dailymission.RequestTodoList;
import org.l2jmobius.gameserver.network.clientpackets.dualinventory.RequestExDualInventorySwap;
import org.l2jmobius.gameserver.network.clientpackets.elementalspirits.ExElementalInitTalent;
import org.l2jmobius.gameserver.network.clientpackets.elementalspirits.ExElementalSpiritAbsorb;
import org.l2jmobius.gameserver.network.clientpackets.elementalspirits.ExElementalSpiritAbsorbInfo;
import org.l2jmobius.gameserver.network.clientpackets.elementalspirits.ExElementalSpiritEvolution;
import org.l2jmobius.gameserver.network.clientpackets.elementalspirits.ExElementalSpiritEvolutionInfo;
import org.l2jmobius.gameserver.network.clientpackets.elementalspirits.ExElementalSpiritExtract;
import org.l2jmobius.gameserver.network.clientpackets.elementalspirits.ExElementalSpiritExtractInfo;
import org.l2jmobius.gameserver.network.clientpackets.elementalspirits.ExElementalSpiritInfo;
import org.l2jmobius.gameserver.network.clientpackets.elementalspirits.ExElementalSpiritSetTalent;
import org.l2jmobius.gameserver.network.clientpackets.enchant.RequestExAddEnchantScrollItem;
import org.l2jmobius.gameserver.network.clientpackets.enchant.RequestExCancelEnchantItem;
import org.l2jmobius.gameserver.network.clientpackets.enchant.RequestExRemoveEnchantSupportItem;
import org.l2jmobius.gameserver.network.clientpackets.enchant.RequestExTryToPutEnchantSupportItem;
import org.l2jmobius.gameserver.network.clientpackets.enchant.RequestExTryToPutEnchantTargetItem;
import org.l2jmobius.gameserver.network.clientpackets.enchant.challengepoint.ExRequestResetEnchantChallengePoint;
import org.l2jmobius.gameserver.network.clientpackets.enchant.challengepoint.ExRequestSetEnchantChallengePoint;
import org.l2jmobius.gameserver.network.clientpackets.enchant.multi.ExRequestFinishMultiEnchantScroll;
import org.l2jmobius.gameserver.network.clientpackets.enchant.multi.ExRequestMultiEnchantItemList;
import org.l2jmobius.gameserver.network.clientpackets.enchant.multi.ExRequestSetMultiEnchantItemList;
import org.l2jmobius.gameserver.network.clientpackets.enchant.multi.ExRequestStartMultiEnchantScroll;
import org.l2jmobius.gameserver.network.clientpackets.enchant.multi.ExRequestViewMultiEnchantResult;
import org.l2jmobius.gameserver.network.clientpackets.enchant.single.ExRequestEnchantFailRewardInfo;
import org.l2jmobius.gameserver.network.clientpackets.enchant.single.ExRequestViewEnchantResult;
import org.l2jmobius.gameserver.network.clientpackets.ensoul.RequestItemEnsoul;
import org.l2jmobius.gameserver.network.clientpackets.ensoul.RequestTryEnSoulExtraction;
import org.l2jmobius.gameserver.network.clientpackets.equipmentupgrade.RequestUpgradeProb;
import org.l2jmobius.gameserver.network.clientpackets.equipmentupgrade.RequestUpgradeSystemProbList;
import org.l2jmobius.gameserver.network.clientpackets.equipmentupgrade.RequestUpgradeSystemResult;
import org.l2jmobius.gameserver.network.clientpackets.equipmentupgradenormal.ExUpgradeSystemNormalRequest;
import org.l2jmobius.gameserver.network.clientpackets.friend.RequestBlockDetailInfo;
import org.l2jmobius.gameserver.network.clientpackets.friend.RequestBlockMemo;
import org.l2jmobius.gameserver.network.clientpackets.friend.RequestFriendDetailInfo;
import org.l2jmobius.gameserver.network.clientpackets.friend.RequestUpdateFriendMemo;
import org.l2jmobius.gameserver.network.clientpackets.gacha.ExUniqueGachaGame;
import org.l2jmobius.gameserver.network.clientpackets.gacha.ExUniqueGachaHistory;
import org.l2jmobius.gameserver.network.clientpackets.gacha.ExUniqueGachaInvenGetItem;
import org.l2jmobius.gameserver.network.clientpackets.gacha.ExUniqueGachaInvenItemList;
import org.l2jmobius.gameserver.network.clientpackets.gacha.ExUniqueGachaOpen;
import org.l2jmobius.gameserver.network.clientpackets.huntingzones.ExTimedHuntingZoneEnter;
import org.l2jmobius.gameserver.network.clientpackets.huntingzones.ExTimedHuntingZoneLeave;
import org.l2jmobius.gameserver.network.clientpackets.huntingzones.ExTimedHuntingZoneList;
import org.l2jmobius.gameserver.network.clientpackets.huntpass.HuntpassSayhasToggle;
import org.l2jmobius.gameserver.network.clientpackets.huntpass.RequestHuntPassBuyPremium;
import org.l2jmobius.gameserver.network.clientpackets.huntpass.RequestHuntPassInfo;
import org.l2jmobius.gameserver.network.clientpackets.huntpass.RequestHuntPassReward;
import org.l2jmobius.gameserver.network.clientpackets.huntpass.RequestHuntPassRewardAll;
import org.l2jmobius.gameserver.network.clientpackets.limitshop.RequestPurchaseLimitCraftItem;
import org.l2jmobius.gameserver.network.clientpackets.limitshop.RequestPurchaseLimitShopItemBuy;
import org.l2jmobius.gameserver.network.clientpackets.limitshop.RequestPurchaseLimitShopItemList;
import org.l2jmobius.gameserver.network.clientpackets.luckygame.RequestLuckyGamePlay;
import org.l2jmobius.gameserver.network.clientpackets.luckygame.RequestLuckyGameStartInfo;
import org.l2jmobius.gameserver.network.clientpackets.mablegame.ExRequestMableGameClose;
import org.l2jmobius.gameserver.network.clientpackets.mablegame.ExRequestMableGameOpen;
import org.l2jmobius.gameserver.network.clientpackets.mablegame.ExRequestMableGamePopupOk;
import org.l2jmobius.gameserver.network.clientpackets.mablegame.ExRequestMableGameReset;
import org.l2jmobius.gameserver.network.clientpackets.mablegame.ExRequestMableGameRollDice;
import org.l2jmobius.gameserver.network.clientpackets.magiclamp.ExMagicLampGameInfo;
import org.l2jmobius.gameserver.network.clientpackets.magiclamp.ExMagicLampGameStart;
import org.l2jmobius.gameserver.network.clientpackets.mentoring.ConfirmMenteeAdd;
import org.l2jmobius.gameserver.network.clientpackets.mentoring.RequestMenteeAdd;
import org.l2jmobius.gameserver.network.clientpackets.mentoring.RequestMenteeWaitingList;
import org.l2jmobius.gameserver.network.clientpackets.mentoring.RequestMentorCancel;
import org.l2jmobius.gameserver.network.clientpackets.mentoring.RequestMentorList;
import org.l2jmobius.gameserver.network.clientpackets.newcrest.RequestGetPledgeCrestPreset;
import org.l2jmobius.gameserver.network.clientpackets.newcrest.RequestSetPledgeCrestPreset;
import org.l2jmobius.gameserver.network.clientpackets.newhenna.ExRequestNewHennaEnchantReset;
import org.l2jmobius.gameserver.network.clientpackets.newhenna.RequestNewHennaCompose;
import org.l2jmobius.gameserver.network.clientpackets.newhenna.RequestNewHennaEquip;
import org.l2jmobius.gameserver.network.clientpackets.newhenna.RequestNewHennaList;
import org.l2jmobius.gameserver.network.clientpackets.newhenna.RequestNewHennaPotenEnchant;
import org.l2jmobius.gameserver.network.clientpackets.newhenna.RequestNewHennaPotenOpenslot;
import org.l2jmobius.gameserver.network.clientpackets.newhenna.RequestNewHennaPotenOpenslotProbInfo;
import org.l2jmobius.gameserver.network.clientpackets.newhenna.RequestNewHennaPotenSelect;
import org.l2jmobius.gameserver.network.clientpackets.newhenna.RequestNewHennaUnequip;
import org.l2jmobius.gameserver.network.clientpackets.newhenna.RequestNewHennaUnequipInfo;
import org.l2jmobius.gameserver.network.clientpackets.newskillenchant.RequestExSkillEnchantCharge;
import org.l2jmobius.gameserver.network.clientpackets.newskillenchant.RequestExSkillEnchantInfo;
import org.l2jmobius.gameserver.network.clientpackets.newskillenchant.RequestExSpExtractInfo;
import org.l2jmobius.gameserver.network.clientpackets.newskillenchant.RequestExSpExtractItem;
import org.l2jmobius.gameserver.network.clientpackets.olympiad.OlympiadMatchMaking;
import org.l2jmobius.gameserver.network.clientpackets.olympiad.OlympiadMatchMakingCancel;
import org.l2jmobius.gameserver.network.clientpackets.olympiad.OlympiadUI;
import org.l2jmobius.gameserver.network.clientpackets.olympiad.RequestExOlympiadMatchListRefresh;
import org.l2jmobius.gameserver.network.clientpackets.olympiad.RequestOlympiadMatchList;
import org.l2jmobius.gameserver.network.clientpackets.olympiad.RequestOlympiadObserverEnd;
import org.l2jmobius.gameserver.network.clientpackets.payback.ExPaybackGiveReward;
import org.l2jmobius.gameserver.network.clientpackets.payback.ExPaybackList;
import org.l2jmobius.gameserver.network.clientpackets.penaltyitemdrop.ExRequestPenaltyItemList;
import org.l2jmobius.gameserver.network.clientpackets.penaltyitemdrop.ExRequestPenaltyItemRestore;
import org.l2jmobius.gameserver.network.clientpackets.pet.ExEvolvePet;
import org.l2jmobius.gameserver.network.clientpackets.pet.ExPetEquipItem;
import org.l2jmobius.gameserver.network.clientpackets.pet.ExPetUnequipItem;
import org.l2jmobius.gameserver.network.clientpackets.pet.ExTryPetExtractSystem;
import org.l2jmobius.gameserver.network.clientpackets.pet.RequestExAcquirePetSkill;
import org.l2jmobius.gameserver.network.clientpackets.pk.RequestExPkPenaltyList;
import org.l2jmobius.gameserver.network.clientpackets.pk.RequestExPkPenaltyListOnlyLoc;
import org.l2jmobius.gameserver.network.clientpackets.pledgeV3.RequestExPledgeEnemyDelete;
import org.l2jmobius.gameserver.network.clientpackets.pledgeV3.RequestExPledgeEnemyInfoList;
import org.l2jmobius.gameserver.network.clientpackets.pledgeV3.RequestExPledgeEnemyRegister;
import org.l2jmobius.gameserver.network.clientpackets.pledgeV3.RequestExPledgeV3Info;
import org.l2jmobius.gameserver.network.clientpackets.pledgeV3.RequestExPledgeV3SetAnnounce;
import org.l2jmobius.gameserver.network.clientpackets.pledgebonus.RequestPledgeBonusOpen;
import org.l2jmobius.gameserver.network.clientpackets.pledgebonus.RequestPledgeBonusReward;
import org.l2jmobius.gameserver.network.clientpackets.pledgebonus.RequestPledgeBonusRewardList;
import org.l2jmobius.gameserver.network.clientpackets.pledgedonation.RequestExPledgeContributionList;
import org.l2jmobius.gameserver.network.clientpackets.pledgedonation.RequestExPledgeDonationInfo;
import org.l2jmobius.gameserver.network.clientpackets.pledgedonation.RequestExPledgeDonationRequest;
import org.l2jmobius.gameserver.network.clientpackets.primeshop.RequestBRBuyProduct;
import org.l2jmobius.gameserver.network.clientpackets.primeshop.RequestBRGamePoint;
import org.l2jmobius.gameserver.network.clientpackets.primeshop.RequestBRPresentBuyProduct;
import org.l2jmobius.gameserver.network.clientpackets.primeshop.RequestBRProductInfo;
import org.l2jmobius.gameserver.network.clientpackets.primeshop.RequestBRProductList;
import org.l2jmobius.gameserver.network.clientpackets.primeshop.RequestBRRecentProductList;
import org.l2jmobius.gameserver.network.clientpackets.prison.RequestPrisonUserDonation;
import org.l2jmobius.gameserver.network.clientpackets.prison.RequestPrisonUserInfo;
import org.l2jmobius.gameserver.network.clientpackets.quest.RequestExQuestAccept;
import org.l2jmobius.gameserver.network.clientpackets.quest.RequestExQuestAcceptableList;
import org.l2jmobius.gameserver.network.clientpackets.quest.RequestExQuestCancel;
import org.l2jmobius.gameserver.network.clientpackets.quest.RequestExQuestComplete;
import org.l2jmobius.gameserver.network.clientpackets.quest.RequestExQuestNotificationAll;
import org.l2jmobius.gameserver.network.clientpackets.quest.RequestExQuestTeleport;
import org.l2jmobius.gameserver.network.clientpackets.quest.RequestExQuestUI;
import org.l2jmobius.gameserver.network.clientpackets.quest.RequestExTeleportUI;
import org.l2jmobius.gameserver.network.clientpackets.raidbossinfo.RequestRaidBossSpawnInfo;
import org.l2jmobius.gameserver.network.clientpackets.raidbossinfo.RequestRaidServerInfo;
import org.l2jmobius.gameserver.network.clientpackets.randomcraft.ExRequestRandomCraftExtract;
import org.l2jmobius.gameserver.network.clientpackets.randomcraft.ExRequestRandomCraftInfo;
import org.l2jmobius.gameserver.network.clientpackets.randomcraft.ExRequestRandomCraftLockSlot;
import org.l2jmobius.gameserver.network.clientpackets.randomcraft.ExRequestRandomCraftMake;
import org.l2jmobius.gameserver.network.clientpackets.randomcraft.ExRequestRandomCraftRefresh;
import org.l2jmobius.gameserver.network.clientpackets.ranking.RequestExRankingCharBuffzoneNpcPosition;
import org.l2jmobius.gameserver.network.clientpackets.ranking.RequestExRankingCharSpawnBuffzoneNpc;
import org.l2jmobius.gameserver.network.clientpackets.ranking.RequestOlympiadHeroAndLegendInfo;
import org.l2jmobius.gameserver.network.clientpackets.ranking.RequestOlympiadMyRankingInfo;
import org.l2jmobius.gameserver.network.clientpackets.ranking.RequestOlympiadRankingInfo;
import org.l2jmobius.gameserver.network.clientpackets.ranking.RequestPetRankingList;
import org.l2jmobius.gameserver.network.clientpackets.ranking.RequestPetRankingMyInfo;
import org.l2jmobius.gameserver.network.clientpackets.ranking.RequestPledgeRankingList;
import org.l2jmobius.gameserver.network.clientpackets.ranking.RequestPledgeRankingMyInfo;
import org.l2jmobius.gameserver.network.clientpackets.ranking.RequestPvpRankingList;
import org.l2jmobius.gameserver.network.clientpackets.ranking.RequestPvpRankingMyInfo;
import org.l2jmobius.gameserver.network.clientpackets.ranking.RequestRankingCharHistory;
import org.l2jmobius.gameserver.network.clientpackets.ranking.RequestRankingCharInfo;
import org.l2jmobius.gameserver.network.clientpackets.ranking.RequestRankingCharRankers;
import org.l2jmobius.gameserver.network.clientpackets.relics.RequestRelicsActive;
import org.l2jmobius.gameserver.network.clientpackets.relics.RequestRelicsCloseUI;
import org.l2jmobius.gameserver.network.clientpackets.relics.RequestRelicsCombination;
import org.l2jmobius.gameserver.network.clientpackets.relics.RequestRelicsCombinationComplete;
import org.l2jmobius.gameserver.network.clientpackets.relics.RequestRelicsIdSummon;
import org.l2jmobius.gameserver.network.clientpackets.relics.RequestRelicsOpenUI;
import org.l2jmobius.gameserver.network.clientpackets.relics.RequestRelicsProbList;
import org.l2jmobius.gameserver.network.clientpackets.relics.RequestRelicsSummon;
import org.l2jmobius.gameserver.network.clientpackets.relics.RequestRelicsSummonCloseUI;
import org.l2jmobius.gameserver.network.clientpackets.relics.RequestRelicsSummonList;
import org.l2jmobius.gameserver.network.clientpackets.relics.RequestRelicsUpgrade;
import org.l2jmobius.gameserver.network.clientpackets.revenge.RequestExPvpBookShareRevengeKillerLocation;
import org.l2jmobius.gameserver.network.clientpackets.revenge.RequestExPvpBookShareRevengeList;
import org.l2jmobius.gameserver.network.clientpackets.revenge.RequestExPvpBookShareRevengeReqShareRevengeInfo;
import org.l2jmobius.gameserver.network.clientpackets.revenge.RequestExPvpBookShareRevengeSharedTeleportToKiller;
import org.l2jmobius.gameserver.network.clientpackets.revenge.RequestExPvpBookShareRevengeTeleportToKiller;
import org.l2jmobius.gameserver.network.clientpackets.sayune.RequestFlyMove;
import org.l2jmobius.gameserver.network.clientpackets.sayune.RequestFlyMoveStart;
import org.l2jmobius.gameserver.network.clientpackets.secretshop.ExRequestFestivalBmGame;
import org.l2jmobius.gameserver.network.clientpackets.secretshop.ExRequestFestivalBmInfo;
import org.l2jmobius.gameserver.network.clientpackets.settings.ExInteractModify;
import org.l2jmobius.gameserver.network.clientpackets.settings.ExSaveItemAnnounceSetting;
import org.l2jmobius.gameserver.network.clientpackets.settings.RequestKeyMapping;
import org.l2jmobius.gameserver.network.clientpackets.settings.RequestSaveKeyMapping;
import org.l2jmobius.gameserver.network.clientpackets.shuttle.CannotMoveAnymoreInShuttle;
import org.l2jmobius.gameserver.network.clientpackets.shuttle.MoveToLocationInShuttle;
import org.l2jmobius.gameserver.network.clientpackets.shuttle.RequestShuttleGetOff;
import org.l2jmobius.gameserver.network.clientpackets.shuttle.RequestShuttleGetOn;
import org.l2jmobius.gameserver.network.clientpackets.skillenchantextract.RequestExtractSkillEnchant;
import org.l2jmobius.gameserver.network.clientpackets.skillenchantguarantee.RequestSkillEnchantConfirm;
import org.l2jmobius.gameserver.network.clientpackets.stats.ExResetStatusBonus;
import org.l2jmobius.gameserver.network.clientpackets.stats.ExSetStatusBonus;
import org.l2jmobius.gameserver.network.clientpackets.steadybox.RequestSteadyBoxLoad;
import org.l2jmobius.gameserver.network.clientpackets.steadybox.RequestSteadyGetReward;
import org.l2jmobius.gameserver.network.clientpackets.steadybox.RequestSteadyOpenBox;
import org.l2jmobius.gameserver.network.clientpackets.steadybox.RequestSteadyOpenSlot;
import org.l2jmobius.gameserver.network.clientpackets.storereview.ExRequestPrivateStoreSearchList;
import org.l2jmobius.gameserver.network.clientpackets.storereview.ExRequestPrivateStoreSearchStatistics;
import org.l2jmobius.gameserver.network.clientpackets.subjugation.RequestSubjugationGacha;
import org.l2jmobius.gameserver.network.clientpackets.subjugation.RequestSubjugationGachaUI;
import org.l2jmobius.gameserver.network.clientpackets.subjugation.RequestSubjugationList;
import org.l2jmobius.gameserver.network.clientpackets.subjugation.RequestSubjugationRanking;
import org.l2jmobius.gameserver.network.clientpackets.surveillance.ExRequestUserWatcherAdd;
import org.l2jmobius.gameserver.network.clientpackets.surveillance.ExRequestUserWatcherDelete;
import org.l2jmobius.gameserver.network.clientpackets.surveillance.ExRequestUserWatcherTargetList;
import org.l2jmobius.gameserver.network.clientpackets.teleports.ExRequestSharedLocationTeleport;
import org.l2jmobius.gameserver.network.clientpackets.teleports.ExRequestSharedLocationTeleportUi;
import org.l2jmobius.gameserver.network.clientpackets.teleports.ExRequestSharingLocationUi;
import org.l2jmobius.gameserver.network.clientpackets.teleports.ExRequestTeleport;
import org.l2jmobius.gameserver.network.clientpackets.teleports.ExRequestTeleportFavoriteList;
import org.l2jmobius.gameserver.network.clientpackets.teleports.ExRequestTeleportFavoritesAddDel;
import org.l2jmobius.gameserver.network.clientpackets.teleports.ExRequestTeleportFavoritesUIToggle;
import org.l2jmobius.gameserver.network.clientpackets.teleports.ExTeleportToRaidPosition;
import org.l2jmobius.gameserver.network.clientpackets.teleports.RequestRaidTeleportInfo;
import org.l2jmobius.gameserver.network.clientpackets.training.NotifyTrainingRoomEnd;
import org.l2jmobius.gameserver.network.clientpackets.variation.ExApplyVariationOption;
import org.l2jmobius.gameserver.network.clientpackets.variation.ExVariationCloseUi;
import org.l2jmobius.gameserver.network.clientpackets.variation.ExVariationOpenUi;
import org.l2jmobius.gameserver.network.clientpackets.variation.RequestConfirmGemStone;
import org.l2jmobius.gameserver.network.clientpackets.variation.RequestRefine;
import org.l2jmobius.gameserver.network.clientpackets.vip.ExRequestVipInfo;
import org.l2jmobius.gameserver.network.clientpackets.vip.RequestVipLuckGameInfo;
import org.l2jmobius.gameserver.network.clientpackets.worldexchange.ExWorldExchangeAveragePrice;
import org.l2jmobius.gameserver.network.clientpackets.worldexchange.ExWorldExchangeBuyItem;
import org.l2jmobius.gameserver.network.clientpackets.worldexchange.ExWorldExchangeItemList;
import org.l2jmobius.gameserver.network.clientpackets.worldexchange.ExWorldExchangeRegisterItem;
import org.l2jmobius.gameserver.network.clientpackets.worldexchange.ExWorldExchangeSettleList;
import org.l2jmobius.gameserver.network.clientpackets.worldexchange.ExWorldExchangeSettleRecvResult;
import org.l2jmobius.gameserver.network.clientpackets.worldexchange.ExWorldExchangeTotalList;

/**
 * @author Mobius
 */
public enum ExClientPackets
{
	EX_REQ_MANOR_LIST(0x01, RequestManorList::new, ConnectionState.IN_GAME),
	EX_PROCURE_CROP_LIST(0x02, RequestProcureCropList::new, ConnectionState.IN_GAME),
	EX_SET_SEED(0x03, RequestSetSeed::new, ConnectionState.IN_GAME),
	EX_SET_CROP(0x04, RequestSetCrop::new, ConnectionState.IN_GAME),
	EX_WRITE_HERO_WORDS(0x05, RequestWriteHeroWords::new, ConnectionState.IN_GAME),
	EX_ASK_JOIN_MPCC(0x06, RequestExAskJoinMPCC::new, ConnectionState.IN_GAME),
	EX_ACCEPT_JOIN_MPCC(0x07, RequestExAcceptJoinMPCC::new, ConnectionState.IN_GAME),
	EX_OUST_FROM_MPCC(0x08, RequestExOustFromMPCC::new, ConnectionState.IN_GAME),
	EX_OUST_FROM_PARTY_ROOM(0x09, RequestOustFromPartyRoom::new, ConnectionState.IN_GAME),
	EX_DISMISS_PARTY_ROOM(0x0A, RequestDismissPartyRoom::new, ConnectionState.IN_GAME),
	EX_WITHDRAW_PARTY_ROOM(0x0B, RequestWithdrawPartyRoom::new, ConnectionState.IN_GAME),
	EX_HAND_OVER_PARTY_MASTER(0x0C, RequestChangePartyLeader::new, ConnectionState.IN_GAME),
	EX_AUTO_SOULSHOT(0x0D, RequestAutoSoulShot::new, ConnectionState.IN_GAME),
	EX_ENCHANT_SKILL_INFO(0x0E, RequestExEnchantSkillInfo::new, ConnectionState.IN_GAME),
	EX_REQ_ENCHANT_SKILL(0x0F, RequestExEnchantSkill::new, ConnectionState.IN_GAME),
	EX_PLEDGE_EMBLEM(0x10, RequestExPledgeCrestLarge::new, ConnectionState.IN_GAME),
	EX_SET_PLEDGE_EMBLEM(0x11, RequestExSetPledgeCrestLarge::new, ConnectionState.IN_GAME),
	EX_SET_ACADEMY_MASTER(0x12, RequestPledgeSetAcademyMaster::new, ConnectionState.IN_GAME),
	EX_PLEDGE_POWER_GRADE_LIST(0x13, RequestPledgePowerGradeList::new, ConnectionState.IN_GAME),
	EX_VIEW_PLEDGE_POWER(0x14, RequestPledgeMemberPowerInfo::new, ConnectionState.IN_GAME),
	EX_SET_PLEDGE_POWER_GRADE(0x15, RequestPledgeSetMemberPowerGrade::new, ConnectionState.IN_GAME),
	EX_VIEW_PLEDGE_MEMBER_INFO(0x16, RequestPledgeMemberInfo::new, ConnectionState.IN_GAME),
	EX_VIEW_PLEDGE_WARLIST(0x17, RequestPledgeWarList::new, ConnectionState.IN_GAME),
	EX_FISH_RANKING(0x18, RequestExFishRanking::new, ConnectionState.IN_GAME),
	EX_PCCAFE_COUPON_USE(0x19, RequestPCCafeCouponUse::new, ConnectionState.IN_GAME),
	EX_ORC_MOVE(0x1A, null, ConnectionState.IN_GAME),
	EX_DUEL_ASK_START(0x1B, RequestDuelStart::new, ConnectionState.IN_GAME),
	EX_DUEL_ACCEPT_START(0x1C, RequestDuelAnswerStart::new, ConnectionState.IN_GAME),
	EX_SET_TUTORIAL(0x1D, null, ConnectionState.IN_GAME),
	EX_RQ_ITEMLINK(0x1E, RequestExRqItemLink::new, ConnectionState.IN_GAME),
	EX_CAN_NOT_MOVE_ANYMORE_IN_AIRSHIP(0x1F, null, ConnectionState.IN_GAME),
	EX_MOVE_TO_LOCATION_IN_AIRSHIP(0x20, MoveToLocationInAirShip::new, ConnectionState.IN_GAME),
	EX_LOAD_UI_SETTING(0x21, RequestKeyMapping::new, ConnectionState.ENTERING, ConnectionState.IN_GAME),
	EX_SAVE_UI_SETTING(0x22, RequestSaveKeyMapping::new, ConnectionState.IN_GAME),
	EX_REQUEST_BASE_ATTRIBUTE_CANCEL(0x23, RequestExRemoveItemAttribute::new, ConnectionState.IN_GAME),
	EX_CHANGE_INVENTORY_SLOT(0x24, RequestSaveInventoryOrder::new, ConnectionState.IN_GAME),
	EX_EXIT_PARTY_MATCHING_WAITING_ROOM(0x25, RequestExitPartyMatchingWaitingRoom::new, ConnectionState.IN_GAME),
	EX_TRY_TO_PUT_ITEM_FOR_VARIATION_MAKE(0x26, RequestConfirmTargetItem::new, ConnectionState.IN_GAME),
	EX_TRY_TO_PUT_INTENSIVE_FOR_VARIATION_MAKE(0x27, RequestConfirmRefinerItem::new, ConnectionState.IN_GAME),
	EX_TRY_TO_PUT_COMMISSION_FOR_VARIATION_MAKE(0x28, RequestConfirmGemStone::new, ConnectionState.IN_GAME),
	EX_OLYMPIAD_OBSERVER_END(0x29, RequestOlympiadObserverEnd::new, ConnectionState.IN_GAME),
	EX_CURSED_WEAPON_LIST(0x2A, RequestCursedWeaponList::new, ConnectionState.IN_GAME),
	EX_EXISTING_CURSED_WEAPON_LOCATION(0x2B, RequestCursedWeaponLocation::new, ConnectionState.IN_GAME),
	EX_REORGANIZE_PLEDGE_MEMBER(0x2C, RequestPledgeReorganizeMember::new, ConnectionState.IN_GAME),
	EX_MPCC_SHOW_PARTY_MEMBERS_INFO(0x2D, RequestExMPCCShowPartyMembersInfo::new, ConnectionState.IN_GAME),
	EX_OLYMPIAD_MATCH_LIST(0x2E, RequestOlympiadMatchList::new, ConnectionState.IN_GAME),
	EX_ASK_JOIN_PARTY_ROOM(0x2F, RequestAskJoinPartyRoom::new, ConnectionState.IN_GAME),
	EX_ANSWER_JOIN_PARTY_ROOM(0x30, AnswerJoinPartyRoom::new, ConnectionState.IN_GAME),
	EX_LIST_PARTY_MATCHING_WAITING_ROOM(0x31, RequestListPartyMatchingWaitingRoom::new, ConnectionState.IN_GAME),
	EX_CHOOSE_INVENTORY_ATTRIBUTE_ITEM(0x32, RequestExEnchantItemAttribute::new, ConnectionState.IN_GAME),
	EX_CHARACTER_BACK(0x33, RequestGotoLobby::new, ConnectionState.AUTHENTICATED),
	EX_CANNOT_AIRSHIP_MOVE_ANYMORE(0x34, null, ConnectionState.IN_GAME),
	EX_MOVE_TO_LOCATION_AIRSHIP(0x35, MoveToLocationAirShip::new, ConnectionState.IN_GAME),
	EX_ITEM_AUCTION_BID(0x36, RequestBidItemAuction::new, ConnectionState.IN_GAME),
	EX_ITEM_AUCTION_INFO(0x37, RequestInfoItemAuction::new, ConnectionState.IN_GAME),
	EX_CHANGE_NAME(0x38, RequestExChangeName::new, ConnectionState.IN_GAME),
	EX_SHOW_CASTLE_INFO(0x39, RequestAllCastleInfo::new, ConnectionState.IN_GAME),
	EX_SHOW_FORTRESS_INFO(0x3A, RequestAllFortressInfo::new, ConnectionState.IN_GAME),
	EX_SHOW_AGIT_INFO(0x3B, RequestAllAgitInfo::new, ConnectionState.IN_GAME),
	EX_SHOW_FORTRESS_SIEGE_INFO(0x3C, RequestFortressSiegeInfo::new, ConnectionState.IN_GAME),
	EX_GET_BOSS_RECORD(0x3D, RequestGetBossRecord::new, ConnectionState.IN_GAME),
	EX_TRY_TO_MAKE_VARIATION(0x3E, RequestRefine::new, ConnectionState.IN_GAME),
	EX_TRY_TO_PUT_ITEM_FOR_VARIATION_CANCEL(0x3F, RequestConfirmCancelItem::new, ConnectionState.IN_GAME),
	EX_CLICK_VARIATION_CANCEL_BUTTON(0x40, RequestRefineCancel::new, ConnectionState.IN_GAME),
	EX_MAGIC_SKILL_USE_GROUND(0x41, RequestExMagicSkillUseGround::new, ConnectionState.IN_GAME),
	EX_DUEL_SURRENDER(0x42, RequestDuelSurrender::new, ConnectionState.IN_GAME),
	EX_ENCHANT_SKILL_INFO_DETAIL(0x43, RequestExEnchantSkillInfoDetail::new, ConnectionState.IN_GAME),
	EX_REQUEST_ANTI_FREE_SERVER(0x44, null, ConnectionState.IN_GAME),
	EX_SHOW_FORTRESS_MAP_INFO(0x45, RequestFortressMapInfo::new, ConnectionState.IN_GAME),
	EX_REQUEST_PVPMATCH_RECORD(0x46, RequestPVPMatchRecord::new, ConnectionState.IN_GAME),
	EX_PRIVATE_STORE_WHOLE_SET_MSG(0x47, SetPrivateStoreWholeMsg::new, ConnectionState.IN_GAME),
	EX_DISPEL(0x48, RequestDispel::new, ConnectionState.IN_GAME),
	EX_TRY_TO_PUT_ENCHANT_TARGET_ITEM(0x49, RequestExTryToPutEnchantTargetItem::new, ConnectionState.IN_GAME),
	EX_TRY_TO_PUT_ENCHANT_SUPPORT_ITEM(0x4A, RequestExTryToPutEnchantSupportItem::new, ConnectionState.IN_GAME),
	EX_CANCEL_ENCHANT_ITEM(0x4B, RequestExCancelEnchantItem::new, ConnectionState.IN_GAME),
	EX_CHANGE_NICKNAME_COLOR(0x4C, RequestChangeNicknameColor::new, ConnectionState.IN_GAME),
	EX_REQUEST_RESET_NICKNAME(0x4D, RequestResetNickname::new, ConnectionState.IN_GAME),
	EX_USER_BOOKMARK(0x4E, null, ConnectionState.IN_GAME),
	EX_WITHDRAW_PREMIUM_ITEM(0x4F, RequestWithDrawPremiumItem::new, ConnectionState.IN_GAME),
	EX_JUMP(0x50, null, ConnectionState.IN_GAME),
	EX_START_REQUEST_PVPMATCH_CC_RANK(0x51, null, ConnectionState.IN_GAME),
	EX_STOP_REQUEST_PVPMATCH_CC_RANK(0x52, null, ConnectionState.IN_GAME),
	EX_NOTIFY_START_MINIGAME(0x53, null, ConnectionState.IN_GAME),
	EX_REQUEST_REGISTER_DOMINION(0x54, null, ConnectionState.IN_GAME),
	EX_REQUEST_DOMINION_INFO(0x55, null, ConnectionState.IN_GAME),
	EX_CLEFT_ENTER(0x56, null, ConnectionState.IN_GAME),
	EX_BLOCK_UPSET_ENTER(0x57, null, ConnectionState.IN_GAME),
	EX_END_SCENE_PLAYER(0x58, EndScenePlayer::new, ConnectionState.IN_GAME),
	EX_BLOCK_UPSET_VOTE(0x59, null, ConnectionState.IN_GAME),
	EX_LIST_MPCC_WAITING(0x5A, RequestExListMpccWaiting::new, ConnectionState.IN_GAME),
	EX_MANAGE_MPCC_ROOM(0x5B, RequestExManageMpccRoom::new, ConnectionState.IN_GAME),
	EX_JOIN_MPCC_ROOM(0x5C, RequestExJoinMpccRoom::new, ConnectionState.IN_GAME),
	EX_OUST_FROM_MPCC_ROOM(0x5D, RequestExOustFromMpccRoom::new, ConnectionState.IN_GAME),
	EX_DISMISS_MPCC_ROOM(0x5E, RequestExDismissMpccRoom::new, ConnectionState.IN_GAME),
	EX_WITHDRAW_MPCC_ROOM(0x5F, RequestExWithdrawMpccRoom::new, ConnectionState.IN_GAME),
	EX_SEED_PHASE(0x60, RequestSeedPhase::new, ConnectionState.IN_GAME),
	EX_MPCC_PARTYMASTER_LIST(0x61, RequestExMpccPartymasterList::new, ConnectionState.IN_GAME),
	EX_REQUEST_POST_ITEM_LIST(0x62, RequestPostItemList::new, ConnectionState.IN_GAME),
	EX_SEND_POST(0x63, RequestSendPost::new, ConnectionState.IN_GAME),
	EX_REQUEST_RECEIVED_POST_LIST(0x64, RequestReceivedPostList::new, ConnectionState.IN_GAME),
	EX_DELETE_RECEIVED_POST(0x65, RequestDeleteReceivedPost::new, ConnectionState.IN_GAME),
	EX_REQUEST_RECEIVED_POST(0x66, RequestReceivedPost::new, ConnectionState.IN_GAME),
	EX_RECEIVE_POST(0x67, RequestPostAttachment::new, ConnectionState.IN_GAME),
	EX_REJECT_POST(0x68, RequestRejectPostAttachment::new, ConnectionState.IN_GAME),
	EX_REQUEST_SENT_POST_LIST(0x69, RequestSentPostList::new, ConnectionState.IN_GAME),
	EX_DELETE_SENT_POST(0x6A, RequestDeleteSentPost::new, ConnectionState.IN_GAME),
	EX_REQUEST_SENT_POST(0x6B, RequestSentPost::new, ConnectionState.IN_GAME),
	EX_CANCEL_SEND_POST(0x6C, RequestCancelPostAttachment::new, ConnectionState.IN_GAME),
	EX_POST_ITEM_FEE(0x6D, RequestPostItemFee::new, ConnectionState.IN_GAME),
	EX_REQUEST_SHOW_PETITION(0x6E, null, ConnectionState.IN_GAME),
	EX_POST_TRADE_FEE(0x6F, null, ConnectionState.IN_GAME),
	EX_POST_TRADE_COUNT(0x70, null, ConnectionState.IN_GAME),
	EX_REQUEST_SHOWSTEP_TWO(0x71, null, ConnectionState.IN_GAME),
	EX_REQUEST_SHOWSTEP_THREE(0x72, null, ConnectionState.IN_GAME),
	EX_CONNECT_TO_RAID_SERVER(0x73, null, ConnectionState.IN_GAME),
	EX_RETURN_FROM_RAID(0x74, null, ConnectionState.IN_GAME),
	EX_REFUND_REQ(0x75, RequestRefundItem::new, ConnectionState.IN_GAME),
	EX_BUY_SELL_UI_CLOSE_REQ(0x76, RequestBuySellUIClose::new, ConnectionState.IN_GAME),
	EX_EVENT_MATCH(0x77, null, ConnectionState.IN_GAME),
	EX_PARTY_LOOTING_MODIFY(0x78, RequestPartyLootModification::new, ConnectionState.IN_GAME),
	EX_PARTY_LOOTING_MODIFY_AGREEMENT(0x79, AnswerPartyLootModification::new, ConnectionState.IN_GAME),
	EX_ANSWER_COUPLE_ACTION(0x7A, AnswerCoupleAction::new, ConnectionState.IN_GAME),
	EX_BR_LOAD_EVENT_TOP_RANKERS_REQ(0x7B, BrEventRankerList::new, ConnectionState.IN_GAME),
	EX_ASK_MY_MEMBERSHIP(0x7C, null, ConnectionState.IN_GAME),
	EX_QUEST_NPC_LOG_LIST(0x7D, RequestAddExpandQuestAlarm::new, ConnectionState.IN_GAME),
	EX_VOTE_SYSTEM(0x7E, RequestVoteNew::new, ConnectionState.IN_GAME),
	EX_GETON_SHUTTLE(0x7F, RequestShuttleGetOn::new, ConnectionState.IN_GAME),
	EX_GETOFF_SHUTTLE(0x80, RequestShuttleGetOff::new, ConnectionState.IN_GAME),
	EX_MOVE_TO_LOCATION_IN_SHUTTLE(0x81, MoveToLocationInShuttle::new, ConnectionState.IN_GAME),
	EX_CAN_NOT_MOVE_ANYMORE_IN_SHUTTLE(0x82, CannotMoveAnymoreInShuttle::new, ConnectionState.IN_GAME),
	EX_AGITAUCTION_CMD(0x83, null, ConnectionState.IN_GAME), // TODO: Implement / HANDLE SWITCH
	EX_ADD_POST_FRIEND(0x84, RequestExAddContactToContactList::new, ConnectionState.IN_GAME),
	EX_DELETE_POST_FRIEND(0x85, RequestExDeleteContactFromContactList::new, ConnectionState.IN_GAME),
	EX_SHOW_POST_FRIEND(0x86, RequestExShowContactList::new, ConnectionState.IN_GAME),
	EX_FRIEND_LIST_FOR_POSTBOX(0x87, RequestExFriendListExtended::new, ConnectionState.IN_GAME),
	EX_GFX_OLYMPIAD(0x88, RequestExOlympiadMatchListRefresh::new, ConnectionState.IN_GAME),
	EX_BR_GAME_POINT_REQ(0x89, RequestBRGamePoint::new, ConnectionState.IN_GAME),
	EX_BR_PRODUCT_LIST_REQ(0x8A, RequestBRProductList::new, ConnectionState.IN_GAME),
	EX_BR_PRODUCT_INFO_REQ(0x8B, RequestBRProductInfo::new, ConnectionState.IN_GAME),
	EX_BR_BUY_PRODUCT_REQ(0x8C, RequestBRBuyProduct::new, ConnectionState.IN_GAME),
	EX_BR_RECENT_PRODUCT_REQ(0x8D, RequestBRRecentProductList::new, ConnectionState.IN_GAME),
	EX_BR_MINIGAME_LOAD_SCORES_REQ(0x8E, null, ConnectionState.IN_GAME),
	EX_BR_MINIGAME_INSERT_SCORE_REQ(0x8F, null, ConnectionState.IN_GAME),
	EX_BR_SET_LECTURE_MARK_REQ(0x90, null, ConnectionState.IN_GAME),
	EX_REQUEST_CRYSTALITEM_INFO(0x91, RequestCrystallizeEstimate::new, ConnectionState.IN_GAME),
	EX_REQUEST_CRYSTALITEM_CANCEL(0x92, RequestCrystallizeItemCancel::new, ConnectionState.IN_GAME),
	EX_STOP_SCENE_PLAYER(0x93, RequestExEscapeScene::new, ConnectionState.IN_GAME),
	EX_FLY_MOVE(0x94, RequestFlyMove::new, ConnectionState.IN_GAME),
	EX_SURRENDER_PLEDGE_WAR(0x95, null, ConnectionState.IN_GAME),
	EX_DYNAMIC_QUEST(0x96, null, ConnectionState.IN_GAME), // TODO: Implement / HANDLE SWITCH
	EX_FRIEND_DETAIL_INFO(0x97, RequestFriendDetailInfo::new, ConnectionState.IN_GAME),
	EX_UPDATE_FRIEND_MEMO(0x98, RequestUpdateFriendMemo::new, ConnectionState.IN_GAME),
	EX_UPDATE_BLOCK_MEMO(0x99, RequestBlockMemo::new, ConnectionState.IN_GAME),
	EX_LOAD_INZONE_PARTY_HISTORY(0x9A, null, ConnectionState.IN_GAME),
	EX_REQUEST_COMMISSION_ITEM_LIST(0x9B, RequestCommissionRegistrableItemList::new, ConnectionState.IN_GAME),
	EX_REQUEST_COMMISSION_INFO(0x9C, RequestCommissionInfo::new, ConnectionState.IN_GAME),
	EX_REQUEST_COMMISSION_REGISTER(0x9D, RequestCommissionRegister::new, ConnectionState.IN_GAME),
	EX_REQUEST_COMMISSION_CANCEL(0x9E, RequestCommissionCancel::new, ConnectionState.IN_GAME),
	EX_REQUEST_COMMISSION_DELETE(0x9F, RequestCommissionDelete::new, ConnectionState.IN_GAME),
	EX_REQUEST_COMMISSION_SEARCH(0xA0, RequestCommissionList::new, ConnectionState.IN_GAME),
	EX_REQUEST_COMMISSION_BUY_INFO(0xA1, RequestCommissionBuyInfo::new, ConnectionState.IN_GAME),
	EX_REQUEST_COMMISSION_BUY_ITEM(0xA2, RequestCommissionBuyItem::new, ConnectionState.IN_GAME),
	EX_REQUEST_COMMISSION_REGISTERED_ITEM(0xA3, RequestCommissionRegisteredItem::new, ConnectionState.IN_GAME),
	EX_CALL_TO_CHANGE_CLASS(0xA4, null, ConnectionState.IN_GAME),
	EX_CHANGE_TO_AWAKENED_CLASS(0xA5, RequestChangeToAwakenedClass::new, ConnectionState.IN_GAME),
	EX_REQUEST_WORLD_STATISTICS(0xA6, null, ConnectionState.IN_GAME),
	EX_REQUEST_USER_STATISTICS(0xA7, null, ConnectionState.IN_GAME),
	EX_REQUEST_WEB_SESSION_ID(0xA8, null, ConnectionState.IN_GAME),
	EX_2ND_PASSWORD_CHECK(0xA9, RequestEx2ndPasswordCheck::new, ConnectionState.AUTHENTICATED),
	EX_2ND_PASSWORD_VERIFY(0xAA, RequestEx2ndPasswordVerify::new, ConnectionState.AUTHENTICATED),
	EX_2ND_PASSWORD_REQ(0xAB, RequestEx2ndPasswordReq::new, ConnectionState.AUTHENTICATED),
	EX_CHECK_CHAR_NAME(0xAC, RequestCharacterNameCreatable::new, ConnectionState.AUTHENTICATED),
	EX_REQUEST_GOODS_INVENTORY_INFO(0xAD, null, ConnectionState.IN_GAME),
	EX_REQUEST_USE_GOODS_IVENTORY_ITEM(0xAE, null, ConnectionState.IN_GAME),
	EX_NOTIFY_PLAY_START(0xAF, null, ConnectionState.IN_GAME),
	EX_FLY_MOVE_START(0xB0, RequestFlyMoveStart::new, ConnectionState.IN_GAME),
	EX_USER_HARDWARE_INFO(0xB1, RequestHardWareInfo::new, ConnectionState.values()),
	EX_USER_INTERFACE_INFO(0xB2, null, ConnectionState.IN_GAME),
	EX_CHANGE_ATTRIBUTE_ITEM(0xB3, SendChangeAttributeTargetItem::new, ConnectionState.IN_GAME),
	EX_REQUEST_CHANGE_ATTRIBUTE(0xB4, RequestChangeAttributeItem::new, ConnectionState.IN_GAME),
	EX_CHANGE_ATTRIBUTE_CANCEL(0xB5, RequestChangeAttributeCancel::new, ConnectionState.IN_GAME),
	EX_BR_BUY_PRODUCT_GIFT_REQ(0xB6, RequestBRPresentBuyProduct::new, ConnectionState.IN_GAME),
	EX_MENTOR_ADD(0xB7, ConfirmMenteeAdd::new, ConnectionState.IN_GAME),
	EX_MENTOR_CANCEL(0xB8, RequestMentorCancel::new, ConnectionState.IN_GAME),
	EX_MENTOR_LIST(0xB9, RequestMentorList::new, ConnectionState.IN_GAME),
	EX_REQUEST_MENTOR_ADD(0xBA, RequestMenteeAdd::new, ConnectionState.IN_GAME),
	EX_MENTEE_WAITING_LIST(0xBB, RequestMenteeWaitingList::new, ConnectionState.IN_GAME),
	EX_JOIN_PLEDGE_BY_NAME(0xBC, RequestClanAskJoinByName::new, ConnectionState.IN_GAME),
	EX_INZONE_WAITING_TIME(0xBD, RequestInzoneWaitingTime::new, ConnectionState.IN_GAME),
	EX_JOIN_CURIOUS_HOUSE(0xBE, null, ConnectionState.IN_GAME),
	EX_CANCEL_CURIOUS_HOUSE(0xBF, null, ConnectionState.IN_GAME),
	EX_LEAVE_CURIOUS_HOUSE(0xC0, null, ConnectionState.IN_GAME),
	EX_OBSERVE_LIST_CURIOUS_HOUSE(0xC1, null, ConnectionState.IN_GAME),
	EX_OBSERVE_CURIOUS_HOUSE(0xC2, null, ConnectionState.IN_GAME),
	EX_EXIT_OBSERVE_CURIOUS_HOUSE(0xC3, null, ConnectionState.IN_GAME),
	EX_REQ_CURIOUS_HOUSE_HTML(0xC4, null, ConnectionState.IN_GAME),
	EX_REQ_CURIOUS_HOUSE_RECORD(0xC5, null, ConnectionState.IN_GAME),
	EX_SYS_STRING(0xC6, null, ConnectionState.IN_GAME),
	EX_TRY_TO_PUT_SHAPE_SHIFTING_TARGET_ITEM(0xC7, RequestExTryToPutShapeShiftingTargetItem::new, ConnectionState.IN_GAME),
	EX_TRY_TO_PUT_SHAPE_SHIFTING_EXTRACTION_ITEM(0xC8, RequestExTryToPutShapeShiftingEnchantSupportItem::new, ConnectionState.IN_GAME),
	EX_CANCEL_SHAPE_SHIFTING(0xC9, RequestExCancelShape_Shifting_Item::new, ConnectionState.IN_GAME),
	EX_REQUEST_SHAPE_SHIFTING(0xCA, RequestShapeShiftingItem::new, ConnectionState.IN_GAME),
	EX_NCGUARD(0xCB, null, ConnectionState.IN_GAME),
	EX_REQUEST_KALIE_TOKEN(0xCC, null, ConnectionState.IN_GAME),
	EX_REQUEST_SHOW_REGIST_BEAUTY(0xCD, RequestShowBeautyList::new, ConnectionState.IN_GAME),
	EX_REQUEST_REGIST_BEAUTY(0xCE, RequestRegistBeauty::new, ConnectionState.IN_GAME),
	EX_REQUEST_SHOW_RESET_BEAUTY(0xCF, null, ConnectionState.IN_GAME),
	EX_REQUEST_RESET_BEAUTY(0xD0, RequestShowResetShopList::new, ConnectionState.IN_GAME),
	EX_CHECK_SPEEDHACK(0xD1, null, ConnectionState.IN_GAME),
	EX_BR_ADD_INTERESTED_PRODUCT(0xD2, null, ConnectionState.IN_GAME),
	EX_BR_DELETE_INTERESTED_PRODUCT(0xD3, null, ConnectionState.IN_GAME),
	EX_BR_EXIST_NEW_PRODUCT_REQ(0xD4, null, ConnectionState.IN_GAME),
	EX_EVENT_CAMPAIGN_INFO(0xD5, null, ConnectionState.IN_GAME),
	EX_PLEDGE_RECRUIT_INFO(0xD6, RequestPledgeRecruitInfo::new, ConnectionState.IN_GAME),
	EX_PLEDGE_RECRUIT_BOARD_SEARCH(0xD7, RequestPledgeRecruitBoardSearch::new, ConnectionState.IN_GAME),
	EX_PLEDGE_RECRUIT_BOARD_APPLY(0xD8, RequestPledgeRecruitBoardAccess::new, ConnectionState.IN_GAME),
	EX_PLEDGE_RECRUIT_BOARD_DETAIL(0xD9, RequestPledgeRecruitBoardDetail::new, ConnectionState.IN_GAME),
	EX_PLEDGE_WAITING_LIST_APPLY(0xDA, RequestPledgeWaitingApply::new, ConnectionState.IN_GAME),
	EX_PLEDGE_WAITING_LIST_APPLIED(0xDB, RequestPledgeWaitingApplied::new, ConnectionState.IN_GAME),
	EX_PLEDGE_WAITING_LIST(0xDC, RequestPledgeWaitingList::new, ConnectionState.IN_GAME),
	EX_PLEDGE_WAITING_USER(0xDD, RequestPledgeWaitingUser::new, ConnectionState.IN_GAME),
	EX_PLEDGE_WAITING_USER_ACCEPT(0xDE, RequestPledgeWaitingUserAccept::new, ConnectionState.IN_GAME),
	EX_PLEDGE_DRAFT_LIST_SEARCH(0xDF, RequestPledgeDraftListSearch::new, ConnectionState.IN_GAME),
	EX_PLEDGE_DRAFT_LIST_APPLY(0xE0, RequestPledgeDraftListApply::new, ConnectionState.IN_GAME),
	EX_PLEDGE_RECRUIT_APPLY_INFO(0xE1, RequestPledgeRecruitApplyInfo::new, ConnectionState.IN_GAME),
	EX_PLEDGE_JOIN_SYS(0xE2, null, ConnectionState.IN_GAME),
	EX_RESPONSE_WEB_PETITION_ALARM(0xE3, null, ConnectionState.IN_GAME),
	EX_NOTIFY_EXIT_BEAUTYSHOP(0xE4, NotifyExitBeautyShop::new, ConnectionState.IN_GAME),
	EX_EVENT_REGISTER_XMAS_WISHCARD(0xE5, null, ConnectionState.IN_GAME),
	EX_ENCHANT_SCROLL_ITEM_ADD(0xE6, RequestExAddEnchantScrollItem::new, ConnectionState.IN_GAME),
	EX_ENCHANT_SUPPORT_ITEM_REMOVE(0xE7, RequestExRemoveEnchantSupportItem::new, ConnectionState.IN_GAME),
	EX_SELECT_CARD_REWARD(0xE8, null, ConnectionState.IN_GAME),
	EX_DIVIDE_ADENA_START(0xE9, RequestDivideAdenaStart::new, ConnectionState.IN_GAME),
	EX_DIVIDE_ADENA_CANCEL(0xEA, RequestDivideAdenaCancel::new, ConnectionState.IN_GAME),
	EX_DIVIDE_ADENA(0xEB, RequestDivideAdena::new, ConnectionState.IN_GAME),
	EX_ACQUIRE_POTENTIAL_SKILL(0xEC, null, ConnectionState.IN_GAME),
	EX_REQUEST_POTENTIAL_SKILL_LIST(0xED, null, ConnectionState.IN_GAME),
	EX_RESET_POTENTIAL_SKILL(0xEE, null, ConnectionState.IN_GAME),
	EX_CHANGE_POTENTIAL_POINT(0xEF, null, ConnectionState.IN_GAME),
	EX_STOP_MOVE(0xF0, RequestStopMove::new, ConnectionState.IN_GAME),
	EX_ABILITY_WND_OPEN(0xF1, null, ConnectionState.IN_GAME),
	EX_ABILITY_WND_CLOSE(0xF2, null, ConnectionState.IN_GAME),
	EX_START_LUCKY_GAME(0xF3, RequestLuckyGameStartInfo::new, ConnectionState.IN_GAME),
	EX_BETTING_LUCKY_GAME(0xF4, RequestLuckyGamePlay::new, ConnectionState.IN_GAME),
	EX_TRAININGZONE_LEAVING(0xF5, NotifyTrainingRoomEnd::new, ConnectionState.IN_GAME),
	EX_ENCHANT_ONE(0xF6, RequestNewEnchantPushOne::new, ConnectionState.IN_GAME),
	EX_ENCHANT_ONE_REMOVE(0xF7, RequestNewEnchantRemoveOne::new, ConnectionState.IN_GAME),
	EX_ENCHANT_TWO(0xF8, RequestNewEnchantPushTwo::new, ConnectionState.IN_GAME),
	EX_ENCHANT_TWO_REMOVE(0xF9, RequestNewEnchantRemoveTwo::new, ConnectionState.IN_GAME),
	EX_ENCHANT_CLOSE(0xFA, RequestNewEnchantClose::new, ConnectionState.IN_GAME),
	EX_ENCHANT_TRY(0xFB, RequestNewEnchantTry::new, ConnectionState.IN_GAME),
	EX_ENCHANT_RETRY_TO_PUT_ITEMS(0xFC, RequestNewEnchantRetryToPutItems::new, ConnectionState.IN_GAME),
	EX_REQUEST_CARD_REWARD_LIST(0xFD, null, ConnectionState.IN_GAME),
	EX_REQUEST_ACCOUNT_ATTENDANCE_INFO(0xFE, null, ConnectionState.IN_GAME),
	EX_REQUEST_ACCOUNT_ATTENDANCE_REWARD(0xFF, null, ConnectionState.IN_GAME),
	EX_TARGET(0x100, RequestTargetActionMenu::new, ConnectionState.IN_GAME),
	EX_SELECTED_QUEST_ZONEID(0x101, ExSendSelectedQuestZoneID::new, ConnectionState.IN_GAME),
	EX_ALCHEMY_SKILL_LIST(0x102, RequestAlchemySkillList::new, ConnectionState.IN_GAME),
	EX_TRY_MIX_CUBE(0x103, null, ConnectionState.IN_GAME),
	C_REQUEST_ALCHEMY_CONVERSION(0x104, null, ConnectionState.IN_GAME),
	EX_EXECUTED_UIEVENTS_COUNT(0x105, null, ConnectionState.IN_GAME),
	EX_CLIENT_INI(0x106, null, ConnectionState.AUTHENTICATED),
	EX_REQUEST_AUTOFISH(0x107, ExRequestAutoFish::new, ConnectionState.IN_GAME),
	EX_REQUEST_VIP_ATTENDANCE_ITEMLIST(0x108, RequestVipAttendanceItemList::new, ConnectionState.IN_GAME),
	EX_REQUEST_VIP_ATTENDANCE_CHECK(0x109, RequestVipAttendanceCheck::new, ConnectionState.IN_GAME),
	EX_TRY_ENSOUL(0x10A, RequestItemEnsoul::new, ConnectionState.IN_GAME),
	EX_CASTLEWAR_SEASON_REWARD(0x10B, null, ConnectionState.IN_GAME),
	EX_BR_VIP_PRODUCT_LIST_REQ(0x10C, null, ConnectionState.IN_GAME),
	EX_REQUEST_LUCKY_GAME_INFO(0x10D, RequestVipLuckGameInfo::new, ConnectionState.IN_GAME),
	EX_REQUEST_LUCKY_GAME_ITEMLIST(0x10E, null, ConnectionState.IN_GAME),
	EX_REQUEST_LUCKY_GAME_BONUS(0x10F, null, ConnectionState.IN_GAME),
	EX_VIP_INFO(0x110, ExRequestVipInfo::new, ConnectionState.IN_GAME),
	EX_CAPTCHA_ANSWER(0x111, RequestCaptchaAnswer::new, ConnectionState.IN_GAME),
	EX_REFRESH_CAPTCHA_IMAGE(0x112, RequestRefreshCaptcha::new, ConnectionState.IN_GAME),
	EX_PLEDGE_SIGNIN(0x113, RequestPledgeSignInForOpenJoiningMethod::new, ConnectionState.IN_GAME),
	EX_REQUEST_MATCH_ARENA(0x114, null, ConnectionState.IN_GAME),
	EX_CONFIRM_MATCH_ARENA(0x115, null, ConnectionState.IN_GAME),
	EX_CANCEL_MATCH_ARENA(0x116, null, ConnectionState.IN_GAME),
	EX_CHANGE_CLASS_ARENA(0x117, null, ConnectionState.IN_GAME),
	EX_CONFIRM_CLASS_ARENA(0x118, null, ConnectionState.IN_GAME),
	EX_DECO_NPC_INFO(0x119, null, ConnectionState.IN_GAME),
	EX_DECO_NPC_SET(0x11A, null, ConnectionState.IN_GAME),
	EX_FACTION_INFO(0x11B, null, ConnectionState.IN_GAME),
	EX_EXIT_ARENA(0x11C, null, ConnectionState.IN_GAME),
	EX_REQUEST_BALTHUS_TOKEN(0x11D, RequestEventBalthusToken::new, ConnectionState.IN_GAME),
	EX_PARTY_MATCHING_ROOM_HISTORY(0x11E, RequestPartyMatchingHistory::new, ConnectionState.IN_GAME),
	EX_ARENA_CUSTOM_NOTIFICATION(0x11F, null, ConnectionState.IN_GAME),
	EX_TODOLIST(0x120, RequestTodoList::new, ConnectionState.IN_GAME),
	EX_TODOLIST_HTML(0x121, null, ConnectionState.IN_GAME),
	EX_ONE_DAY_RECEIVE_REWARD(0x122, RequestOneDayRewardReceive::new, ConnectionState.IN_GAME),
	EX_QUEUETICKET(0x123, null, ConnectionState.IN_GAME),
	EX_PLEDGE_BONUS_UI_OPEN(0x124, RequestPledgeBonusOpen::new, ConnectionState.IN_GAME),
	EX_PLEDGE_BONUS_REWARD_LIST(0x125, RequestPledgeBonusRewardList::new, ConnectionState.IN_GAME),
	EX_PLEDGE_BONUS_REWARD(0x126, RequestPledgeBonusReward::new, ConnectionState.IN_GAME),
	EX_SSO_AUTHNTOKEN_REQ(0x127, null, ConnectionState.IN_GAME),
	EX_QUEUETICKET_LOGIN(0x128, null, ConnectionState.IN_GAME),
	EX_BLOCK_DETAIL_INFO(0x129, RequestBlockDetailInfo::new, ConnectionState.IN_GAME),
	EX_TRY_ENSOUL_EXTRACTION(0x12A, RequestTryEnSoulExtraction::new, ConnectionState.IN_GAME),
	EX_RAID_BOSS_SPAWN_INFO(0x12B, RequestRaidBossSpawnInfo::new, ConnectionState.IN_GAME),
	EX_RAID_SERVER_INFO(0x12C, RequestRaidServerInfo::new, ConnectionState.IN_GAME),
	EX_SHOW_AGIT_SIEGE_INFO(0x12D, null, ConnectionState.IN_GAME),
	EX_ITEM_AUCTION_STATUS(0x12E, null, ConnectionState.IN_GAME),
	EX_MONSTER_BOOK_OPEN(0x12F, null, ConnectionState.IN_GAME),
	EX_MONSTER_BOOK_CLOSE(0x130, null, ConnectionState.IN_GAME),
	EX_REQ_MONSTER_BOOK_REWARD(0x131, null, ConnectionState.IN_GAME),
	EX_MATCHGROUP(0x132, null, ConnectionState.IN_GAME),
	EX_MATCHGROUP_ASK(0x133, null, ConnectionState.IN_GAME),
	EX_MATCHGROUP_ANSWER(0x134, null, ConnectionState.IN_GAME),
	EX_MATCHGROUP_WITHDRAW(0x135, null, ConnectionState.IN_GAME),
	EX_MATCHGROUP_OUST(0x136, null, ConnectionState.IN_GAME),
	EX_MATCHGROUP_CHANGE_MASTER(0x137, null, ConnectionState.IN_GAME),
	EX_UPGRADE_SYSTEM_REQUEST(0x138, RequestUpgradeSystemResult::new, ConnectionState.IN_GAME),
	EX_CARD_UPDOWN_PICK_NUMB(0x139, null, ConnectionState.IN_GAME),
	EX_CARD_UPDOWN_GAME_REWARD_REQUEST(0x13A, null, ConnectionState.IN_GAME),
	EX_CARD_UPDOWN_GAME_RETRY(0x13B, null, ConnectionState.IN_GAME),
	EX_CARD_UPDOWN_GAME_QUIT(0x13C, null, ConnectionState.IN_GAME),
	EX_ARENA_RANK_ALL(0x13D, null, ConnectionState.IN_GAME),
	EX_ARENA_MYRANK(0x13E, null, ConnectionState.IN_GAME),
	EX_SWAP_AGATHION_SLOT_ITEMS(0x13F, null, ConnectionState.IN_GAME),
	EX_PLEDGE_CONTRIBUTION_RANK(0x140, null, ConnectionState.IN_GAME),
	EX_PLEDGE_CONTRIBUTION_INFO(0x141, null, ConnectionState.IN_GAME),
	EX_PLEDGE_CONTRIBUTION_REWARD(0x142, null, ConnectionState.IN_GAME),
	EX_PLEDGE_LEVEL_UP(0x143, null, ConnectionState.IN_GAME),
	EX_PLEDGE_MISSION_INFO(0x144, null, ConnectionState.IN_GAME),
	EX_PLEDGE_MISSION_REWARD(0x145, null, ConnectionState.IN_GAME),
	EX_PLEDGE_MASTERY_INFO(0x146, null, ConnectionState.IN_GAME),
	EX_PLEDGE_MASTERY_SET(0x147, null, ConnectionState.IN_GAME),
	EX_PLEDGE_MASTERY_RESET(0x148, null, ConnectionState.IN_GAME),
	EX_PLEDGE_SKILL_INFO(0x149, null, ConnectionState.IN_GAME),
	EX_PLEDGE_SKILL_ACTIVATE(0x14A, null, ConnectionState.IN_GAME),
	EX_PLEDGE_ITEM_LIST(0x14B, null, ConnectionState.IN_GAME),
	EX_PLEDGE_ITEM_ACTIVATE(0x14C, null, ConnectionState.IN_GAME),
	EX_PLEDGE_ANNOUNCE(0x14D, null, ConnectionState.IN_GAME),
	EX_PLEDGE_ANNOUNCE_SET(0x14E, null, ConnectionState.IN_GAME),
	EX_CREATE_PLEDGE(0x14F, null, ConnectionState.IN_GAME),
	EX_PLEDGE_ITEM_INFO(0x150, null, ConnectionState.IN_GAME),
	EX_PLEDGE_ITEM_BUY(0x151, null, ConnectionState.IN_GAME),
	EX_ELEMENTAL_SPIRIT_INFO(0x152, ExElementalSpiritInfo::new, ConnectionState.IN_GAME),
	EX_ELEMENTAL_SPIRIT_EXTRACT_INFO(0x153, ExElementalSpiritExtractInfo::new, ConnectionState.IN_GAME),
	EX_ELEMENTAL_SPIRIT_EXTRACT(0x154, ExElementalSpiritExtract::new, ConnectionState.IN_GAME),
	EX_ELEMENTAL_SPIRIT_EVOLUTION_INFO(0x155, ExElementalSpiritEvolutionInfo::new, ConnectionState.IN_GAME),
	EX_ELEMENTAL_SPIRIT_EVOLUTION(0x156, ExElementalSpiritEvolution::new, ConnectionState.IN_GAME),
	EX_ELEMENTAL_SPIRIT_SET_TALENT(0x157, ExElementalSpiritSetTalent::new, ConnectionState.IN_GAME),
	EX_ELEMENTAL_SPIRIT_INIT_TALENT(0x158, ExElementalInitTalent::new, ConnectionState.IN_GAME),
	EX_ELEMENTAL_SPIRIT_ABSORB_INFO(0x159, ExElementalSpiritAbsorbInfo::new, ConnectionState.IN_GAME),
	EX_ELEMENTAL_SPIRIT_ABSORB(0x15A, ExElementalSpiritAbsorb::new, ConnectionState.IN_GAME),
	EX_REQUEST_LOCKED_ITEM(0x15B, null, ConnectionState.IN_GAME),
	EX_REQUEST_UNLOCKED_ITEM(0x15C, null, ConnectionState.IN_GAME),
	EX_LOCKED_ITEM_CANCEL(0x15D, null, ConnectionState.IN_GAME),
	EX_UNLOCKED_ITEM_CANCEL(0x15E, null, ConnectionState.IN_GAME),
	// 152
	EX_BLOCK_PACKET_FOR_AD(0x15F, null, ConnectionState.IN_GAME),
	EX_USER_BAN_INFO(0x160, null, ConnectionState.IN_GAME),
	EX_INTERACT_MODIFY(0x161, ExInteractModify::new, ConnectionState.IN_GAME),
	EX_TRY_ENCHANT_ARTIFACT(0x162, null, ConnectionState.IN_GAME),
	EX_UPGRADE_SYSTEM_NORMAL_REQUEST(0x163, ExUpgradeSystemNormalRequest::new, ConnectionState.IN_GAME),
	EX_PURCHASE_LIMIT_SHOP_ITEM_LIST(0x164, RequestPurchaseLimitShopItemList::new, ConnectionState.IN_GAME),
	EX_PURCHASE_LIMIT_SHOP_ITEM_BUY(0x165, RequestPurchaseLimitShopItemBuy::new, ConnectionState.IN_GAME),
	// 228
	EX_OPEN_HTML(0x166, ExOpenHtml::new, ConnectionState.IN_GAME),
	EX_REQUEST_CLASS_CHANGE(0x167, ExRequestClassChange::new, ConnectionState.IN_GAME),
	EX_REQUEST_CLASS_CHANGE_VERIFYING(0x168, ExRequestClassChangeVerifying::new, ConnectionState.IN_GAME),
	EX_REQUEST_TELEPORT(0x169, ExRequestTeleport::new, ConnectionState.IN_GAME),
	EX_COSTUME_USE_ITEM(0x16A, null, ConnectionState.IN_GAME),
	EX_COSTUME_LIST(0x16B, null, ConnectionState.IN_GAME),
	EX_COSTUME_COLLECTION_SKILL_ACTIVE(0x16C, null, ConnectionState.IN_GAME),
	EX_COSTUME_EVOLUTION(0x16D, null, ConnectionState.IN_GAME),
	EX_COSTUME_EXTRACT(0x16E, null, ConnectionState.IN_GAME),
	EX_COSTUME_LOCK(0x16F, null, ConnectionState.IN_GAME),
	EX_COSTUME_CHANGE_SHORTCUT(0x170, null, ConnectionState.IN_GAME),
	EX_MAGICLAMP_GAME_INFO(0x171, ExMagicLampGameInfo::new, ConnectionState.IN_GAME),
	EX_MAGICLAMP_GAME_START(0x172, ExMagicLampGameStart::new, ConnectionState.IN_GAME),
	EX_ACTIVATE_AUTO_SHORTCUT(0x173, ExRequestActivateAutoShortcut::new, ConnectionState.IN_GAME),
	EX_PREMIUM_MANAGER_LINK_HTML(0x174, null, ConnectionState.IN_GAME),
	EX_PREMIUM_MANAGER_PASS_CMD_TO_SERVER(0x175, null, ConnectionState.IN_GAME),
	EX_ACTIVATED_CURSED_TREASURE_BOX_LOCATION(0x176, null, ConnectionState.IN_GAME),
	EX_PAYBACK_LIST(0x177, ExPaybackList::new, ConnectionState.IN_GAME),
	EX_PAYBACK_GIVE_REWARD(0x178, ExPaybackGiveReward::new, ConnectionState.IN_GAME),
	EX_AUTOPLAY_SETTING(0x179, ExAutoPlaySetting::new, ConnectionState.IN_GAME),
	EX_OLYMPIAD_MATCH_MAKING(0x17A, OlympiadMatchMaking::new, ConnectionState.IN_GAME),
	EX_OLYMPIAD_MATCH_MAKING_CANCEL(0x17B, OlympiadMatchMakingCancel::new, ConnectionState.IN_GAME),
	EX_FESTIVAL_BM_INFO(0x17C, ExRequestFestivalBmInfo::new, ConnectionState.IN_GAME),
	EX_FESTIVAL_BM_GAME(0x17D, ExRequestFestivalBmGame::new, ConnectionState.IN_GAME),
	EX_GACHA_SHOP_INFO(0x17E, null, ConnectionState.IN_GAME),
	EX_GACHA_SHOP_GACHA_GROUP(0x17F, null, ConnectionState.IN_GAME),
	EX_GACHA_SHOP_GACHA_ITEM(0x180, null, ConnectionState.IN_GAME),
	EX_TIME_RESTRICT_FIELD_LIST(0x181, ExTimedHuntingZoneList::new, ConnectionState.IN_GAME),
	EX_TIME_RESTRICT_FIELD_USER_ENTER(0x182, ExTimedHuntingZoneEnter::new, ConnectionState.IN_GAME),
	EX_TIME_RESTRICT_FIELD_USER_LEAVE(0x183, ExTimedHuntingZoneLeave::new, ConnectionState.IN_GAME),
	EX_RANKING_CHAR_INFO(0x184, RequestRankingCharInfo::new, ConnectionState.IN_GAME),
	EX_RANKING_CHAR_HISTORY(0x185, RequestRankingCharHistory::new, ConnectionState.IN_GAME),
	EX_RANKING_CHAR_RANKERS(0x186, RequestRankingCharRankers::new, ConnectionState.IN_GAME),
	EX_RANKING_CHAR_SPAWN_BUFFZONE_NPC(0x187, RequestExRankingCharSpawnBuffzoneNpc::new, ConnectionState.IN_GAME),
	EX_RANKING_CHAR_BUFFZONE_NPC_POSITION(0x188, RequestExRankingCharBuffzoneNpcPosition::new, ConnectionState.IN_GAME),
	EX_PLEDGE_MERCENARY_RECRUIT_INFO_SET(0x189, ExPledgeMercenaryRecruitInfoSet::new, ConnectionState.IN_GAME),
	EX_MERCENARY_CASTLEWAR_CASTLE_INFO(0x18A, ExMercenaryCastleWarCastleSiegeInfo::new, ConnectionState.IN_GAME),
	EX_MERCENARY_CASTLEWAR_CASTLE_SIEGE_INFO(0x18B, ExMercenaryCastleWarCastleSiegeHudInfo::new, ConnectionState.IN_GAME),
	EX_MERCENARY_CASTLEWAR_CASTLE_SIEGE_ATTACKER_LIST(0x18C, ExMercenaryCastleWarCastleSiegeAttackerList::new, ConnectionState.IN_GAME),
	EX_MERCENARY_CASTLEWAR_CASTLE_SIEGE_DEFENDER_LIST(0x18D, ExMercenaryCastleWarCastleSiegeDefenderList::new, ConnectionState.IN_GAME),
	EX_PLEDGE_MERCENARY_MEMBER_LIST(0x18E, ExPledgeMercenaryMemberList::new, ConnectionState.IN_GAME),
	EX_PLEDGE_MERCENARY_MEMBER_JOIN(0x18F, ExPledgeMercenaryMemberJoin::new, ConnectionState.IN_GAME),
	EX_PVPBOOK_LIST(0x190, ExPvpBookList::new, ConnectionState.IN_GAME),
	EX_PVPBOOK_KILLER_LOCATION(0x191, null, ConnectionState.IN_GAME),
	EX_PVPBOOK_TELEPORT_TO_KILLER(0x192, null, ConnectionState.IN_GAME),
	EX_LETTER_COLLECTOR_TAKE_REWARD(0x193, ExLetterCollectorTakeReward::new, ConnectionState.IN_GAME),
	EX_SET_STATUS_BONUS(0x194, ExSetStatusBonus::new, ConnectionState.IN_GAME),
	EX_RESET_STATUS_BONUS(0x195, ExResetStatusBonus::new, ConnectionState.IN_GAME),
	EX_OLYMPIAD_MY_RANKING_INFO(0x196, RequestOlympiadMyRankingInfo::new, ConnectionState.IN_GAME),
	EX_OLYMPIAD_RANKING_INFO(0x197, RequestOlympiadRankingInfo::new, ConnectionState.IN_GAME),
	EX_OLYMPIAD_HERO_AND_LEGEND_INFO(0x198, RequestOlympiadHeroAndLegendInfo::new, ConnectionState.IN_GAME),
	EX_CASTLEWAR_OBSERVER_START(0x199, ExCastleWarObserverStart::new, ConnectionState.IN_GAME),
	EX_RAID_TELEPORT_INFO(0x19A, RequestRaidTeleportInfo::new, ConnectionState.IN_GAME),
	EX_TELEPORT_TO_RAID_POSITION(0x19B, ExTeleportToRaidPosition::new, ConnectionState.IN_GAME),
	EX_CRAFT_EXTRACT(0x19C, ExRequestRandomCraftExtract::new, ConnectionState.IN_GAME),
	EX_CRAFT_RANDOM_INFO(0x19D, ExRequestRandomCraftInfo::new, ConnectionState.IN_GAME),
	EX_CRAFT_RANDOM_LOCK_SLOT(0x19E, ExRequestRandomCraftLockSlot::new, ConnectionState.IN_GAME),
	EX_CRAFT_RANDOM_REFRESH(0x19F, ExRequestRandomCraftRefresh::new, ConnectionState.IN_GAME),
	EX_CRAFT_RANDOM_MAKE(0x1A0, ExRequestRandomCraftMake::new, ConnectionState.IN_GAME),
	EX_MULTI_SELL_LIST(0x1A1, RequestMultisellList::new, ConnectionState.IN_GAME),
	EX_SAVE_ITEM_ANNOUNCE_SETTING(0x1A2, ExSaveItemAnnounceSetting::new, ConnectionState.IN_GAME),
	EX_OLYMPIAD_UI(0x1A3, OlympiadUI::new, ConnectionState.IN_GAME),
	// 270
	EX_SHARED_POSITION_SHARING_UI(0x1A4, ExRequestSharingLocationUi::new, ConnectionState.IN_GAME),
	EX_SHARED_POSITION_TELEPORT_UI(0x1A5, ExRequestSharedLocationTeleportUi::new, ConnectionState.IN_GAME),
	EX_SHARED_POSITION_TELEPORT(0x1A6, ExRequestSharedLocationTeleport::new, ConnectionState.IN_GAME),
	EX_AUTH_RECONNECT(0x1A7, null, ConnectionState.IN_GAME),
	EX_PET_EQUIP_ITEM(0x1A8, ExPetEquipItem::new, ConnectionState.IN_GAME),
	EX_PET_UNEQUIP_ITEM(0x1A9, ExPetUnequipItem::new, ConnectionState.IN_GAME),
	EX_SHOW_HOMUNCULUS_INFO(0x1AA, null, ConnectionState.IN_GAME),
	EX_HOMUNCULUS_CREATE_START(0x1AB, null, ConnectionState.IN_GAME),
	EX_HOMUNCULUS_INSERT(0x1AC, null, ConnectionState.IN_GAME),
	EX_HOMUNCULUS_SUMMON(0x1AD, null, ConnectionState.IN_GAME),
	EX_DELETE_HOMUNCULUS_DATA(0x1AE, null, ConnectionState.IN_GAME),
	EX_REQUEST_ACTIVATE_HOMUNCULUS(0x1AF, null, ConnectionState.IN_GAME),
	EX_HOMUNCULUS_GET_ENCHANT_POINT(0x1B0, null, ConnectionState.IN_GAME),
	EX_HOMUNCULUS_INIT_POINT(0x1B1, null, ConnectionState.IN_GAME),
	EX_EVOLVE_PET(0x1B2, ExEvolvePet::new, ConnectionState.IN_GAME),
	EX_ENCHANT_HOMUNCULUS_SKILL(0x1B3, null, ConnectionState.IN_GAME),
	EX_HOMUNCULUS_ENCHANT_EXP(0x1B4, null, ConnectionState.IN_GAME),
	EX_TELEPORT_FAVORITES_LIST(0x1B5, ExRequestTeleportFavoriteList::new, ConnectionState.IN_GAME),
	EX_TELEPORT_FAVORITES_UI_TOGGLE(0x1B6, ExRequestTeleportFavoritesUIToggle::new, ConnectionState.IN_GAME),
	EX_TELEPORT_FAVORITES_ADD_DEL(0x1B7, ExRequestTeleportFavoritesAddDel::new, ConnectionState.IN_GAME),
	EX_ANTIBOT(0x1B8, null, ConnectionState.IN_GAME),
	EX_DPSVR(0x1B9, null, ConnectionState.IN_GAME),
	EX_TENPROTECT_DECRYPT_ERROR(0x1BA, null, ConnectionState.IN_GAME),
	EX_NET_LATENCY(0x1BB, null, ConnectionState.IN_GAME),
	EX_MABLE_GAME_OPEN(0x1BC, ExRequestMableGameOpen::new, ConnectionState.IN_GAME),
	EX_MABLE_GAME_ROLL_DICE(0x1BD, ExRequestMableGameRollDice::new, ConnectionState.IN_GAME),
	EX_MABLE_GAME_POPUP_OK(0x1BE, ExRequestMableGamePopupOk::new, ConnectionState.IN_GAME),
	EX_MABLE_GAME_RESET(0x1BF, ExRequestMableGameReset::new, ConnectionState.IN_GAME),
	EX_MABLE_GAME_CLOSE(0x1C0, ExRequestMableGameClose::new, ConnectionState.IN_GAME),
	EX_RETURN_TO_ORIGIN(0x1C1, null, ConnectionState.IN_GAME),
	EX_PK_PENALTY_LIST(0x1C2, RequestExPkPenaltyList::new, ConnectionState.IN_GAME),
	EX_PK_PENALTY_LIST_ONLY_LOC(0x1C3, RequestExPkPenaltyListOnlyLoc::new, ConnectionState.IN_GAME),
	EX_BLESS_OPTION_PUT_ITEM(0x1C4, RequestBlessOptionPutItem::new, ConnectionState.IN_GAME),
	EX_BLESS_OPTION_ENCHANT(0x1C5, RequestBlessOptionEnchant::new, ConnectionState.IN_GAME),
	EX_BLESS_OPTION_CANCEL(0x1C6, RequestBlessOptionCancel::new, ConnectionState.IN_GAME),
	EX_PVP_RANKING_MY_INFO(0x1C7, RequestPvpRankingMyInfo::new, ConnectionState.IN_GAME),
	EX_PVP_RANKING_LIST(0x1C8, RequestPvpRankingList::new, ConnectionState.IN_GAME),
	EX_ACQUIRE_PET_SKILL(0x1C9, RequestExAcquirePetSkill::new, ConnectionState.IN_GAME),
	EX_PLEDGE_V3_INFO(0x1CA, RequestExPledgeV3Info::new, ConnectionState.IN_GAME),
	EX_PLEDGE_ENEMY_INFO_LIST(0x1CB, RequestExPledgeEnemyInfoList::new, ConnectionState.IN_GAME),
	EX_PLEDGE_ENEMY_REGISTER(0x1CC, RequestExPledgeEnemyRegister::new, ConnectionState.IN_GAME),
	EX_PLEDGE_ENEMY_DELETE(0x1CD, RequestExPledgeEnemyDelete::new, ConnectionState.IN_GAME),
	EX_TRY_PET_EXTRACT_SYSTEM(0x1CE, ExTryPetExtractSystem::new, ConnectionState.IN_GAME),
	EX_PLEDGE_V3_SET_ANNOUNCE(0x1CF, RequestExPledgeV3SetAnnounce::new, ConnectionState.IN_GAME),
	// 306
	EX_RANKING_FESTIVAL_OPEN(0x1D0, null, ConnectionState.IN_GAME),
	EX_RANKING_FESTIVAL_BUY(0x1D1, null, ConnectionState.IN_GAME),
	EX_RANKING_FESTIVAL_BONUS(0x1D2, null, ConnectionState.IN_GAME),
	EX_RANKING_FESTIVAL_RANKING(0x1D3, null, ConnectionState.IN_GAME),
	EX_RANKING_FESTIVAL_MY_RECEIVED_BONUS(0x1D4, null, ConnectionState.IN_GAME),
	EX_RANKING_FESTIVAL_REWARD(0x1D5, null, ConnectionState.IN_GAME),
	EX_TIMER_CHECK(0x1D6, null, ConnectionState.IN_GAME),
	EX_STEADY_BOX_LOAD(0x1D7, RequestSteadyBoxLoad::new, ConnectionState.IN_GAME),
	EX_STEADY_OPEN_SLOT(0x1D8, RequestSteadyOpenSlot::new, ConnectionState.IN_GAME),
	EX_STEADY_OPEN_BOX(0x1D9, RequestSteadyOpenBox::new, ConnectionState.IN_GAME),
	EX_STEADY_GET_REWARD(0x1DA, RequestSteadyGetReward::new, ConnectionState.IN_GAME),
	EX_PET_RANKING_MY_INFO(0x1DB, RequestPetRankingMyInfo::new, ConnectionState.IN_GAME),
	EX_PET_RANKING_LIST(0x1DC, RequestPetRankingList::new, ConnectionState.IN_GAME),
	EX_COLLECTION_OPEN_UI(0x1DD, RequestExCollectionOpenUI::new, ConnectionState.IN_GAME),
	EX_COLLECTION_CLOSE_UI(0x1DE, RequestCollectionCloseUI::new, ConnectionState.IN_GAME),
	EX_COLLECTION_LIST(0x1DF, RequestExCollectionList::new, ConnectionState.IN_GAME),
	EX_COLLECTION_UPDATE_FAVORITE(0x1E0, RequestCollectionUpdateFavorite::new, ConnectionState.IN_GAME),
	EX_COLLECTION_FAVORITE_LIST(0x1E1, RequestCollectionFavoriteList::new, ConnectionState.IN_GAME),
	EX_COLLECTION_SUMMARY(0x1E2, RequestExCollectionSummary::new, ConnectionState.IN_GAME),
	EX_COLLECTION_REGISTER(0x1E3, RequestCollectionRegister::new, ConnectionState.IN_GAME),
	EX_COLLECTION_RECEIVE_REWARD(0x1E4, RequestCollectionReceiveReward::new, ConnectionState.IN_GAME),
	EX_PVPBOOK_SHARE_REVENGE_LIST(0x1E5, RequestExPvpBookShareRevengeList::new, ConnectionState.IN_GAME),
	EX_PVPBOOK_SHARE_REVENGE_REQ_SHARE_REVENGEINFO(0x1E6, RequestExPvpBookShareRevengeReqShareRevengeInfo::new, ConnectionState.IN_GAME),
	EX_PVPBOOK_SHARE_REVENGE_KILLER_LOCATION(0x1E7, RequestExPvpBookShareRevengeKillerLocation::new, ConnectionState.IN_GAME),
	EX_PVPBOOK_SHARE_REVENGE_TELEPORT_TO_KILLER(0x1E8, RequestExPvpBookShareRevengeTeleportToKiller::new, ConnectionState.IN_GAME),
	EX_PVPBOOK_SHARE_REVENGE_SHARED_TELEPORT_TO_KILLER(0x1E9, RequestExPvpBookShareRevengeSharedTeleportToKiller::new, ConnectionState.IN_GAME),
	EX_PENALTY_ITEM_LIST(0x1EA, ExRequestPenaltyItemList::new, ConnectionState.IN_GAME),
	EX_PENALTY_ITEM_RESTORE(0x1EB, ExRequestPenaltyItemRestore::new, ConnectionState.IN_GAME),
	EX_USER_WATCHER_TARGET_LIST(0x1EC, ExRequestUserWatcherTargetList::new, ConnectionState.IN_GAME),
	EX_USER_WATCHER_ADD(0x1ED, ExRequestUserWatcherAdd::new, ConnectionState.IN_GAME),
	EX_USER_WATCHER_DELETE(0x1EE, ExRequestUserWatcherDelete::new, ConnectionState.IN_GAME),
	EX_HOMUNCULUS_ACTIVATE_SLOT(0x1EF, null, ConnectionState.IN_GAME),
	EX_SUMMON_HOMUNCULUS_COUPON(0x1F0, null, ConnectionState.IN_GAME),
	EX_SUBJUGATION_LIST(0x1F1, RequestSubjugationList::new, ConnectionState.IN_GAME),
	EX_SUBJUGATION_RANKING(0x1F2, RequestSubjugationRanking::new, ConnectionState.IN_GAME),
	EX_SUBJUGATION_GACHA_UI(0x1F3, RequestSubjugationGachaUI::new, ConnectionState.IN_GAME),
	EX_SUBJUGATION_GACHA(0x1F4, RequestSubjugationGacha::new, ConnectionState.IN_GAME),
	EX_PLEDGE_DONATION_INFO(0x1F5, RequestExPledgeDonationInfo::new, ConnectionState.IN_GAME),
	EX_PLEDGE_DONATION_REQUEST(0x1F6, RequestExPledgeDonationRequest::new, ConnectionState.IN_GAME),
	EX_PLEDGE_CONTRIBUTION_LIST(0x1F7, RequestExPledgeContributionList::new, ConnectionState.IN_GAME),
	EX_PLEDGE_RANKING_MY_INFO(0x1F8, RequestPledgeRankingMyInfo::new, ConnectionState.IN_GAME),
	EX_PLEDGE_RANKING_LIST(0x1F9, RequestPledgeRankingList::new, ConnectionState.IN_GAME),
	EX_ITEM_RESTORE_LIST(0x1FA, RequestItemRestoreList::new, ConnectionState.IN_GAME),
	EX_ITEM_RESTORE(0x1FB, RequestItemRestore::new, ConnectionState.IN_GAME),
	// 338
	EX_DETHRONE_INFO(0x1FC, null, ConnectionState.IN_GAME),
	EX_DETHRONE_RANKING_INFO(0x1FD, null, ConnectionState.IN_GAME),
	EX_DETHRONE_SERVER_INFO(0x1FE, null, ConnectionState.IN_GAME),
	EX_DETHRONE_DISTRICT_OCCUPATION_INFO(0x1FF, null, ConnectionState.IN_GAME),
	EX_DETHRONE_DAILY_MISSION_INFO(0x200, null, ConnectionState.IN_GAME),
	EX_DETHRONE_DAILY_MISSION_GET_REWARD(0x201, null, ConnectionState.IN_GAME),
	EX_DETHRONE_PREV_SEASON_INFO(0x202, null, ConnectionState.IN_GAME),
	EX_DETHRONE_GET_REWARD(0x203, null, ConnectionState.IN_GAME),
	EX_DETHRONE_ENTER(0x204, null, ConnectionState.IN_GAME),
	EX_DETHRONE_LEAVE(0x205, null, ConnectionState.IN_GAME),
	EX_DETHRONE_CHECK_NAME(0x206, null, ConnectionState.IN_GAME),
	EX_DETHRONE_CHANGE_NAME(0x207, null, ConnectionState.IN_GAME),
	EX_DETHRONE_CONNECT_CASTLE(0x208, null, ConnectionState.IN_GAME),
	EX_DETHRONE_DISCONNECT_CASTLE(0x209, null, ConnectionState.IN_GAME),
	EX_CHANGE_NICKNAME_COLOR_ICON(0x20A, RequestChangeNicknameEmote::new, ConnectionState.IN_GAME),
	EX_WORLDCASTLEWAR_MOVE_TO_HOST(0x20B, null, ConnectionState.IN_GAME),
	EX_WORLDCASTLEWAR_RETURN_TO_ORIGIN_PEER(0x20C, null, ConnectionState.IN_GAME),
	EX_WORLDCASTLEWAR_CASTLE_INFO(0x20D, null, ConnectionState.IN_GAME),
	EX_WORLDCASTLEWAR_CASTLE_SIEGE_INFO(0x20E, null, ConnectionState.IN_GAME),
	EX_WORLDCASTLEWAR_CASTLE_SIEGE_JOIN(0x20F, null, ConnectionState.IN_GAME),
	EX_WORLDCASTLEWAR_CASTLE_SIEGE_ATTACKER_LIST(0x210, null, ConnectionState.IN_GAME),
	EX_WORLDCASTLEWAR_PLEDGE_MERCENARY_RECRUIT_INFO_SET(0x211, null, ConnectionState.IN_GAME),
	EX_WORLDCASTLEWAR_PLEDGE_MERCENARY_MEMBER_LIST(0x212, null, ConnectionState.IN_GAME),
	EX_WORLDCASTLEWAR_PLEDGE_MERCENARY_MEMBER_JOIN(0x213, null, ConnectionState.IN_GAME),
	EX_WORLDCASTLEWAR_TELEPORT(0x214, null, ConnectionState.IN_GAME),
	EX_WORLDCASTLEWAR_OBSERVER_START(0x215, null, ConnectionState.IN_GAME),
	EX_PRIVATE_STORE_SEARCH_LIST(0x216, ExRequestPrivateStoreSearchList::new, ConnectionState.IN_GAME),
	EX_PRIVATE_STORE_SEARCH_STATISTICS(0x217, ExRequestPrivateStoreSearchStatistics::new, ConnectionState.IN_GAME),
	EX_WORLDCASTLEWAR_HOST_CASTLE_SIEGE_RANKING_INFO(0x218, null, ConnectionState.IN_GAME),
	EX_WORLDCASTLEWAR_CASTLE_SIEGE_RANKING_INFO(0x219, null, ConnectionState.IN_GAME),
	EX_WORLDCASTLEWAR_SIEGE_MAINBATTLE_HUD_INFO(0x21A, null, ConnectionState.IN_GAME),
	EX_NEW_HENNA_LIST(0x21B, RequestNewHennaList::new, ConnectionState.IN_GAME),
	EX_NEW_HENNA_EQUIP(0x21C, RequestNewHennaEquip::new, ConnectionState.IN_GAME),
	EX_NEW_HENNA_UNEQUIP(0x21D, RequestNewHennaUnequip::new, ConnectionState.IN_GAME),
	EX_NEW_HENNA_POTEN_SELECT(0x21E, RequestNewHennaPotenSelect::new, ConnectionState.IN_GAME),
	EX_NEW_HENNA_POTEN_ENCHANT(0x21F, RequestNewHennaPotenEnchant::new, ConnectionState.IN_GAME),
	EX_NEW_HENNA_COMPOSE(0x220, RequestNewHennaCompose::new, ConnectionState.IN_GAME),
	EX_REQUEST_INVITE_PARTY(0x221, RequestNewInvitePartyInquiry::new, ConnectionState.IN_GAME),
	EX_ITEM_USABLE_LIST(0x222, null, ConnectionState.IN_GAME),
	EX_PACKETREADCOUNTPERSECOND(0x223, null, ConnectionState.IN_GAME),
	EX_SELECT_GLOBAL_EVENT_UI(0x224, null, ConnectionState.IN_GAME),
	EX_L2PASS_INFO(0x225, RequestHuntPassInfo::new, ConnectionState.IN_GAME),
	EX_L2PASS_REQUEST_REWARD(0x226, RequestHuntPassReward::new, ConnectionState.IN_GAME),
	EX_L2PASS_REQUEST_REWARD_ALL(0x227, RequestHuntPassRewardAll::new, ConnectionState.IN_GAME),
	EX_L2PASS_BUY_PREMIUM(0x228, RequestHuntPassBuyPremium::new, ConnectionState.IN_GAME),
	EX_SAYHAS_SUPPORT_TOGGLE(0x229, HuntpassSayhasToggle::new, ConnectionState.IN_GAME),
	// 362
	EX_REQ_ENCHANT_FAIL_REWARD_INFO(0x22A, ExRequestEnchantFailRewardInfo::new, ConnectionState.IN_GAME),
	EX_SET_ENCHANT_CHALLENGE_POINT(0x22B, ExRequestSetEnchantChallengePoint::new, ConnectionState.IN_GAME),
	EX_RESET_ENCHANT_CHALLENGE_POINT(0x22C, ExRequestResetEnchantChallengePoint::new, ConnectionState.IN_GAME),
	EX_REQ_VIEW_ENCHANT_RESULT(0x22D, ExRequestViewEnchantResult::new, ConnectionState.IN_GAME),
	EX_REQ_START_MULTI_ENCHANT_SCROLL(0x22E, ExRequestStartMultiEnchantScroll::new, ConnectionState.IN_GAME),
	EX_REQ_VIEW_MULTI_ENCHANT_RESULT(0x22F, ExRequestViewMultiEnchantResult::new, ConnectionState.IN_GAME),
	EX_REQ_FINISH_MULTI_ENCHANT_SCROLL(0x230, ExRequestFinishMultiEnchantScroll::new, ConnectionState.IN_GAME),
	EX_REQ_CHANGE_MULTI_ENCHANT_SCROLL(0x231, null, ConnectionState.IN_GAME),
	EX_REQ_SET_MULTI_ENCHANT_ITEM_LIST(0x232, ExRequestSetMultiEnchantItemList::new, ConnectionState.IN_GAME),
	EX_REQ_MULTI_ENCHANT_ITEM_LIST(0x233, ExRequestMultiEnchantItemList::new, ConnectionState.IN_GAME),
	EX_WORLDCASTLEWAR_SUPPORT_PLEDGE_FLAG_SET(0x234, null, ConnectionState.IN_GAME),
	EX_WORLDCASTLEWAR_SUPPORT_PLEDGE_INFO_SET(0x235, null, ConnectionState.IN_GAME),
	EX_REQ_HOMUNCULUS_PROB_LIST(0x236, null, ConnectionState.IN_GAME),
	EX_WORLDCASTLEWAR_HOST_CASTLE_SIEGE_ALL_RANKING_INFO(0x237, null, ConnectionState.IN_GAME),
	EX_WORLDCASTLEWAR_CASTLE_SIEGE_ALL_RANKING_INFO(0x238, null, ConnectionState.IN_GAME),
	EX_MISSION_LEVEL_REWARD_LIST(0x239, RequestMissionRewardList::new, ConnectionState.IN_GAME),
	EX_MISSION_LEVEL_RECEIVE_REWARD(0x23A, RequestMissionLevelReceiveReward::new, ConnectionState.IN_GAME),
	EX_MISSION_LEVEL_JUMP_LEVEL(0x23B, RequestMissionLevelJumpLevel::new, ConnectionState.IN_GAME),
	EX_BALROGWAR_TELEPORT(0x23C, ExBalrogWarTeleport::new, ConnectionState.IN_GAME),
	EX_BALROGWAR_SHOW_UI(0x23D, ExBalrogWarShowUI::new, ConnectionState.IN_GAME),
	EX_BALROGWAR_SHOW_RANKING(0x23E, ExBalrogWarShowRanking::new, ConnectionState.IN_GAME),
	EX_BALROGWAR_GET_REWARD(0x23F, ExBalrogWarGetReward::new, ConnectionState.IN_GAME),
	EX_USER_RESTART_LOCKER_UPDATE(0x240, null, ConnectionState.IN_GAME),
	EX_WORLD_EXCHANGE_ITEM_LIST(0x241, ExWorldExchangeItemList::new, ConnectionState.IN_GAME),
	EX_WORLD_EXCHANGE_REGI_ITEM(0x242, ExWorldExchangeRegisterItem::new, ConnectionState.IN_GAME),
	EX_WORLD_EXCHANGE_BUY_ITEM(0x243, ExWorldExchangeBuyItem::new, ConnectionState.IN_GAME),
	EX_WORLD_EXCHANGE_SETTLE_LIST(0x244, ExWorldExchangeSettleList::new, ConnectionState.IN_GAME),
	EX_WORLD_EXCHANGE_SETTLE_RECV_RESULT(0x245, ExWorldExchangeSettleRecvResult::new, ConnectionState.IN_GAME),
	EX_READY_ITEM_AUTO_PEEL(0x246, ExRequestReadyItemAutoPeel::new, ConnectionState.IN_GAME),
	EX_REQUEST_ITEM_AUTO_PEEL(0x247, ExRequestItemAutoPeel::new, ConnectionState.IN_GAME),
	EX_STOP_ITEM_AUTO_PEEL(0x248, ExRequestStopItemAutoPeel::new, ConnectionState.IN_GAME),
	EX_VARIATION_OPEN_UI(0x249, ExVariationOpenUi::new, ConnectionState.IN_GAME),
	EX_VARIATION_CLOSE_UI(0x24A, ExVariationCloseUi::new, ConnectionState.IN_GAME),
	EX_APPLY_VARIATION_OPTION(0x24B, ExApplyVariationOption::new, ConnectionState.IN_GAME),
	EX_REQUEST_AUDIO_LOG_SAVE(0x24C, null, ConnectionState.IN_GAME),
	EX_BR_VERSION(0x24D, RequestBRVersion::new, ConnectionState.AUTHENTICATED, ConnectionState.CONNECTED),
	// 388
	EX_WRANKING_FESTIVAL_INFO(0x24E, null, ConnectionState.IN_GAME),
	EX_WRANKING_FESTIVAL_OPEN(0x24F, null, ConnectionState.IN_GAME),
	EX_WRANKING_FESTIVAL_BUY(0x250, null, ConnectionState.IN_GAME),
	EX_WRANKING_FESTIVAL_BONUS(0x251, null, ConnectionState.IN_GAME),
	EX_WRANKING_FESTIVAL_RANKING(0x252, null, ConnectionState.IN_GAME),
	EX_WRANKING_FESTIVAL_MY_RECEIVED_BONUS(0x253, null, ConnectionState.IN_GAME),
	EX_WRANKING_FESTIVAL_REWARD(0x254, null, ConnectionState.IN_GAME),
	EX_HENNA_UNEQUIP_INFO(0x255, RequestNewHennaUnequipInfo::new, ConnectionState.IN_GAME),
	EX_HERO_BOOK_CHARGE(0x256, null, ConnectionState.IN_GAME),
	EX_HERO_BOOK_ENCHANT(0x257, null, ConnectionState.IN_GAME),
	EX_HERO_BOOK_CHARGE_PROB(0x258, null, ConnectionState.IN_GAME),
	EX_TELEPORT_UI(0x259, RequestExTeleportUI::new, ConnectionState.IN_GAME),
	EX_GOODS_GIFT_LIST_INFO(0x25A, null, ConnectionState.IN_GAME),
	EX_GOODS_GIFT_ACCEPT(0x25B, null, ConnectionState.IN_GAME),
	EX_GOODS_GIFT_REFUSE(0x25C, null, ConnectionState.IN_GAME),
	EX_WORLD_EXCHANGE_AVERAGE_PRICE(0x25D, ExWorldExchangeAveragePrice::new, ConnectionState.IN_GAME),
	EX_WORLD_EXCHANGE_TOTAL_LIST(0x25E, ExWorldExchangeTotalList::new, ConnectionState.IN_GAME),
	EX_PRISON_USER_INFO(0x25F, RequestPrisonUserInfo::new, ConnectionState.IN_GAME),
	EX_PRISON_USER_DONATION(0x260, RequestPrisonUserDonation::new, ConnectionState.IN_GAME),
	// 414
	EX_TRADE_LIMIT_INFO(0x261, null, ConnectionState.IN_GAME),
	EX_UNIQUE_GACHA_OPEN(0x262, ExUniqueGachaOpen::new, ConnectionState.IN_GAME),
	EX_UNIQUE_GACHA_GAME(0x263, ExUniqueGachaGame::new, ConnectionState.IN_GAME),
	EX_UNIQUE_GACHA_INVEN_ITEM_LIST(0x264, ExUniqueGachaInvenItemList::new, ConnectionState.IN_GAME),
	EX_UNIQUE_GACHA_INVEN_GET_ITEM(0x265, ExUniqueGachaInvenGetItem::new, ConnectionState.IN_GAME),
	EX_UNIQUE_GACHA_HISTORY(0x266, ExUniqueGachaHistory::new, ConnectionState.IN_GAME),
	EX_SET_PLEDGE_CREST_PRESET(0x267, RequestSetPledgeCrestPreset::new, ConnectionState.IN_GAME),
	EX_GET_PLEDGE_CREST_PRESET(0x268, RequestGetPledgeCrestPreset::new, ConnectionState.IN_GAME),
	EX_DUAL_INVENTORY_SWAP(0x269, RequestExDualInventorySwap::new, ConnectionState.IN_GAME),
	EX_SP_EXTRACT_INFO(0x26A, RequestExSpExtractInfo::new, ConnectionState.IN_GAME),
	EX_SP_EXTRACT_ITEM(0x26B, RequestExSpExtractItem::new, ConnectionState.IN_GAME),
	EX_QUEST_TELEPORT(0x26C, RequestExQuestTeleport::new, ConnectionState.IN_GAME),
	EX_QUEST_ACCEPT(0x26D, RequestExQuestAccept::new, ConnectionState.IN_GAME),
	EX_QUEST_CANCEL(0x26E, RequestExQuestCancel::new, ConnectionState.IN_GAME),
	EX_QUEST_COMPLETE(0x26F, RequestExQuestComplete::new, ConnectionState.IN_GAME),
	EX_QUEST_NOTIFICATION_ALL(0x270, RequestExQuestNotificationAll::new, ConnectionState.IN_GAME),
	EX_QUEST_UI(0x271, RequestExQuestUI::new, ConnectionState.IN_GAME),
	EX_QUEST_ACCEPTABLE_LIST(0x272, RequestExQuestAcceptableList::new, ConnectionState.IN_GAME),
	EX_SKILL_ENCHANT_INFO(0x273, RequestExSkillEnchantInfo::new, ConnectionState.IN_GAME),
	EX_SKILL_ENCHANT_CHARGE(0x274, RequestExSkillEnchantCharge::new, ConnectionState.IN_GAME),
	EX_TIME_RESTRICT_FIELD_HOST_USER_ENTER(0x275, null, ConnectionState.IN_GAME),
	EX_TIME_RESTRICT_FIELD_HOST_USER_LEAVE(0x276, null, ConnectionState.IN_GAME),
	EX_DETHRONE_SHOP_OPEN_UI(0x277, null, ConnectionState.IN_GAME),
	EX_DETHRONE_SHOP_BUY(0x278, null, ConnectionState.IN_GAME),
	EX_ENHANCED_ABILITY_OF_FIRE_OPEN_UI(0x279, null, ConnectionState.IN_GAME),
	EX_ENHANCED_ABILITY_OF_FIRE_INIT(0x27A, null, ConnectionState.IN_GAME),
	EX_ENHANCED_ABILITY_OF_FIRE_EXP_UP(0x27B, null, ConnectionState.IN_GAME),
	EX_ENHANCED_ABILITY_OF_FIRE_LEVEL_UP(0x27C, null, ConnectionState.IN_GAME),
	EX_HOLY_FIRE_OPEN_UI(0x27D, null, ConnectionState.IN_GAME),
	EX_PRIVATE_STORE_BUY_SELL(0x27E, null, ConnectionState.IN_GAME),
	// 430
	EX_VIP_ATTENDANCE_LIST(0x27F, RequestVipAttendanceItemList::new, ConnectionState.IN_GAME),
	EX_VIP_ATTENDANCE_CHECK(0x280, RequestVipAttendanceCheck::new, ConnectionState.IN_GAME),
	EX_VIP_ATTENDANCE_REWARD(0x281, RequestVipAttendanceItemReward::new, ConnectionState.IN_GAME),
	EX_CHANGE_ABILITY_PRESET(0x282, null, ConnectionState.IN_GAME),
	EX_NEW_HENNA_POTEN_ENCHANT_RESET(0x283, ExRequestNewHennaEnchantReset::new, ConnectionState.IN_GAME),
	EX_INZONE_RANKING_MY_INFO(0x284, null, ConnectionState.IN_GAME),
	EX_INZONE_RANKING_LIST(0x285, null, ConnectionState.IN_GAME),
	EX_TIME_RESTRICT_FIELD_HOST_USER_ENTER_BY_NPC(0x286, null, ConnectionState.IN_GAME),
	// 439
	EX_PREPARE_LOGIN(0x287, null, ConnectionState.IN_GAME),
	// 447
	EX_RELICS_OPEN_UI(0x288, RequestRelicsOpenUI::new, ConnectionState.IN_GAME),
	EX_RELICS_CLOSE_UI(0x289, RequestRelicsCloseUI::new, ConnectionState.IN_GAME),
	EX_RELICS_SUMMON_CLOSE_UI(0x28A, RequestRelicsSummonCloseUI::new, ConnectionState.IN_GAME),
	EX_RELICS_ACTIVE(0x28B, RequestRelicsActive::new, ConnectionState.IN_GAME),
	EX_RELICS_SUMMON(0x28C, RequestRelicsSummon::new, ConnectionState.IN_GAME),
	EX_RELICS_EXCHANGE(0x28D, null, ConnectionState.IN_GAME),
	EX_RELICS_EXCHANGE_CONFIRM(0x28E, null, ConnectionState.IN_GAME),
	EX_RELICS_UPGRADE(0x28F, RequestRelicsUpgrade::new, ConnectionState.IN_GAME),
	EX_RELICS_COMBINATION(0x290, RequestRelicsCombination::new, ConnectionState.IN_GAME),
	EX_SERVERWAR_FIELD_ENTER_USER_INFO(0x291, null, ConnectionState.IN_GAME),
	EX_SERVERWAR_MOVE_TO_HOST(0x292, null, ConnectionState.IN_GAME),
	EX_SERVERWAR_BATTLE_HUD_INFO(0x293, null, ConnectionState.IN_GAME),
	EX_SERVERWAR_LEADER_LIST(0x294, null, ConnectionState.IN_GAME),
	EX_SERVERWAR_SELECT_LEADER(0x295, null, ConnectionState.IN_GAME),
	EX_SERVERWAR_SELECT_LEADER_INFO(0x296, null, ConnectionState.IN_GAME),
	EX_SERVERWAR_MOVE_TO_LEADER_CAMP(0x297, null, ConnectionState.IN_GAME),
	EX_SERVERWAR_REWARD_ITEM_INFO(0x298, null, ConnectionState.IN_GAME),
	EX_SERVERWAR_REWARD_INFO(0x299, null, ConnectionState.IN_GAME),
	EX_SERVERWAR_GET_REWARD(0x29A, null, ConnectionState.IN_GAME),
	EX_RELICS_COMBINATION_COMPLETE(0x29B, RequestRelicsCombinationComplete::new, ConnectionState.IN_GAME),
	EX_VIRTUALITEM_SYSTEM(0x29C, null, ConnectionState.IN_GAME),
	// 464
	EX_CROSS_EVENT_DATA(0x29D, RequestCrossEventData::new, ConnectionState.IN_GAME),
	EX_CROSS_EVENT_INFO(0x29E, RequestCrossEventInfo::new, ConnectionState.IN_GAME),
	EX_CROSS_EVENT_NORMAL_REWARD(0x29F, RequestCrossEventNormalReward::new, ConnectionState.IN_GAME),
	EX_CROSS_EVENT_RARE_REWARD(0x2A0, RequestCrossEventRareReward::new, ConnectionState.IN_GAME),
	EX_CROSS_EVENT_RESET(0x2A1, RequestCrossEventReset::new, ConnectionState.IN_GAME),
	EX_ADENLAB_BOSS_LIST(0x2A2, RequestAdenLabBossList::new, ConnectionState.IN_GAME),
	EX_ADENLAB_UNLOCK_BOSS(0x2A3, RequestAdenLabBossUnlock::new, ConnectionState.IN_GAME),
	EX_ADENLAB_BOSS_INFO(0x2A4, RequestAdenLabBossInfo::new, ConnectionState.IN_GAME),
	EX_ADENLAB_NORMAL_SLOT(0x2A5, RequestAdenLabNormalSlot::new, ConnectionState.IN_GAME),
	EX_ADENLAB_NORMAL_PLAY(0x2A6, RequestAdenLabNormalPlay::new, ConnectionState.IN_GAME),
	EX_ADENLAB_SPECIAL_SLOT(0x2A7, RequestAdenLabSpecialSlot::new, ConnectionState.IN_GAME),
	EX_ADENLAB_SPECIAL_PROB(0x2A8, RequestAdenLabSpecialProbability::new, ConnectionState.IN_GAME),
	EX_ADENLAB_SPECIAL_PLAY(0x2A9, RequestAdenLabSpecialPlay::new, ConnectionState.IN_GAME),
	EX_ADENLAB_SPECIAL_FIX(0x2AA, RequestAdenLabSpecialFix::new, ConnectionState.IN_GAME),
	EX_ADENLAB_TRANSCEND_ENCHANT(0x2AB, RequestAdenLabTranscendentEnchant::new, ConnectionState.IN_GAME),
	EX_ADENLAB_TRANSCEND_PROB(0x2AC, RequestAdenLabTranscendentProbability::new, ConnectionState.IN_GAME),
	EX_HOMUNCULUS_EVOLVE(0x2AD, null, ConnectionState.IN_GAME),
	EX_EXTRACT_SKILL_ENCHANT(0x2AE, RequestExtractSkillEnchant::new, ConnectionState.IN_GAME),
	EX_REQUEST_SKILL_ENCHANT_CONFIRM(0x2AF, RequestSkillEnchantConfirm::new, ConnectionState.IN_GAME),
	// 474
	EX_CREATE_ITEM_PROB_LIST(0x2B0, RequestCreateItemProbList::new, ConnectionState.IN_GAME),
	EX_CRAFT_SLOT_PROB_LIST(0x2B1, RequestCreateSlotProbList::new, ConnectionState.IN_GAME),
	EX_NEW_HENNA_COMPOSE_PROB_LIST(0x2B2, RequestNewHennaComposeProbList::new, ConnectionState.IN_GAME),
	EX_VARIATION_PROB_LIST(0x2B3, RequestVariationProbList::new, ConnectionState.IN_GAME),
	EX_RELICS_PROB_LIST(0x2B4, RequestRelicsProbList::new, ConnectionState.IN_GAME),
	EX_UPGRADE_SYSTEM_PROB_LIST(0x2B5, RequestUpgradeSystemProbList::new, ConnectionState.IN_GAME),
	EX_COMBINATION_PROB_LIST(0x2B6, RequestCombinationProbList::new, ConnectionState.IN_GAME),
	// 493
	EX_RELICS_ID_SUMMON(0x2B7, RequestRelicsIdSummon::new, ConnectionState.IN_GAME),
	EX_RELICS_SUMMON_LIST(0x2B8, RequestRelicsSummonList::new, ConnectionState.IN_GAME),
	EX_RELICS_CONFIRM_COMBINATION(0x2B9, null, ConnectionState.IN_GAME),
	EX_NEW_HENNA_POTEN_OPENSLOT_PROB_INFO(0x2BA, RequestNewHennaPotenOpenslotProbInfo::new, ConnectionState.IN_GAME),
	EX_NEW_HENNA_POTEN_OPENSLOT(0x2BB, RequestNewHennaPotenOpenslot::new, ConnectionState.IN_GAME),
	EX_DYEEFFECT_LIST(0x2BC, null, ConnectionState.IN_GAME),
	EX_DYEEFFECT_ENCHANT_PROB_INFO(0x2BD, null, ConnectionState.IN_GAME),
	EX_DYEEFFECT_ENCHANT_NORMALSKILL(0x2BE, null, ConnectionState.IN_GAME),
	EX_DYEEFFECT_ACQUIRE_HIDDENSKILL(0x2BF, null, ConnectionState.IN_GAME),
	EX_DYEEFFECT_ENCHANT_RESET(0x2C0, null, ConnectionState.IN_GAME),
	EX_LOAD_PET_PREVIEW_BY_SID(0x2C1, null, ConnectionState.IN_GAME),
	EX_LOAD_PET_PREVIEW_BY_DBID(0x2C2, null, ConnectionState.IN_GAME),
	EX_CHECK_CLIENT_INFO(0x2C3, null, ConnectionState.IN_GAME),
	// 507
	EX_MATCHINGINZONE_FIELD_ENTER_USER_INFO(0x2C4, null, ConnectionState.IN_GAME),
	EX_RAID_AUCTION_BID(0x2C5, null, ConnectionState.IN_GAME),
	EX_RAID_AUCTION_CANCEL_BID(0x2C6, null, ConnectionState.IN_GAME),
	EX_RAID_AUCTION_POST_LIST(0x2C7, null, ConnectionState.IN_GAME),
	EX_RAID_AUCTION_POST_RECEIVE(0x2C8, null, ConnectionState.IN_GAME),
	EX_RAID_AUCTION_POST_RECEIVE_ALL(0x2C9, null, ConnectionState.IN_GAME),
	EX_REPAIR_ALL_EQUIPMENT(0x2CA, null, ConnectionState.IN_GAME),
	EX_CLASS_CHANGE(0x2CB, null, ConnectionState.IN_GAME),
	EX_CHAT_BAN_START(0x2CC, null, ConnectionState.IN_GAME),
	EX_CHAT_BAN_END(0x2CD, null, ConnectionState.IN_GAME),
	EX_BLESS_OPTION_PROB_LIST(0x2CE, RequestBlessOptionProbList::new, ConnectionState.IN_GAME),
	// 520
	EX_CHARACTER_STYLE_LIST(0x2CF, ExRequestCharacterStyleList::new, ConnectionState.IN_GAME),
	EX_CHARACTER_STYLE_REGIST(0x2D0, ExRequestCharacterStyleRegister::new, ConnectionState.IN_GAME),
	EX_CHARACTER_STYLE_SELECT(0x2D1, ExRequestCharacterStyleSelect::new, ConnectionState.IN_GAME),
	EX_CHARACTER_STYLE_UPDATE_FAVORITE(0x2D2, ExRequestCharacterStyleUpdateFavorite::new, ConnectionState.IN_GAME),
	EX_PURCHASE_LIMIT_CRAFT_ITEM(0x2D3, RequestPurchaseLimitCraftItem::new, ConnectionState.IN_GAME),
	EX_PARTY_ROOM_ANNOUNCE(0x2D4, RequestPartyMatchingAnnounce::new, ConnectionState.IN_GAME),
	// 541
	EX_CHARACTER_STYLE_UNREGIST(0x2D5, null, ConnectionState.IN_GAME),
	EX_UPGRADE_PROB(0x2D6, RequestUpgradeProb::new, ConnectionState.IN_GAME),
	EX_ALLIANCE_CREATE(0x2D7, AllyCreate::new, ConnectionState.IN_GAME),
	EX_MAX(0x2D8, null, ConnectionState.IN_GAME);
	
	public static final ExClientPackets[] PACKET_ARRAY;
	static
	{
		final int maxPacketId = Arrays.stream(values()).mapToInt(ExClientPackets::getPacketId).max().orElse(0);
		PACKET_ARRAY = new ExClientPackets[maxPacketId + 1];
		for (ExClientPackets packet : values())
		{
			PACKET_ARRAY[packet.getPacketId()] = packet;
		}
	}
	
	private final int _packetId;
	private final Supplier<ClientPacket> _packetSupplier;
	private final Set<ConnectionState> _connectionStates;
	
	ExClientPackets(int packetId, Supplier<ClientPacket> packetSupplier, ConnectionState... connectionStates)
	{
		// Packet id is an unsigned short.
		if (packetId > 0xFFFF)
		{
			throw new IllegalArgumentException("Packet id must not be bigger than 0xFFFF");
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
		if (DevelopmentConfig.DEBUG_EX_CLIENT_PACKETS)
		{
			if (packet != null)
			{
				final String name = packet.getClass().getSimpleName();
				if (!DevelopmentConfig.EXCLUDED_DEBUG_PACKETS.contains(name))
				{
					PacketLogger.info("[C EX] " + name);
				}
			}
			else if (DevelopmentConfig.DEBUG_UNKNOWN_PACKETS)
			{
				PacketLogger.info("[C EX] 0x" + Integer.toHexString(_packetId).toUpperCase());
			}
		}
		
		return packet;
	}
	
	public Set<ConnectionState> getConnectionStates()
	{
		return _connectionStates;
	}
}
