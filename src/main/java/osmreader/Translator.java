package osmreader;

import io.FileSorter;
import io.FileUtility;

import javax.xml.stream.XMLStreamException;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Stream;

public class Translator implements Closeable {
    private final Reader reader;
    private final IDStrategy idStrategy;
    private Map<String, List<String>> filter;
    private FileSorter sorter;
    private Charset charset;
    private final Path tempDirectory;

    public Translator(Reader reader, long maxChunkSize, IDStrategy idStrategy) throws IOException {
        this.reader = reader;
        this.idStrategy = idStrategy;
        this.tempDirectory = Files.createTempDirectory("osm_reader_temporary");
        this.charset = StandardCharsets.US_ASCII;
        this.sorter = new FileSorter(maxChunkSize, charset, null);
        this.sorter.setTempDirectory(tempDirectory);
    }

    public void translate(Path outputDirectory) throws XMLStreamException, IOException {
        // Step one: Extract relevant elements from XML, and write to file.
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss");
        System.out.println(dtf.format(LocalDateTime.now())+" : Processing XML");
        Path[] files = extractXMLToFiles();
        Path nodes = files[0];
        Path edges = files[1];
        Path registrations = files[2];
        // Step two: Sort nodes, and registrations.
        System.out.println(dtf.format(LocalDateTime.now())+" : Sorting nodes : " + String.format("%,d kilobytes", Files.size(nodes) / 1024));
        sorter.setLineComparator(Comparator.comparing(o -> o.split(",")[0]));
        Path sortedNodes = sorter.externalSort(nodes);

        System.out.println(dtf.format(LocalDateTime.now())+" : Sorting node registrations : " + String.format("%,d kilobytes", Files.size(registrations) / 1024));
        sorter.setLineComparator(String::compareTo);
        Path sortedRegistration = sorter.externalSort(registrations);

        // Step three: Filter redundant nodes:
        System.out.println(dtf.format(LocalDateTime.now())+" : Filtering nodes");
        Path filteredNodes = FileUtility.filterFile(sortedNodes, tempDirectory, sortedRegistration, ((o1, o2) -> o1.split(",")[0].compareTo(o2)));
        System.out.println(dtf.format(LocalDateTime.now())+" : Filtered nodes : " + String.format("%,d kilobytes", Files.size(filteredNodes) / 1024));

        // Step four: Generate new IDs.
        System.out.println(dtf.format(LocalDateTime.now())+" : Generating new node IDs");
        Path unsortedNodesWithNewIds = generateNewIDs(filteredNodes);

        // Step five: Sort nodes by old ID.
        System.out.println(dtf.format(LocalDateTime.now())+" : Sorting nodes by old ID : " + String.format("%,d kilobytes", Files.size(unsortedNodesWithNewIds) / 1024));
        sorter.setLineComparator(Comparator.comparing(o -> o.split(",")[1]));
        Path nodesWithNewIDs = sorter.externalSort(unsortedNodesWithNewIds);

        // Step five: Sort edges by origin:
        System.out.println(dtf.format(LocalDateTime.now())+" : Sorting edges by origin : " + String.format("%,d kilobytes", Files.size(edges) / 1024));
        sorter.setLineComparator(Comparator.comparing(o -> o.split(",")[0]));
        Path edgesSortedByOrigin = sorter.externalSort(edges);

        // Step six: Replace origin IDs with new IDs.
        System.out.println(dtf.format(LocalDateTime.now())+" : Replacing origin IDs with new IDs");
        Path edgesWithNewOriginID = replaceIDs(nodesWithNewIDs, edgesSortedByOrigin, 0);

        // Step seven: Sort edges by destination:
        System.out.println(dtf.format(LocalDateTime.now())+" : Sorting edges by destination");
        sorter.setLineComparator(Comparator.comparing(o -> o.split(",")[1]));
        Path edgesSortedByDestination = sorter.externalSort(edgesWithNewOriginID);

        System.out.println(dtf.format(LocalDateTime.now())+" : Replacing destination IDs with new IDs");

        // Step eight: Replace destination with new ID.
        Path edgesWithNewIDs = replaceIDs(nodesWithNewIDs, edgesSortedByDestination, 1);

        // Step eight point two five : Sort edges by origin for easier loading
        sorter.setLineComparator(Comparator.comparing(o -> Integer.parseInt(o.split(",")[0])));
        Path outputEdges = sorter.externalSort(edgesWithNewIDs);

        // Step eight point five : Sort nodes for easy loading.
        sorter.setLineComparator(Comparator.comparing(o -> o.split(",")[0]));
        Path outputNodes = sorter.externalSort(nodesWithNewIDs);

        System.out.println(dtf.format(LocalDateTime.now())+" : Copying files to : "+outputDirectory.toString());
        // Step nine: Copy temporary output files to permanent output directory.
        FileUtility.copyWithOverwrite(outputEdges, Paths.get(outputDirectory.toString(),"edges.txt"));
        FileUtility.copyWithOverwrite(outputNodes, Paths.get(outputDirectory.toString(),"nodes.txt"));

    }

    private Path[] extractXMLToFiles() throws XMLStreamException, IOException {
        // Create temporary output files...
        Path nodes = Files.createTempFile(tempDirectory,"tmp","nodes");
        Path edges = Files.createTempFile(tempDirectory, "tmp","nodes");
        Path registration = Files.createTempFile(tempDirectory, "tmp", "reg");

        try (OSMReader osmReader = new OSMReader(reader);
             BufferedWriter nodeWriter = Files.newBufferedWriter(nodes, charset);
             BufferedWriter edgeWriter = Files.newBufferedWriter(edges, charset);
             BufferedWriter nodeRegistrationWriter = Files.newBufferedWriter(registration, charset)) {
            while (osmReader.hasNext()) {
                Element element = osmReader.next();
                switch (element.getType()) {
                    case NODE -> {
                        NodeElement nodeElement = (NodeElement) element;
                        String nodeEntry = nodeElement.id()+","+nodeElement.lat()+","+nodeElement.lon();
                        nodeWriter.write(nodeEntry);
                        nodeWriter.newLine();
                    }
                    case WAY -> {
                        WayElement wayElement = (WayElement) element;
                        boolean isOneWay = isOneWay(wayElement.tags()) || isRoundabout(wayElement.tags());
                        boolean isReversed = isReverse(wayElement.tags());
                        if (!hasCorrectTags(wayElement.tags()))
                            break;
                        for (String id : wayElement.nodes()) {
                            nodeRegistrationWriter.write(id);
                            nodeRegistrationWriter.newLine();
                        }
                        for (int index = 0; index < wayElement.nodes().size()-1; index ++) {
                            if (!isReversed) {
                                String edgeEntry = wayElement.nodes().get(index)+","+wayElement.nodes().get(index+1);
                                edgeWriter.write(edgeEntry);
                                edgeWriter.newLine();
                            }
                            if (!isOneWay || isReversed) {
                                int reverseIndex = wayElement.nodes().size() - index - 1;
                                String reverseEdgeEntry = wayElement.nodes().get(reverseIndex)+","+wayElement.nodes().get(reverseIndex-1);
                                edgeWriter.write(reverseEdgeEntry);
                                edgeWriter.newLine();
                            }
                        }
                    }
                    case RELATION -> { }
                }
            }
        }
        return new Path[]{nodes, edges, registration};
    }

    private Path generateNewIDs(Path input) throws IOException {
        Path temp = Files.createTempFile(tempDirectory,"tmp","newID");
        try (BufferedReader reader = Files.newBufferedReader(input, charset);
            BufferedWriter writer = Files.newBufferedWriter(temp, charset)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] splitLine = line.split(",");
                String newID = idStrategy.generateID(splitLine[0], splitLine[1], splitLine[2]);
                String entry = newID+","+line;
                writer.write(entry);
                writer.newLine();
            }
        }
        Path output = Files.createTempFile(tempDirectory,"tmp", "newID");
        sorter.setLineComparator((Comparator.comparing(o -> Long.parseLong(o.split(",")[0]))));
        Path sortedTemp = sorter.externalSort(temp);
        try (BufferedReader reader = Files.newBufferedReader(sortedTemp, charset);
             BufferedWriter writer = Files.newBufferedWriter(output, charset)) {
            long counter = 0;
            String line;
            while ((line = reader.readLine()) != null) {
                String[] splitLine = line.split(",");
                String newID = Long.toString(counter);
                counter++;
                String entry = newID+","+String.join(",", Arrays.copyOfRange(splitLine, 1, 4));
                writer.write(entry);
                writer.newLine();
            }
        }
        return output;
    }

    private Path replaceIDs(Path nodes, Path edges, int index) throws IOException {
        Path output = Files.createTempFile(tempDirectory,"tmp", "replaceID");
        try (Scanner nodeScanner = new Scanner(nodes);
             Scanner edgeScanner = new Scanner(edges);
             BufferedWriter writer = Files.newBufferedWriter(output, charset)) {
            if (!nodeScanner.hasNext())
                return output;
            String nodeEntry = nodeScanner.nextLine();
            String nodeID = nodeEntry.split(",")[1];
            String newNodeID = nodeEntry.split(",")[0];
            String edgeID;
            while (edgeScanner.hasNext()) {
                String edge = edgeScanner.nextLine();
                edgeID = edge.split(",")[index];
                while (nodeID.compareTo(edgeID) < 0) {
                    nodeEntry = nodeScanner.nextLine();
                    nodeID = nodeEntry.split(",")[1];
                    newNodeID = nodeEntry.split(",")[0];
                }
                if (nodeID.compareTo(edgeID) == 0) {
                    String[] splitEdge = edge.split(",");
                    splitEdge[index] = newNodeID;
                    String entry = splitEdge[0]+","+splitEdge[1];
                    writer.write(entry);
                    writer.newLine();
                }
            }
        }
        return output;
    }

    public void setFilter(Map<String, List<String>> filter) {
        this.filter = filter;
    }

    private boolean hasCorrectTags(Map<String, String> tags) {
        for (String key : tags.keySet())
            if (filter.getOrDefault(key, new ArrayList<>(0)).contains(tags.get(key)))
                return true;
        return false;
    }

    private boolean isOneWay(Map<String, String> tags) {
       String value = tags.get("oneway");
       if (value == null)
           return false;
       return value.equals("yes") || value.equals("1") || value.equals("true") || value.equals("-1");
    }


    private boolean isRoundabout(Map<String, String> tags) {
        String value = tags.get("junction");
        if (value == null)
            return false;
        return value.equals("roundabout");
    }

    private boolean isReverse(Map<String, String> tags) {
        String value = tags.get("oneway");
        if (value == null)
            return false;
        return value.equals("-1");
    }

    @Override
    public void close() throws IOException {
        reader.close();
        // Delete all temporary files...
        try (Stream<Path> walk = Files.walk(tempDirectory)) {
            for (Path file : walk.sorted(Comparator.reverseOrder()).toList())
                Files.delete(file);
        }
    }
}
