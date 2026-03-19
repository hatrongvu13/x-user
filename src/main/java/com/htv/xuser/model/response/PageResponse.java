package com.htv.xuser.model.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.time.Instant;
import java.util.List;

/**
 * PageResponse<T> — Response phân trang chuẩn, nhúng vào ApiResponse.data
 *
 * Cấu trúc điển hình khi wrap vào ApiResponse:
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
 *
 * Cách dùng:
 * <pre>
 *   return ResponseEntity.ok(ApiResponse.ok(PageResponse.of(userPage)));
 * </pre>
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class PageResponse<T> {

    private final List<T> content;
    private final int     page;           // trang hiện tại, 0-based
    private final int     size;           // số phần tử mỗi trang
    private final long    totalElements;
    private final int     totalPages;
    private final boolean last;

    private PageResponse(List<T> content, int page, int size,
                         long totalElements, int totalPages, boolean last) {
        this.content       = content;
        this.page          = page;
        this.size          = size;
        this.totalElements = totalElements;
        this.totalPages    = totalPages;
        this.last          = last;
    }

    public static <T> PageResponse<T> of(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }
}
