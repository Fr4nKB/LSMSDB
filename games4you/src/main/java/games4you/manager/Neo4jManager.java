package games4you.manager;

import org.neo4j.driver.*;
import static org.neo4j.driver.Values.parameters;

import java.io.InputStream;
import java.util.Properties;

public class Neo4jManager implements AutoCloseable{

    private final Driver driver;
    private static Neo4jManager instance = null;

    public Neo4jManager(){
        Properties prop = new Properties();
        try {
            InputStream stream = Neo4jManager.class.getClassLoader().getResourceAsStream("dbconfig.properties");
            prop.load(stream);
        }
        catch (Exception e) {
            throw new RuntimeException("Database configuration not loaded");
        }
        driver = GraphDatabase.driver(prop.getProperty("neo4jUriLocal"),
                AuthTokens.basic(prop.getProperty("neo4jUsername"), prop.getProperty("neo4jPassword")));
    }

    public static Neo4jManager getInstance() {
        if(instance == null){
            instance = new Neo4jManager();
        }
        return instance;
    }

    @Override
    public void close() throws Exception {
        driver.close();
    }

    public boolean addUser(String username) {
        try (Session session = driver.session()) {
            session.run("MERGE (u:User {name: $username})",
                    parameters("username", username));
            return true;
        } catch (Exception e){
            System.out.println(e.toString());
            return false;
        }
    }

    public boolean addGame(String game) {
        try (Session session = driver.session()) {
            session.run("MERGE (u:Game {game: $game})",
                    parameters("game", game));
            return true;
        } catch (Exception e){
            System.out.println(e.toString());
            return false;
        }
    }
}