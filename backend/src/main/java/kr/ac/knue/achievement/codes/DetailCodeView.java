package kr.ac.knue.achievement.codes;

import java.util.Map;
import java.util.UUID;

public record DetailCodeView(UUID detailCodeId, String groupId, String codeValue, String codeName,
        UUID parentDetailCodeId, int displayOrder, Map<String, Object> additionalAttributes, String useStatus) { }
