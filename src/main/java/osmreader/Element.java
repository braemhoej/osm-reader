package osmreader;

public interface Element {
    enum Type {
        WAY,
        NODE,
        RELATION
    }
    Type getType();
}

