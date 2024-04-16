package games4you.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;
import games4you.entities.Admin;
import games4you.entities.Gamer;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.*;

/**
 * Class used to read from the preprocessed json data and populate the databases
 */
public class Populator {

    private final Admin a;
    private final Gamer g;

    public Populator() {
        a = new Admin();
        g = new Gamer();

        try {
            ProcessBuilder pbMongo = new ProcessBuilder("mongosh", "--eval", "use games4you", "--eval", "db.dropDatabase()");
            Process processMongo = pbMongo.start();
            processMongo.waitFor();

            ProcessBuilder pbNeo4j = new ProcessBuilder("cypher-shell", "-u", "neo4j", "-p", "password", "MATCH(n) OPTIONAL MATCH (n)-[r]-() DELETE n,r;");
            Process processNeo4j = pbNeo4j.start();
            processNeo4j.waitFor();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private ArrayList<HashMap<String, Object>> readJson(String jsonfile) {
        ObjectMapper objectMapper = new ObjectMapper();
        ArrayList<HashMap<String, Object>> dict = new ArrayList<>();

        try {
            InputStream stream = Populator.class.getClassLoader().getResourceAsStream(jsonfile);
            JsonParser parser = objectMapper.getFactory().createParser(stream);

            // Start processing the JSON file
            while (!parser.isClosed()) {
                JsonToken jsonToken = parser.nextToken();

                // Check if we've reached the end of the JSON file
                if (jsonToken == null) {
                    break;
                }

                // Process each JSON object individually
                if (JsonToken.START_OBJECT.equals(jsonToken)) {
                    HashMap<String, Object> map = parser.readValueAs(new TypeReference<HashMap<String, Object>>() {});

                    // Convert from integers to long
                    for (String key : new String[]{"uid", "gid", "rid", "datecreation", "creation_date"}) {
                        if (map.containsKey(key)) {
                            Integer intValue = (Integer) map.get(key);
                            map.put(key, intValue.longValue());
                        }
                    }

                    dict.add(map);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        return dict;
    }

    public int populateConcurrent(String file_name, int choice) {
        ArrayList<HashMap<String, Object>> json = readJson(file_name);
        if(json == null) return 0;

        // Create an ExecutorService with 16 threads
        ExecutorService executor = Executors.newFixedThreadPool(16);

        // Create a list to hold the Future objects
        List<Future<Integer>> list = new ArrayList<Future<Integer>>();

        // Calculate the size of each submap
        int subSize = json.size() / 16;

        // Split the json into submaps and submit a new task for each
        for (int i = 0; i < 16; i++) {
            int start = i * subSize;
            int end = (i + 1) * subSize;
            if (i == 15) end = json.size(); // Make sure the last submap includes any remaining elements

            // Submit a new task and add the Future to the list
            Callable<Integer> callable = null;
            if(choice == 0) callable = new AddUserTask(g, json, start, end);
            else if(choice == 1) callable = new AddGameTask(a, json, start, end);
            else if(choice == 2) callable = new AddReviewTask(g, json, start, end);
            Future<Integer> future = executor.submit(callable);
            list.add(future);
        }

        // Calculate the total number of added gamers
        int added = 0;
        for (Future<Integer> fut : list) {
            try {
                added += fut.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }

        // Shutdown the executor
        executor.shutdown();

        return added;
    }

    class AddUserTask implements Callable<Integer> {
        private final Gamer g;
        private final List<HashMap<String, Object>> json;
        private final int start;
        private final int end;

        public AddUserTask(Gamer g, List<HashMap<String, Object>> json, int start, int end) {
            this.g = g;
            this.json = json;
            this.start = start;
            this.end = end;
        }

        @Override
        public Integer call() {
            int added = 0;
            for (int i = start; i < end; i++) {
                HashMap<String, Object> map = json.get(i);
                if (g.signup(map)) {
                    added += 1;
                }
            }
            return added;
        }
    }

    class AddGameTask implements Callable<Integer> {
        private final Admin a;
        private final List<HashMap<String, Object>> json;
        private final int start;
        private final int end;

        public AddGameTask(Admin a, List<HashMap<String, Object>> json, int start, int end) {
            this.a = a;
            this.json = json;
            this.start = start;
            this.end = end;
        }

        @Override
        public Integer call() {
            int added = 0;
            for (int i = start; i < end; i++) {
                HashMap<String, Object> map = json.get(i);
                if (a.insertGame(map)) {
                    added += 1;
                }
            }
            return added;
        }
    }

    class AddReviewTask implements Callable<Integer> {
        private final Gamer g;
        private final List<HashMap<String, Object>> json;
        private final int start;
        private final int end;

        public AddReviewTask(Gamer g, List<HashMap<String, Object>> json, int start, int end) {
            this.g = g;
            this.json = json;
            this.start = start;
            this.end = end;
        }

        @Override
        public Integer call() {
            int added = 0;
            for (int i = start; i < end; i++) {
                HashMap<String, Object> map = json.get(i);
                if (g.addReview(map) > 0) {
                    added += 1;
                }
            }
            return added;
        }
    }




    public static void main(String[] args) {
        Populator pop = new Populator();
        System.out.println(pop.populateConcurrent("dataset/userDB.json", 0));
        System.out.println(pop.populateConcurrent("dataset/old_gameDB.json", 1));
        System.out.println(pop.populateConcurrent("dataset/old_reviewDB.json", 2));
    }



}
