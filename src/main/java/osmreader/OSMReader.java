package osmreader;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OSMReader implements Closeable {
    private static final XMLInputFactory xmlInputFactory = XMLInputFactory.newFactory();
    private final XMLStreamReader xmlStreamReader;
    private Element elementCache;

    public OSMReader(Reader reader) throws XMLStreamException {
        xmlStreamReader = xmlInputFactory.createXMLStreamReader(reader);
        cacheNextElement();
    }

    public Element next() throws XMLStreamException {
        Element output = elementCache;
        cacheNextElement();
        return output;
    }

    public boolean hasNext() {
        return elementCache != null;
    }

    private void cacheNextElement() throws XMLStreamException {
        while (xmlStreamReader.hasNext()) {
            xmlStreamReader.next();
            if (xmlStreamReader.isStartElement()) {
                switch (xmlStreamReader.getName().getLocalPart()) {
                    case "node" -> {
                        this.elementCache = createNodeElement();
                        return;
                    }
                    case "way" -> {
                        this.elementCache = createWayElement();
                        return;
                    }
                    default -> {
                    }
                }
            }
        }
        this.elementCache = null;
    }

    public Element createNodeElement() {
        String id = xmlStreamReader.getAttributeValue(null, "id");
        String lat = xmlStreamReader.getAttributeValue(null, "lat");
        String lon = xmlStreamReader.getAttributeValue(null, "lon");
        return new NodeElement(id, lat, lon);
    }

    public Element createWayElement() throws XMLStreamException {
        String id = xmlStreamReader.getAttributeValue(null, "id");
        List<String> nodes = new ArrayList<>();
        Map<String, String> tags = new HashMap<>();
        while (xmlStreamReader.hasNext()) {
            xmlStreamReader.next();
            if (xmlStreamReader.isEndElement()) {
                if (xmlStreamReader.getName().getLocalPart().equals("way"))
                    break;
            } else if (xmlStreamReader.isStartElement()) {
                switch (xmlStreamReader.getName().getLocalPart()) {
                    case "nd" -> {
                        String ref = xmlStreamReader.getAttributeValue(null, "ref");
                        nodes.add(ref);
                    }
                    case "tag" -> {
                        String key = xmlStreamReader.getAttributeValue(null, "k");
                        String value = xmlStreamReader.getAttributeValue(null, "v");
                        tags.put(key, value);
                    }
                    default -> { }
                }
            }
        }
        return new WayElement(id, nodes, tags);
    }

    @Override
    public void close() throws IOException {
        try {
            xmlStreamReader.close();
        } catch (XMLStreamException e) {
            e.printStackTrace();
        }
    }
}
