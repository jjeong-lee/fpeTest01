package kr.ac.knue.achievement.codes;

public record CodeGroupRequest(String groupId, String groupName, String description,
        String managingDepartment, String reason) { }
