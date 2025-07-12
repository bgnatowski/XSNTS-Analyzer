package pl.bgnat.master.xsnts.scrapper.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import pl.bgnat.master.xsnts.scrapper.dto.TweetDTO;
import pl.bgnat.master.xsnts.scrapper.mapper.TweetMapper;
import pl.bgnat.master.xsnts.scrapper.model.Tweet;
import pl.bgnat.master.xsnts.scrapper.repository.TweetRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TweetService {

    private final TweetRepository tweetRepository;

    /**
     * Przetwarza zbiór zescrapowanych tweetów.
     * Zapisuje nowe tweety i aktualizuje istniejące na podstawie unikalnego linku.
     *
     * @param scrappedTweets Zbiór tweetów do przetworzenia.
     */
    @Transactional
    public void saveOrUpdateTweets(Set<Tweet> scrappedTweets) {
        if (scrappedTweets == null || scrappedTweets.isEmpty()) {
            log.info("Otrzymano pusty zbiór tweetów. Brak operacji do wykonania.");
            return;
        }

        // 1. Wyodrębnij linki z zescrapowanych tweetów
        Set<String> links = scrappedTweets.stream()
                .map(Tweet::getLink)
                .filter(StringUtils::hasLength)
                .collect(Collectors.toSet());

        // 2. Pobierz wszystkie istniejące tweety jednym zapytaniem
        Map<String, Tweet> existingTweetsByLink = tweetRepository.findByLinkIn(links).stream()
                .collect(Collectors.toMap(Tweet::getLink, Function.identity()));

        List<Tweet> tweetsToSave = new ArrayList<>();
        int newTweetsCount = 0;
        int updatedTweetsCount = 0;
        int skippedTweetsCount = 0;

        // 3. Przetwórz każdy zescrapowany tweet
        for (Tweet scrappedTweet : scrappedTweets) {
            if (!StringUtils.hasLength(scrappedTweet.getLink())) {
                skippedTweetsCount++;
                continue;
            }

            Tweet existingTweet = existingTweetsByLink.get(scrappedTweet.getLink());

            if (existingTweet != null) {
                // Tweet istnieje -> zaktualizuj dane
                updateTweetData(existingTweet, scrappedTweet);
                tweetsToSave.add(existingTweet);
                updatedTweetsCount++;
            } else {
                // Tweet jest nowy -> dodaj do zapisu
                tweetsToSave.add(scrappedTweet);
                newTweetsCount++;
            }
        }

        // 4. Zapisz wszystkie zmiany (nowe i zaktualizowane) w jednej transakcji
        if (!tweetsToSave.isEmpty()) {
            tweetRepository.saveAll(tweetsToSave);
        }

        log.info("Zakończono przetwarzanie tweetów. " +
                        "Utworzono: {}, Zaktualizowano: {}, Pobranych: {}, Pominięto (brak linku): {}",
                newTweetsCount, updatedTweetsCount, scrappedTweets.size(), skippedTweetsCount);
    }

    /**
     * Prywatna metoda pomocnicza do aktualizacji pól istniejącego tweeta.
     *
     * @param existingTweet Encja tweeta z bazy danych.
     * @param newData       Nowe dane ze scrappera.
     */
    private void updateTweetData(Tweet existingTweet, Tweet newData) {
        existingTweet.setLikeCount(newData.getLikeCount());
        existingTweet.setRepostCount(newData.getRepostCount());
        existingTweet.setCommentCount(newData.getCommentCount());
        existingTweet.setViews(newData.getViews());
        existingTweet.setMediaLinks(newData.getMediaLinks());
        existingTweet.setContent(newData.getContent()); // Aktualizuj treść na wypadek edycji
        existingTweet.setUpdateDate(LocalDateTime.now());
        existingTweet.setNeedsRefresh(false); // Oznacz jako odświeżony
    }

    public Tweet findTweetById(Long id) {
        return tweetRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono tweeta o ID: " + id));
    }

    public TweetDTO saveTweet(Tweet tweet) {
        Tweet saved = tweetRepository.save(tweet);
        return TweetMapper.INSTANCE.toDto(saved);
    }

    public List<Long> findOldestTweetIds(int limit) {
        return tweetRepository.findOldestTweetIds(PageRequest.of(0, limit));
    }

    public List<Long> findIdsToRefresh(LocalDateTime cutoff, int limit) {
        return tweetRepository.findIdsToRefresh(cutoff, PageRequest.of(0, limit));
    }

    public List<Tweet> findAllByIds(List<Long> ids) {
        return tweetRepository.findAllById(ids);
    }

    public boolean existsTweetByLink(String link) {
        return tweetRepository.existsByLink(link);
    }

    public void updateTweets(Set<Tweet> updatedTweets) {
        tweetRepository.saveAll(updatedTweets);
    }
}
