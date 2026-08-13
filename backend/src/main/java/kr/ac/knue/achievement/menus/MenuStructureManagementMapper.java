package kr.ac.knue.achievement.menus;

import java.util.List;
import java.util.UUID;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MenuStructureManagementMapper {
    List<MenuRow> findActiveMenus();

    MenuRow findActiveMenu(@Param("menuId") UUID menuId);

    int updateParent(@Param("menuId") UUID menuId, @Param("parentMenuId") UUID parentMenuId);

    int updateDisplayOrder(@Param("menuId") UUID menuId, @Param("displayOrder") int displayOrder);

    record MenuRow(UUID menuId, UUID parentMenuId, String menuName, int displayOrder, String useStatus) { }
}
