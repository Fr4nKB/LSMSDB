package games4you.util;

import games4you.dbmanager.Neo4jManager;

public class NeoComplexQueries {
    public boolean addGameToLibrary(long uid, long gid) {
        //create relationships between review, game and gamer
        Neo4jManager neo4j = Neo4jManager.getInstance();
        String query = String.format(
                "MATCH (u:User {id: %d})-[:FRIEND]-(friend:User)"+
                "WITH u, collect(DISTINCT friend) AS friends)" +
                "MATCH (u)-[:OWNS]->(game:Game)<-[HAS_REVIEW]-(r:Review)<-[HAS_WROTE]-(reviewer)" +
                "WHERE reviewer IN friends" +
                "WITH game, COUNT(DISTINCT review) AS reviewCount" +
                "MATCH (game)<-[:OWNS]-(owner:User)"+
                "WHERE owner IN friends"+
                "WITH game, COUNT(DISTINCT owner) AS friendsOwning"+
                "RETURN game, reviewCount + friendsOwning*2 AS totalScore"
                ,
                uid);
        neo4j.executeWriteTransactionQuery(query);
        return true;
    }
}
