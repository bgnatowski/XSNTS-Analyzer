package pl.bgnat.master.xscrapper.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import pl.bgnat.master.xscrapper.service.TweetService;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TweetRefreshProducer {
    private static final String TOPIC = "tweet-refresh";

    private final TweetService tweetService;
    private final KafkaTemplate<String, Long> kafkaTemplate;

    //    @Scheduled(cron = "0 0 */2 * * *")  // co 2 godziny
//    @PostConstruct
    public void enqueueTweets() {
        var cutoff = LocalDateTime.now().minusDays(2);
        List<Long> ids = tweetService.findIdsToRefresh(cutoff, 500);
        ids.forEach(id -> kafkaTemplate.send(TOPIC, id));
        log.info("Enqueued {} tweet IDs for refresh", ids.size());
    }
}
