package games4you.dbmanager;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.neo4j.driver.*;
import org.neo4j.driver.Record;
import org.neo4j.driver.summary.SummaryCounters;

import java.io.InputStream;
import java.util.*;

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
    public void close() {
        driver.close();
    }

    private boolean executeSimpleQuery(String query) {
        query = query.replace("'", "''");
        try (Session session = driver.session()) {
            session.run(query);
            return true;
        } catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Adds a generic element to the graph db
     * @param node_type the class of the node
     * @param attributes hash map that contains the attribute to add
     *                   it need at least a key "id" which specifies an unique int value
     *                   which is used to identify nodes
     * @return false if node is not added, true otherwise
     */
    public boolean addNode(String node_type, HashMap<String, Object> attributes) {
        StringBuilder query = new StringBuilder(String.format(
                "MERGE (n1:%s {",
                node_type));
        for(Map.Entry<String, Object> entry: attributes.entrySet()) {
            query.append(entry.getKey()).append(": ");
            Object obj = entry.getValue();
            if(obj.getClass().equals(String.class)) query.append("\"").append(entry.getValue()).append("\"");
            else query.append(entry.getValue());
            query.append(",");
        }
        query.deleteCharAt(query.length()-1);
        query.append("})");
        return executeWriteTransactionQuery(query.toString()) > 0;
    }

    /**
     * Removes a node from the graph db and all the connected relationships
     * @param node_type the class of the node
     * @param id the id of the node to remove
     * @return false if node is not removed, true otherwise
     */
    public boolean removeNode(String node_type, long id) {
        String query = String.format(
                "MATCH (n1:%s {id: %d}) DETACH DELETE n1",
                node_type, id);
        return executeWriteTransactionQuery(query) > 0;
    }

    public boolean removeSubNodes(String parent, String relation, String child, long value) {
        String query = String.format(
                "MATCH (:%s {name: %d})-[%s]-(n:%s) DELETE n",
                parent, value, relation, child);
        return executeWriteTransactionQuery(query) > 0;
    }

    /**
     * Adds a relationship between two generic nodes
     * @param node_types classes of the nodes in the relationship
     * @param relation name of the relation and eventual properties
     * @param node1 name of the relationship source node
     * @param node2 name of the relationship destination node
     * @return false if relationship couldn't be added, true otherwise
     */
    public int addRelationship(String[] node_types, String relation, long node1, long node2) {
        String query = String.format(
                "MATCH (n1:%s {id: %d}), (n2:%s {id: %d}) MERGE (n1)-[:%s]->(n2)",
                node_types[0], node1, node_types[1], node2, relation
        );
        return executeWriteTransactionQuery(query);
    }

    public int removeRelationship(String[] node_types, String relation, long node1, long node2) {
        String query = String.format(
                "MATCH (n1:%s {id: %d})-[r:%s]-(n2:%s {id: %d}) DELETE r",
                node_types[0], node1, relation, node_types[1], node2
        );
        return executeWriteTransactionQuery(query);
    }


    public ArrayList<Object> getQueryResultAsList(String query) {
        try (Session session = driver.session()) {
            Result res = session.run(query);
            ArrayList<Object> ret = new ArrayList<>();
            ObjectMapper objectMapper = new ObjectMapper();

            while(res.hasNext()) {
                Record n = res.next();
                Object obj = n.get(0).asObject();
                ret.add(objectMapper.writeValueAsString(obj));
            }

            return ret;
        }
        catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    public boolean addAttribute(String node_type, long node_name, String attribute_name, Object attribute) {
        String query = String.format(
                "MATCH (u:%s {id: %s}) SET u.%s = $attribute",
                node_type, node_name, attribute_name);

        try (Session session = driver.session()) {
            session.run(query, parameters("attribute", attribute));
        }
        catch (Exception e){
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public boolean incAttribute(String[] node_types, long[] node_names, String relation,
                                            String attribute_name, int amount) {

        String query;
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

        return executeWriteTransactionQuery(query) > 0;
    }

    public int executeWriteTransactionQuery(String query) {
        try (Session session = driver.session()) {
            return session.executeWrite(tx -> {
                Result result = tx.run(query);
                SummaryCounters counters = result.consume().counters();

                //if any change was applied return true, otherwise false
                return (counters.nodesCreated() > 0 || counters.nodesDeleted() > 0 ||
                counters.relationshipsCreated() > 0 ||  counters.relationshipsDeleted() > 0 ||
                counters.propertiesSet() > 0) ? 1 : 0;
            });
        }
        catch (Exception e){
            e.printStackTrace();
            return -1;
        }
    }

}