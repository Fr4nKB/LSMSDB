package games4you.util;

import games4you.dbmanager.Neo4jManager;

public class NeoComplexQueries {
    public boolean addGameToLibrary(long uid, long gid) {
        //create relationships between review, game and gamer
        Neo4jManager neo4j = Neo4jManager.getInstance();
        String query = String.format(
                """
                MATCH (u:User {id: %d})-[:FRIEND]-(friend:User)
                WITH u, collect(DISTINCT friend) AS friends)
                MATCH (reviewer:User)-[HAS_WROTE]->(r:Review)-[HAS_REVIEW]->(game:Game)
                WHERE reviewer IN friends
                WITH game, COUNT(DISTINCT r) AS reviewCount
                MATCH (owner:User)-[:OWNS]->(game1:Game)
                WHERE owner IN friends
                WITH game, COUNT(DISTINCT owner) AS friendsOwning
                RETURN game, reviewCount + friendsOwning*2 AS totalScore
                """
                ,
                uid);
        neo4j.executeWriteTransactionQuery(query);
        return true;
    }
}
