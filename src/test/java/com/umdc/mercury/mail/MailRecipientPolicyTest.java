package com.umdc.mercury.mail;

import com.umdc.mercury.api.v1.to.EmailContact;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Test recipients for QA: default list and the flag that ignores the request's recipients")
class MailRecipientPolicyTest {

    private static EmailContact contact(String email) {
        return new EmailContact(email, "n", null);
    }

    private static MailRecipientPolicy policy(boolean honorRequest, String... defaults) {
        return new MailRecipientPolicy(new MailRecipientProperties(List.of(defaults), honorRequest), new String[]{"qa"});
    }

    @Test
    @DisplayName("without configuration the request's recipients are used, as before")
    void passThroughKeepsTheRequest() {
        var policy = MailRecipientPolicy.passThrough();

        assertEquals(List.of("a@x.test"), policy.resolveTo(List.of(contact("a@x.test"))));
        assertEquals(List.of("c@x.test"), policy.resolveCc(List.of(contact("c@x.test"))));
        assertEquals(List.of(), policy.resolveTo(List.of()));
    }

    @Test
    @DisplayName("honoring the request, the default list only stands in for a message with no recipient")
    void defaultsOnlyFillAnEmptyRequest() {
        var policy = policy(true, "qa1@x.test", "qa2@x.test");

        assertEquals(List.of("real@x.test"), policy.resolveTo(List.of(contact("real@x.test"))));
        assertEquals(List.of("qa1@x.test", "qa2@x.test"), policy.resolveTo(List.of()));
        assertEquals(List.of("qa1@x.test", "qa2@x.test"), policy.resolveTo(null));
        assertEquals(List.of("copy@x.test"), policy.resolveCc(List.of(contact("copy@x.test"))));
    }

    @Test
    @DisplayName("ignoring the request, every e-mail goes to the default list and cc is dropped")
    void overrideReplacesEveryone() {
        var policy = policy(false, "qa1@x.test", "qa2@x.test");

        assertEquals(List.of("qa1@x.test", "qa2@x.test"), policy.resolveTo(List.of(contact("real@x.test"))));
        assertEquals(List.of("qa1@x.test", "qa2@x.test"), policy.resolveTo(List.of()));
        assertEquals(List.of(), policy.resolveCc(List.of(contact("copy@x.test"))));
        assertEquals(List.of(), policy.resolveCc(null));
    }

    @Test
    @DisplayName("null contacts in the request are ignored")
    void nullContactsAreSkipped() {
        var policy = policy(true);
        var withNull = new ArrayList<EmailContact>();
        withNull.add(null);
        withNull.add(contact("a@x.test"));

        assertEquals(List.of("a@x.test"), policy.resolveTo(withNull));
        assertEquals(List.of("a@x.test"), policy.resolveCc(withNull));
    }

    @Test
    @DisplayName("the list is cleaned: blanks dropped, spaces stripped, duplicates removed")
    void propertiesCleanTheList() {
        var props = new MailRecipientProperties(Arrays.asList(" a@x.test ", "", null, "a@x.test", "b@x.test"), true);

        assertEquals(List.of("a@x.test", "b@x.test"), props.defaults());
        assertEquals(List.of(), new MailRecipientProperties(null, true).defaults());
    }

    @Test
    @DisplayName("an invalid address in the list stops Mercury at start")
    void invalidAddressRefusesToStart() {
        var error = assertThrows(IllegalStateException.class, () -> new MailRecipientProperties(List.of("not-an-email"), true));

        assertTrue(error.getMessage().contains("not-an-email"));
    }

    @Test
    @DisplayName("ignoring the request's recipients without a default list stops Mercury at start")
    void overrideWithoutListRefusesToStart() {
        var error = assertThrows(IllegalStateException.class, () -> new MailRecipientProperties(List.of(), false));

        assertTrue(error.getMessage().contains("MAIL_DEFAULT_RECIPIENTS"));
        assertThrows(IllegalStateException.class, () -> new MailRecipientProperties(null, false));
    }

    @Test
    @DisplayName("the override runs in qa and dev, whatever the case")
    void overrideAllowedInQaAndDev() {
        var props = new MailRecipientProperties(List.of("qa@x.test"), false);

        assertDoesNotThrow(() -> new MailRecipientPolicy(props, new String[]{"dev"}));
        assertDoesNotThrow(() -> new MailRecipientPolicy(props, new String[]{"QA", "other"}));
    }

    @Test
    @DisplayName("the override refuses to start in production, in an unlisted profile or with no profile")
    void overrideRefusedOutsideQaAndDev() {
        var props = new MailRecipientProperties(List.of("qa@x.test"), false);

        for (var profile : List.of("prd", "prod", "production", "PRD", "staging")) {
            var error = assertThrows(IllegalStateException.class, () -> new MailRecipientPolicy(props, new String[]{profile}));
            assertTrue(error.getMessage().contains("only allowed in the profiles"), profile);
        }
        assertThrows(IllegalStateException.class, () -> new MailRecipientPolicy(props, new String[0]));
        assertThrows(IllegalStateException.class, () -> new MailRecipientPolicy(props, (String[]) null));
    }

    @Test
    @DisplayName("a production profile next to qa still refuses")
    void productionAlongsideQaRefuses() {
        var props = new MailRecipientProperties(List.of("qa@x.test"), false);

        assertThrows(IllegalStateException.class, () -> new MailRecipientPolicy(props, new String[]{"qa", "prod"}));
    }

    @Test
    @DisplayName("the profile only matters when the override is on")
    void honoredRequestNeedsNoProfile() {
        assertDoesNotThrow(() -> new MailRecipientPolicy(new MailRecipientProperties(List.of(), true), new String[]{"prd"}));
    }

    @Test
    @DisplayName("the allowed profiles can be changed but never to a production one")
    void allowedProfilesConfigurable() {
        var props = new MailRecipientProperties(List.of("a@x.test"), false, List.of(" Staging ", ""));

        assertEquals(List.of("staging"), props.overrideProfiles());
        assertDoesNotThrow(() -> new MailRecipientPolicy(props, new String[]{"staging"}));
        assertEquals(List.of("qa", "dev"), new MailRecipientProperties(List.of(), true, List.of(" ")).overrideProfiles());
        for (var profile : List.of("prd", "prod", "Production")) {
            assertThrows(IllegalStateException.class, () -> new MailRecipientProperties(List.of("a@x.test"), false, List.of("qa", profile)));
        }
    }

    @Test
    @DisplayName("the Spring constructor reads the active profiles from the environment")
    void environmentConstructor() {
        var environment = new org.springframework.mock.env.MockEnvironment();
        environment.setActiveProfiles("prd");

        assertThrows(IllegalStateException.class, () -> new MailRecipientPolicy(new MailRecipientProperties(List.of("a@x.test"), false), environment));
        environment.setActiveProfiles("qa");
        assertDoesNotThrow(() -> new MailRecipientPolicy(new MailRecipientProperties(List.of("a@x.test"), false), environment));
    }
}
