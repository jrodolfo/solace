package org.orgname.solace.broker.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.orgname.solace.broker.api.jpa.DeliveryMode;
import org.orgname.solace.broker.api.jpa.Message;
import org.orgname.solace.broker.api.jpa.PayloadType;
import org.orgname.solace.broker.api.jpa.PublishStatus;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
public class FilteredMessagesExportResponseDTO {

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

    private final LocalDateTime exportedAt;
    private final FiltersDTO filters;
    private final long totalElements;
    private final PagedMessagesResponseDTO.LifecycleCountsDTO lifecycleCounts;
    private final List<StoredMessageDTO> items;

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
}
