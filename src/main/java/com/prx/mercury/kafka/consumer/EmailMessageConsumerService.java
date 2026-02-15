package com.prx.mercury.kafka.consumer;

import com.prx.commons.exception.StandardException;
import com.prx.mercury.constant.MercuryMessage;
import com.prx.mercury.jpa.nosql.entity.EmailMessageDocument;
import com.prx.mercury.kafka.to.EmailMessageTO;

import java.util.List;
import java.util.Optional;

/**
 * Defines operations for consuming and caching email message data from Kafka.
 * <p>
 * Implementations may persist messages to a cache or database. Default implementations
 * throw {@link StandardException} with {@link MercuryMessage#METHOD_NOT_IMPLEMENTED}.
 */
public interface EmailMessageConsumerService {

    /**
     * Finds all email messages in the cache.
     *
     * @return a list of all cached {@link EmailMessageDocument} instances
     * @throws StandardException when the method is not implemented by the concrete service
     */
    default List<EmailMessageDocument> findAll() {
        throw new StandardException(MercuryMessage.METHOD_NOT_IMPLEMENTED);
    }

    /**
     * Finds an email message in the cache by its id.
     *
     * @param id the id of the email message to find
     * @return an {@link Optional} containing the found {@link EmailMessageTO}, or an empty {@link Optional}
     * when no message exists for the given id
     * @throws StandardException when the method is not implemented by the concrete service
     */
    default Optional<EmailMessageTO> findById(String id) {
        throw new StandardException(MercuryMessage.METHOD_NOT_IMPLEMENTED);
    }

    /**
     * Saves an email message in the cache.
     *
     * @param emailMessageTO the email message to save
     * @return the saved {@link EmailMessageTO}
     * @throws StandardException when the method is not implemented by the concrete service
     */
    default EmailMessageTO save(EmailMessageTO emailMessageTO) {
        throw new StandardException(MercuryMessage.METHOD_NOT_IMPLEMENTED);
    }

    /**
     * Saves a list of email messages in the cache.
     *
     * @param emailMessageTOS list of email messages to save
     * @return the list of saved {@link EmailMessageTO}
     * @throws StandardException when the method is not implemented by the concrete service
     */
    default List<EmailMessageTO> saveAll(List<EmailMessageTO> emailMessageTOS) {
        throw new StandardException(MercuryMessage.METHOD_NOT_IMPLEMENTED);
    }

    /**
     * Deletes an email message from the cache by its id.
     *
     * @param id the id of the email message to delete
     * @throws StandardException when the method is not implemented by the concrete service
     */
    default void deleteById(String id) {
        throw new StandardException(MercuryMessage.METHOD_NOT_IMPLEMENTED);
    }

    /**
     * Deletes all email messages from the cache.
     *
     * @throws StandardException when the method is not implemented by the concrete service
     */
    default void deleteAll() {
        throw new StandardException(MercuryMessage.METHOD_NOT_IMPLEMENTED);
    }

    /**
     * Updates an existing email message in the cache.
     *
     * @param emailMessageTO the email message to update
     * @return the updated {@link EmailMessageTO}
     * @throws StandardException when the method is not implemented by the concrete service
     */
    default EmailMessageTO update(EmailMessageTO emailMessageTO) {
        throw new StandardException(MercuryMessage.METHOD_NOT_IMPLEMENTED);
    }
}
