package com.umdc.mercury.jpa.nosql.document;

import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

@Document(collection = "messages")
public class PushNotificationMessageDocument extends MessageDocument {
    private String deviceToken;
    private String platform;
    private String title;
    private String body;
    private String icon;
    private String image;
    private String clickAction;
    private String priority;
    private Integer badge;
    private String sound;
    /** Opaque map delivered to the app with the notification (ids only, never content). */
    private Map<String, Object> data;
    /** Expo receipt id of the accepted ticket; used to ask later whether the device accepted it. */
    private String providerReceiptId;
    /** When the provider accepted the message (UTC). */
    private LocalDateTime sentAt;
    /** Delivery attempts that ended in a transient failure; bounded by {@code umdc.mercury.push.expo.max-delivery-attempts}. */
    private Integer deliveryAttempts;
    /** When the receipt was read (UTC); {@code null} while it is still pending. */
    private LocalDateTime receiptCheckedAt;

    public String getDeviceToken() {
        return deviceToken;
    }

    public void setDeviceToken(String deviceToken) {
        this.deviceToken = deviceToken;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getClickAction() {
        return clickAction;
    }

    public void setClickAction(String clickAction) {
        this.clickAction = clickAction;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public Integer getBadge() {
        return badge;
    }

    public void setBadge(Integer badge) {
        this.badge = badge;
    }

    public String getSound() {
        return sound;
    }

    public void setSound(String sound) {
        this.sound = sound;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    public String getProviderReceiptId() {
        return providerReceiptId;
    }

    public void setProviderReceiptId(String providerReceiptId) {
        this.providerReceiptId = providerReceiptId;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }

    public Integer getDeliveryAttempts() {
        return deliveryAttempts;
    }

    public void setDeliveryAttempts(Integer deliveryAttempts) {
        this.deliveryAttempts = deliveryAttempts;
    }

    public LocalDateTime getReceiptCheckedAt() {
        return receiptCheckedAt;
    }

    public void setReceiptCheckedAt(LocalDateTime receiptCheckedAt) {
        this.receiptCheckedAt = receiptCheckedAt;
    }
}
