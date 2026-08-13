package kr.ac.knue.achievement.codes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import kr.ac.knue.achievement.common.ApiException;
import kr.ac.knue.achievement.common.AuditedCommandExecutor;

@Service
@Profile("!test")
public class MyBatisDetailCodeManagementService implements DetailCodeManagementService {
    private final DetailCodeManagementMapper mapper;
    private final AuditedCommandExecutor auditedCommandExecutor;
    private final ObjectMapper objectMapper;

    public MyBatisDetailCodeManagementService(DetailCodeManagementMapper mapper,
            AuditedCommandExecutor auditedCommandExecutor, ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.auditedCommandExecutor = auditedCommandExecutor;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<DetailCodeView> listByGroup(String groupId) {
        requireGroup(groupId);
        return mapper.findActiveDetailCodesByGroup(groupId).stream().map(this::view).toList();
    }

    @Override
    public DetailCodeView create(DetailCodeRequest request, UUID actorUserId) {
        requireGroup(request.groupId());
        if (mapper.findActiveDetailCodeByValue(request.groupId(), request.codeValue()) != null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "DETAIL_CODE_DUPLICATE", "같은 그룹에 이미 등록된 코드값입니다.");
        }
        validateParent(request.groupId(), request.parentDetailCodeId(), null);
        UUID detailCodeId = UUID.randomUUID();
        auditedCommandExecutor.execute(actorUserId, "detail_code", detailCodeId.toString(), "null", json(request), request.reason(),
                () -> mapper.insertDetailCode(detailCodeId, request.groupId(), request.codeValue(), request.codeName(),
                        request.parentDetailCodeId(), request.displayOrder(), attributesJson(request.additionalAttributes())));
        return view(requireDetailCode(detailCodeId));
    }

    @Override
    public DetailCodeView update(UUID detailCodeId, DetailCodeRequest request, UUID actorUserId) {
        DetailCodeManagementMapper.DetailCodeRow current = requireDetailCode(detailCodeId);
        requireGroup(request.groupId());
        DetailCodeManagementMapper.DetailCodeRow duplicate = mapper.findActiveDetailCodeByValue(request.groupId(), request.codeValue());
        if (duplicate != null && !detailCodeId.equals(duplicate.detailCodeId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "DETAIL_CODE_DUPLICATE", "같은 그룹에 이미 등록된 코드값입니다.");
        }
        validateParent(request.groupId(), request.parentDetailCodeId(), detailCodeId);
        auditedCommandExecutor.execute(actorUserId, "detail_code", detailCodeId.toString(), json(current), json(request), request.reason(),
                () -> mapper.updateDetailCode(detailCodeId, request.groupId(), request.codeValue(), request.codeName(),
                        request.parentDetailCodeId(), request.displayOrder(), attributesJson(request.additionalAttributes())));
        return view(requireDetailCode(detailCodeId));
    }

    private void requireGroup(String groupId) {
        if (!mapper.existsActiveCodeGroup(groupId)) throw new ApiException(HttpStatus.NOT_FOUND, "CODE_GROUP_NOT_FOUND", "코드그룹을 찾을 수 없습니다.");
    }

    private DetailCodeManagementMapper.DetailCodeRow requireDetailCode(UUID detailCodeId) {
        DetailCodeManagementMapper.DetailCodeRow code = mapper.findActiveDetailCode(detailCodeId);
        if (code == null) throw new ApiException(HttpStatus.NOT_FOUND, "DETAIL_CODE_NOT_FOUND", "상세코드를 찾을 수 없습니다.");
        return code;
    }

    private void validateParent(String groupId, UUID parentDetailCodeId, UUID detailCodeId) {
        if (parentDetailCodeId == null) return;
        if (parentDetailCodeId.equals(detailCodeId)) throw parentError();
        DetailCodeManagementMapper.DetailCodeRow parent = mapper.findActiveDetailCode(parentDetailCodeId);
        if (parent == null || !groupId.equals(parent.groupId())) throw parentError();
    }

    private ApiException parentError() {
        return new ApiException(HttpStatus.BAD_REQUEST, "DETAIL_CODE_PARENT_INVALID", "상위코드는 같은 그룹의 다른 상세코드여야 합니다.");
    }

    private DetailCodeView view(DetailCodeManagementMapper.DetailCodeRow row) {
        return new DetailCodeView(row.detailCodeId(), row.groupId(), row.codeValue(), row.codeName(), row.parentDetailCodeId(),
                row.displayOrder(), attributes(row.additionalAttributes()), row.useStatus());
    }

    private Map<String, Object> attributes(String json) {
        if (json == null || json.isBlank()) return Map.of();
        try {
            return objectMapper.readValue(json, new TypeReference<LinkedHashMap<String, Object>>() { });
        } catch (Exception exception) {
            throw new IllegalStateException("저장된 추가속성을 읽을 수 없습니다.", exception);
        }
    }

    private String attributesJson(Map<String, Object> attributes) {
        try {
            return objectMapper.writeValueAsString(attributes == null ? Map.of() : attributes);
        } catch (Exception exception) {
            throw new IllegalArgumentException("추가속성 형식이 올바르지 않습니다.", exception);
        }
    }

    private String json(DetailCodeManagementMapper.DetailCodeRow row) {
        return json(new DetailCodeRequest(row.groupId(), row.codeValue(), row.codeName(), row.parentDetailCodeId(),
                row.displayOrder(), attributes(row.additionalAttributes()), ""));
    }

    private String json(DetailCodeRequest request) {
        try {
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("groupId", request.groupId());
            value.put("codeValue", request.codeValue());
            value.put("codeName", request.codeName());
            value.put("parentDetailCodeId", request.parentDetailCodeId());
            value.put("displayOrder", request.displayOrder());
            value.put("additionalAttributes", request.additionalAttributes() == null ? Map.of() : request.additionalAttributes());
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException("상세코드 변경 값을 기록할 수 없습니다.", exception);
        }
    }
}
