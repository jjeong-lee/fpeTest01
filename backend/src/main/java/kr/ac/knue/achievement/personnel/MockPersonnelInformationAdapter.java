package kr.ac.knue.achievement.personnel;

import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class MockPersonnelInformationAdapter implements PersonnelInformationPort {
    @Override
    public List<PersonnelSnapshot> findPersonnel() {
        return Collections.emptyList();
    }

    @Override
    public List<OrganizationSnapshot> findOrganizations() {
        return Collections.emptyList();
    }
}
