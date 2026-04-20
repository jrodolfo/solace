package org.orgname.solace.broker.api.dto;

import org.orgname.solace.broker.api.jpa.Message;
import org.springframework.data.domain.Page;

import java.util.List;

public class PagedMessagesResponseDTO {

    private final List<Message> items;
    private final int page;
    private final int size;
    private final long totalElements;
    private final int totalPages;
    private final boolean first;
    private final boolean last;

    public PagedMessagesResponseDTO(Page<Message> messagePage) {
        this.items = messagePage.getContent();
        this.page = messagePage.getNumber();
        this.size = messagePage.getSize();
        this.totalElements = messagePage.getTotalElements();
        this.totalPages = messagePage.getTotalPages();
        this.first = messagePage.isFirst();
        this.last = messagePage.isLast();
    }

    public List<Message> getItems() {
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
}
