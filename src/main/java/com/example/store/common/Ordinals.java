package com.example.store.common;

import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.IntStream;

public final class Ordinals {

    private static final int FIRST_ORDINAL = 1;

    private Ordinals() {
    }

    public static <S, R> List<R> assign(List<S> sources, BiFunction<Integer, S, R> mapper) {
        return IntStream.range(0, sources.size())
                .mapToObj(index -> mapper.apply(index + FIRST_ORDINAL, sources.get(index)))
                .toList();
    }
}
