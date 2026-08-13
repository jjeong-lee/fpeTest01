package kr.ac.knue.achievement.auth;

import java.time.Instant;
import java.util.UUID;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface AuthenticationMapper {
    @Select("SELECT user_id, username, organization_code FROM user_account WHERE username = #{username} AND password_hash = #{password} AND system_enabled = TRUE AND account_status = 'ACTIVE'")
    Account findActiveAccount(@Param("username") String username, @Param("password") String password);

    @Insert("INSERT INTO session (session_id, user_id, expires_at, status) VALUES (#{sessionId}, #{userId}, #{expiresAt}, 'ACTIVE')")
    void insertSession(@Param("sessionId") UUID sessionId, @Param("userId") UUID userId, @Param("expiresAt") Instant expiresAt);

    @Select("SELECT s.session_id, u.user_id, u.username, u.organization_code, s.expires_at FROM session s JOIN user_account u ON u.user_id = s.user_id WHERE s.session_id = #{sessionId} AND s.status = 'ACTIVE' AND s.expires_at > CURRENT_TIMESTAMP")
    SessionAccount findActiveSession(@Param("sessionId") UUID sessionId);

    @Update("UPDATE session SET status = 'INVALIDATED', updated_at = CURRENT_TIMESTAMP WHERE session_id = #{sessionId} AND status = 'ACTIVE'")
    int invalidateSession(@Param("sessionId") UUID sessionId);

    @Select("SELECT role_code FROM user_role WHERE user_id = #{userId} AND status = 'ACTIVE'")
    java.util.Set<String> findActiveRoleCodes(@Param("userId") UUID userId);

    record Account(UUID userId, String username, String organizationCode) { }
    record SessionAccount(UUID sessionId, UUID userId, String username, String organizationCode, Instant expiresAt) { }
}
