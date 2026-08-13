package kr.ac.knue.achievement.common;

import java.time.Instant;
import java.util.UUID;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ChangeHistoryMapper {
    @Insert("""
            INSERT INTO change_history (change_history_id, entity_name, entity_id, before_value, after_value,
              actor_user_id, reason, processed_at)
            VALUES (#{historyId}, #{entityName}, #{entityId}, CAST(#{beforeValue} AS jsonb), CAST(#{afterValue} AS jsonb),
              #{actorUserId}, #{reason}, #{processedAt})
            """)
    void insert(@Param("historyId") UUID historyId, @Param("entityName") String entityName,
            @Param("entityId") String entityId, @Param("beforeValue") String beforeValue,
            @Param("afterValue") String afterValue, @Param("actorUserId") UUID actorUserId,
            @Param("reason") String reason, @Param("processedAt") Instant processedAt);
}
