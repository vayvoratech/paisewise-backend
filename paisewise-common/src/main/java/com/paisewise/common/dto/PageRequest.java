package com.paisewise.common.dto;

import org.springframework.data.domain.Sort;

public class PageRequest {
    private int page = 0;
    private int size = 10;
    private String sortBy = "createdAt";
    private Sort.Direction direction = Sort.Direction.DESC;

    public org.springframework.data.domain.Pageable toSpringPageable() {
        return org.springframework.data.domain.PageRequest.of(page, size, Sort.by(direction, sortBy));
    }

    // Getters and Setters
    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }

    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }

    public String getSortBy() { return sortBy; }
    public void setSortBy(String sortBy) { this.sortBy = sortBy; }

    public Sort.Direction getDirection() { return direction; }
    public void setDirection(Sort.Direction direction) { this.direction = direction; }
}