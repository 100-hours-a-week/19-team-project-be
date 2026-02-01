package org.refit.refitbackend.global.common.dto;

import java.util.List;

public record CursorPage<T> (
        List<T> items,
        String nextCursor,
        boolean hasMore
) { }
