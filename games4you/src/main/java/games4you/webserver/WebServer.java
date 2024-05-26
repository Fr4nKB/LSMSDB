package games4you.webserver;

import games4you.dbmanager.MongoManager;
import games4you.entities.Gamer;
import games4you.util.MongoComplexQueries;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class WebServer {

    private static void updateAnalysis() {
        MongoComplexQueries mongoCQ = new MongoComplexQueries();
        mongoCQ.updateAnalysis();
    }

    public static void main(String[] args) {
        updateAnalysis();
        SpringApplication.run(WebServer.class, args);
    }

    @Scheduled(cron = "0 0 2 * * ?")    //schedule work at 2:00AM every day
    public void scheduleFixedRateTask() {
        updateAnalysis();
    }
}
