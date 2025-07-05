package pl.bgnat.master.xscrapper.service.scrapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.chrome.ChromeDriver;
import org.springframework.stereotype.Service;
import pl.bgnat.master.xscrapper.config.scrapper.CredentialProperties;
import pl.bgnat.master.xscrapper.dto.UserCredential;
import pl.bgnat.master.xscrapper.dto.UserCredential.User;
import pl.bgnat.master.xscrapper.model.Tweet;
import pl.bgnat.master.xscrapper.pages.LoginPage;
import pl.bgnat.master.xscrapper.pages.WallPage;
import pl.bgnat.master.xscrapper.pages.WallPage.WallType;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static pl.bgnat.master.xscrapper.dto.UserCredential.User.USER_2;
import static pl.bgnat.master.xscrapper.pages.WallPage.WallType.LATEST;
import static pl.bgnat.master.xscrapper.pages.WallPage.WallType.POPULAR;
import static pl.bgnat.master.xscrapper.utils.WaitUtils.waitRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScrapperService {
    private final TweetService tweetService;
    private final AdsPowerService adsPowerService;
    private final CredentialProperties credentialProperties;
    private final Trends24ScraperService trends24ScraperService;

    private List<String> currentTrendingKeyword = new ArrayList<>();

    public void scheduledScrapeForYouWallAsync() {
        int credentialCount = 5;
        int index = 0;

        ExecutorService executor = Executors.newFixedThreadPool(credentialCount);
        for (User user : User.values()) {
            final int userIndex = index % credentialCount;
            final int keywordIndex = index + 1;
            index++;

            executor.submit(() -> {
                String originalName = Thread.currentThread().getName();
                try {
                    String formattedThreadName = getFormattedThreadName(user.name(), userIndex, keywordIndex);
                    Thread.currentThread().setName(formattedThreadName);

                    scrapeForYou(user);
                    waitRandom();
                } catch (Exception e) {
                    log.error("Błąd przy przetwarzaniu ForYou dla: {}", user.name());
                } finally {
                    Thread.currentThread().setName(originalName);
                }
            });
        }

        executor.shutdown();
        try {
            if (!executor.awaitTermination(4, TimeUnit.HOURS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

//    @Scheduled(cron = "0 0 */3 * * *")
//    @PostConstruct
    public void scheduledScrapePopularKeywords() {
        do {
            updateTrendingKeywords();
        } while (currentTrendingKeyword.isEmpty());

        scrapeTrendingWall(POPULAR);
        waitRandom();
    }

//    @Scheduled(cron = "0 0 */4 * * *")
    public void scheduledScrapeLatestKeywords() {
        do {
            updateTrendingKeywords();
        } while (currentTrendingKeyword.isEmpty());

        scrapeTrendingWall(LATEST);
        waitRandom();
    }

//    @PostConstruct
    private void scrapeOneByKeyword() {
        String keyword = "konklawa";
        WallType wallType = LATEST;
        User user = USER_2;
        WallPage wallPage;
        ChromeDriver trendDriver = null;
        try {
            trendDriver = adsPowerService.getDriverForUser(user);

            LoginPage loginPage = new LoginPage(trendDriver, credentialProperties);
            loginPage.loginIfNeeded(user);

            wallPage = new WallPage(trendDriver, user);

            switch (wallType) {
                case POPULAR -> wallPage.openPopular(keyword);
                case LATEST -> wallPage.openLatest(keyword);
            }

            Set<Tweet> scrappedTweets = wallPage.scrapeTweets();

            tweetService.saveTweets(scrappedTweets);
            waitRandom();
        } catch (Exception e) {
            log.error("Błąd przy przetwarzaniu keyworda: {}", keyword);
        } finally {
            try {
                if (trendDriver != null) {
                    adsPowerService.stopDriver(user);
                }
            } catch (Exception e) {
                log.error("Błąd podczas zatrzymywania przeglądarki dla użytkownika: {}: {}",
                        user, e.getMessage());
            }
        }
    }

    private void scrapeForYou(User user) {
        ChromeDriver userDriver = adsPowerService.getDriverForUser(user);

        LoginPage loginPage = new LoginPage(userDriver, credentialProperties);
        loginPage.loginIfNeeded(user);

        WallPage wallPage = new WallPage(userDriver, user);
        wallPage.openForYou();

        Set<Tweet> scrappedTweets = wallPage.scrapeTweets();

        tweetService.saveTweets(scrappedTweets);

        log.info("Zamykam ForYou dla: {}", user);
        wallPage.exit();
    }

    private void scrapeTrendingWall(WallType wallType) {
        int credentialCount = 5;

        Queue<String> keywordsQueue = new LinkedList<>(currentTrendingKeyword);

        ExecutorService executor = Executors.newFixedThreadPool(credentialCount);

        AtomicInteger counter = new AtomicInteger(0);

        while (!keywordsQueue.isEmpty()) {
            String keyword = keywordsQueue.poll();
            int currentCounter = counter.incrementAndGet();

            executor.submit(() -> {
                String originalName = Thread.currentThread().getName();

                int userIndex = (currentCounter - 1) % credentialCount;
                User user = UserCredential.getUser(userIndex);

                ChromeDriver trendDriver = null;

                try {
                    String formattedThreadName = getFormattedThreadName(keyword, userIndex, currentCounter);
                    Thread.currentThread().setName(formattedThreadName);

                    log.info("Rozpoczynam przetwarzanie trendu: {} dla użytkownika: {}", keyword, user);

                    trendDriver = adsPowerService.getDriverForUser(user);

                    if (trendDriver == null) {
                        log.error("Nie udało się uzyskać przeglądarki dla użytkownika: {} i trendu: {}", user, keyword);
                        return;
                    }

                    LoginPage loginPage = new LoginPage(trendDriver, credentialProperties);
                    loginPage.loginIfNeeded(user);

                    WallPage wallPage = new WallPage(trendDriver, user);

                    switch (wallType) {
                        case POPULAR -> wallPage.openPopular(keyword);
                        case LATEST -> wallPage.openLatest(keyword);
                    }

                    Set<Tweet> scrappedTweets = wallPage.scrapeTweets();
                    tweetService.saveTweets(scrappedTweets);

                    log.info("Zakończono przetwarzanie trendu: {} dla użytkownika: {}", keyword, user);
                } catch (Exception e) {
                    log.error("Błąd przy przetwarzaniu trendu: {} dla użytkownika: {}. ErrorMsg: {}",
                            keyword, user, e.getMessage());
                } finally {
                    try {
                        if (trendDriver != null) {
                            adsPowerService.stopDriver(user);
                        }
                    } catch (Exception e) {
                        log.error("Błąd podczas zatrzymywania przeglądarki dla użytkownika: {}: {}",
                                user, e.getMessage());
                    }

                    Thread.currentThread().setName(originalName);
                    log.info("Zamykam trendujacy tag: {}", keyword);
                }
            });

            // Dodaj małe opóźnienie między zadaniami, aby zapobiec przeciążeniu API AdsPower
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        executor.shutdown();
        try {
            if (!executor.awaitTermination(4, TimeUnit.HOURS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private static String getFormattedThreadName(String keyword, int userIndex, int keywordIndex) {
        String leftPart = String.format("t%d%02d", userIndex, keywordIndex);
        int maxKeywordLength = 15 - (leftPart.length() + 1);
        String truncatedKeyword = (keyword.length() > maxKeywordLength)
                ? keyword.substring(0, maxKeywordLength)
                : keyword;
        return String.format("%s-%s", leftPart, truncatedKeyword);
    }

    private void updateTrendingKeywords() {
        ArrayList<String> lista1 = new ArrayList<>();
        lista1.add("gospodarka");
        lista1.add("bezpieczeństwo");
        lista1.add("portfele Polaków");
        lista1.add("podatki");
        lista1.add("inflacja");
        lista1.add("suwerenność");
        lista1.add("NATO");
        lista1.add("Unia Europejska");
        lista1.add("Zielony Ład");
        lista1.add("migracja");
        lista1.add("pakt migracyjny");
        lista1.add("samorząd");
        lista1.add("rozwój regionalny");
        lista1.add("inwestycje");
        lista1.add("polityka prorodzinna");
        lista1.add("edukacja");
        lista1.add("zdrowie");
        lista1.add("prawo kobiet");
        lista1.add("równość");
        lista1.add("sprawiedliwość społeczna");
        lista1.add("reforma sądownictwa");
        lista1.add("wolność słowa");
        lista1.add("wolne media");
        lista1.add("demokracja");
        lista1.add("debata prezydencka");
        lista1.add("mobilizacja wyborców");
        lista1.add("frekwencja");
        lista1.add("kampania negatywna");
        lista1.add("snus");
        lista1.add("gangus");
        lista1.add("afera");

        ArrayList<String> hashtagiWybory2025 = new ArrayList<>();
        hashtagiWybory2025.add("#wybory2025");
        hashtagiWybory2025.add("#wybory");
        hashtagiWybory2025.add("#głosowanie");
        hashtagiWybory2025.add("#polska");
        hashtagiWybory2025.add("#vote");
        hashtagiWybory2025.add("#poland");
        hashtagiWybory2025.add("#Trzaskowski2025");
        hashtagiWybory2025.add("#Nawrocki2025");
        hashtagiWybory2025.add("#Mentzen2025");
        hashtagiWybory2025.add("#Hołownia2025");
        hashtagiWybory2025.add("#KobietyzTrzaskiem");
        hashtagiWybory2025.add("#NormalnaPolska");
        hashtagiWybory2025.add("#PoPierwszePolska");
        hashtagiWybory2025.add("#CałaPolskaNaprzód");
        hashtagiWybory2025.add("#DebataPrezydencka");
        hashtagiWybory2025.add("#WybieramPolskę");
        hashtagiWybory2025.add("#PolskaPrzyszłości");
        hashtagiWybory2025.add("#BezpiecznaGranica");
        hashtagiWybory2025.add("#Suwerenność");
        hashtagiWybory2025.add("#ZjednoczyćPrawicę");
        hashtagiWybory2025.add("#Konfederacja");
        hashtagiWybory2025.add("#Lewica2025");
        hashtagiWybory2025.add("#KoalicjaObywatelska");
        hashtagiWybory2025.add("#PrawoiSprawiedliwość");
        hashtagiWybory2025.add("#PolskaSilna");
        hashtagiWybory2025.add("#PolskaDlaWszystkich");
        hashtagiWybory2025.add("#MłodziGłosują");
        hashtagiWybory2025.add("#MarszPatriotów");
        hashtagiWybory2025.add("#WyboryPrezydenckie");

        ArrayList<String> lista2 = new ArrayList<>();
        lista2.add("Karol Nawrocki");
        lista2.add("Rafał Trzaskowski");
        lista2.add("Szymon Hołownia");
        lista2.add("Sławomir Mentzen");
        lista2.add("Marek Jakubiak");
        lista2.add("Adrian Zandberg");
        lista2.add("Grzegorz Braun");
        lista2.add("Donald Tusk");
        lista2.add("Małgorzata Trzaskowska");
        lista2.add("Konfederacja");
        lista2.add("#GłosujęNaNawrockiegoo");
        lista2.add("#GłosujęNaTrzaskowskiegoo");
        lista2.add("#Konfederacja2025");
        lista2.add("#Bezpieczeństwo");
        lista2.add("Koalicja Obywatelska");
        lista2.add("Prawo i Sprawiedliwość");
        lista2.add("Lewica");
        lista2.add("Trzecia Droga");
        lista2.add("Bracia Kamraci");
        lista2.add("wyborcy młodzi");
        lista2.add("wyborcy seniorzy");
        lista2.add("influencerzy");
        lista2.add("dziennikarze");
        lista2.add("eksperci");
        lista2.add("sztab wyborczy");
        lista2.add("wolontariusze");
        lista2.add("liderzy opinii");


        ArrayList<String> lista3 = new ArrayList<>();
        lista3.add("Cała Polska naprzód");
        lista3.add("Normalna Polska");
        lista3.add("Po pierwsze Polska");
        lista3.add("Polska bezpieczna");
        lista3.add("Polska dla wszystkich");
        lista3.add("Polska przyszłości");
        lista3.add("Silny prezydent");
        lista3.add("Zmieniamy Polskę");
        lista3.add("Nowy prezydent");
        lista3.add("Wybieram Polskę");
        lista3.add("Polska w Europie");
        lista3.add("Bezpieczna granica");
        lista3.add("Przyszłość młodych");
        lista3.add("Praca dla wszystkich");
        lista3.add("Emerytury bezpieczne");
        lista3.add("Polska wolna od korupcji");
        lista3.add("Polska suwerenna");
        lista3.add("Polska nowoczesna");
        lista3.add("Polska tradycyjna");
        lista3.add("Wspólnota narodowa");
        lista3.add("Dialog pokoleń");
        lista3.add("Polska rodzina");
        lista3.add("Polska silna gospodarczo");
        lista3.add("Polska sprawiedliwa");
        lista3.add("Polska otwarta");
        lista3.add("Polska innowacyjna");
        lista3.add("Polska ekologiczna");
        lista3.add("Polska bez inflacji");
        lista3.add("Polska solidarności");
        lista3.add("Polska demokracji");

        ArrayList<String> lista4 = new ArrayList<>();
        lista4.add("alfons");
        lista4.add("dziadek z Wermachtu");
        lista4.add("brutalizacja kampanii");
        lista4.add("hejt");
        lista4.add("");
        lista4.add("zamordyzm pandemiczny");
        lista4.add("eurokołchoz");
        lista4.add("antyamerykańskość");
        lista4.add("afera wyborcza");
        lista4.add("atak na media");
        lista4.add("podsłuchy");
        lista4.add("manipulacja");
        lista4.add("polaryzacja");
        lista4.add("wojna polsko-polska");
        lista4.add("wojna informacyjna");
        lista4.add("#polska");
        lista4.add("boty");
        lista4.add("dezinformacja");
        lista4.add("kampania negatywna");
        lista4.add("fake news");
        lista4.add("memy wyborcze");
        lista4.add("ironia polityczna");
        lista4.add("satyra polityczna");
        lista4.add("#wybory2025");
        lista4.add("#wybory");
        lista4.add("#poland");
        lista4.add("#KobietyzTrzaskiem");
        lista4.add("#NormalnaPolska");
        lista4.add("wyciek danych");
        lista4.add("polityczny trolling");

        ArrayList<String> lista5 = new ArrayList<>();
        lista5.add("aura");
        lista5.add("quebonafide");
        lista5.add("best feeling");
        lista5.add("pozdro z fanem");
        lista5.add("lody dresscode");
        lista5.add("polnoc poludnie");
        lista5.add("azbest");
        lista5.add("brainrot");
        lista5.add("brat");
        lista5.add("cringe");
        lista5.add("czemó");
        lista5.add("delulu");
        lista5.add("fr/FR");
        lista5.add("GOAT");
        lista5.add("oporowo");
        lista5.add("sigma");
        lista5.add("slay");
        lista5.add("womp womp");
        lista5.add("yapping");
        lista5.add("glamur");
        lista5.add("riz/rizzler/rizz");
        lista5.add("oi oi oi baka");
        lista5.add("fe!n/fein/fin");
        lista5.add("Iran");
        lista5.add("Aktobe");
        lista5.add("Zmechanizowanych");
        lista5.add("Święto Wojsk Pancernych");
        lista5.add("Giertycha");
        lista5.add("Wenecji");
        lista5.add("Kijów");
        lista5.add("#gangłysego");

        currentTrendingKeyword = hashtagiWybory2025;
    }

//    private void updateTrendingKeywords() {
//        try {
//            List<String> uniqueTrends = trends24ScraperService.scrapeUniqueTrendingKeywords();
//
//            if (!uniqueTrends.isEmpty()) {
//                currentTrendingKeyword.clear();
//                currentTrendingKeyword.addAll(uniqueTrends);
//
//                if (currentTrendingKeyword.size() > 50) {
//                    currentTrendingKeyword = currentTrendingKeyword.subList(0, 50);
//                }
//
//                log.info("Zaktualizowano listę trendujących słów kluczowych: {}", currentTrendingKeyword);
//            } else {
//                log.warn("Nie znaleziono nowych unikalnych trendów");
//            }
//        } catch (Exception e) {
//            log.error("Błąd podczas aktualizacji trendujących słów kluczowych: {}", e.getMessage(), e);
//        }
//    }
}