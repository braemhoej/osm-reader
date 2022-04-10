package osmreader;

public record NodeElement(String id, String lat, String lon) implements Element {
    @Override
    public Type getType() {
        return Type.NODE;
    }
}
