package com.prx.mercury.api.v1.to;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * A record that represents an email contact.
 *
 * @param email The email address of the contact.
 * @param name  The name of the contact.
 * @param alias The alias of the contact.
 */
public record EmailContact(@NotNull @NotEmpty @Email String email, @NotNull @NotEmpty String name, String alias) {
    public EmailContact {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email cannot be null or empty");
        }
    }

    @Override
    public String toString() {
        return "EmailContact{" +
                "email='" + email + '\'' +
                ", name='" + name + '\'' +
                ", alias='" + alias + '\'' +
                '}';
    }
}
