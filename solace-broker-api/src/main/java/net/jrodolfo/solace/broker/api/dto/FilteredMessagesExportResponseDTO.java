package net.jrodolfo.solace.broker.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import net.jrodolfo.solace.broker.api.jpa.DeliveryMode;
import net.jrodolfo.solace.broker.api.jpa.Message;
import net.jrodolfo.solace.broker.api.jpa.PayloadType;
import net.jrodolfo.solace.broker.api.jpa.PublishStatus;
import net.jrodolfo.solace.broker.api.service.MessageLifecycleSupport;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Data Transfer Object representing the response for an export request of filtered messages.
 * Includes the exported data, the filters applied, and lifecycle statistics.
 */
@Data
@AllArgsConstructor
public class FilteredMessagesExportResponseDTO {

    /**
     * DTO containing the filter criteria applied to the export.
     */
    public record FiltersDTO(
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
    }

    /**
     * Timestamp indicating when the export was generated.
     */
    private final LocalDateTime exportedAt;

    /**
     * The filters used to produce this exported set of messages.
     */
    private final FiltersDTO filters;

    /**
     * Total number of messages included in the export.
     */
    private final long totalElements;

    /**
     * Aggregated lifecycle counts for the messages matching the filters.
     */
    private final PagedMessagesResponseDTO.LifecycleCountsDTO lifecycleCounts;

    /**
     * The list of exported message details.
     */
    private final List<StoredMessageDTO> items;

    /**
     * Factory method to create an export response from a list of JPA {@link Message} entities.
     *
     * @param exportedAt      The export generation timestamp.
     * @param filters         The filters applied.
     * @param lifecycleCounts The aggregated lifecycle counts.
     * @param messages        The list of message entities to export.
     * @return A new instance of {@link FilteredMessagesExportResponseDTO}.
     */
    public static FilteredMessagesExportResponseDTO fromMessages(
            LocalDateTime exportedAt,
            FiltersDTO filters,
            PagedMessagesResponseDTO.LifecycleCountsDTO lifecycleCounts,
            List<Message> messages) {
        return fromMessages(
                exportedAt,
                filters,
                lifecycleCounts,
                messages,
                MessageLifecycleSupport.DEFAULT_STALE_PENDING_THRESHOLD
        );
    }

    /**
     * Factory method to create an export response from a list of JPA {@link Message} entities.
     *
     * @param exportedAt            The export generation timestamp.
     * @param filters               The filters applied.
     * @param lifecycleCounts       The aggregated lifecycle counts.
     * @param messages              The list of message entities to export.
     * @param stalePendingThreshold The threshold used to derive stale pending flags.
     * @return A new instance of {@link FilteredMessagesExportResponseDTO}.
     */
    public static FilteredMessagesExportResponseDTO fromMessages(
            LocalDateTime exportedAt,
            FiltersDTO filters,
            PagedMessagesResponseDTO.LifecycleCountsDTO lifecycleCounts,
            List<Message> messages,
            Duration stalePendingThreshold) {
        return new FilteredMessagesExportResponseDTO(
                exportedAt,
                filters,
                messages.size(),
                lifecycleCounts,
                messages.stream().map(message -> new StoredMessageDTO(message, stalePendingThreshold)).toList()
        );
    }
}
