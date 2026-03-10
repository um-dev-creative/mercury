package com.prx.mercury.jpa.nosql.repository;

import com.prx.mercury.constant.ChannelType;
import com.prx.mercury.constant.DeliveryStatusType;
import com.prx.mercury.jpa.nosql.document.MessageDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Unified MongoDB repository for all message channel documents.
 */
@Repository
public interface MessageNSRepository extends MongoRepository<MessageDocument, String> {

    /**
     * Find messages by delivery status.
     *
     * @param deliveryStatus the delivery status to filter by
     * @return list of messages with the specified status
     */
    List<MessageDocument> findByDeliveryStatus(DeliveryStatusType deliveryStatus);

    /**
     * Find messages by channel type and delivery status.
     *
     * @param channelType    the channel type to filter by
     * @param deliveryStatus the delivery status to filter by
     * @return list of messages matching both criteria
     */
    List<MessageDocument> findByChannelTypeAndDeliveryStatus(ChannelType channelType, DeliveryStatusType deliveryStatus);

    /**
     * Find messages by campaign ID.
     *
     * @param campaignId the campaign identifier
     * @return list of messages belonging to the campaign
     */
    List<MessageDocument> findByCampaignId(UUID campaignId);

    /**
     * Find messages by campaign ID and delivery status.
     *
     * @param campaignId     the campaign identifier
     * @param deliveryStatus the delivery status to filter by
     * @return list of messages matching both criteria
     */
    List<MessageDocument> findByCampaignIdAndDeliveryStatus(UUID campaignId, DeliveryStatusType deliveryStatus);

    /**
     * Count messages by delivery status.
     *
     * @param deliveryStatus the delivery status to count
     * @return count of messages with the specified status
     */
    long countByDeliveryStatus(DeliveryStatusType deliveryStatus);

    /**
     * Count messages by campaign ID.
     *
     * @param campaignId the campaign identifier
     * @return count of messages in the campaign
     */
    long countByCampaignId(UUID campaignId);

    /**
     * Count messages by campaign ID and delivery status.
     *
     * @param campaignId     the campaign identifier
     * @param deliveryStatus the delivery status to count
     * @return count of messages matching both criteria
     */
    long countByCampaignIdAndDeliveryStatus(UUID campaignId, DeliveryStatusType deliveryStatus);

    /**
     * Find messages sent within a date range.
     *
     * @param from start of the date range (inclusive)
     * @param to   end of the date range (inclusive)
     * @return list of messages sent within the range
     */
    List<MessageDocument> findBySendDateBetween(LocalDateTime from, LocalDateTime to);

    /**
     * Find messages by user ID within a date range.
     *
     * @param userId the user identifier
     * @param from   start of the date range (inclusive)
     * @param to     end of the date range (inclusive)
     * @return list of messages for the user within the range
     */
    List<MessageDocument> findByUserIdAndSendDateBetween(UUID userId, LocalDateTime from, LocalDateTime to);

    /**
     * Delete messages by campaign ID. Silently succeeds if no messages are found.
     *
     * @param campaignId the campaign identifier whose messages should be deleted
     */
    void deleteByCampaignId(UUID campaignId);
}
