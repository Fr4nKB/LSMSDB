package games4you.util;

import games4you.dbmanager.Neo4jManager;

import java.util.ArrayList;

public class NeoComplexQueries {

    /**
     * Gives an array of json strings, each string containing a game the user may like
     * based on tags from games the user has already played or reviewed
     * @param uid user
     * @return arraylist of strings
     */
    public ArrayList<Object> tagsBasedRecommendations(long uid) {
        Neo4jManager neo4j = Neo4jManager.getInstance();
        String query = String.format("""
             CALL {
                MATCH (:User {id: %d})-[o:OWNS]->(g:Game)
                 UNWIND g.tags AS tags
                 WITH DISTINCT tags AS t, COUNT(DISTINCT tags) AS num
                 RETURN t, num
                 UNION
                 MATCH (:User {id: %d})-[:HAS_WROTE]->(r:Review)-[:HAS_REVIEW]-(g1:Game)
                 UNWIND g1.tags AS tags
                 WITH DISTINCT tags AS t, COUNT(DISTINCT tags) AS num
                 RETURN t, num}
             WITH DISTINCT(t) as tags, COUNT(num) as tot
             ORDER BY tot
             WITH COLLECT(tags) AS top5Tags
             MATCH(g:Game)
             WHERE NOT EXISTS((:User {id: %d})-[:OWNS]-(g))
             WITH g, [tag IN g.tags WHERE tag IN top5Tags] AS matchingTags
             WHERE size(matchingTags) >= 2
             RETURN {id: g.id, name: g.name} LIMIT 100""",
                uid, uid, uid);
        return neo4j.getQueryResultAsList(query);
    }

    /**
     * Gives an array of json strings, each string containing a game the user may like
     * based on tags from games owned by friends and friends of friends
     * @param uid user
     * @return arraylist of strings
     */
    public ArrayList<Object> friendsTagsBasedRecommendationNORED(long uid) {
        String query = String.format(
                """
                    MATCH (u:User {id: %d})-[:IS_FRIEND_WITH*1..2]-(fof), (fof)-[:OWNS]->(g:Game)
                    UNWIND g.tags AS tag
                    WITH u, tag, count(*) AS tagCount
                    ORDER BY tagCount DESC LIMIT 5
                    WITH u, collect(tag) AS topTags
                    MATCH (game:Game)
                    WHERE NOT((u)-[:OWNS]->(game)) AND ANY(t IN game.tags WHERE t IN topTags)
                    WITH game, [t IN game.tags WHERE t IN topTags] AS Tags
                    RETURN {id: game.id, name: game.name}
                    ORDER BY size(Tags) DESC LIMIT 5""",
                uid);
        Neo4jManager neo4j = Neo4jManager.getInstance();
        return neo4j.getQueryResultAsList(query);
    }

    /**
     * Same as before but this time using some redundancies
     * @param uid user
     * @return arraylist of strings
     */
    public ArrayList<Object> friendsTagsBasedRecommendationRED(long uid) {
        String query = String.format(
                """
                    MATCH (u:User {id: %d})-[:IS_FRIEND_WITH*1..2]-(friend:User)
                    WITH u, friend
                    UNWIND friend.tags AS tag
                    WITH u, tag, COUNT(DISTINCT friend) AS tagCount
                    ORDER BY tagCount DESC
                    LIMIT 5
                    WITH u, COLLECT(tag) AS topFriendTags
                    MATCH (g:Game)
                    WHERE NOT((u)-[:OWNS]->(g)) AND ANY(tag IN g.tags WHERE tag IN topFriendTags)
                    RETURN {id: g.id, name: g.name}""",
                uid);
        Neo4jManager neo4j = Neo4jManager.getInstance();
        return neo4j.getQueryResultAsList(query);
    }

    /**
     * Gives an array of json strings, each string containing a game the user may like
     * based on a score given to each game owned or reviewed by the user's friend
     * @param uid user
     * @return arraylist of strings
     */
    public ArrayList<Object> friendScoreBasedRecommendations(long uid) {
        //create relationships between review, game and gamer
        Neo4jManager neo4j = Neo4jManager.getInstance();
        String query = String.format("""
             CALL {
                MATCH (:User {id: %d})-[:IS_FRIEND_WITH]->(f:User)-[o:OWNS]->(g:Game)
                 WITH DISTINCT(g) AS game, COUNT(DISTINCT f) AS totOwn, SUM(o.hours) AS totHrs
                 RETURN game, 0.2*totHrs+0.1*totOwn AS partialScore
                 UNION
                 MATCH (:User {id: %d})-[:IS_FRIEND_WITH]->(f:User)-[:HAS_WROTE]->(r:Review)-[:HAS_REVIEW]-(g1:Game)
                 WITH DISTINCT(g1) AS game, COUNT(r) AS partialScore
                 RETURN game, partialScore}
             WITH DISTINCT(game) as g, SUM(partialScore) as finalScore
             RETURN {id: g.id, name: g.name, score: finalScore}
             ORDER BY score DESC LIMIT 5""",
                uid, uid);
        return neo4j.getQueryResultAsList(query);
    }

    public ArrayList<Object> friendsRanking(long uid) {
        Neo4jManager neo4j = Neo4jManager.getInstance();
        String query = String.format("""
            MATCH (u:User {id:'%d'})-[:IS_FRIEND_WITH]->(f:User)
            WITH f
            OPTIONAL MATCH (f)-[:HAS_WROTE]->(r:Review)
            WITH f, COUNT(DISTINCT r) AS totReviews, SUM(r.numUpvotes) AS totUps
            OPTIONAL MATCH (f)-[o:OWNS]->(g:Game)
            WITH f, totReviews, totUps, COUNT(DISTINCT g) AS totOwn, SUM(o.hours) AS totHrs
            WITH f, totReviews * 5 + totUps * 2 + totOwn * 5 + totHrs * 1 AS score
            RETURN {id: f.id, score: score}
            ORDER BY score DESC LIMIT 10""",
                uid);
        return neo4j.getQueryResultAsList(query);
    }
}
