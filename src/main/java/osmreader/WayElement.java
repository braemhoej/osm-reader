package osmreader;

import java.util.List;
import java.util.Map;

public record WayElement(String id, List<String> nodes, Map<String, String> tags) implements Element {
    @Override
    public Type getType() {
        return Type.WAY;
    }
}
