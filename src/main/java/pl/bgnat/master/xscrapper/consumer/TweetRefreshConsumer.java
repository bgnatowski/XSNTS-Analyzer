package pl.bgnat.master.xscrapper.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;
import pl.bgnat.master.xscrapper.service.TweetUpdateService;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TweetRefreshConsumer {

    private final TweetUpdateService updateService;

    @KafkaListener(
            topics = "tweet-refresh",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onMessage(List<Long> tweetIds, Acknowledgment ack) {
        updateService.bulkRefreshTweets(tweetIds);
        ack.acknowledge();
        log.info("Processed batch of {} tweets", tweetIds.size());
    }
}
