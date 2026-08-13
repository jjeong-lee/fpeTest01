package kr.ac.knue.achievement.codes;

import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import kr.ac.knue.achievement.common.ApiException;
import kr.ac.knue.achievement.common.AuditedCommandExecutor;

@Service
@Profile("!test")
public class MyBatisCodeGroupManagementService implements CodeGroupManagementService {
    private final CodeGroupManagementMapper mapper;
    private final AuditedCommandExecutor auditedCommandExecutor;

    public MyBatisCodeGroupManagementService(CodeGroupManagementMapper mapper,
            AuditedCommandExecutor auditedCommandExecutor) {
        this.mapper = mapper;
        this.auditedCommandExecutor = auditedCommandExecutor;
    }

    @Override
    public List<CodeGroupView> listCodeGroups() {
        return mapper.findActiveCodeGroups().stream().map(this::view).toList();
    }

    @Override
    public CodeGroupView create(CodeGroupRequest request, java.util.UUID actorUserId) {
        if (mapper.findActiveCodeGroup(request.groupId()) != null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "CODE_GROUP_DUPLICATE", "이미 등록된 그룹ID입니다.");
        }
        auditedCommandExecutor.execute(actorUserId, "code_group", request.groupId(), "null", json(request), request.reason(),
                () -> mapper.insertCodeGroup(request.groupId(), request.groupName(), request.description(), request.managingDepartment()));
        return view(requireCodeGroup(request.groupId()));
    }

    @Override
    public CodeGroupView update(String groupId, CodeGroupRequest request, java.util.UUID actorUserId) {
        if (!groupId.equals(request.groupId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "GROUP_ID_MISMATCH", "경로의 그룹ID와 요청 그룹ID가 일치하지 않습니다.");
        }
        CodeGroupManagementMapper.CodeGroupRow current = requireCodeGroup(groupId);
        auditedCommandExecutor.execute(actorUserId, "code_group", groupId, json(current), json(request), request.reason(),
                () -> mapper.updateCodeGroup(groupId, request.groupName(), request.description(), request.managingDepartment()));
        return view(requireCodeGroup(groupId));
    }

    private CodeGroupManagementMapper.CodeGroupRow requireCodeGroup(String groupId) {
        CodeGroupManagementMapper.CodeGroupRow group = mapper.findActiveCodeGroup(groupId);
        if (group == null) throw new ApiException(HttpStatus.NOT_FOUND, "CODE_GROUP_NOT_FOUND", "코드그룹을 찾을 수 없습니다.");
        return group;
    }

    private CodeGroupView view(CodeGroupManagementMapper.CodeGroupRow row) {
        return new CodeGroupView(row.groupId(), row.groupName(), row.description(), row.managingDepartment(), row.useStatus());
    }

    private String json(CodeGroupManagementMapper.CodeGroupRow row) {
        return json(new CodeGroupRequest(row.groupId(), row.groupName(), row.description(), row.managingDepartment(), ""));
    }

    private String json(CodeGroupRequest request) {
        return "{\"groupId\":\"" + escape(request.groupId()) + "\",\"groupName\":\"" + escape(request.groupName())
                + "\",\"description\":" + nullable(request.description()) + ",\"managingDepartment\":"
                + nullable(request.managingDepartment()) + "}";
    }

    private String nullable(String value) { return value == null ? "null" : "\"" + escape(value) + "\""; }
    private String escape(String value) { return value.replace("\\", "\\\\").replace("\"", "\\\""); }
}
