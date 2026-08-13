package kr.ac.knue.achievement.codes;

import java.util.List;
import java.util.UUID;

public interface DetailCodeManagementService {
    List<DetailCodeView> listByGroup(String groupId);
    DetailCodeView create(DetailCodeRequest request, UUID actorUserId);
    DetailCodeView update(UUID detailCodeId, DetailCodeRequest request, UUID actorUserId);
}
