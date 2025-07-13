package pl.bgnat.master.xsnts.topicmodeling.service;

import cc.mallet.pipe.*;
import cc.mallet.pipe.iterator.StringArrayIterator;
import cc.mallet.topics.ParallelTopicModel;
import cc.mallet.types.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.stat.descriptive.rank.Median;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import pl.bgnat.master.xsnts.topicmodeling.dto.CoherenceMetrics;
import pl.bgnat.master.xsnts.topicmodeling.dto.Document;
import pl.bgnat.master.xsnts.topicmodeling.dto.TopicModelingRequest;
import pl.bgnat.master.xsnts.topicmodeling.dto.TopicModelingResponse;
import pl.bgnat.master.xsnts.normalization.model.ProcessedTweet;
import pl.bgnat.master.xsnts.topicmodeling.model.DocumentTopicAssignment;
import pl.bgnat.master.xsnts.topicmodeling.model.TopicModelingResult;
import pl.bgnat.master.xsnts.topicmodeling.model.TopicModelingResult.ModelStatus;
import pl.bgnat.master.xsnts.topicmodeling.model.TopicResult;
import pl.bgnat.master.xsnts.normalization.repository.ProcessedTweetRepository;
import pl.bgnat.master.xsnts.topicmodeling.repository.DocumentTopicAssignmentRepository;
import pl.bgnat.master.xsnts.topicmodeling.repository.TopicModelingResultRepository;
import pl.bgnat.master.xsnts.topicmodeling.repository.TopicResultRepository;
import pl.bgnat.master.xsnts.topicmodeling.strategy.HashtagPoolingStrategy;
import pl.bgnat.master.xsnts.topicmodeling.strategy.TemporalPoolingStrategy;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static pl.bgnat.master.xsnts.topicmodeling.dto.TopicModelingResponse.*;
import static pl.bgnat.master.xsnts.topicmodeling.model.TopicModelingResult.ModelStatus.*;

/**
 * Zoptymalizowany serwis do topic modeling z poprawkami wydajnościowymi
 * Implementuje najlepsze praktyki z literatury naukowej + optymalizacje pamięci
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class MalletTopicModelingService {

    public static final int MIN_DOCUMENT_SIZE = 10;
    public static final int MAX_MODEL_ITER    = 1000;
    private static final List<Integer> K_GRID = List.of(6, 8, 10, 12, 14);
    private static final Set<String> TWO_LETTER_WHITELIST = Set.of("po", "na", "od", "do");

    /* ─────────────────────────  repozytoria  ───────────────────────── */
    private final ProcessedTweetRepository processedTweetRepository;
    private final TopicModelingResultRepository topicModelingResultRepository;
    private final TopicResultRepository topicResultRepository;
    private final DocumentTopicAssignmentRepository documentTopicAssignmentRepository;

    /* ─────────────────────────  usługi  ───────────────────────── */
    private final HashtagPoolingStrategy hashtagPoolingStrategy;
    private final TemporalPoolingStrategy temporalPoolingStrategy;

    private final TopicCoherenceCalculator coherenceCalculator;
    private final ObjectMapper objectMapper;

    /* ─────────────────────────  konfig  ───────────────────────── */
    @Value("${app.topic-modeling.models-directory:./output/topic_models}")
    private String modelsDirectory;
    @Value("${app.topic-modeling.default-iterations:1000}")
    private int defaultIterations;
    @Value("${app.topic-modeling.num-threads:2}")
    private int numThreads;
    @Value("${app.topic-modeling.responses-directory:./output/topic_responses}")
    private String responsesDir;

    @Value("${app.topic-modeling.alpha-sum:5.0}")
    private double alphaSum;
    @Value("${app.topic-modeling.beta:0.01}")
    private double beta;

    /**
     * Główna metoda uruchamiająca proces topic modeling
     */
    public TopicModelingResponse performTopicModeling(TopicModelingRequest request) {
        log.info("Rozpoczynam topic modeling strategia: {}", request.getPoolingStrategy());

        logMemoryUsage("Start");
        TopicModelingResult modelResult = createModelRecordToBeTrained(request);

        try {
            // 1. Pobierz znormalizowane tweety
            List<ProcessedTweet> processedTweets = getProcessedTweets(request);
            logMemoryUsage("Po pobraniu znormalizowanych tweetów");

            // 2. Grupuj tweety z filtrowaniem spamu angielskiego
            Map<String, List<ProcessedTweet>> groupedTweets = poolTweetsWithStrategy(processedTweets, request.getPoolingStrategy());
            logMemoryUsage("Po grupowaniu");

            // 3. Dynamiczny próg długości tekstu
            int medianGroup = (int) new Median().evaluate(groupedTweets.values().stream().mapToDouble(List::size).toArray());
            int dynMinTextLen = Math.max(10, medianGroup / 2);

            // 4. Przygotuj dokumenty (zoptymalizowane)
            List<Document> documents = prepareDocumentsFromTweets(groupedTweets, request, dynMinTextLen);
            logMemoryUsage("Po przygotowaniu dokumentów");

            // Zwolnij pamięć po przygotowaniu dokumentów (bardzo duze obiekty)
            processedTweets.clear();
            groupedTweets.clear();

            // 5. Grid-search liczby tematów
            int bestK = selectBestK(documents);
            request.setNumberOfTopics(bestK);

            // 6. Uruchom MALLET LDA
            ParallelTopicModel lda = trainLDAModel(documents, request);

            // 7. Zapisy i metryki
            String modelPath = saveModel(lda, modelResult.getModelName());
            extractAndSaveResults(lda, modelResult, documents);
            CoherenceMetrics metrics = calculateAdvancedCoherence(lda, documents);

            // 8. Aktualizuj rekord w bazie
            updateModelRecord(modelResult, modelPath, metrics, COMPLETED, null);


            log.info("Topic modeling zakończony pomyślnie. Model ID: {}", modelResult.getId());
            logMemoryUsage("Koniec");

            TopicModelingResponse response = buildResponse(modelResult);
            persistResponse(response);
            return response;
        } catch (Exception e) {
            log.error("Błąd podczas topic modeling: {}", e.getMessage(), e);
            updateModelRecord(modelResult, null, CoherenceMetrics.empty(),  FAILED, e.getMessage());
            throw new RuntimeException("Topic modeling failed", e);
        }
    }

    /* ─────────────────────────  grid-search  ───────────────────────── */
    private int selectBestK(List<Document> docs) throws IOException {
        double bestNpmi = Double.NEGATIVE_INFINITY;
        int    bestK    = K_GRID.get(0);

        for (int k : K_GRID) {
            TopicModelingRequest tmp = TopicModelingRequest.builder().numberOfTopics(k).build();
            ParallelTopicModel mdl  = trainLDAModel(docs, tmp);
            CoherenceMetrics  cm   = calculateAdvancedCoherence(mdl, docs);
            if (cm.getNpmi() > bestNpmi) {
                bestNpmi = cm.getNpmi();
                bestK    = k;
            }
            log.info("grid k={}  NPMI={}", k, cm.getNpmi());
        }
        log.info("Wybrano k={} (NPMI={})", bestK, bestNpmi);
        return bestK;
    }

    private List<Document> prepareDocumentsFromTweets(Map<String, List<ProcessedTweet>> groupedTweets, TopicModelingRequest request, int dynMinTextLen) {

        int minSize = Optional.ofNullable(request.getMinDocumentSize())
                              .orElse(MIN_DOCUMENT_SIZE);

        List<Document> documents = groupedTweets.entrySet().stream()
                .filter(e -> e.getValue().size() >= minSize)
                .map(e -> createDocument(e.getKey(), e.getValue(), request, dynMinTextLen))
                .filter(Objects::nonNull)
                .toList();

        log.info("Utworzono {} dokumentów z minimalnym rozmiarem {}", documents.size(), minSize);
        return documents;
    }

    private Document createDocument(String documentId, List<ProcessedTweet> tweets, TopicModelingRequest request, int minLen) {
        StringBuilder sb = new StringBuilder();
        List<Long> tweetIds = new ArrayList<>();

        for (ProcessedTweet tweet : tweets) {
            String json = "lemmatized".equals(request.getTokenStrategy())
                    ? tweet.getTokensLemmatized() : tweet.getTokens();

            sb.append(extractTokensAsText(tweet.getId(), json, request.isSkipMentions()))
                    .append(' ');
            tweetIds.add(tweet.getOriginalTweet().getId());
        }
        String text = sb.toString().trim();
        if (text.length() < minLen) return null;

        return new Document(documentId, text, tweetIds, tweets.size());
    }

    private ParallelTopicModel trainLDAModel(List<Document> documents, TopicModelingRequest request) throws IOException {
        log.info("Trenuję model LDA na {} utworzonych dokumentach", documents.size());

        // Ustawienia wstepne pipelineu modelu
        ArrayList<Pipe> pipeList = new ArrayList<>();
        pipeList.add(new CharSequence2TokenSequence(Pattern.compile("\\S+")));
        pipeList.add(new TokenSequenceLowercase());

        // opcjonalne bigramy
        if (request.isUseBigrams()) pipeList.add(new TokenSequenceNGrams(new int[]{2}));

        pipeList.add(new TokenSequence2FeatureSequence()); //wymagane na koncu
        Pipe pipe = new SerialPipes(pipeList);

        // Utwórz instancje
        InstanceList instances = new InstanceList(pipe);

        String[] documentsArray = documents.stream()
                .map(Document::text)
                .toArray(String[]::new);

        instances.addThruPipe(new StringArrayIterator(documentsArray));

        int k = request.getNumberOfTopics();
        ParallelTopicModel model = new ParallelTopicModel(k);
        model.addInstances(instances);

        int numThreadsCalculated = Math.min(numThreads, Runtime.getRuntime().availableProcessors());
        model.setNumThreads(numThreadsCalculated);

        int iterations = Math.min(Optional.ofNullable(request.getMaxIterations()).orElse(defaultIterations), MAX_MODEL_ITER);
        model.setNumIterations(iterations);

        //  Griffitha: np: alphaSum = 50/k, beta = 0.01, k - liczba tematow
        double calculatedAlphaSum = this.alphaSum / k;
        double beta = this.beta;
        model.alphaSum = calculatedAlphaSum;
        model.beta = beta;
        model.betaSum = beta * instances.getDataAlphabet().size();

        // Zaktualizuj tablicę alpha
        for (int i = 0; i < k; i++) {
            model.alpha[i] = alphaSum / k;
        }

        // MALLET będzie adaptował alpha i beta co 10 iteracj
        model.setSymmetricAlpha(false);   // asym
        model.setBurninPeriod(50);        // burn-in
        model.setOptimizeInterval(10);    // 10 iter

        log.info("Parametry modelu: tematów={}, iteracji={}, wątków={}, alpha={}, beta={}",
                model.numTopics, iterations, model.getNumTopics(), alphaSum, beta);


        // Trenuj model
        model.estimate();

        log.info("Model LDA wytrenowany pomyślnie");
        return model;
    }

    private Map<String, List<ProcessedTweet>> poolTweetsWithStrategy(List<ProcessedTweet> tweets, String strategy) {

        Map<String, List<ProcessedTweet>> groupedTweets = switch (strategy.toLowerCase()) {
            case "hashtag" -> hashtagPoolingStrategy.poolTweets(tweets);
            case "temporal" -> temporalPoolingStrategy.poolTweets(tweets);
            default -> {
                log.warn("Nieznana strategia pooling: {}, używam hashtag", strategy);
                yield hashtagPoolingStrategy.poolTweets(tweets);
            }
        };

        log.info("Utworzono {} grup tweetow", groupedTweets.size());

        return groupedTweets;
    }

    private void extractAndSaveResults(ParallelTopicModel model, TopicModelingResult modelResult, List<Document> documents) {
        log.info("Wyciągam i zapisuję wyniki modelu");

        // 1. Zapisz wyniki tematów
        saveTopicResults(model, modelResult);

        // 2. Zapisz przypisania dokumentów
        saveDocumentAssignments(model, modelResult, documents);

        // 3. Aktualizuj statystyki tematów
        updateTopicStatistics(modelResult);

        // 4. Aktualizuj liczniki w głównym rekordzie
        modelResult.setDocumentsCount(documents.size());
        modelResult.setOriginalTweetsCount(documents.stream().mapToInt(Document::tweetsCount).sum());

        topicModelingResultRepository.save(modelResult);
        log.info("Zapisano wyniki modelu z {} dokumentami", documents.size());
    }

    /**
     * Oblicza zaawansowane metryki
     */
    private CoherenceMetrics calculateAdvancedCoherence(ParallelTopicModel model, List<Document> documents) {
        // Pobierz top słowa z najlepszego tematu
        Alphabet alphabet = model.getAlphabet();
        TreeSet<IDSorter> sortedWords = model.getSortedWords().get(0);

        List<String> topWords = sortedWords.stream()
                .limit(10)
                .map(idSorter -> (String) alphabet.lookupObject(idSorter.getID()))
                .collect(Collectors.toList());

        List<String> documentTexts = documents.stream()
                .map(Document::text)
                .collect(Collectors.toList());

        CoherenceMetrics coherenceMetrics = coherenceCalculator.calculateAllMetrics(topWords, documentTexts);
        coherenceMetrics.setPerplexity(calculatePerplexity(model));
        return coherenceMetrics;
    }

    private void updateTopicStatistics(TopicModelingResult modelResult) {
        log.info("Aktualizuję statystyki tematów dla modelu {}", modelResult.getId());

        List<TopicResult> topicResults = topicResultRepository.findByTopicModelingResultIdOrderByTopicId(modelResult.getId());

        for (TopicResult topicResult : topicResults) {
            try {
                // Policz dokumenty dla tego tematu
                Long documentCount = documentTopicAssignmentRepository.countDocumentsByTopic(topicResult.getTopicId(), modelResult.getId());

                // Oblicz średnie prawdopodobieństwo (fallback w Javie)
                Double averageProbability = calculateAverageTopicProbabilityJava(topicResult.getTopicId(), modelResult.getId());

                // Aktualizuj statystyki
                topicResult.setDocumentCount(documentCount != null ? documentCount.intValue(): 0);
                topicResult.setAverageProbability(averageProbability != null ? averageProbability : 0.0);

                topicResultRepository.save(topicResult);

                log.debug("Zaktualizowano temat {}: {} dokumentów, prawdopodobieństwo: {}}", topicResult.getTopicId(), documentCount, averageProbability);

            } catch (Exception e) {
                log.error("Błąd podczas aktualizacji statystyk tematu {}: {}", topicResult.getTopicId(), e.getMessage());
            }
        }

        log.info("Zakończono aktualizację statystyk tematów");
    }

    private Double calculateAverageTopicProbabilityJava(Integer topicId, Long modelId) {
        try {
            List<DocumentTopicAssignment> assignments = documentTopicAssignmentRepository
                    .findByTopicModelingResultId(modelId);

            double sum = 0.0;
            int count = 0;

            for (DocumentTopicAssignment assignment : assignments) {
                try {
                    Map<String, Double> probabilities = objectMapper.readValue(
                            assignment.getTopicProbabilities(),
                            new TypeReference<Map<String, Double>>() {});

                    Double probability = probabilities.get(topicId.toString());
                    if (probability != null) {
                        sum += probability;
                        count++;
                    }
                } catch (Exception e) {
                    // Skip problematic records
                }
            }

            return count > 0 ? sum / count : 0.0;

        } catch (Exception e) {
            log.error("Błąd obliczania średniego prawdopodobieństwa dla tematu {}: {}",
                    topicId, e.getMessage());
            return 0.0;
        }
    }

    public List<TopicModelingResponse> getAvailableModels() {
        return topicModelingResultRepository.findByStatusOrderByTrainingDateDesc(
                        COMPLETED)
                .stream()
                .map(this::buildResponse)
                .collect(Collectors.toList());
    }

    public TopicModelingResponse getModelDetails(Long modelId) {
        TopicModelingResult model = topicModelingResultRepository.findById(modelId)
                .orElseThrow(() -> new RuntimeException("Model not found: " + modelId));
        return buildResponse(model);
    }
    private void logMemoryUsage(String phase) {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory() / (1024 * 1024); // MB
        long freeMemory = runtime.freeMemory() / (1024 * 1024);   // MB
        long usedMemory = totalMemory - freeMemory;

        log.info("Pamięć [{}]: Użyte={}MB, Całkowite={}MB, Wolne={}MB",
                phase, usedMemory, totalMemory, freeMemory);
    }

    private TopicModelingResult createModelRecordToBeTrained(TopicModelingRequest request) {
        TopicModelingResult model = TopicModelingResult.builder()
                .topicModel(request.getTopicModel())
                .tokenStrategy(request.getTokenStrategy())
                .modelName(StringUtils.hasLength(request.getModelName()) ? request.getModelName() : generateModelName(request))
                .numberOfTopics(request.getNumberOfTopics())
                .poolingStrategy(request.getPoolingStrategy())
                .documentsCount(0)
                .originalTweetsCount(0)
                .trainingDate(LocalDateTime.now())
                .status(TRAINING)
                .build();

        return topicModelingResultRepository.save(model);
    }

    private List<ProcessedTweet> getProcessedTweets(TopicModelingRequest request) {

        LocalDateTime from = request.getStartDate();
        LocalDateTime to   = request.getEndDate();

        // log – początek
        log.info("Pobieram tweety: od={}  do={}", from, to);

        // oba krańce
        if (from != null && to != null) {
            List<ProcessedTweet> list = processedTweetRepository.findAllInDateRange(from, to);
            log.debug("Zwrócono {} tweetów (BETWEEN)", list.size());
            return list;
        }

        // tylko dolna granica
        if (from != null) {
            List<ProcessedTweet> list = processedTweetRepository.findByOriginalTweet_PostDateAfter(from);
            log.debug("Zwrócono {} tweetów (>= {} )", list.size(), from);
            return list;
        }

        // tylko górna granica
        if (to != null) {
            List<ProcessedTweet> list = processedTweetRepository.findByOriginalTweet_PostDateBefore(to);
            log.debug("Zwrócono {} tweetów (<= {} )", list.size(), to);
            return list;
        }

        // brak filtrów
        List<ProcessedTweet> list = processedTweetRepository.findAll();
        log.debug("Zwrócono {} tweetów (bez filtra daty)", list.size());
        return list;
    }

    private String extractTokensAsText(Long tweetId, String tweetTokensJson, boolean skipMentions) {
        try {
            List<String> toks = objectMapper.readValue(tweetTokensJson, new TypeReference<>() {});
            return toks.stream()
                    .filter(t -> !skipMentions || !t.equals("@anonymized"))
                    .filter(t -> t.length() > 2 ||
                            (t.length() == 2 && TWO_LETTER_WHITELIST.contains(t)))
                    .collect(Collectors.joining(" "));
        } catch (Exception e) {
            log.warn("Token parse error tweet {}", tweetId, e);
            return "";
        }
    }



    private String saveModel(ParallelTopicModel model, String modelName) {
        File modelsDir = new File(modelsDirectory);
        if (!modelsDir.exists()) {
            modelsDir.mkdirs();
        }

        String modelPath = modelsDirectory + "/" + modelName + ".mallet";
        model.write(new File(modelPath));

        log.info("Model zapisany do: {}", modelPath);
        return modelPath;
    }

    private void saveTopicResults(ParallelTopicModel model, TopicModelingResult modelResult) {
        Alphabet alphabet = model.getAlphabet();

        for (int topicId = 0; topicId < model.getNumTopics(); topicId++) {
            TreeSet<IDSorter> sortedWords = model.getSortedWords().get(topicId);

            List<WordWeight> topWords = new ArrayList<>();
            int wordCount = 0;

            for (IDSorter idSorter : sortedWords) {
                if (wordCount >= 20) break;

                String word = (String) alphabet.lookupObject(idSorter.getID());
                double weight = idSorter.getWeight();

                topWords.add(WordWeight.builder()
                        .word(word)
                        .weight(weight)
                        .build());
                wordCount++;
            }

            String topicLabel = generateTopicLabel(topWords);

            try {
                TopicResult topicResult = TopicResult.builder()
                        .topicModelingResult(modelResult)
                        .topicId(topicId)
                        .topicLabel(topicLabel)
                        .topWords(objectMapper.writeValueAsString(topWords))
                        .wordCount(sortedWords.size())
                        .documentCount(0)
                        .averageProbability(0.0)
                        .build();

                topicResultRepository.save(topicResult);
            } catch (Exception e) {
                log.error("Błąd podczas zapisywania tematu {}: {}", topicId, e.getMessage());
            }
        }
    }

    private void saveDocumentAssignments(ParallelTopicModel model, TopicModelingResult modelResult, List<Document> documents) {
        for (int docIndex = 0; docIndex < documents.size(); docIndex++) {
            Document document = documents.get(docIndex);

            try {
                double[] topicProbabilities = model.getTopicProbabilities(docIndex);

                int dominantTopicId = 0;
                double maxProbability = topicProbabilities[0];

                for (int i = 1; i < topicProbabilities.length; i++) {
                    if (topicProbabilities[i] > maxProbability) {
                        maxProbability = topicProbabilities[i];
                        dominantTopicId = i;
                    }
                }

                Map<Integer, Double> probabilities = new HashMap<>();
                for (int i = 0; i < topicProbabilities.length; i++) {
                    probabilities.put(i, topicProbabilities[i]);
                }

                DocumentTopicAssignment assignment = DocumentTopicAssignment.builder()
                        .topicModelingResult(modelResult)
                        .documentId(document.id())
                        .documentType(determineDocumentType(document.id()))
                        .dominantTopicId(dominantTopicId)
                        .topicProbabilities(objectMapper.writeValueAsString(probabilities))
                        .tweetIds(objectMapper.writeValueAsString(document.tweetIds()))
                        .tweetsCount(document.tweetsCount())
                        .build();

                documentTopicAssignmentRepository.save(assignment);

            } catch (Exception e) {
                log.error("Błąd podczas zapisywania przypisania dokumentu {}: {}", document.id(), e.getMessage());
            }
        }
    }

    private String generateTopicLabel(List<WordWeight> topWords) {
        if (topWords.isEmpty()) return "Empty Topic";

        return topWords.stream()
                .limit(3)
                .map(WordWeight::getWord)
                .collect(Collectors.joining(", "));
    }

    private String determineDocumentType(String documentId) {
        if (documentId.startsWith("author_")) return "author";
        if (documentId.startsWith("day_")) return "temporal";
        return "hashtag";
    }

    private String generateModelName(TopicModelingRequest request) {
        return String.format("LDA_%d_topics_%s_%s",
                request.getNumberOfTopics(),
                request.getPoolingStrategy(),
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")));
    }

    private void updateModelRecord(TopicModelingResult model, String modelPath,
                                   CoherenceMetrics coherenceMetrics,
                                   ModelStatus status, String errorMessage) {
        model.setModelPath(modelPath);
        model.setPmi(coherenceMetrics.getPmi());
        model.setNpmi(coherenceMetrics.getNpmi());
        model.setUci(coherenceMetrics.getUci());
        model.setUmass(coherenceMetrics.getUmass());
        model.setCoherenceInterpretation(coherenceMetrics.getPmiInterpretation());
        model.setUmassInterpretation(coherenceMetrics.getUmassInterpretation());
        model.setPerplexity(coherenceMetrics.getPerplexity());
        model.setStatus(status);
        model.setErrorMessage(errorMessage);

        topicModelingResultRepository.save(model);
    }

    private TopicModelingResponse buildResponse(TopicModelingResult model) {
        List<TopicResult> topicResults = topicResultRepository
                .findByTopicModelingResultIdOrderByTopicId(model.getId());

        List<TopicSummary> topics = topicResults.stream()
                .map(this::buildTopicSummary)
                .collect(Collectors.toList());

        return builder()
                .modelId(model.getId())
                .modelName(model.getModelName())
                .numberOfTopics(model.getNumberOfTopics())
                .poolingStrategy(model.getPoolingStrategy())
                .tokenStrategy(model.getTokenStrategy())
                .skipMentions(model.isSkipMentions())
                .isUseBigrams(model.isUseBigrams())
                .documentsCount(model.getDocumentsCount())
                .originalTweetsCount(model.getOriginalTweetsCount())
                .trainingDate(model.getTrainingDate())
                .npmiScore(model.getNpmi())
                .pmiScore(model.getPmi())
                .uciScore(model.getUci())
                .umassScore(model.getUmass())
                .pmiInterpretation(model.getCoherenceInterpretation())
                .umassInterpretation(model.getUmassInterpretation())
                .perplexity(model.getPerplexity())
                .status(model.getStatus().name())
                .topics(topics)
                .build();
    }

    private double calculatePerplexity(ParallelTopicModel model) {
        try {
            double logLikelihood = model.modelLogLikelihood();
            int totalTokens = model.totalTokens;

            if (totalTokens == 0) {
                log.warn("Total tokens is 0, cannot calculate perplexity");
                return Double.NaN;
            }

            double perplexity = Math.exp(-logLikelihood / totalTokens);

            log.debug("Obliczono perplexity: {} (logLikelihood: {}, totalTokens: {})", perplexity, logLikelihood, totalTokens);

            return perplexity;

        } catch (Exception e) {
            log.error("Błąd podczas obliczania perplexity: {}", e.getMessage());
            return Double.NaN;
        }
    }

    private TopicSummary buildTopicSummary(TopicResult topicResult) {
        List<WordWeight> topWords = new ArrayList<>();

        try {
            topWords = objectMapper.readValue(topicResult.getTopWords(), new TypeReference<List<WordWeight>>() {});
        } catch (Exception e) {
            log.warn("Błąd podczas parsowania top words dla tematu {}: {}",
                    topicResult.getTopicId(), e.getMessage());
        }

        return TopicSummary.builder()
                .topicId(topicResult.getTopicId())
                .topicLabel(topicResult.getTopicLabel())
                .topWords(topWords)
                .documentCount(topicResult.getDocumentCount())
                .averageProbability(topicResult.getAverageProbability())
                .build();
    }

    private void persistResponse(TopicModelingResponse resp) {
        try {
            File dir = new File(responsesDir);
            if (!dir.exists()) dir.mkdirs();

            String timestamp = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm"));
            String fileName  = String.format(
                    "model_%d_%s_%s.json",
                    resp.getModelId(),
                    resp.getModelName().replaceAll("\\s+", "_"),
                    timestamp);

            File out = new File(dir, fileName);
            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(out, resp);

            log.info("Zapisano TopicModelingResponse → {}", out.getAbsolutePath());
        } catch (Exception e) {
            log.error("Nie udało się zapisać response'a", e);
        }
    }

}
