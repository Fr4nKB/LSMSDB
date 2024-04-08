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

    private ArrayList<HashMap<String, Object>> readJson(String jsonfile) {
        ObjectMapper objectMapper = new ObjectMapper();
        ArrayList<HashMap<String, Object>> dict;
        try {
            InputStream stream = Populator.class.getClassLoader().getResourceAsStream(jsonfile);
            dict = objectMapper.readValue(stream, new TypeReference<ArrayList<HashMap<String, Object>>>(){});
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        return dict;
    }

    public int populateGamers() {
        ArrayList<HashMap<String, Object>> json = readJson("userDB.json");
        int added = 0;
        for (HashMap<String, Object> map: json) {
            if(g.signup(map)) {
                added += 1;
            }
        }
        return added;
    }


    public void populateGames() {
        List<HashMap<String, Object>> json = readJson("gameDB.json");
        for (HashMap<String, Object> map : json) {
            if(!a.insertGame(map)) System.out.println("Game not added");
        }
    }

    public void populateReviews() {
        List<HashMap<String, Object>> json = readJson("reviewDB.json");
        for (HashMap<String, Object> map : json) {
            if(g.addReview(map) <= 0) System.out.println("Review not added");
        }
    }

}
