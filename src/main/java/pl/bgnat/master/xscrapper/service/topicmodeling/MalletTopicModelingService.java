package pl.bgnat.master.xscrapper.service.topicmodeling;

import cc.mallet.pipe.*;
import cc.mallet.pipe.iterator.StringArrayIterator;
import cc.mallet.topics.ParallelTopicModel;
import cc.mallet.types.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.bgnat.master.xscrapper.dto.TopicModelingRequest;
import pl.bgnat.master.xscrapper.dto.TopicModelingResponse;
import pl.bgnat.master.xscrapper.model.*;
import pl.bgnat.master.xscrapper.repository.*;
import pl.bgnat.master.xscrapper.service.topicmodeling.TopicCoherenceCalculator.CoherenceMetrics;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Zoptymalizowany serwis do topic modeling z poprawkami wydajnościowymi
 * Implementuje najlepsze praktyki z literatury naukowej + optymalizacje pamięci
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class MalletTopicModelingService {

    private final ProcessedTweetRepository processedTweetRepository;
    private final TopicModelingResultRepository topicModelingResultRepository;
    private final TopicResultRepository topicResultRepository;
    private final DocumentTopicAssignmentRepository documentTopicAssignmentRepository;
    private final ObjectMapper objectMapper;

    private final HashtagPoolingStrategy hashtagPoolingStrategy;
    private final TemporalPoolingStrategy temporalPoolingStrategy;

    private final TopicCoherenceCalculator coherenceCalculator;

    @Value("${app.topic-modeling.models-directory:./topic_models}")
    private String modelsDirectory;

    @Value("${app.topic-modeling.default-iterations:1000}")
    private int defaultIterations;

    @Value("${app.topic-modeling.num-threads:2}") // OPTYMALIZACJA: Zmniejsz z 4 na 2
    private int numThreads;

    // NOWE: Filtrowanie spamu
    private static final Set<String> SPAM_KEYWORDS = Set.of(
            "doorhandle", "profilehandle", "whatsapp",
            "click", "phone", "doorfittings", "hardwarefittings"
    );

    /**
     * Główna metoda uruchamiająca proces topic modeling
     * OPTYMALIZACJA: Dodano zarządzanie pamięcią i filtrowanie spamu
     */
    public TopicModelingResponse performTopicModeling(TopicModelingRequest request) {
        log.info("Rozpoczynam zoptymalizowany topic modeling: {} tematów, strategia: {}",
                request.getNumberOfTopics(), request.getPoolingStrategy());

        // OPTYMALIZACJA: Uruchom garbage collection przed rozpoczęciem
        System.gc();
        logMemoryUsage("Start");

        TopicModelingResult modelResult = createModelRecord(request);

        try {
            // 1. Pobierz przetworzone tweety
            List<ProcessedTweet> processedTweets = getProcessedTweets();
            logMemoryUsage("Po pobraniu tweetów");

            // 2. Grupuj tweety ze filtrowaniem spamu
            Map<String, List<ProcessedTweet>> groupedTweets = poolTweetsWithSpamFilter(
                    processedTweets, request.getPoolingStrategy());
            logMemoryUsage("Po grupowaniu");

            // 3. Przygotuj dokumenty (zoptymalizowane)
            List<Document> documents = prepareDocumentsOptimized(groupedTweets, request.getMinDocumentSize());
            logMemoryUsage("Po przygotowaniu dokumentów");

            // Zwolnij pamięć po przygotowaniu dokumentów
            processedTweets.clear();
            groupedTweets.clear();
            System.gc();

            // 4. Uruchom MALLET LDA (z optymalizacjami)
            ParallelTopicModel model = trainLDAModelOptimized(documents, request);
            logMemoryUsage("Po treningu modelu");

            // 5. Zapisz model do pliku
            String modelPath = saveModel(model, modelResult.getModelName());

            // 6. Wyciągnij wyniki i zapisz do bazy (NAPRAWIONE)
            extractAndSaveResults(model, modelResult, documents, new HashMap<>());

            // 7. Oblicz metryki
            CoherenceMetrics metrics = calculateAdvancedCoherence(model, documents);
            double perplexity = calculatePerplexity(model);

            // 8. Aktualizuj rekord w bazie
            updateModelRecord(modelResult, modelPath, metrics, perplexity,
                    TopicModelingResult.ModelStatus.COMPLETED, null);

            log.info("Topic modeling zakończony pomyślnie. Model ID: {}", modelResult.getId());
            logMemoryUsage("Koniec");

            return buildResponse(modelResult);

        } catch (Exception e) {
            log.error("Błąd podczas topic modeling: {}", e.getMessage(), e);
            updateModelRecord(modelResult, null, null, null,
                    TopicModelingResult.ModelStatus.FAILED, e.getMessage());
            throw new RuntimeException("Topic modeling failed", e);
        }
    }

    /**
     * Oblicza zaawansowane metryki koherencji
     */
    private CoherenceMetrics calculateAdvancedCoherence(
            ParallelTopicModel model, List<Document> documents) {

        // Pobierz top słowa z najlepszego tematu jako przykład
        Alphabet alphabet = model.getAlphabet();
        TreeSet<IDSorter> sortedWords = model.getSortedWords().get(0);

        List<String> topWords = sortedWords.stream()
                .limit(10)
                .map(idSorter -> (String) alphabet.lookupObject(idSorter.getID()))
                .collect(Collectors.toList());

        List<String> documentTexts = documents.stream()
                .map(Document::getText)
                .collect(Collectors.toList());

        return coherenceCalculator.calculateAllMetrics(topWords, documentTexts);
    }

    private Map<String, List<ProcessedTweet>> poolTweetsWithSpamFilter(
            List<ProcessedTweet> tweets, String strategy) {

        Map<String, List<ProcessedTweet>> groupedTweets = switch (strategy.toLowerCase()) {
            case "hashtag" -> hashtagPoolingStrategy.poolTweets(tweets);
            case "temporal" -> temporalPoolingStrategy.poolTweets(tweets);
            default -> {
                log.warn("Nieznana strategia pooling: {}, używam hashtag", strategy);
                yield hashtagPoolingStrategy.poolTweets(tweets);
            }
        };

        // FILTROWANIE SPAMU: Usuń grupy z spam keywords
        Map<String, List<ProcessedTweet>> filteredGroups = groupedTweets.entrySet().stream()
                .filter(entry -> !isSpamGroup(entry.getKey()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue
                ));

        log.info("Filtrowanie spamu: {} → {} grup",
                groupedTweets.size(), filteredGroups.size());

        return filteredGroups;
    }

    private boolean isSpamGroup(String groupKey) {
        String lowerKey = groupKey.toLowerCase();
        return SPAM_KEYWORDS.stream().anyMatch(lowerKey::contains);
    }

    private List<Document> prepareDocumentsOptimized(
            Map<String, List<ProcessedTweet>> groupedTweets, Integer minDocumentSize) {

        int minSize = minDocumentSize != null ? minDocumentSize : 10; // Domyślnie 10

        List<Document> documents = groupedTweets.entrySet().stream()
                .filter(entry -> entry.getValue().size() >= minSize)
                .map(entry -> createDocumentOptimized(entry.getKey(), entry.getValue()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        log.info("Utworzono {} dokumentów z minimalnym rozmiarem {}", documents.size(), minSize);
        return documents;
    }

    private Document createDocumentOptimized(String documentId, List<ProcessedTweet> tweets) {
        try {
            StringBuilder textBuilder = new StringBuilder();
            List<Long> tweetIds = new ArrayList<>();

            for (ProcessedTweet tweet : tweets) {
                String tokens = extractTokensAsText(tweet);
                if (!tokens.isEmpty()) {
                    textBuilder.append(tokens).append(" ");
                    tweetIds.add(tweet.getOriginalTweet().getId());
                }
            }

            String combinedText = textBuilder.toString().trim();
            if (combinedText.length() < 50) { // Filtruj bardzo krótkie dokumenty
                return null;
            }

            return new Document(documentId, combinedText, tweetIds, tweets.size());

        } catch (Exception e) {
            log.warn("Błąd podczas tworzenia dokumentu {}: {}", documentId, e.getMessage());
            return null;
        }
    }

    private ParallelTopicModel trainLDAModelOptimized(List<Document> documents, TopicModelingRequest request)
            throws IOException {
        log.info("Trenuję zoptymalizowany model LDA na {} dokumentach", documents.size());

        ArrayList<Pipe> pipeList = new ArrayList<>();
        pipeList.add(new CharSequenceLowercase());
        pipeList.add(new CharSequence2TokenSequence(Pattern.compile("\\S+")));
        pipeList.add(new TokenSequenceRemoveStopwords(false, false));
        pipeList.add(new TokenSequence2FeatureSequence());

        Pipe pipe = new SerialPipes(pipeList);

        // Utwórz instancje
        InstanceList instances = new InstanceList(pipe);

        String[] documentsArray = documents.stream()
                .map(Document::getText)
                .toArray(String[]::new);

        instances.addThruPipe(new StringArrayIterator(documentsArray));

        ParallelTopicModel model = new ParallelTopicModel(request.getNumberOfTopics());
        model.addInstances(instances);
        model.setNumThreads(Math.min(numThreads, 2)); // Max 2 wątki

        int iterations = Math.min(
                request.getMaxIterations() != null ? request.getMaxIterations() : defaultIterations,
                1000  // Max 1000 iteracji
        );
        model.setNumIterations(iterations);

        // Parametry alpha i beta (bezpośrednie ustawienie pól)
        double alphaSum = 5.0;  // Zmniejszone z 50.0
        double beta = 0.01;     // Zwiększone z 0.01

        model.alphaSum = alphaSum;
        model.beta = beta;
        model.betaSum = beta * instances.getDataAlphabet().size();

        // Zaktualizuj tablicę alpha
        for (int i = 0; i < model.numTopics; i++) {
            model.alpha[i] = alphaSum / model.numTopics;
        }

        log.info("Parametry modelu: tematów={}, iteracji={}, wątków={}, alpha={}, beta={}",
                model.numTopics, iterations, model.getNumTopics(), alphaSum, beta);

        // Trenuj model
        model.estimate();

        log.info("Model LDA wytrenowany pomyślnie");
        return model;
    }

    private void extractAndSaveResults(ParallelTopicModel model, TopicModelingResult modelResult,
                                       List<Document> documents, Map<String, List<ProcessedTweet>> groupedTweets) {
        log.info("Wyciągam i zapisuję wyniki modelu");

        // 1. Zapisz wyniki tematów
        saveTopicResults(model, modelResult);

        // 2. Zapisz przypisania dokumentów
        saveDocumentAssignments(model, modelResult, documents);

        // 3. NAPRAWIONE: Aktualizuj statystyki tematów
        updateTopicStatistics(modelResult);

        // 4. Aktualizuj liczniki w głównym rekordzie
        modelResult.setDocumentsCount(documents.size());
        modelResult.setOriginalTweetsCount(
                documents.stream().mapToInt(Document::getTweetsCount).sum()
        );

        topicModelingResultRepository.save(modelResult);
        log.info("Zapisano wyniki modelu z {} dokumentami", documents.size());
    }

    private void updateTopicStatistics(TopicModelingResult modelResult) {
        log.info("Aktualizuję statystyki tematów dla modelu {}", modelResult.getId());

        List<TopicResult> topicResults = topicResultRepository
                .findByTopicModelingResultIdOrderByTopicId(modelResult.getId());

        for (TopicResult topicResult : topicResults) {
            try {
                // Policz dokumenty dla tego tematu
                Long documentCount = documentTopicAssignmentRepository
                        .countDocumentsByTopic(topicResult.getTopicId(), modelResult.getId());

                // Oblicz średnie prawdopodobieństwo (fallback w Javie)
                Double averageProbability = calculateAverageTopicProbabilityJava(
                        topicResult.getTopicId(), modelResult.getId());

                // Aktualizuj statystyki
                topicResult.setDocumentCount(documentCount != null ? documentCount.intValue() : 0);
                topicResult.setAverageProbability(averageProbability != null ? averageProbability : 0.0);

                topicResultRepository.save(topicResult);

                log.debug("Zaktualizowano temat {}: {} dokumentów, prawdopodobieństwo: {:.3f}",
                        topicResult.getTopicId(), documentCount, averageProbability);

            } catch (Exception e) {
                log.error("Błąd podczas aktualizacji statystyk tematu {}: {}",
                        topicResult.getTopicId(), e.getMessage());
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

    private double calculatePerplexity(ParallelTopicModel model) {
        try {
            double logLikelihood = model.modelLogLikelihood();
            int totalTokens = model.totalTokens;

            if (totalTokens == 0) {
                log.warn("Total tokens is 0, cannot calculate perplexity");
                return Double.NaN;
            }

            double perplexity = Math.exp(-logLikelihood / totalTokens);

            log.debug("Obliczono perplexity: {:.2f} (logLikelihood: {:.2f}, totalTokens: {})",
                    perplexity, logLikelihood, totalTokens);

            return perplexity;

        } catch (Exception e) {
            log.error("Błąd podczas obliczania perplexity: {}", e.getMessage());
            return Double.NaN;
        }
    }

    private void logMemoryUsage(String phase) {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory() / (1024 * 1024); // MB
        long freeMemory = runtime.freeMemory() / (1024 * 1024);   // MB
        long usedMemory = totalMemory - freeMemory;

        log.info("Pamięć [{}]: Użyte={}MB, Całkowite={}MB, Wolne={}MB",
                phase, usedMemory, totalMemory, freeMemory);
    }

    public List<TopicModelingResponse> getAvailableModels() {
        return topicModelingResultRepository.findByStatusOrderByTrainingDateDesc(
                        TopicModelingResult.ModelStatus.COMPLETED)
                .stream()
                .map(this::buildResponse)
                .collect(Collectors.toList());
    }

    public TopicModelingResponse getModelDetails(Long modelId) {
        TopicModelingResult model = topicModelingResultRepository.findById(modelId)
                .orElseThrow(() -> new RuntimeException("Model not found: " + modelId));
        return buildResponse(model);
    }

    private TopicModelingResult createModelRecord(TopicModelingRequest request) {
        TopicModelingResult model = TopicModelingResult.builder()
                .modelName(request.getModelName() != null ? request.getModelName() :
                        generateModelName(request))
                .numberOfTopics(request.getNumberOfTopics())
                .poolingStrategy(request.getPoolingStrategy())
                .documentsCount(0)
                .originalTweetsCount(0)
                .trainingDate(LocalDateTime.now())
                .status(TopicModelingResult.ModelStatus.TRAINING)
                .build();

        return topicModelingResultRepository.save(model);
    }

    private List<ProcessedTweet> getProcessedTweets() {
        log.info("Pobieranie wszystkich przetworzonych tweetów (bez filtrów)");
        return processedTweetRepository.findAll();
    }

    private Map<String, List<ProcessedTweet>> poolTweets(List<ProcessedTweet> tweets, String strategy) {
        return poolTweetsWithSpamFilter(tweets, strategy);
    }

    private List<Document> prepareDocuments(Map<String, List<ProcessedTweet>> groupedTweets,
                                            Integer minDocumentSize) {
        return prepareDocumentsOptimized(groupedTweets, minDocumentSize);
    }

    private String extractTokensAsText(ProcessedTweet tweet) {
        try {
            List<String> tokens = objectMapper.readValue(tweet.getTokens(),
                    new TypeReference<List<String>>() {});
            return String.join(" ", tokens);
        } catch (Exception e) {
            log.warn("Błąd podczas parsowania tokenów dla tweeta {}: {}",
                    tweet.getId(), e.getMessage());
            return "";
        }
    }

    private String saveModel(ParallelTopicModel model, String modelName) throws IOException {
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

            List<TopicModelingResponse.WordWeight> topWords = new ArrayList<>();
            int wordCount = 0;

            for (IDSorter idSorter : sortedWords) {
                if (wordCount >= 20) break;

                String word = (String) alphabet.lookupObject(idSorter.getID());
                double weight = idSorter.getWeight();

                topWords.add(TopicModelingResponse.WordWeight.builder()
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

    private void saveDocumentAssignments(ParallelTopicModel model, TopicModelingResult modelResult,
                                         List<Document> documents) {
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
                        .documentId(document.getId())
                        .documentType(determineDocumentType(document.getId()))
                        .dominantTopicId(dominantTopicId)
                        .topicProbabilities(objectMapper.writeValueAsString(probabilities))
                        .tweetIds(objectMapper.writeValueAsString(document.getTweetIds()))
                        .tweetsCount(document.getTweetsCount())
                        .build();

                documentTopicAssignmentRepository.save(assignment);

            } catch (Exception e) {
                log.error("Błąd podczas zapisywania przypisania dokumentu {}: {}",
                        document.getId(), e.getMessage());
            }
        }
    }

    private String generateTopicLabel(List<TopicModelingResponse.WordWeight> topWords) {
        if (topWords.isEmpty()) return "Empty Topic";

        return topWords.stream()
                .limit(3)
                .map(TopicModelingResponse.WordWeight::getWord)
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
                LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")));
    }

    private void updateModelRecord(TopicModelingResult model, String modelPath,
                                   CoherenceMetrics coherenceMetrics, Double perplexity,
                                   TopicModelingResult.ModelStatus status, String errorMessage) {
        model.setModelPath(modelPath);
        model.setPmi(coherenceMetrics.getPmi());
        model.setNpmi(coherenceMetrics.getNpmi());
        model.setUci(coherenceMetrics.getUci());
        model.setUmass(coherenceMetrics.getUmass());
        model.setCoherenceInterpretation(coherenceMetrics.getPmiInterpretation());
        model.setUmassInterpretation(coherenceMetrics.getUmassInterpretation());
        model.setPerplexity(perplexity);
        model.setStatus(status);
        model.setErrorMessage(errorMessage);

        topicModelingResultRepository.save(model);
    }

    private TopicModelingResponse buildResponse(TopicModelingResult model) {
        List<TopicResult> topicResults = topicResultRepository
                .findByTopicModelingResultIdOrderByTopicId(model.getId());

        List<TopicModelingResponse.TopicSummary> topics = topicResults.stream()
                .map(this::buildTopicSummary)
                .collect(Collectors.toList());

        return TopicModelingResponse.builder()
                .modelId(model.getId())
                .modelName(model.getModelName())
                .numberOfTopics(model.getNumberOfTopics())
                .poolingStrategy(model.getPoolingStrategy())
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

    private TopicModelingResponse.TopicSummary buildTopicSummary(TopicResult topicResult) {
        List<TopicModelingResponse.WordWeight> topWords = new ArrayList<>();

        try {
            topWords = objectMapper.readValue(topicResult.getTopWords(),
                    new TypeReference<List<TopicModelingResponse.WordWeight>>() {});
        } catch (Exception e) {
            log.warn("Błąd podczas parsowania top words dla tematu {}: {}",
                    topicResult.getTopicId(), e.getMessage());
        }

        return TopicModelingResponse.TopicSummary.builder()
                .topicId(topicResult.getTopicId())
                .topicLabel(topicResult.getTopicLabel())
                .topWords(topWords)
                .documentCount(topicResult.getDocumentCount())
                .averageProbability(topicResult.getAverageProbability())
                .build();
    }


    private static class Document {
        private final String id;
        private final String text;
        private final List<Long> tweetIds;
        private final int tweetsCount;

        public Document(String id, String text, List<Long> tweetIds, int tweetsCount) {
            this.id = id;
            this.text = text;
            this.tweetIds = tweetIds;
            this.tweetsCount = tweetsCount;
        }

        public String getId() { return id; }
        public String getText() { return text; }
        public List<Long> getTweetIds() { return tweetIds; }
        public int getTweetsCount() { return tweetsCount; }
    }
}
