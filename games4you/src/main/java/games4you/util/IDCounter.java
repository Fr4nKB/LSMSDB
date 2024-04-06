package games4you.util;


import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Projections;
import games4you.dbmanager.MongoManager;
import org.bson.BsonDocument;
import org.bson.Document;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class IDCounter {
    private int maxId;

    public IDCounter() {
        maxId = -1;
    }

    public IDCounter(String collection, String id_name){
        MongoManager mongo = MongoManager.getInstance();
        MongoCollection<Document> current_collection = mongo.getCollection(collection);

        Document sortQuery = new Document(id_name, -1);
        MongoCursor<Document> cur = current_collection.find()
                .sort(sortQuery)
                .limit(1)
                .iterator();

        List<BsonDocument> list = new ArrayList<>();
        if(!cur.hasNext()) {
            maxId = -1;
            return;
        }

        Document elem = cur.next();
        maxId = elem.getInteger(id_name);
    }


    public void setMaxId(int newId){
        maxId = newId;
    }

    public int getMaxId(){
        return maxId;
    }

    public int incGetMaxId(){
        maxId += 1;
        return maxId;
    }
}
