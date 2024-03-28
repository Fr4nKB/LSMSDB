package games4you.util;

import com.fasterxml.jackson.core.type.TypeReference;
import games4you.Admin;
import games4you.User;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Class used to read from the preprocessed json data and populate the databases
 */
public class Populator {

    private final Admin a;
    private final User u;

    public Populator() {
        a = new Admin();
        u = new User();
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

    public void populateUsers() {
        List<ArrayList<Object>> json = readJson("userDB.json");
        for (ArrayList<Object> sublist : json) {
            List<String> strings = sublist.stream()
                    .map(object -> Objects.toString(object, null))
                    .toList();
            u.signup(strings);
        }
    }


    public void populateGames() {
        boolean ret;
        List<ArrayList<Object>> json = readJson("gameDB.json");
        for (ArrayList<Object> sublist : json) {
            ret = a.addGame(sublist);
            if(!ret) System.out.println("Game not added");
        }
    }

    public void populateReviews() {
        int ret;
        List<ArrayList<Object>> json = readJson("reviewDB.json");
        for (ArrayList<Object> sublist : json) {
            ret = u.addReview(sublist);
            if(ret <= 0) System.out.println(STR."Review not added: \{ret}");
        }
    }
}
