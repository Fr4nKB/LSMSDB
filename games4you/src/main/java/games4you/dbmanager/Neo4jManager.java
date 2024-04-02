package games4you.dbmanager;

import org.neo4j.driver.*;
import org.neo4j.driver.Record;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.neo4j.driver.Values.parameters;


public class Neo4jManager implements AutoCloseable{

    private final Driver driver;
    private static Neo4jManager instance = null;

    private Neo4jManager(){
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

    private boolean executeSimpleQuery(String query) {
        try (Session session = driver.session()) {
            session.run(query);
            return true;
        } catch (Exception e){
            System.out.println(e.toString());
            return false;
        }
    }

    /**
     * Adds a generic element to the graph db
     * @param node_type the class of the node
     * @param value the value to assign to the 'name' property
     * @return false if node is not added, true otherwise
     */
    public boolean addNode(String node_type, String value) {
        String query = String.format("MERGE (n1:%s {name: '%s'})", node_type, value);
        return executeSimpleQuery(query);
    }

    /**
     * Removes a node from the graph db and all the connected relationships
     * @param node_type the class of the node
     * @param value the value to assign to the 'name' property
     * @return false if node is not removed, true otherwise
     */
    public boolean removeNode(String node_type, String value) {
        String query = String.format("MATCH (n1:%s {name: '%s'}) DETACH DELETE n1", node_type, value);
        return executeSimpleQuery(query);
    }

    public boolean removeSubNodes(String parent, String relation, String child, String value) {
        String query = String.format("MATCH (:%s {name: '%s'})-[%s]-(n:%s) DELETE n", parent, relation, value, child);
        return executeSimpleQuery(query);
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
        String query = String.format(
                "MATCH (n1:%s {name: '%s'}), (n2:%s {name: '%s'}) MERGE (n1)-[:%s]->(n2)",
                node_types[0], node1, node_types[1], node2, relation
        );
        try (Session session = driver.session()) {
            session.run(query);
            return true;
        } catch (Exception e){
            System.out.println(e.toString());
            return false;
        }
    }

    public boolean removeRelationship(String[] node_types, String relation, String node1, String node2) {
        String query = String.format(
                "MATCH (n1:%s {name: '%s'})-[r:%s]->(n2:%s {name: '%s'}) DELETE r",
                node_types[0], node1, relation, node_types[1], node2
        );
        try (Session session = driver.session()) {
            session.run(query);
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
    public ArrayList<ArrayList<Object>> getGenericList(String[] node_types, String relation, String node, int offset) {
        String query = String.format(
                "MATCH (n1:%s {name: '%s'})-[:%s]->(n2:%s) RETURN n2, n2.name SKIP %d LIMIT 20",
                node_types[0], node, relation, node_types[1], offset
        );
        try (Session session = driver.session()) {
            Result res = session.run(query);
            ArrayList<ArrayList<Object>> list = new ArrayList<>();

            while(res.hasNext()) {
                Record n = res.next();
                ArrayList<Object> objectList = new ArrayList<Object>(n.values());
                list.add(objectList);
            }

            return list;
        } catch (Exception e){
            System.out.println(e.toString());
            return null;
        }
    }

    public boolean addAttribute(String node_type, String node_name, String attribute_name, Object attribute) {
        try (Session session = driver.session()) {
            session.run(STR."MATCH (u:\{node_type} {name: '\{node_name}')" +
                            STR."SET u.\{attribute_name}=" + "$attribute",
                    parameters("attribute", attribute));
        } catch (Exception e){
            System.out.println(e.toString());
            return false;
        }

        return true;
    }

    public boolean incAttribute(String node_type, String node_name, String attribute_name) {
        try (Session session = driver.session()) {
            session.run(STR."""
                MATCH (u:\{node_type} {name: '\{node_name}')
                SET u.\{attribute_name}= u.\{attribute_name}" + "+ 1
                """);
        } catch (Exception e){
            System.out.println(e.toString());
            return false;
        }

        return true;
    }

    public ArrayList<String> getQueryResultAsList(String query) {
        try (Session session = driver.session()) {
            Result res = session.run(query);
            ArrayList<String> list = new ArrayList<>();

            while(res.hasNext()) {
                Record n = res.next();
                list.add(n.values().getFirst().toString());
            }
            return list;
        }
        catch (Exception e){
            System.out.println(e.toString());
            return null;
        }
    }
}