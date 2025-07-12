package pl.bgnat.master.xsnts.sentiment.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.bgnat.master.xsnts.normalization.model.ProcessedTweet;
import pl.bgnat.master.xsnts.sentiment.model.SentimentResult;
import pl.bgnat.master.xsnts.normalization.repository.ProcessedTweetRepository;
import pl.bgnat.master.xsnts.sentiment.repository.SentimentResultRepository;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class SentimentAnalysisService {

    private static final int PAGE_SIZE = 5_000;

    private final ProcessedTweetRepository processedTweetRepo;
    private final SentimentResultRepository sentimentRepo;
    private final SentimentAnalyzer analyzer;
    @PersistenceContext
    private EntityManager em;

    @Transactional
    public int analyzeAll() {
        int page = 0, inserted = 0;
        long started = System.currentTimeMillis();

        while (true) {
            log.info("📥  Strona={}  size={}", page, PAGE_SIZE);
            Page<ProcessedTweet> tweets =
                    processedTweetRepo.findAll(PageRequest.of(page, PAGE_SIZE));

            if (tweets.isEmpty()) break;

            long t0 = System.currentTimeMillis();
            var results = tweets.getContent().parallelStream()
                    .map(this::buildResult)
                    .toList();
            sentimentRepo.saveAll(results);
            inserted += results.size();

            sentimentRepo.flush();
            em.clear();

            long t1 = System.currentTimeMillis();
            log.info("✅  Strona={}  records={}  time={} ms", page, results.size(), t1 - t0);
            page++;
        }
        long total = System.currentTimeMillis() - started;
        log.info("🏁  Zakończono batch — nowych wyników={}  łączny czas={} ms", inserted, total);
        return inserted;
    }

    private SentimentResult buildResult(ProcessedTweet pt) {
        double score = analyzer.computeScore(pt.getNormalizedContent());
        return SentimentResult.builder()
                .processedTweet(pt)
                .score(score)
                .label(analyzer.classify(score))
                .analysisDate(LocalDateTime.now())
                .build();
    }
}
