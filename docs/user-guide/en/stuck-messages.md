# Messages that stay queued

E-mails (including verification codes) are processed once a minute. Until now a message of another channel waiting in the same queue (a Telegram,
SMS, WhatsApp or Push message that had not been sent) could stop the e-mails behind it, so a verification code never arrived or was never stored.

E-mails and channel messages are now read separately: a waiting SMS (there is no SMS provider yet) no longer blocks any e-mail. A malformed queued
message is skipped and written to the log as `Skipping OPENED message <id>: it has no template definition`.
