package kr.ac.knue.achievement.personnel;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public interface PersonnelInformationPort {
    List<PersonnelSnapshot> findPersonnel();
    List<OrganizationSnapshot> findOrganizations();

    record PersonnelSnapshot(String employeeNo, String name, String organizationCode, String position,
                             String employmentStatus, String duty, LocalDate retirementDate, OffsetDateTime lastSyncedAt) { }
    record OrganizationSnapshot(String organizationCode, String organizationName, String organizationType, String useStatus) { }
}
