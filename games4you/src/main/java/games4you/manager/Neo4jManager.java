package games4you.manager;

import org.neo4j.driver.*;

import java.io.InputStream;
import java.util.ArrayList;
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

    /**
     * Adds a generic element to the graph db
     * @param node_type the class of the node
     * @param value the value to assign to the 'name' property
     * @return false if node is not added, true otherwise
     */
    private boolean addElem(String node_type, String value) {
        try (Session session = driver.session()) {
            session.run(STR."MERGE (u:\{node_type} {name: '\{value}'})");
            return true;
        } catch (Exception e){
            System.out.println(e.toString());
            return false;
        }
    }

    public boolean addUser(String username) {
        return addElem("User", username);
    }

    public boolean addGame(String game) {
        return addElem("Game", game);
    }

    public boolean addReview(String review) {
        return addElem("Review", review);
    }

    /**
     * Adds a relationship between two generic nodes
     * @param node_types classes of the nodes in the relationship
     * @param relation name of the relation and eventual properties
     * @param node1 name of the relationship source node
     * @param node2 name of the relationship destination node
     * @return false if relationship couldn't be added, true otherwise
     */
    public boolean addRelationship(String[] node_types, String relation, String node1, String node2) {
        try (Session session = driver.session()) {
            session.run(STR."""
                    MATCH (n1:\{node_types[0]} {name: '\{node1}'}), (n1:\{node_types[1]} {name: '\{node2}'})
                    MERGE (n1)-[:\{relation}]->(n2)
                    """);
            return true;
        } catch (Exception e){
            System.out.println(e.toString());
            return false;
        }
    }

    public boolean removeRelationship(String[] node_types, String relation, String node1, String node2) {
        try (Session session = driver.session()) {
            session.run(STR."""
                    MATCH (n1:\{node_types[0]} {name: '\{node1}'})-[r:\{relation}]->(n1:\{node_types[1]} {name: '\{node2}'})
                    DELETE r
                    """);
            return true;
        } catch (Exception e){
            System.out.println(e.toString());
            return false;
        }
    }

    /**
     * Returns a subset of all the elements belonging to a relationship
     * @param node_types classes of the nodes in the relationship
     * @param relation name of the relation and eventual properties
     * @param node name of the relationship source node
     * @param offset number of elements to skip to implement pagination
     * @return a maximum of 20 elem after the offset-th elem
     */
    public ArrayList<String> getGenericList(String[] node_types, String relation, String node, int offset) {
        ArrayList<String> friends = new ArrayList<>();
        try (Session session = driver.session()) {
            session.run(STR."""
                            MATCH (n1:\{node_types[0]} {name: '\{node}'})-[\{relation}]->(n2:\{node_types[1]})
                            RETURN n2
                            SKIP \{offset} LIMIT 20""");
            return friends;
        } catch (Exception e){
            System.out.println(e.toString());
            return null;
        }
    }
}