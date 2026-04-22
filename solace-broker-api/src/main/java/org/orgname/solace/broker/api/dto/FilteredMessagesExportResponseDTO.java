package org.orgname.solace.broker.api.dto;

import org.orgname.solace.broker.api.jpa.Message;
import org.orgname.solace.broker.api.jpa.PublishStatus;

import java.time.LocalDateTime;
import java.util.List;

public class FilteredMessagesExportResponseDTO {

    public record FiltersDTO(
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
    }

    private final LocalDateTime exportedAt;
    private final FiltersDTO filters;
    private final long totalElements;
    private final PagedMessagesResponseDTO.LifecycleCountsDTO lifecycleCounts;
    private final List<StoredMessageDTO> items;

    public FilteredMessagesExportResponseDTO(
            LocalDateTime exportedAt,
            FiltersDTO filters,
            long totalElements,
            PagedMessagesResponseDTO.LifecycleCountsDTO lifecycleCounts,
            List<StoredMessageDTO> items) {
        this.exportedAt = exportedAt;
        this.filters = filters;
        this.totalElements = totalElements;
        this.lifecycleCounts = lifecycleCounts;
        this.items = items;
    }

    public static FilteredMessagesExportResponseDTO fromMessages(
            LocalDateTime exportedAt,
            FiltersDTO filters,
            PagedMessagesResponseDTO.LifecycleCountsDTO lifecycleCounts,
            List<Message> messages) {
        return new FilteredMessagesExportResponseDTO(
                exportedAt,
                filters,
                messages.size(),
                lifecycleCounts,
                messages.stream().map(StoredMessageDTO::new).toList()
        );
    }

    public LocalDateTime getExportedAt() {
        return exportedAt;
    }

    public FiltersDTO getFilters() {
        return filters;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public PagedMessagesResponseDTO.LifecycleCountsDTO getLifecycleCounts() {
        return lifecycleCounts;
    }

    public List<StoredMessageDTO> getItems() {
        return items;
    }
}
