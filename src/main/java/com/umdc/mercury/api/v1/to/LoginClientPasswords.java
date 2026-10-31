package com.umdc.mercury.api.v1.to;

/// The rule every supplied login-client password follows. bcrypt only uses the first 72 bytes, so the password is limited to 72
/// printable ASCII characters (no spaces), where one character is one byte.
final class LoginClientPasswords {

    static final String PATTERN = "^[\\x21-\\x7E]{12,72}$";
    static final String MESSAGE = "password must be 12 to 72 printable ASCII characters without spaces";

    private LoginClientPasswords() {
        // constants only
    }
}
