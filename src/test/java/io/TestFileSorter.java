package io;
import org.junit.jupiter.api.*;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class TestFileSorter {
    private static List<Path> filesList;
    private static Path tempFile;
    private static Path sortedFile;

    @BeforeAll
    public static void createFiles() throws IOException {
        FileSorter sorter = new FileSorter(1024*1024, StandardCharsets.UTF_8, String::compareTo);
        filesList = new ArrayList<>();
        // Create randomized input file ...
        tempFile = Files.createTempFile("to", "sort");
        filesList.add(tempFile);
        try (BufferedWriter writer = Files.newBufferedWriter(tempFile)) {
            for (int index = 0; index < 10000000; index++)   {
                int number = (int) (Math.random() * 1000000);
                writer.write(Integer.toString(number));
                writer.newLine();
            }
        }
        // Sort input file ...
        sortedFile = sorter.externalSort(tempFile);
        filesList.add(sortedFile);
    }

    @AfterAll
    public static void deleteFiles() throws IOException {
        for (Path file : filesList)
            Files.delete(file);
    }
    @Test
    public void TestSortedFilesAreSorted() throws IOException {
        // Ensure that output file is sorted...
        try (BufferedReader reader = Files.newBufferedReader(sortedFile)) {
            String previous = reader.readLine();
            String line;
            while ((line = reader.readLine()) != null) {
                assert previous.compareTo(line) <= 0;
            }
        }
    }

    @Test
    public void TestSortedFilesAreAsLongAsInput() throws IOException {
        // Count lines in output file ...
        int lineCount = 0;
        try (BufferedReader reader = Files.newBufferedReader(sortedFile)) {
            while (reader.readLine() != null)
                lineCount++;
        }
        Assertions.assertEquals(10000000, lineCount);
    }
    @Test
    public void TestSortedFilesContainTheSameElementsAsInput() throws IOException {
        // Count entries in input file ...
        Map<String, Integer> countMap = new HashMap<>();
        Map<String, Integer> sortedCountMap = new HashMap<>();
        String tempLine;
        try (BufferedReader tempReader = Files.newBufferedReader(tempFile)) {
            while ((tempLine = tempReader.readLine()) != null) {
                countMap.put(tempLine, countMap.getOrDefault(tempLine, 0)+1);
            }
        }
        // Count entries in output file ...
        try (BufferedReader reader = Files.newBufferedReader(sortedFile)) {
            while ((tempLine = reader.readLine()) != null)
                sortedCountMap.put(tempLine, sortedCountMap.getOrDefault(tempLine, 0)+1);
        }
        Assertions.assertEquals(countMap.size(), sortedCountMap.size());
        //Compare entry counts.
        for (String key : countMap.keySet()) {
            Assertions.assertEquals(countMap.get(key), sortedCountMap.get(key));
        }
    }
}
