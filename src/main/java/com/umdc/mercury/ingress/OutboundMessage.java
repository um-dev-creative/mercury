package com.umdc.mercury.ingress;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * A message normalised for any channel: what the ingress hands to a {@link ChannelProvider}. {@code target} is the recipient address of the channel
 * (an e-mail address, a phone number in E.164, a chat id, a device token...). Never logged whole: it carries personal data.
 *
 * @param id         the id answered to the caller (the tracking id)
 * @param channel    channel code, lower case
 * @param userId     the owner of the message in the calling application
 * @param target     recipient address in the channel
 * @param templateId the template registered in mercury (required by e-mail, optional elsewhere)
 * @param language   language of the content
 * @param from       sender (e-mail)
 * @param subject    subject (e-mail) or title (push)
 * @param body       text; for e-mail the template renders the real one
 * @param sendDate   not before this instant (UTC); {@code null} = as soon as possible
 * @param params     what the template prints
 * @param options    channel-specific options (e.g. {@code sender_id}, {@code parse_mode}, {@code platform})
 * @param mediaUrl   attached media, when the channel supports it
 * @param mediaType  {@code IMAGE}, {@code VIDEO} or {@code NONE}
 */
public record OutboundMessage(UUID id, String channel, UUID userId, String target, UUID templateId, String language, String from, String subject,
                              String body, LocalDateTime sendDate, Map<String, Object> params, Map<String, Object> options, String mediaUrl, String mediaType) {

    /** No target, body or parameters: they are personal data and mercury logs what it handles. */
    @Override
    public String toString() {
        return "OutboundMessage{id=" + id + ", channel=" + channel + ", userId=" + userId + ", templateId=" + templateId + ", sendDate=" + sendDate + "}";
    }
}
