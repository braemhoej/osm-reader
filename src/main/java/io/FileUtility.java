package io;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class FileUtility {

    public static void copyWithOverwrite(Path source, Path destination) throws IOException {
        try {
            Files.copy(source, destination);
        } catch (FileAlreadyExistsException ignored) {
            Files.delete(destination);
            Files.copy(source, destination);
        }
    }

    /**
     *  Filters one file, given a file of node IDs... This method works under the precondition that both files are sorted.
     *  Duplicates in filter file are ignored.
     * @param input path to the file to be filtered.
     * @param filter path to file containing the filter.
     * @return path to new file, containing filtered input.
     */
    public static Path filterFile(Path input, Path tempDirectory, Path filter, Comparator<String> comparator) throws IOException {
        Path output = Files.createTempFile(tempDirectory,"temp", "filtered");
        try (BufferedWriter writer = Files.newBufferedWriter(output, StandardCharsets.US_ASCII);
             Scanner inputScanner = new Scanner(input);
             Scanner filterScanner = new Scanner(filter)) {
            String previousFilterLine = null;
            while (filterScanner.hasNext()) {
                String filterLine = filterScanner.nextLine();
                if (previousFilterLine != null && previousFilterLine.equals(filterLine))
                    continue;
                previousFilterLine = filterLine;
                while (inputScanner.hasNext()) {
                    String inputLine = inputScanner.nextLine();
                    if (comparator.compare(inputLine, filterLine) == 0) {
                        writer.write(inputLine);
                        writer.newLine();
                        break;
                    }
                }
            }
        }
        return output;
    }
}
