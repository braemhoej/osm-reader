package io;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class FileSorter {
    private Comparator<String> lineComparator;
    private long maxChunkSize;
    private Path tempDirectory;
    private Charset charset;

    public FileSorter(long maxChunkSize, Charset charset, Comparator<String> lineComparator) {
        this.maxChunkSize = maxChunkSize;
        this.lineComparator = lineComparator;
        this.tempDirectory = Paths.get(System.getProperty("java.io.tmpdir"));
        this.charset = charset;
    }

    public Path externalSort(Path input) throws IOException {
        Path output = Files.createTempFile("sorted",input.toFile().getName());
        List<Path> sortedBatches = sortInBatches(input);
        return mergeSortedBatches(sortedBatches, output);
    }

    private Path mergeSortedBatches(List<Path> sortedBatches, Path output) throws IOException {
        ReaderQueue readerQueue = new ReaderQueue(lineComparator);
        try (BufferedWriter writer = Files.newBufferedWriter(output, charset)) {
            try {
                for (Path file : sortedBatches)
                    readerQueue.add(new CachedReader(Files.newBufferedReader(file, charset)));
                while (readerQueue.size() > 0) {
                    CachedReader reader = readerQueue.poll();
                    String line = reader.readLine();
                    writer.write(line);
                    writer.newLine();
                    if (!reader.isEmpty())
                        readerQueue.add(reader);
                    else
                        reader.close();
                }
            }
            finally {
                for (CachedReader reader : readerQueue)
                    reader.close();
                for (Path temporaryFile : sortedBatches)
                    Files.delete(temporaryFile);
            }
        }
        return output;
    }

    private List<Path> sortInBatches(Path path) throws IOException {
        List<Path> sortedBatches = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(path, charset)) {
            List<String> chunk = new ArrayList<>();
            long currentChunkSize = 0;
            while (true) {
                String line = reader.readLine();
                if (line == null)
                    break;
                if (currentChunkSize + sizeOf(line) > maxChunkSize) {
                    sortedBatches.add(sortAndSave(chunk));
                    chunk = new ArrayList<>();
                    currentChunkSize=0;
                }
                chunk.add(line);
                currentChunkSize+=sizeOf(line) ;
            }
            if (currentChunkSize > 0)
                sortedBatches.add(sortAndSave(chunk));
        }
        return sortedBatches;
    }

    private Path sortAndSave(List<String> chunk) throws IOException {
        Path tempFile = Files.createTempFile(tempDirectory,"sorted_batch", "temporary_file");
        chunk.sort(lineComparator);
        try (BufferedWriter writer = Files.newBufferedWriter(tempFile, charset)) {
            for (String line : chunk) {
                writer.write(line);
                writer.newLine();
            }
        }
        return tempFile;
    }

    private static long sizeOf(String line) {
        // (Size of string under UTF-16) + object header + array header + object reference
        return (line.length() * 2L) + 16 + 24 + 8;
    }

    public void setLineComparator(Comparator<String> lineComparator) {
        this.lineComparator = lineComparator;
    }

    public void setCharset(Charset charset) {
        this.charset = charset;
    }

    public void setTempDirectory(Path tempDirectory) {
        this.tempDirectory = tempDirectory;
    }
}
