package io;

import java.io.BufferedReader;
import java.io.IOException;

public class CachedReader {
    private String cache;
    private boolean empty;
    private final BufferedReader reader;

    public CachedReader(BufferedReader reader) throws IOException {
        this.reader = reader;
        this.cache = this.reader.readLine();
        this.empty = cache == null;
    }
    public String peek() {
        return cache;
    }

    public boolean isEmpty() {
        return empty;
    }

    public String readLine() throws IOException {
        String temporary = cache;
        cache = this.reader.readLine();
        empty = cache == null;
        return temporary;
    }

    public void close() throws IOException {
        this.reader.close();
    }
}
