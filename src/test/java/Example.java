import application.Main;

public class Example {
    public static void main(String[] args) {
        String pathToFile = "C:\\Users\\nicol\\Documents\\Education\\Bachelor Project\\OSM Data\\map.osm";
        String pathToFilter = "C:\\Users\\nicol\\Documents\\Education\\Bachelor Project\\OSM Data\\filter.txt";
        Main.main("-i", pathToFile, "-f", pathToFilter);
    }
}
