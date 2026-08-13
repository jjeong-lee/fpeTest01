package kr.ac.knue.achievement.menus;

import java.util.List;
import java.util.UUID;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MenuInformationManagementMapper {
    List<MenuInformationRow> findActiveMenus();
    MenuInformationRow findActiveMenu(@Param("menuId") UUID menuId);
    int updateExecutionInformation(@Param("menuId") UUID menuId, @Param("menuName") String menuName,
            @Param("screenId") String screenId, @Param("url") String url, @Param("icon") String icon,
            @Param("businessCategory") String businessCategory, @Param("description") String description);

    record MenuInformationRow(UUID menuId, String menuName, String screenId, String url,
            String icon, String businessCategory, String description, String useStatus) { }
}
