package games4you.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.*;
import games4you.dbmanager.MongoManager;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.data.domain.Sort;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MongoComplexQueries {

    private ArrayList<Object> getResultAsList(String collection, List<Bson> pipeline) {
        MongoManager mongo = MongoManager.getInstance();
        MongoCollection<Document> coll = mongo.getCollection(collection);

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

    public ArrayList<Object> getTop10Haters() {
        Instant oneMonthAgo = Instant.now().minus(30, ChronoUnit.DAYS);
        int oneMonthAgoInSeconds =  (int) oneMonthAgo.getEpochSecond();

        List<Bson> pipeline = Arrays.asList(
                Aggregates.match(new Document("creation_date", new Document("$lte", oneMonthAgoInSeconds))),
                Aggregates.unwind("$reports.reporters"),
                Aggregates.group(new Document().append("reporter", "$reports.reporters")
                        .append("creator", "$uid").append("creator_name", "$uname"),
                        Accumulators.sum("count", 1)),
                Aggregates.project(
                        new Document("_id", 0).append("reporter_id", "$_id.reporter")
                                .append("creator_id", "$_id.creator")
                                .append("creator_name", "$_id.creator_name")
                                .append("count", 1)),
                Aggregates.sort(Sorts.descending("count")),
                Aggregates.limit(10)
        );

        return getResultAsList("reviews", pipeline);
    }

    /**
     * “For the top 5 games with the highest amount of total upvotes
     * and lowest amount of total reports, who are the users that have
     * reviewed these games and have the highest amount of upvotes
     * and at the same time the smallest amount of reports?”
     */
    public ArrayList<Object> mostValuableReviewersOnMostAppreciatedGames() {

        List<Bson> pipeline = Arrays.asList(
                Aggregates.group("$gid",
                        Accumulators.sum("totalUpvotes", "$numUpvotes"),
                        Accumulators.sum("totalReports",
                                Projections.computed(
                                        "$ifNull",
                                        Arrays.asList("$reports.numRep", 0))),
                        Accumulators.max("bestReview",
                                Projections.fields(
                                        Projections.computed("revPerf",
                                                new Document("$subtract",
                                                        Arrays.asList(
                                                                "$numUpvotes",
                                                                Projections.computed("$ifNull", Arrays.asList("$reports.numRep", 0))
                                                        )
                                                )
                                        ),
                                        Projections.computed("gid", "$gid"),
                                        Projections.computed("game", "$game"),
                                        Projections.computed("uid", "$uid"),
                                        Projections.computed("uname", "$uname"),
                                        Projections.computed("rid", "$rid")
                                )
                        )
                ),
                Aggregates.addFields(
                        new Field("upvotesMinusReports",
                                new Document(
                                        "$subtract",
                                        Arrays.asList("$totalUpvotes", "$totalReports")))
                ),
                Aggregates.sort(
                        Sorts.descending("upvotesMinusReports")
                ),
                Aggregates.limit(5),
                Aggregates.project(
                        Projections.fields(
                                Projections.excludeId(),
                                Projections.computed("gid", "$bestReview.gid"),
                                Projections.computed("game", "$bestReview.game"),
                                Projections.computed("uid", "$bestReview.uid"),
                                Projections.computed("uname", "$bestReview.uname"),
                                Projections.computed("rid", "$bestReview.rid"),
                                Projections.computed("upvotesMinusReports", "$upvotesMinusReports"),
                                Projections.computed("revPerf", "$bestReview.revPerf")

                        )
                )
        );

        return getResultAsList("reviews", pipeline);
    }

    public ArrayList<Object> top10HottestGamesOfWeek() {

        List<Bson> pipeline = Arrays.asList(
                Aggregates.group(new Document().append("gid", "$gid").append("name", "$name"),
                        Accumulators.sum("totalHours", "$hrs"),
                        Accumulators.addToSet("uniqueUids", "$uid") // accumulate unique uids
                ),
                Aggregates.project(
                        Projections.fields(
                                Projections.include("totalHours"),
                                Projections.computed("playerCount", new Document("$size", "$uniqueUids")), // count unique uids
                                Projections.computed("gid", "$_id.gid"),
                                Projections.computed("name", "$_id.name"),
                                Projections.excludeId()
                        )
                ),
                Aggregates.project(
                        Projections.fields(
                                Projections.include("totalHours", "playerCount", "gid", "name"),
                                Projections.computed("score",
                                        new Document("$add",
                                                Arrays.asList(new Document("$multiply", Arrays.asList("$totalHours", 0.4)),
                                                        new Document("$multiply", Arrays.asList("$playerCount", 0.6))))),
                                Projections.excludeId()
                        )
                ),
                Aggregates.sort(Sorts.descending("score")),
                Aggregates.limit(10)
        );
        return getResultAsList("hottest", pipeline);
    }

    public ArrayList<Object> getTop10CatchyGames() {
        List<Bson> pipeline = Arrays.asList(
                Aggregates.group(new Document().append("gid", "$gid").append("uid", "$uid").append("name", "$name"),
                        Accumulators.sum("totalHours", "$hrs")
                ),
                Aggregates.group(
                        new Document("uid", "$_id.uid"),
                        Accumulators.sum("totalGames", 1),
                        Accumulators.sum("playerHours", "$totalHours"),
                        Accumulators.addToSet("games", new Document().append("gid", "$_id.gid").append("name", "$_id.name"))
                ),
                Aggregates.match(Filters.eq("totalGames", 1)),
                Aggregates.unwind("$games"),
                Aggregates.group(new Document().append("gid", "$games.gid").append("name", "$games.name"),
                        Accumulators.sum("gameHours", "$playerHours")),
                Aggregates.sort(Sorts.descending("gameHours")),
                Aggregates.limit(10),
                Aggregates.project(
                        Projections.fields(
                                Projections.include("gameHours"),
                                Projections.computed("gid", "$_id.gid"),
                                Projections.computed("name", "$_id.name"),
                                Projections.excludeId()
                        )
                )
        );

        return getResultAsList("hottest", pipeline);
    }

    public static void main(String[] args){
        MongoComplexQueries m = new MongoComplexQueries();
        System.out.println(m.getTop10CatchyGames());
    }

}
