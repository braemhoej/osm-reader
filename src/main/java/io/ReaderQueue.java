package io;

import java.util.Comparator;
import java.util.PriorityQueue;

public class ReaderQueue extends PriorityQueue<CachedReader> {

    public ReaderQueue(Comparator<String> comparator) {
        super((o1, o2) -> comparator.compare(o1.peek(), o2.peek()));
    }
}
