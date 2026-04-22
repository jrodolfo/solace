package org.orgname.solace.broker.api.dto;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

public class PagedMessagesResponseDTO {

    public record LifecycleCountsDTO(
            long publishedCount,
            long failedCount,
            long pendingCount,
            long stalePendingCount,
            long retryableFailedCount,
            long nonRetryableFailedCount) {
    }

    private final List<StoredMessageDTO> items;
    private final int page;
    private final int size;
    private final long totalElements;
    private final int totalPages;
    private final boolean first;
    private final boolean last;
    private final LifecycleCountsDTO lifecycleCounts;

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

    public PagedMessagesResponseDTO(Page<StoredMessageDTO> page, LifecycleCountsDTO lifecycleCounts) {
        this.items = page.getContent();
        this.page = page.getNumber();
        this.size = page.getSize();
        this.totalElements = page.getTotalElements();
        this.totalPages = page.getTotalPages();
        this.first = page.isFirst();
        this.last = page.isLast();
        this.lifecycleCounts = lifecycleCounts;
    }

    public static PagedMessagesResponseDTO fromMessages(Page<org.orgname.solace.broker.api.jpa.Message> messagePage, LifecycleCountsDTO lifecycleCounts) {
        return new PagedMessagesResponseDTO(messagePage, StoredMessageDTO::new, lifecycleCounts);
    }

    public List<StoredMessageDTO> getItems() {
        return items;
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public boolean isFirst() {
        return first;
    }

    public boolean isLast() {
        return last;
    }

    public LifecycleCountsDTO getLifecycleCounts() {
        return lifecycleCounts;
    }
}
