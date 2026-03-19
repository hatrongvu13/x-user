package com.htv.xuser.model.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.time.Instant;
import java.util.List;

/**
 * PageResponse<T> — Response phân trang chuẩn
 *
 * <pre>
 * {
 *   "success": true,
 *   "code": 200,
 *   "data": {
 *     "content": [...],
 *     "page": 0,
 *     "size": 20,
 *     "totalElements": 150,
 *     "totalPages": 8,
 *     "last": false
 *   },
 *   "timestamp": "..."
 * }
 * </pre>
 *
 * Cách dùng:
 * <pre>
 *   return ResponseEntity.ok(PageResponse.of(userPage));
 * </pre>
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PageResponse<T> {

    private final boolean  success    = true;
    private final int      code       = 200;
    private final PageData<T> data;
    private final Instant timestamp;

    private PageResponse(PageData<T> data) {
        this.data      = data;
        this.timestamp = Instant.now();
    }

    public static <T> PageResponse<T> of(Page<T> page) {
        return new PageResponse<>(new PageData<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        ));
    }

    @Getter
    public static class PageData<T> {
        private final List<T> content;
        private final int     page;
        private final int     size;
        private final long    totalElements;
        private final int     totalPages;
        private final boolean last;

        PageData(List<T> content, int page, int size,
                 long totalElements, int totalPages, boolean last) {
            this.content       = content;
            this.page          = page;
            this.size          = size;
            this.totalElements = totalElements;
            this.totalPages    = totalPages;
            this.last          = last;
        }
    }
}
