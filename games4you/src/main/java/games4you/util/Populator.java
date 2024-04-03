package games4you.util;

import com.fasterxml.jackson.core.type.TypeReference;
import games4you.entities.Admin;
import games4you.entities.Gamer;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Class used to read from the preprocessed json data and populate the databases
 */
public class Populator {

    private final Admin a;
    private final Gamer g;

    public Populator() {
        a = new Admin();
        g = new Gamer();
    }

    private List<ArrayList<Object>> readJson(String jsonfile) {
        ObjectMapper objectMapper = new ObjectMapper();
        List<ArrayList<Object>> json_list = new ArrayList<>();
        try {
            InputStream stream = Populator.class.getClassLoader().getResourceAsStream(jsonfile);
            List<Map<String, Object>> dict = objectMapper.readValue(stream, new TypeReference<List<Map<String, Object>>>(){});

            for (Map<String, Object> map: dict) {
                ArrayList<Object> list = new ArrayList<>(map.values());
                json_list.add(list);
            }
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        return json_list;
    }

    public int populateGamers() {
        List<ArrayList<Object>> json = readJson("userDB.json");
        int added = 0;
        for (ArrayList<Object> sublist : json) {
            if(g.signup(sublist)) {
                added += 1;
            }
        }
        return added;
    }


    public void populateGames() {
        List<ArrayList<Object>> json = readJson("gameDB.json");
        for (ArrayList<Object> sublist : json) {
            if(!a.addGame(sublist)) System.out.println("Game not added");
        }
    }

    public void populateReviews() {
        List<ArrayList<Object>> json = readJson("reviewDB.json");
        for (ArrayList<Object> sublist : json) {
            if(g.addReview(sublist) <= 0) System.out.println("Review not added");
        }
    }
}
