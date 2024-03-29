package games4you.dbmanager;

import com.mongodb.MongoException;
import com.mongodb.ReadPreference;
import com.mongodb.WriteConcern;
import com.mongodb.client.*;
import com.mongodb.ConnectionString;
import com.mongodb.client.model.Filters;
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

    public MongoCursor<Document> findDocumentByKeyValue(String collection, String key, String value) throws MongoException {

        //Preliminary collection set
        currentCollection = getCollection(collection);

        //Returns the cursor with all the documents found
        return currentCollection.find(eq(key, value)).iterator();
    }

    public boolean addElem(String coll, Document doc) {
        currentCollection = getCollection(coll);
        if(currentCollection == null) return false;

        currentCollection.insertOne(doc);
        return true;
    }

    public boolean removeElem(String coll, String key, String value) {
        currentCollection = getCollection(coll);
        if(currentCollection == null) return false;

        Bson filter = Filters.eq(key, value);
        currentCollection.deleteOne(filter);
        return true;
    }
}
