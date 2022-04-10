package io;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TestFileUtilityFilter {
    private Path input;
    private Path filter;
    @BeforeEach
    public void createFiles() throws IOException {
        Set<String> ids = new HashSet<>();
        while (ids.size() < 100000) {
            int number = (int) (Math.random() * 1000000);
            ids.add(Integer.toString(number));
        }
        input = Files.createTempFile("input", "TestFileUtility");
        try (BufferedWriter writer = Files.newBufferedWriter(input)) {
            for (String id : ids) {
                writer.write(id);
                writer.newLine();
            }
            writer.flush();
        }
        Set<String> idFilter = new HashSet<>();
        while (idFilter.size() < 1000) {
            int number = (int) (Math.random() * 1000000);
            if (ids.contains(Integer.toString(number)))
                idFilter.add(Integer.toString(number));
        }
        filter = Files.createTempFile("filter", "TestFileUtility");
        try (BufferedWriter writer = Files.newBufferedWriter(filter)) {
            for (String id : idFilter) {
                writer.write(id);
                writer.newLine();
            }

            writer.flush();
        }
    }
    @AfterEach
    public void deleteFiles() throws IOException {
        Files.delete(input);
        Files.delete(filter);
    }

    @Test
    public void TestFileFilterExtractsAllIDsPresentInFilter() throws IOException {
        Path sortedInput = null;
        Path sortedFilter = null;
        Path filteredInput = null;
        try {
            FileSorter sorter = new FileSorter(2048*2048, StandardCharsets.UTF_8, String::compareTo);
            sortedInput = sorter.externalSort(input);
            sortedFilter = sorter.externalSort(filter);
            Path temporaryDirectory = Paths.get(System.getProperty("java.io.tmpdir"));
            filteredInput = FileUtility.filterFile(sortedInput, temporaryDirectory, sortedFilter, String::compareTo);
            List<String> IDs = new ArrayList<>(1000);
            List<String> filter = new ArrayList<>(1000);
            List<String> filteredIDs = new ArrayList<>(1000);
            try (BufferedReader reader = Files.newBufferedReader(sortedInput)) {
                String id;
                while ((id = reader.readLine()) != null) {
                    IDs.add(id);
                }
            }
            try (BufferedReader reader = Files.newBufferedReader(sortedFilter)) {
                String id;
                while ((id = reader.readLine()) != null) {
                    filter.add(id);
                }
            }
            try (BufferedReader reader = Files.newBufferedReader(filteredInput)) {
                String id;
                while ((id = reader.readLine()) != null) {
                    filteredIDs.add(id);
                }
            }
            Assertions.assertTrue(IDs.containsAll(filter));
            Assertions.assertTrue(filteredIDs.containsAll(filter));
        } finally {
            if (sortedInput != null)
                Files.deleteIfExists(sortedInput);
            if (sortedFilter != null)
                Files.deleteIfExists(sortedFilter);
            if (filteredInput != null)
                Files.deleteIfExists(filteredInput);
        }
    }
}
