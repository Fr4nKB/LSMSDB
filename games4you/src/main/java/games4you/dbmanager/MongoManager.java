package games4you.dbmanager;

import com.mongodb.MongoException;
import com.mongodb.ReadPreference;
import com.mongodb.WriteConcern;
import com.mongodb.client.*;
import com.mongodb.ConnectionString;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.io.InputStream;
import java.util.Properties;

import static com.mongodb.client.model.Filters.eq;

public class MongoManager implements AutoCloseable {
    private final MongoClient client;
    private final MongoDatabase database;
    private MongoCollection<Document> currentCollection;
    private static MongoManager instance = null;


    private MongoManager() {
        Properties prop = new Properties();
        try {
            InputStream stream = Neo4jManager.class.getClassLoader().getResourceAsStream("dbconfig.properties");
            prop.load(stream);
        }
        catch (Exception e) {
            throw new RuntimeException("Database configuration not loaded");
        }
        client = MongoClients.create(new ConnectionString(prop.getProperty("mongoUriSingle")));
        database = client.getDatabase(prop.getProperty("mongoDatabaseName"));
    }

    @Override
    public void close() {
        client.close();
    }

    public static MongoManager getInstance() throws MongoException {
        try {
            if (instance == null) { instance = new MongoManager(); }
        } catch (MongoException me) {
            System.out.println("Failed to open a connection with MongoDB server");
            throw me;
        }
        return instance;
    }

    public MongoCollection<Document> getCollection(String collection) {
        //Creates empty MongoCollection
        MongoCollection<Document> mongoCollection;

        // Get access to a collection from the database
        try {
            mongoCollection = database.getCollection(collection)
                    .withWriteConcern(WriteConcern.W1)
                    .withReadPreference(ReadPreference.nearest());

        } catch (MongoException me) {
            System.out.println(STR."Failed to find \{collection} collection in database \{database.getName()}");
            return null;
        }

        //returns the collection of documents
        return mongoCollection;
    }

    public MongoCursor<Document> findDocumentByKeyValue(String collection, String key, String value) {

        //Preliminary collection set
        currentCollection = getCollection(collection);

        //Returns the cursor with all the documents found
        return currentCollection.find(eq(key, value)).iterator();
    }

    public MongoCursor<Document> findDocument(String collection, Document doc) {

        //Preliminary collection set
        currentCollection = getCollection(collection);

        //Returns the cursor with all the documents found
        return currentCollection.find(doc).iterator();
    }

    public boolean addDoc(String coll, Document doc) {
        currentCollection = getCollection(coll);
        if(currentCollection == null) return false;

        currentCollection.insertOne(doc);
        return true;
    }

    /**
     * Removes one or all the documents that can be found in the specified collection
     * with the specified value of a key
     * @param choice false to remove the first element, true to remove all
     * @param coll the collision to search the document in
     * @param key the key which should be checked for
     * @param value the value that the key should have for the document to be eliminated
     */
    public void removeDoc(boolean choice, String coll, String key, String value) {
        currentCollection = getCollection(coll);
        if(currentCollection == null) return;

        Bson filter = Filters.eq(key, value);
        if(!choice) currentCollection.deleteOne(filter);
        else currentCollection.deleteMany(filter);
    }

    public boolean incVote(String review) {
        currentCollection = getCollection("reviews");
        if(currentCollection == null) return false;

        UpdateResult res = currentCollection.updateOne(
                Filters.eq("review", review),
                Updates.inc("votes", 1));

        return res.getModifiedCount() > 0;
    }

    public boolean addReporter(String review, String reporter_uname) {
        currentCollection = getCollection("reviews");
        if(currentCollection == null) return false;

        UpdateResult res = currentCollection.updateOne(
                Filters.eq("review", review),
                Updates.push("reporters", reporter_uname));

        return res.getModifiedCount() > 0;
    }

}
