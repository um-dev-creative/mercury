package com.umdc.mercury.api.v1.service;

import com.umdc.mercury.api.v1.exception.ForbiddenException;
import com.umdc.mercury.jpa.sql.entity.ApplicationEntity;
import com.umdc.mercury.jpa.sql.repository.ApplicationRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AdministeringApplicationTest {

    private final ApplicationRepository applications = mock(ApplicationRepository.class);

    private static ApplicationEntity application(UUID id) {
        var entity = new ApplicationEntity();
        entity.setId(id);
        return entity;
    }

    @Test
    void aForcedIdWinsAndTheTableIsNotConsulted() {
        var id = UUID.randomUUID();

        assertEquals(id, new AdministeringApplication(applications, " " + id + " ", "iris").id());
        verifyNoInteractions(applications);
    }

    @Test
    void anIdThatIsNotAUuidRefusesEveryone() {
        var broken = new AdministeringApplication(applications, "IRIS_CLIENTS_ADMIN", "iris");

        assertThrows(ForbiddenException.class, broken::id);
        verifyNoInteractions(applications);
    }

    @Test
    void withoutIdOrCodeNameNobodyIsAnAdministrator() {
        for (String codeName : new String[] {"", "  ", null}) {
            var unconfigured = new AdministeringApplication(applications, null, codeName);

            assertThrows(ForbiddenException.class, unconfigured::id);
        }
        verifyNoInteractions(applications);
    }

    @Test
    void theApplicationIsFoundByItsCodeName() {
        var id = UUID.randomUUID();
        when(applications.findAllByCodeNameIgnoreCase("iris")).thenReturn(List.of(application(id)));

        assertEquals(id, new AdministeringApplication(applications, " ", " iris ").id());
    }

    @Test
    void aCodeNameThatMatchesNoneOrSeveralApplicationsRefusesEveryone() {
        when(applications.findAllByCodeNameIgnoreCase("iris")).thenReturn(List.of());
        when(applications.findAllByCodeNameIgnoreCase("lucky_le")).thenReturn(List.of(application(UUID.randomUUID()), application(UUID.randomUUID())));

        for (String codeName : new String[] {"iris", "lucky_le"}) {
            var ambiguous = new AdministeringApplication(applications, "", codeName);

            assertThrows(ForbiddenException.class, ambiguous::id);
        }
    }
}
