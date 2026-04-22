package org.orgname.solace.broker.api.service;

import org.orgname.solace.broker.api.dto.InnerMessageDTO;
import org.orgname.solace.broker.api.dto.FilteredMessagesExportResponseDTO;
import org.orgname.solace.broker.api.dto.MessageWrapperDTO;
import org.orgname.solace.broker.api.dto.PagedMessagesResponseDTO;
import org.orgname.solace.broker.api.dto.PayloadDTO;
import org.orgname.solace.broker.api.jpa.Message;
import org.orgname.solace.broker.api.jpa.Payload;
import org.orgname.solace.broker.api.jpa.PublishStatus;
import org.orgname.solace.broker.api.jpa.Property;
import org.orgname.solace.broker.api.repository.MessageRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.time.LocalDateTime;
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
    @PersistenceContext
    private EntityManager entityManager;
    private final MessageRepository messageRepository;

    public DatabaseImpl(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    @Override
    /**
     * Executes the paginated read contract used by the browser and API tests.
     */
    public PagedMessagesResponseDTO getAllMessages(
            int page,
            int size,
            String destination,
            String deliveryMode,
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
        return PagedMessagesResponseDTO.fromMessages(messageRepository.findAll(specification, pageRequest), lifecycleCounts);
    }

    @Override
    public FilteredMessagesExportResponseDTO exportMessages(
            String destination,
            String deliveryMode,
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
                messages
        );
    }

    @Override
    /**
     * Creates the initial stored record for a publish attempt.
     *
     * <p>New records always start as {@code PENDING}. Retry metadata is derived
     * from whether the original request used explicit broker credentials.
     */
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

    @Override
    /**
     * Loads a stored message by id or fails fast if it does not exist.
     */
    public Message findMessageById(Long messageId) {
        return getRequiredMessage(messageId);
    }

    @Override
    @Transactional
    /**
     * Reclassifies an existing row as {@code PENDING}, typically before retry.
     */
    public Message markMessagePending(Long messageId) {
        messageRepository.markPending(messageId, PublishStatus.PENDING);
        clearPersistenceContext();
        return getRequiredMessage(messageId);
    }

    @Override
    @Transactional
    /**
     * Reclassifies an existing row as {@code PUBLISHED} and stamps publish time.
     */
    public Message markMessagePublished(Long messageId) {
        messageRepository.markPublished(messageId, PublishStatus.PUBLISHED, LocalDateTime.now());
        clearPersistenceContext();
        return getRequiredMessage(messageId);
    }

    @Override
    @Transactional
    /**
     * Reclassifies an existing row as {@code FAILED} with an operator-visible
     * failure reason.
     */
    public Message markMessageFailed(Long messageId, String failureReason) {
        messageRepository.markFailed(messageId, PublishStatus.FAILED, failureReason);
        clearPersistenceContext();
        return getRequiredMessage(messageId);
    }

    private Message getRequiredMessage(Long messageId) {
        return messageRepository.findById(messageId)
                .orElseThrow(() -> new NoSuchElementException("Message not found for id " + messageId));
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static Specification<Message> stringContains(String fieldName, String value) {
        String normalizedValue = "%" + value.trim().toLowerCase() + "%";
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.like(criteriaBuilder.lower(root.get(fieldName)), normalizedValue);
    }

    private static <T extends Enum<T>> Specification<Message> enumEquals(String fieldName, T value) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get(fieldName), value);
    }

    private static Specification<Message> booleanEquals(String fieldName, boolean value) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get(fieldName), value);
    }

    private static Specification<Message> dateTimeOnOrAfter(String fieldName, LocalDateTime value) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.greaterThanOrEqualTo(root.get(fieldName), value);
    }

    private static Specification<Message> dateTimeOnOrBefore(String fieldName, LocalDateTime value) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.lessThanOrEqualTo(root.get(fieldName), value);
    }

    private static Specification<Message> stalePendingOnly() {
        LocalDateTime cutoff = LocalDateTime.now().minus(MessageLifecycleSupport.STALE_PENDING_THRESHOLD);
        return (root, query, criteriaBuilder) -> criteriaBuilder.and(
                criteriaBuilder.equal(root.get("publishStatus"), PublishStatus.PENDING),
                criteriaBuilder.isNotNull(root.get("createdAt")),
                criteriaBuilder.lessThan(root.get("createdAt"), cutoff)
        );
    }

    private long countByStatus(Specification<Message> baseSpecification, PublishStatus publishStatus) {
        return messageRepository.count(baseSpecification.and(enumEquals("publishStatus", publishStatus)));
    }

    private long countStalePending(Specification<Message> baseSpecification) {
        return messageRepository.count(baseSpecification.and(stalePendingOnly()));
    }

    private long countFailedByRetrySupport(Specification<Message> baseSpecification, boolean retrySupported) {
        return messageRepository.count(
                baseSpecification
                        .and(enumEquals("publishStatus", PublishStatus.FAILED))
                        .and(booleanEquals("retrySupported", retrySupported))
        );
    }

    private Specification<Message> buildReadSpecification(
            String destination,
            String deliveryMode,
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
        if (hasText(deliveryMode)) {
            specification = specification.and(stringContains("deliveryMode", deliveryMode));
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
