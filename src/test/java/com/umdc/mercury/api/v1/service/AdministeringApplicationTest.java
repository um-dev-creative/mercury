package com.umdc.mercury.api.v1.service;

import com.umdc.mercury.api.v1.exception.ForbiddenException;
import com.umdc.mercury.client.to.BackboneApplication;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AdministeringApplicationTest {

    private static final String TOKEN = "session";

    private final ApplicationDirectory applications = mock(ApplicationDirectory.class);

    private static BackboneApplication application(UUID id) {
        return new BackboneApplication(id, "Iris", "iris", true);
    }

    @Test
    void aForcedIdWinsAndBackboneIsNotAsked() {
        var id = UUID.randomUUID();

        assertEquals(id, new AdministeringApplication(applications, " " + id + " ", "iris").id(TOKEN));
        verifyNoInteractions(applications);
    }

    @Test
    void anIdThatIsNotAUuidRefusesEveryone() {
        var broken = new AdministeringApplication(applications, "IRIS_CLIENTS_ADMIN", "iris");

        assertThrows(ForbiddenException.class, () -> broken.id(TOKEN));
        verifyNoInteractions(applications);
    }

    @Test
    void withoutIdOrCodeNameNobodyIsAnAdministrator() {
        for (String codeName : new String[] {"", "  ", null}) {
            var unconfigured = new AdministeringApplication(applications, null, codeName);

            assertThrows(ForbiddenException.class, () -> unconfigured.id(TOKEN));
        }
        verifyNoInteractions(applications);
    }

    @Test
    void theApplicationIsFoundByItsCodeNameThroughBackbone() {
        var id = UUID.randomUUID();
        when(applications.byCodeName(TOKEN, "iris")).thenReturn(List.of(application(id)));

        assertEquals(id, new AdministeringApplication(applications, " ", " iris ").id(TOKEN));
    }

    @Test
    void aCodeNameThatMatchesNoneOrSeveralApplicationsRefusesEveryone() {
        when(applications.byCodeName(TOKEN, "iris")).thenReturn(List.of());
        when(applications.byCodeName(TOKEN, "lucky_le")).thenReturn(List.of(application(UUID.randomUUID()), application(UUID.randomUUID())));

        for (String codeName : new String[] {"iris", "lucky_le"}) {
            var ambiguous = new AdministeringApplication(applications, "", codeName);

            assertThrows(ForbiddenException.class, () -> ambiguous.id(TOKEN));
        }
    }

    @Test
    void whenBackboneCannotAnswerEveryoneIsRefused() {
        when(applications.byCodeName(TOKEN, "iris")).thenThrow(new ForbiddenException("Applications cannot be listed"));

        var administering = new AdministeringApplication(applications, "", "iris");

        assertThrows(ForbiddenException.class, () -> administering.id(TOKEN));
    }
}
