package kr.ac.knue.achievement.codes;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CodeGroupManagementMapper {
    List<CodeGroupRow> findActiveCodeGroups();
    CodeGroupRow findActiveCodeGroup(@Param("groupId") String groupId);
    int insertCodeGroup(@Param("groupId") String groupId, @Param("groupName") String groupName,
            @Param("description") String description, @Param("managingDepartment") String managingDepartment);
    int updateCodeGroup(@Param("groupId") String groupId, @Param("groupName") String groupName,
            @Param("description") String description, @Param("managingDepartment") String managingDepartment);

    record CodeGroupRow(String groupId, String groupName, String description,
            String managingDepartment, String useStatus) { }
}
