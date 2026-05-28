package org.orgname.solace.broker.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * Data Transfer Object representing a paginated response of stored messages.
 * Includes pagination metadata and aggregated lifecycle statistics.
 */
@Data
@AllArgsConstructor
public class PagedMessagesResponseDTO {

    /**
     * DTO containing aggregated counts for various message publication statuses.
     */
    public record LifecycleCountsDTO(
            long publishedCount,
            long failedCount,
            long pendingCount,
            long stalePendingCount,
            long retryableFailedCount,
            long nonRetryableFailedCount) {
    }

    /**
     * The list of message DTOs for the current page.
     */
    private final List<StoredMessageDTO> items;

    /**
     * Current page number (zero-based).
     */
    private final int page;

    /**
     * Number of items per page.
     */
    private final int size;

    /**
     * Total number of elements across all pages.
     */
    private final long totalElements;

    /**
     * Total number of available pages.
     */
    private final int totalPages;

    /**
     * Whether this is the first page.
     */
    private final boolean first;

    /**
     * Whether this is the last page.
     */
    private final boolean last;

    /**
     * Summary of message statuses across all messages matching the query (not just the current page).
     */
    private final LifecycleCountsDTO lifecycleCounts;

    /**
     * Constructs a {@link PagedMessagesResponseDTO} from a Spring {@link Page}.
     *
     * @param page            The Spring Data page object.
     * @param itemMapper      A function to map items from the page to {@link StoredMessageDTO}.
     * @param lifecycleCounts Aggregate lifecycle statistics.
     * @param <T>             The type of elements in the Spring page.
     */
    public <T> PagedMessagesResponseDTO(Page<T> page, Function<T, StoredMessageDTO> itemMapper, LifecycleCountsDTO lifecycleCounts) {
        this.items = page.getContent().stream().map(itemMapper).toList();
        this.page = page.getNumber();
        this.size = page.getSize();
        this.totalElements = page.getTotalElements();
        this.totalPages = page.getTotalPages();
        this.first = page.isFirst();
        this.last = page.isLast();
        this.lifecycleCounts = lifecycleCounts;
    }

    /**
     * Factory method to create a response from a page of JPA {@link org.orgname.solace.broker.api.jpa.Message} entities.
     *
     * @param messagePage     The page of message entities.
     * @param lifecycleCounts Aggregate lifecycle statistics.
     * @return A new instance of {@link PagedMessagesResponseDTO}.
     */
    public static PagedMessagesResponseDTO fromMessages(Page<org.orgname.solace.broker.api.jpa.Message> messagePage, LifecycleCountsDTO lifecycleCounts) {
        return new PagedMessagesResponseDTO(messagePage, StoredMessageDTO::new, lifecycleCounts);
    }
}
