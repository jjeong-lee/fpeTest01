package kr.ac.knue.achievement.codes;

import java.util.List;
import java.util.UUID;

public interface CodeGroupManagementService {
    List<CodeGroupView> listCodeGroups();
    CodeGroupView create(CodeGroupRequest request, UUID actorUserId);
    CodeGroupView update(String groupId, CodeGroupRequest request, UUID actorUserId);
}
