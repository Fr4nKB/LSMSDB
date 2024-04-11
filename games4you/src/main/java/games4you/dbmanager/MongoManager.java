package games4you.dbmanager;

import com.mongodb.MongoException;
import com.mongodb.ReadPreference;
import com.mongodb.WriteConcern;
import com.mongodb.client.*;
import com.mongodb.ConnectionString;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import org.bson.BsonDocument;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.io.InputStream;
import java.time.Instant;
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
            System.out.println("Failed to find " + collection + " collection in database");
            return null;
        }

        //returns the collection of documents
        return mongoCollection;
    }

    public BsonDocument convert2BsonDoc(Document doc) {
        return doc.toBsonDocument(BsonDocument.class, database.getCodecRegistry());
    }

    public MongoCursor<Document> findDocumentByKeyValue(String collection, String key, Object value) {

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
    public boolean removeDoc(boolean choice, String coll, String key, Object value) {
        currentCollection = getCollection(coll);
        if(currentCollection == null) return false;

        Bson filter = Filters.eq(key, value);
        DeleteResult res = null;
        if(!choice) res = currentCollection.deleteOne(filter);
        else res = currentCollection.deleteMany(filter);

        return res.getDeletedCount() > 0;
    }

    public boolean addReporter(long rid, long uid) {
        currentCollection = getCollection("reviews");
        if(currentCollection == null) return false;


        long timestamp = Instant.now().getEpochSecond();
        UpdateResult res = currentCollection.updateOne(
                Filters.eq("rid", rid),
                Updates.combine(
                        Updates.set("reports.lastRep", timestamp),
                        Updates.inc("reports.numRep", 1),
                        Updates.push("reports.reporters", uid)));

        return res.getModifiedCount() > 0;
    }

}
