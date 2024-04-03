package games4you.dbmanager;

import org.neo4j.driver.*;
import org.neo4j.driver.Record;
import org.neo4j.driver.summary.ResultSummary;
import org.neo4j.driver.summary.SummaryCounters;

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
    public boolean addNode(String node_type, int id) {
        String query = String.format(
                "MERGE (n1:%s {id: %d})",
                node_type, id);
        return executeSimpleQuery(query);
    }

    /**
     * Removes a node from the graph db and all the connected relationships
     * @param node_type the class of the node
     * @param value the value to assign to the 'name' property
     * @return false if node is not removed, true otherwise
     */
    public boolean removeNode(String node_type, int id) {
        String query = String.format(
                "MATCH (n1:%s {id: %d}) DETACH DELETE n1",
                node_type, id);
        return executeSimpleQuery(query);
    }

    public boolean removeSubNodes(String parent, String relation, String child, int value) {
        String query = String.format(
                "MATCH (:%s {name: %d})-[%s]-(n:%s) DELETE n",
                parent, value, relation, child);
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
    public boolean addRelationship(String[] node_types, String relation, int node1, int node2) {
        String query = String.format(
                "MATCH (n1:%s {id: %d}), (n2:%s {id: %d}) MERGE (n1)-[:%s]->(n2)",
                node_types[0], node1, node_types[1], node2, relation
        );
        return executeSimpleQuery(query);
    }

    public boolean removeRelationship(String[] node_types, String relation, int node1, int node2) {
        String query = String.format(
                "MATCH (n1:%s {id: %d})-[r:%s]->(n2:%s {id: %d}) DELETE r",
                node_types[0], node1, relation, node_types[1], node2
        );
        return executeSimpleQuery(query);
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

    /**
     * Returns a subset of all the elements belonging to a relationship
     * @param node_types classes of the nodes in the relationship
     * @param relation name of the relation and eventual properties
     * @param node name of the relationship source node
     * @param offset number of elements to skip to implement pagination
     * @return a maximum of 20 elem after the offset-th elem
     */
    public ArrayList<ArrayList<Object>> getQueryResultAsListOfLists(String[] node_types, String relation, String node, int offset) {
        String query = String.format(
                "MATCH (n1:%s {name: '%s'})-[:%s]->(n2:%s) RETURN n2.name SKIP %d LIMIT 20",
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

    public boolean addAttribute(String node_type, int node_name, String attribute_name, Object attribute) {
        try (Session session = driver.session()) {
            session.run(STR."MATCH (u:\{node_type} {id: \{node_name}}) " +
                            STR."SET u.\{attribute_name}=" + "$attribute",
                    parameters("attribute", attribute));
        } catch (Exception e){
            System.out.println(e.toString());
            return false;
        }

        return true;
    }

    public boolean incAttribute(String[] node_types, int[] node_names, String relation,
                                            String attribute_name, int amount) {

        String query = "";
        if(node_types.length == 0) return false;
        else if (node_types.length == 1) {
            query = String.format(
                    "MATCH (u:%s {id: %d}) SET u.%s = u.%s + %s",
                    node_types[0], node_names[0], attribute_name, attribute_name, amount
            );
        }
        else {
            query = String.format(
                    "MATCH (n1:%s {id: %d})-[r:%s]-(n2:%s {id: %d}) SET r.%s = r.%s + %s",
                    node_types[0], node_names[0], relation, node_types[1], node_names[1], attribute_name, attribute_name, amount
            );
        }

        return executeSimpleQuery(query);
    }

    public boolean executeWriteTransactionQuery(String query) {
        try (Session session = driver.session()) {
            return session.executeWrite(tx -> {
                Result result = tx.run(query);
                SummaryCounters counters = result.consume().counters();

                //if any change was applied return true, otherwise false
                return (counters.nodesCreated() > 0 || counters.nodesDeleted() > 0 ||
                counters.relationshipsCreated() > 0 ||  counters.relationshipsDeleted() > 0 ||
                counters.propertiesSet() > 0);
            });
        } catch (Exception e){
            System.out.println(e.toString());
            return false;
        }
    }

}