package org.l2jmobius.gameserver.data.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;

public class PromotionRewardDAO
{
    private static final Logger LOGGER = Logger.getLogger(PromotionRewardDAO.class.getName());
    
    public static class RewardRecord
    {
        public int id;
        public String accountName;
        public String rewardDate;
        public int coinAmount;
        public boolean claimed;
        public String claimTime;
        public String charName;
        public String note;
    }
    
    // 獲取帳號未領取的獎勵總數
    public static int getUnclaimedCount(String accountName)
    {
        int count = 0;
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM promotion_rewards WHERE account_name=? AND claimed=0"))
        {
            ps.setString(1, accountName);
            try (ResultSet rs = ps.executeQuery())
            {
                if (rs.next())
                {
                    count = rs.getInt(1);
                }
            }
        }
        catch (SQLException e)
        {
            LOGGER.log(Level.WARNING, "無法獲取未領取獎勵數量: " + accountName, e);
        }
        return count;
    }
    
    // 獲取帳號未領取的硬幣總數
    public static int getUnclaimedTotalCoins(String accountName)
    {
        int total = 0;
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT SUM(coin_amount) FROM promotion_rewards WHERE account_name=? AND claimed=0"))
        {
            ps.setString(1, accountName);
            try (ResultSet rs = ps.executeQuery())
            {
                if (rs.next())
                {
                    total = rs.getInt(1);
                }
            }
        }
        catch (SQLException e)
        {
            LOGGER.log(Level.WARNING, "無法獲取未領取硬幣總數: " + accountName, e);
        }
        return total;
    }
    
    // 獲取帳號所有獎勵記錄
    public static List<RewardRecord> getRewardRecords(String accountName)
    {
        List<RewardRecord> records = new ArrayList<>();
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT * FROM promotion_rewards WHERE account_name=? ORDER BY reward_date DESC"))
        {
            ps.setString(1, accountName);
            try (ResultSet rs = ps.executeQuery())
            {
                while (rs.next())
                {
                    RewardRecord record = new RewardRecord();
                    record.id = rs.getInt("id");
                    record.accountName = rs.getString("account_name");
                    record.rewardDate = rs.getString("reward_date");
                    record.coinAmount = rs.getInt("coin_amount");
                    record.claimed = rs.getBoolean("claimed");
                    record.claimTime = rs.getString("claim_time");
                    record.charName = rs.getString("char_name");
                    record.note = rs.getString("note");
                    records.add(record);
                }
            }
        }
        catch (SQLException e)
        {
            LOGGER.log(Level.WARNING, "無法獲取獎勵記錄: " + accountName, e);
        }
        return records;
    }
    
    // 領取所有未領取的獎勵
    public static boolean claimAllRewards(String accountName, String charName)
    {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement ps = con.prepareStatement("UPDATE promotion_rewards SET claimed=1, claim_time=NOW(), char_name=? WHERE account_name=? AND claimed=0"))
        {
            ps.setString(1, charName);
            ps.setString(2, accountName);
            int updated = ps.executeUpdate();
            return updated > 0;
        }
        catch (SQLException e)
        {
            LOGGER.log(Level.WARNING, "無法領取獎勵: " + accountName, e);
            return false;
        }
    }
    
    // 新增獎勵記錄
    public static boolean addReward(String accountName, String rewardDate, int coinAmount, String note)
    {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement ps = con.prepareStatement("INSERT INTO promotion_rewards (account_name, reward_date, coin_amount, note) VALUES (?, ?, ?, ?)"))
        {
            ps.setString(1, accountName);
            ps.setString(2, rewardDate);
            ps.setInt(3, coinAmount);
            ps.setString(4, note);
            ps.executeUpdate();
            return true;
        }
        catch (SQLException e)
        {
            LOGGER.log(Level.WARNING, "無法新增獎勵: " + accountName, e);
            return false;
        }
    }
    
    // 刪除獎勵記錄
    public static boolean deleteReward(int id)
    {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM promotion_rewards WHERE id=?"))
        {
            ps.setInt(1, id);
            ps.executeUpdate();
            return true;
        }
        catch (SQLException e)
        {
            LOGGER.log(Level.WARNING, "無法刪除獎勵: " + id, e);
            return false;
        }
    }
}