package net.jrodolfo.solace.broker.api.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import net.jrodolfo.solace.broker.api.config.BrokerApiProperties;
import net.jrodolfo.solace.broker.api.dto.FilteredMessagesExportResponseDTO;
import net.jrodolfo.solace.broker.api.dto.InnerMessageDTO;
import net.jrodolfo.solace.broker.api.dto.MessageWrapperDTO;
import net.jrodolfo.solace.broker.api.dto.PagedMessagesResponseDTO;
import net.jrodolfo.solace.broker.api.dto.PayloadDTO;
import net.jrodolfo.solace.broker.api.dto.StoredMessageDTO;
import net.jrodolfo.solace.broker.api.jpa.DeliveryMode;
import net.jrodolfo.solace.broker.api.jpa.Message;
import net.jrodolfo.solace.broker.api.jpa.Payload;
import net.jrodolfo.solace.broker.api.jpa.PayloadType;
import net.jrodolfo.solace.broker.api.jpa.Property;
import net.jrodolfo.solace.broker.api.jpa.PublishStatus;
import net.jrodolfo.solace.broker.api.repository.MessageRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * JPA-backed implementation of the stored publish-attempt lifecycle.
 *
 * <p>This implementation owns two important behaviors:
 * <ul>
 *   <li>building normalized stored-message entities from incoming publish requests</li>
 *   <li>moving existing rows through lifecycle transitions used by publish, retry,
 *       and stale-pending reconciliation flows</li>
 * </ul>
 *
 * <p>Lifecycle updates use repository-level update queries, so the persistence
 * context is cleared before re-reading entities to avoid returning stale state.
 */
@Service
public class DatabaseImpl implements Database {

    private static final String RETRY_BLOCKED_REASON = "Retries are supported only for messages published with server-side broker configuration.";
    /**
     * JPA entity manager used to clear the persistence context.
     */
    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Repository for performing CRUD operations on {@link Message} entities.
     */
    private final MessageRepository messageRepository;
    private final BrokerApiProperties brokerApiProperties;

    /**
     * Constructs a new {@code DatabaseImpl} with the specified message repository.
     *
     * @param messageRepository the repository for {@link Message} entities
     */
    public DatabaseImpl(MessageRepository messageRepository, BrokerApiProperties brokerApiProperties) {
        this.messageRepository = messageRepository;
        this.brokerApiProperties = brokerApiProperties;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public PagedMessagesResponseDTO getAllMessages(
            int page,
            int size,
            String destination,
            DeliveryMode deliveryMode,
            PayloadType payloadType,
            String innerMessageId,
            PublishStatus publishStatus,
            boolean stalePendingOnly,
            LocalDateTime createdAtFrom,
            LocalDateTime createdAtTo,
            LocalDateTime publishedAtFrom,
            LocalDateTime publishedAtTo,
            String sortBy,
            String sortDirection) {
        Specification<Message> specification = buildReadSpecification(
                destination,
                deliveryMode,
                payloadType,
                innerMessageId,
                publishStatus,
                stalePendingOnly,
                createdAtFrom,
                createdAtTo,
                publishedAtFrom,
                publishedAtTo
        );
        PageRequest pageRequest = PageRequest.of(page, size, buildSort(sortBy, sortDirection));
        PagedMessagesResponseDTO.LifecycleCountsDTO lifecycleCounts = lifecycleCounts(specification);
        return new PagedMessagesResponseDTO(
                messageRepository.findAll(specification, pageRequest),
                message -> new StoredMessageDTO(message, brokerApiProperties.getLifecycle().getStalePendingThreshold()),
                lifecycleCounts
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FilteredMessagesExportResponseDTO exportMessages(
            String destination,
            DeliveryMode deliveryMode,
            PayloadType payloadType,
            String innerMessageId,
            PublishStatus publishStatus,
            boolean stalePendingOnly,
            LocalDateTime createdAtFrom,
            LocalDateTime createdAtTo,
            LocalDateTime publishedAtFrom,
            LocalDateTime publishedAtTo,
            String sortBy,
            String sortDirection) {
        Specification<Message> specification = buildReadSpecification(
                destination,
                deliveryMode,
                payloadType,
                innerMessageId,
                publishStatus,
                stalePendingOnly,
                createdAtFrom,
                createdAtTo,
                publishedAtFrom,
                publishedAtTo
        );
        List<Message> messages = messageRepository.findAll(specification, buildSort(sortBy, sortDirection));
        return FilteredMessagesExportResponseDTO.fromMessages(
                LocalDateTime.now(),
                new FilteredMessagesExportResponseDTO.FiltersDTO(
                        destination,
                        deliveryMode,
                        payloadType,
                        innerMessageId,
                        publishStatus,
                        stalePendingOnly,
                        createdAtFrom,
                        createdAtTo,
                        publishedAtFrom,
                        publishedAtTo,
                        sortBy,
                        sortDirection
                ),
                lifecycleCounts(specification),
                messages,
                brokerApiProperties.getLifecycle().getStalePendingThreshold()
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Message savePendingMessage(MessageWrapperDTO wrapper) {

        // Create the main Message entity
        Message message = new Message();
        InnerMessageDTO inner = wrapper.getMessage();
        message.setInnerMessageId(inner.getInnerMessageId());
        message.setDestination(inner.getDestination());
        message.setDeliveryMode(inner.getDeliveryMode());
        message.setPriority(inner.getPriority());
        message.setPublishStatus(PublishStatus.PENDING);
        message.setFailureReason(null);
        message.setPublishedAt(null);
        message.setRetrySupported(!wrapper.parametersAreValid());
        message.setRetryBlockedReason(wrapper.parametersAreValid() ? RETRY_BLOCKED_REASON : null);

        // Create and attach Payload entity
        Payload payload = new Payload();
        PayloadDTO payloadDTO = inner.getPayload();
        payload.setType(payloadDTO.getType());
        payload.setContent(payloadDTO.getContent());
        payload.setMessage(message);
        message.setPayload(payload);

        // Create and attach Property entities if provided
        Map<String, String> propMap = inner.getProperties();
        if (propMap != null && !propMap.isEmpty()) {
            List<Property> properties = new ArrayList<>();
            propMap.forEach((key, value) -> {
                Property prop = new Property();
                prop.setPropertyKey(key);
                prop.setPropertyValue(value);
                prop.setMessage(message);
                properties.add(prop);
            });
            message.setProperties(properties);
        }

        // Save the entire structure (cascade persists related entities)
        messageRepository.save(message);
        return message;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Message findMessageById(Long messageId) {
        return getRequiredMessage(messageId);
    }

    /**
     * {@inheritDoc}
     */
    @Transactional
    @Override
    public Message markMessagePending(Long messageId) {
        messageRepository.markPending(messageId, PublishStatus.PENDING);
        clearPersistenceContext();
        return getRequiredMessage(messageId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public Message markMessagePublished(Long messageId) {
        messageRepository.markPublished(messageId, PublishStatus.PUBLISHED, LocalDateTime.now());
        clearPersistenceContext();
        return getRequiredMessage(messageId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public Message markMessageFailed(Long messageId, String failureReason) {
        messageRepository.markFailed(messageId, PublishStatus.FAILED, failureReason);
        clearPersistenceContext();
        return getRequiredMessage(messageId);
    }

    /**
     * Retrieves a message by its ID or throws a {@link NoSuchElementException} if not found.
     *
     * @param messageId the database ID of the message
     * @return the found {@link Message} entity
     * @throws NoSuchElementException if the message does not exist
     */
    private Message getRequiredMessage(Long messageId) {
        return messageRepository.findById(messageId)
                .orElseThrow(() -> new NoSuchElementException("Message not found for id " + messageId));
    }

    /**
     * Checks if a string is not null and not empty after trimming.
     *
     * @param value the string to check
     * @return {@code true} if the string has content; {@code false} otherwise
     */
    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    /**
     * Creates a specification for a case-insensitive "contains" search on a string field.
     *
     * @param fieldName the name of the entity field
     * @param value     the search term
     * @return a {@link Specification} for the like query
     */
    private static Specification<Message> stringContains(String fieldName, String value) {
        String normalizedValue = "%" + value.trim().toLowerCase() + "%";
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.like(criteriaBuilder.lower(root.get(fieldName)), normalizedValue);
    }

    /**
     * Creates a specification for an equality check on an enum field.
     *
     * @param <T>       the enum type
     * @param fieldName the name of the entity field
     * @param value     the enum value to match
     * @return a {@link Specification} for the equality check
     */
    private static <T extends Enum<T>> Specification<Message> enumEquals(String fieldName, T value) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get(fieldName), value);
    }

    /**
     * Creates a specification for an equality check on an enum field within an associated entity.
     *
     * @param <T>              the enum type
     * @param associationField the name of the association field (e.g., "payload")
     * @param fieldName        the name of the field within the associated entity
     * @param value            the enum value to match
     * @return a {@link Specification} for the nested equality check
     */
    private static <T extends Enum<T>> Specification<Message> nestedEnumEquals(String associationField, String fieldName, T value) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get(associationField).get(fieldName), value);
    }

    /**
     * Creates a specification for an equality check on a boolean field.
     *
     * @param fieldName the name of the entity field
     * @param value     the boolean value to match
     * @return a {@link Specification} for the equality check
     */
    private static Specification<Message> booleanEquals(String fieldName, boolean value) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get(fieldName), value);
    }

    /**
     * Creates a specification for a greater-than-or-equal check on a date-time field.
     *
     * @param fieldName the name of the entity field
     * @param value     the start date-time
     * @return a {@link Specification} for the range check
     */
    private static Specification<Message> dateTimeOnOrAfter(String fieldName, LocalDateTime value) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.greaterThanOrEqualTo(root.get(fieldName), value);
    }

    /**
     * Creates a specification for a less-than-or-equal check on a date-time field.
     *
     * @param fieldName the name of the entity field
     * @param value     the end date-time
     * @return a {@link Specification} for the range check
     */
    private static Specification<Message> dateTimeOnOrBefore(String fieldName, LocalDateTime value) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.lessThanOrEqualTo(root.get(fieldName), value);
    }

    /**
     * Creates a specification for messages that are in {@code PENDING} state and
     * have exceeded the staleness threshold.
     *
     * @return a {@link Specification} for stale pending messages
     */
    private Specification<Message> stalePendingOnly() {
        LocalDateTime cutoff = LocalDateTime.now().minus(brokerApiProperties.getLifecycle().getStalePendingThreshold());
        return (root, query, criteriaBuilder) -> criteriaBuilder.and(
                criteriaBuilder.equal(root.get("publishStatus"), PublishStatus.PENDING),
                criteriaBuilder.isNotNull(root.get("createdAt")),
                criteriaBuilder.lessThan(root.get("createdAt"), cutoff)
        );
    }

    /**
     * Counts messages matching the base specification that have the given status.
     *
     * @param baseSpecification the filters to apply
     * @param publishStatus     the status to count
     * @return the count of matching messages
     */
    private long countByStatus(Specification<Message> baseSpecification, PublishStatus publishStatus) {
        return messageRepository.count(baseSpecification.and(enumEquals("publishStatus", publishStatus)));
    }

    /**
     * Counts messages matching the base specification that are stale and pending.
     *
     * @param baseSpecification the filters to apply
     * @return the count of stale pending messages
     */
    private long countStalePending(Specification<Message> baseSpecification) {
        return messageRepository.count(baseSpecification.and(stalePendingOnly()));
    }

    /**
     * Counts messages matching the base specification that are in {@code FAILED} state
     * and have the specified retry support status.
     *
     * @param baseSpecification the filters to apply
     * @param retrySupported    whether to count messages where retry is supported
     * @return the count of matching failed messages
     */
    private long countFailedByRetrySupport(Specification<Message> baseSpecification, boolean retrySupported) {
        return messageRepository.count(
                baseSpecification
                        .and(enumEquals("publishStatus", PublishStatus.FAILED))
                        .and(booleanEquals("retrySupported", retrySupported))
        );
    }

    /**
     * Dynamically builds a JPA Specification based on the provided search filters.
     *
     * @param destination      filter by Solace destination/topic
     * @param deliveryMode     filter by {@link DeliveryMode}
     * @param payloadType      filter by {@link PayloadType}
     * @param innerMessageId   filter by the ID assigned by the client
     * @param publishStatus    filter by {@link PublishStatus}
     * @param stalePendingOnly if true, only return messages stuck in {@code PENDING}
     * @param createdAtFrom    filter by creation start timestamp
     * @param createdAtTo      filter by creation end timestamp
     * @param publishedAtFrom  filter by publish start timestamp
     * @param publishedAtTo    filter by publish end timestamp
     * @return a composed {@link Specification} for the query
     */
    private Specification<Message> buildReadSpecification(
            String destination,
            DeliveryMode deliveryMode,
            PayloadType payloadType,
            String innerMessageId,
            PublishStatus publishStatus,
            boolean stalePendingOnly,
            LocalDateTime createdAtFrom,
            LocalDateTime createdAtTo,
            LocalDateTime publishedAtFrom,
            LocalDateTime publishedAtTo) {
        Specification<Message> specification = Specification.where(null);

        if (hasText(destination)) {
            specification = specification.and(stringContains("destination", destination));
        }
        if (deliveryMode != null) {
            specification = specification.and(enumEquals("deliveryMode", deliveryMode));
        }
        if (payloadType != null) {
            specification = specification.and(nestedEnumEquals("payload", "type", payloadType));
        }
        if (hasText(innerMessageId)) {
            specification = specification.and(stringContains("innerMessageId", innerMessageId));
        }
        if (publishStatus != null) {
            specification = specification.and(enumEquals("publishStatus", publishStatus));
        }
        if (stalePendingOnly) {
            specification = specification.and(stalePendingOnly());
        }
        if (createdAtFrom != null) {
            specification = specification.and(dateTimeOnOrAfter("createdAt", createdAtFrom));
        }
        if (createdAtTo != null) {
            specification = specification.and(dateTimeOnOrBefore("createdAt", createdAtTo));
        }
        if (publishedAtFrom != null) {
            specification = specification.and(dateTimeOnOrAfter("publishedAt", publishedAtFrom));
        }
        if (publishedAtTo != null) {
            specification = specification.and(dateTimeOnOrBefore("publishedAt", publishedAtTo));
        }

        return specification;
    }

    /**
     * Calculates the counts of messages in different states for the filtered results.
     *
     * @param specification the filters applied to the query
     * @return a {@link PagedMessagesResponseDTO.LifecycleCountsDTO} with the counts
     */
    private PagedMessagesResponseDTO.LifecycleCountsDTO lifecycleCounts(Specification<Message> specification) {
        return new PagedMessagesResponseDTO.LifecycleCountsDTO(
                countByStatus(specification, PublishStatus.PUBLISHED),
                countByStatus(specification, PublishStatus.FAILED),
                countByStatus(specification, PublishStatus.PENDING),
                countStalePending(specification),
                countFailedByRetrySupport(specification, true),
                countFailedByRetrySupport(specification, false)
        );
    }

    /**
     * Constructs a {@link Sort} object based on field name and direction.
     *
     * @param sortBy        the field to sort by
     * @param sortDirection the direction ("ASC" or "DESC")
     * @return a configured {@link Sort} object
     */
    private Sort buildSort(String sortBy, String sortDirection) {
        Sort.Direction direction = Sort.Direction.fromString(sortDirection);
        return Sort.by(direction, sortBy);
    }

    /**
     * Clears the current JPA persistence context after repository bulk-update
     * queries so subsequent reads return the actual database state.
     */
    private void clearPersistenceContext() {
        if (entityManager != null) {
            entityManager.clear();
        }
    }

}
