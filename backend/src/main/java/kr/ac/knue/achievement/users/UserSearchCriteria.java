package kr.ac.knue.achievement.users;

public record UserSearchCriteria(String employeeNo, String name, String organizationCode, String position,
        String employmentStatus, String roleCode, Boolean systemEnabled, int page, int size) {
}
