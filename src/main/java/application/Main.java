package application;

import osmreader.Translator;
import osmreader.ZCurveIDStrategy;

import javax.xml.stream.XMLStreamException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {
    private static final int KiB = 1024;
    private static final int MiB = 1024 * KiB;
    private static final int GiB = 1024 * MiB;
    public static void main(String... args) {
        Map<String, String> arguments = parseArguments(args);
        if (!argumentsAreValid(arguments)) {
            System.out.println("Invalid Arguments ... ");
            return;
        }
        Path inputPath = Paths.get(arguments.get("i"));
        Path tagPath = Paths.get(arguments.get("f"));
        Path outputDirectory = arguments.get("o") == null ? inputPath.getParent() : Paths.get(arguments.get("o"));
        long memoryLimit = arguments.get("m") == null ? GiB : Long.parseLong(arguments.get("m")) * MiB;

        try (Reader reader = Files.newBufferedReader(inputPath);
            Translator translator = new Translator(reader, memoryLimit, new ZCurveIDStrategy())){
            Map<String, List<String>> tags= loadTags(tagPath);
            System.out.println("The following tags were loaded from \""+tagPath+"\"");
            System.out.println(tags);
            translator.setFilter(tags);
            translator.translate(outputDirectory);
        } catch (XMLStreamException | IOException e) {
            e.printStackTrace();
        }
    }
    private static boolean argumentsAreValid(Map<String, String> arguments) {
        //TODO: Validate the arguments ...
        return true;
    }
    private static Map<String, String> parseArguments(String[] args) {
        Map<String, String> arguments = new HashMap<>();
        for (int index = 0; index < args.length-1; index += 2) {
            String argument = args[index];
            String nextArgument = args[index+1];
            boolean isFlag = argument.charAt(0) == '-';
            boolean nextArgumentIsFlag = nextArgument.charAt(0) == '-';
            if (!isFlag || nextArgumentIsFlag || arguments.containsKey(argument))
                throw new IllegalArgumentException();
            arguments.put(argument.substring(1), nextArgument);
        }
        return arguments;
    }
    private static Map<String, List<String>> loadTags(Path path) throws IOException {
        Map<String, List<String>> tags = new HashMap<>();
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] strings = line.split(",",2);
                List<String> values = tags.getOrDefault(strings[0], new ArrayList<>());
                values.add(strings[1]);
                tags.put(strings[0], values);
            }
        }
        return tags;
    }
}
