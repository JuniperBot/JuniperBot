package ru.caramel.juniperbot.web.dto.api;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.Page;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
public class PageDto<T> implements Serializable {

    private static final long serialVersionUID = 9021594952083685187L;

    private final List<T> content;

    private final int number;

    private final int totalPages;

    private final long totalElements;

    public PageDto(Page<T> page) {
        this.content = page.getContent();
        this.number = page.getNumber();
        this.totalPages = page.getTotalPages();
        this.totalElements = page.getTotalElements();
    }
}
