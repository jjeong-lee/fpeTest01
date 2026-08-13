package kr.ac.knue.achievement.codes;

import java.util.List;
import java.util.UUID;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DetailCodeManagementMapper {
    List<DetailCodeRow> findActiveDetailCodesByGroup(@Param("groupId") String groupId);
    DetailCodeRow findActiveDetailCode(@Param("detailCodeId") UUID detailCodeId);
    DetailCodeRow findActiveDetailCodeByValue(@Param("groupId") String groupId, @Param("codeValue") String codeValue);
    boolean existsActiveCodeGroup(@Param("groupId") String groupId);
    int insertDetailCode(@Param("detailCodeId") UUID detailCodeId, @Param("groupId") String groupId,
            @Param("codeValue") String codeValue, @Param("codeName") String codeName,
            @Param("parentDetailCodeId") UUID parentDetailCodeId, @Param("displayOrder") int displayOrder,
            @Param("additionalAttributes") String additionalAttributes);
    int updateDetailCode(@Param("detailCodeId") UUID detailCodeId, @Param("groupId") String groupId,
            @Param("codeValue") String codeValue, @Param("codeName") String codeName,
            @Param("parentDetailCodeId") UUID parentDetailCodeId, @Param("displayOrder") int displayOrder,
            @Param("additionalAttributes") String additionalAttributes);

    record DetailCodeRow(UUID detailCodeId, String groupId, String codeValue, String codeName,
            UUID parentDetailCodeId, int displayOrder, String additionalAttributes, String useStatus) { }
}
