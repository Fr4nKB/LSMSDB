package games4you.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.*;
import games4you.dbmanager.MongoManager;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MongoComplexQueries {
    public ArrayList<Object> getTop10Haters() {
        Instant oneMonthAgo = Instant.now().minus(30, ChronoUnit.DAYS);
        int oneMonthAgoInSeconds =  (int) oneMonthAgo.getEpochSecond();

        MongoManager mongo = MongoManager.getInstance();
        MongoCollection<Document> coll = mongo.getCollection("reviews");

        List<Bson> pipeline = Arrays.asList(
                Aggregates.match(new Document("creation_date", new Document("$lte", oneMonthAgoInSeconds))),
                Aggregates.unwind("$reports.reporters"),
                Aggregates.group(new Document().append("reporter", "$reports.reporters").append("creator", "$uid"), Accumulators.sum("count", 1)),
                Aggregates.project(
                        new Document("_id", 0).append("reporter_id", "$_id.reporter")
                                .append("creator", "$_id.creator").append("count", 1)),
                Aggregates.sort(Sorts.descending("count")),
                Aggregates.limit(10)
        );

        MongoCursor<Document> cur = coll.aggregate(pipeline).iterator();
        ArrayList<Object> ret = new ArrayList<>();
        ObjectMapper objectMapper = new ObjectMapper();
        while (cur.hasNext()) {
            Document doc = cur.next();
            try {
                ret.add(objectMapper.writeValueAsString(doc));
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

        return ret;
    }

    /**
     * “For the top 5 games with the highest average upvotes,
     * who are the users that have reviewed these games,
     * how many reviews have they made, what’s their average upvotes,
     * and how many reports have they made?”
     */
    public ArrayList<Object> mostValuableReviewersOnMostAppreciatedGames() {
        MongoManager mongo = MongoManager.getInstance();
        MongoCollection<Document> coll = mongo.getCollection("reviews");

        List<Bson> pipeline = Arrays.asList(
                Aggregates.match(Filters.gt("numUpvotes", 10)),
                Aggregates.unwind("$upvotes"),
                Aggregates.group(new Document().append("gid", "$gid").append("uid", "$uid"),
                        Accumulators.avg("averageUpvotes", "$upvotes"),
                        Accumulators.sum("sumReports", "$reports.numRep")
                ),
                Aggregates.sort(Sorts.descending("averageUpvotes")),
                Aggregates.limit(5),
                Aggregates.group(new Document().append("gid", "$_id.gid").append("uid", "$_id.uid"),
                        Accumulators.sum("reviewCount", 1),
                        Accumulators.avg("averageUpvotes", "$averageUpvotes"),
                        Accumulators.sum("sumReports", "$sumReports")
                ),
                Aggregates.sort(Sorts.descending("reviewCount")),
                Aggregates.project(
                        Projections.fields(
                                Projections.excludeId(),
                                Projections.computed("uid", "$_id.uid"),
                                Projections.computed("gid", "$_id.gid"),
                                Projections.include("reviewCount", "averageUpvotes", "sumReports")
                        )
                )
        );


        MongoCursor<Document> cur = coll.aggregate(pipeline).iterator();
        ArrayList<Object> ret = new ArrayList<>();
        ObjectMapper objectMapper = new ObjectMapper();
        while (cur.hasNext()) {
            Document doc = cur.next();
            try {
                ret.add(objectMapper.writeValueAsString(doc));
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

        return ret;
    }
}
