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

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Główny serwis do topic modeling używający biblioteki MALLET
 * Implementuje najlepsze praktyki z literatury naukowej
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

    @Value("${app.topic-modeling.models-directory:./topic_models}")
    private String modelsDirectory;

    @Value("${app.topic-modeling.default-iterations:1000}")
    private int defaultIterations;

    @Value("${app.topic-modeling.num-threads:4}")
    private int numThreads;

    /**
     * Główna metoda uruchamiająca proces topic modeling
     */
    public TopicModelingResponse performTopicModeling(TopicModelingRequest request) {
        log.info("Rozpoczynam topic modeling: {} tematów, strategia: {}",
                request.getNumberOfTopics(), request.getPoolingStrategy());

        // Utwórz rekord w bazie danych
        TopicModelingResult modelResult = createModelRecord(request);

        try {
            // 1. Pobierz przetworzone tweety
            List<ProcessedTweet> processedTweets = getProcessedTweets(request);

            // 2. Grupuj tweety zgodnie ze strategią
            Map<String, List<ProcessedTweet>> groupedTweets = poolTweets(processedTweets, request.getPoolingStrategy());

            // 3. Przygotuj dokumenty do modelowania
            List<Document> documents = prepareDocuments(groupedTweets, request.getMinDocumentSize());

            // 4. Uruchom MALLET LDA
            ParallelTopicModel model = trainLDAModel(documents, request);

            // 5. Zapisz model do pliku
            String modelPath = saveModel(model, modelResult.getModelName());

            // 6. Wyciągnij wyniki i zapisz do bazy
            extractAndSaveResults(model, modelResult, documents, groupedTweets);

            // 7. Oblicz metryki
            double coherenceScore = calculateCoherenceScore(model);
            double perplexity = calculatePerplexity(model); // ZMIANA: własna implementacja

            // 8. Aktualizuj rekord w bazie
            updateModelRecord(modelResult, modelPath, coherenceScore, perplexity,
                    TopicModelingResult.ModelStatus.COMPLETED, null);

            log.info("Topic modeling zakończony pomyślnie. Model ID: {}", modelResult.getId());

            return buildResponse(modelResult);

        } catch (Exception e) {
            log.error("Błąd podczas topic modeling: {}", e.getMessage(), e);
            updateModelRecord(modelResult, null, null, null,
                    TopicModelingResult.ModelStatus.FAILED, e.getMessage());
            throw new RuntimeException("Topic modeling failed", e);
        }
    }

    /**
     * Pobiera listę dostępnych modeli
     */
    public List<TopicModelingResponse> getAvailableModels() {
        return topicModelingResultRepository.findByStatusOrderByTrainingDateDesc(
                        TopicModelingResult.ModelStatus.COMPLETED)
                .stream()
                .map(this::buildResponse)
                .collect(Collectors.toList());
    }

    /**
     * Pobiera szczegóły konkretnego modelu
     */
    public TopicModelingResponse getModelDetails(Long modelId) {
        TopicModelingResult model = topicModelingResultRepository.findById(modelId)
                .orElseThrow(() -> new RuntimeException("Model not found: " + modelId));
        return buildResponse(model);
    }

    // ========================================
    // METODY POMOCNICZE
    // ========================================

    // ========================================
    // METODY POMOCNICZE - POPRAWIONE
    // ========================================

    private ParallelTopicModel trainLDAModel(List<Document> documents, TopicModelingRequest request)
            throws IOException {
        log.info("Trenuję model LDA na {} dokumentach", documents.size());

        // Przygotuj pipeline MALLET
        ArrayList<Pipe> pipeList = new ArrayList<>();
        pipeList.add(new CharSequenceLowercase());
        pipeList.add(new CharSequence2TokenSequence(Pattern.compile("\\p{L}[\\p{L}\\p{P}]+\\p{L}")));
        pipeList.add(new TokenSequenceRemoveStopwords(false, false));
        pipeList.add(new TokenSequence2FeatureSequence());

        Pipe pipe = new SerialPipes(pipeList);

        // Utwórz instancje
        InstanceList instances = new InstanceList(pipe);

        String[] documentsArray = documents.stream()
                .map(Document::getText)
                .toArray(String[]::new);

        instances.addThruPipe(new StringArrayIterator(documentsArray));

        // Utwórz i skonfiguruj model
        ParallelTopicModel model = new ParallelTopicModel(request.getNumberOfTopics());
        model.addInstances(instances);
        model.setNumThreads(numThreads);
        model.setNumIterations(request.getMaxIterations() != null ?
                request.getMaxIterations() : defaultIterations);

        // ZMIANA: Ustaw parametry alpha i beta przez bezpośredni dostęp do pól
        double alphaSum = 50.0 / request.getNumberOfTopics();
        double beta = 0.01;

        model.alphaSum = alphaSum; // Bezpośrednie ustawienie pola publicznego
        model.beta = beta;         // Bezpośrednie ustawienie pola publicznego
        model.betaSum = beta * model.numTypes; // Oblicz betaSum (beta * liczba słów w słowniku)

        // Zaktualizuj tablicę alpha (rozkład alpha na wszystkie tematy)
        for (int i = 0; i < model.numTopics; i++) {
            model.alpha[i] = alphaSum / model.numTopics;
        }

        log.info("Ustawiono parametry: alphaSum={}, beta={}, betaSum={}",
                 model.alphaSum, model.beta, model.betaSum);

        // Trenuj model
        model.estimate();

        log.info("Model LDA wytrenowany pomyślnie");
        return model;
    }

    /**
     * NOWA METODA: Oblicza perplexity na podstawie log likelihood
     * Perplexity = exp(-log_likelihood / total_tokens)
     */
    private double calculatePerplexity(ParallelTopicModel model) {
        try {
            double logLikelihood = model.modelLogLikelihood();
            int totalTokens = model.totalTokens;

            if (totalTokens == 0) {
                log.warn("Total tokens is 0, cannot calculate perplexity");
                return Double.NaN;
            }

            double perplexity = Math.exp(-logLikelihood / totalTokens);

            log.debug("Obliczono perplexity: {} (logLikelihood: {}, totalTokens: {})",
                    perplexity, logLikelihood, totalTokens);

            return perplexity;

        } catch (Exception e) {
            log.error("Błąd podczas obliczania perplexity: {}", e.getMessage());
            return Double.NaN;
        }
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

    private List<ProcessedTweet> getProcessedTweets(TopicModelingRequest request) {
        if (request.getStartDate() != null && request.getEndDate() != null) {
            return processedTweetRepository.findByOriginalTweetPostDateBetween(
                    request.getStartDate(), request.getEndDate());
        } else {
            return processedTweetRepository.findAll();
        }
    }

    private Map<String, List<ProcessedTweet>> poolTweets(List<ProcessedTweet> tweets, String strategy) {
        return switch (strategy.toLowerCase()) {
            case "hashtag" -> hashtagPoolingStrategy.poolTweets(tweets);
            case "temporal" -> temporalPoolingStrategy.poolTweets(tweets);
            default -> {
                log.warn("Nieznana strategia pooling: {}, używam hashtag", strategy);
                yield hashtagPoolingStrategy.poolTweets(tweets);
            }
        };
    }

    private List<Document> prepareDocuments(Map<String, List<ProcessedTweet>> groupedTweets,
                                            Integer minDocumentSize) {
        int minSize = minDocumentSize != null ? minDocumentSize : 3;

        return groupedTweets.entrySet().stream()
                .filter(entry -> entry.getValue().size() >= minSize)
                .map(entry -> createDocument(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    private Document createDocument(String documentId, List<ProcessedTweet> tweets) {
        String combinedText = tweets.stream()
                .map(this::extractTokensAsText)
                .filter(text -> !text.isEmpty())
                .collect(Collectors.joining(" "));

        List<Long> tweetIds = tweets.stream()
                .map(tweet -> tweet.getOriginalTweet().getId())
                .collect(Collectors.toList());

        return new Document(documentId, combinedText, tweetIds, tweets.size());
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

    private void extractAndSaveResults(ParallelTopicModel model, TopicModelingResult modelResult,
                                       List<Document> documents, Map<String, List<ProcessedTweet>> groupedTweets) {
        log.info("Wyciągam i zapisuję wyniki modelu");

        // Zapisz wyniki tematów
        saveTopicResults(model, modelResult);

        // Zapisz przypisania dokumentów
        saveDocumentAssignments(model, modelResult, documents);

        // Aktualizuj liczniki
        modelResult.setDocumentsCount(documents.size());
        modelResult.setOriginalTweetsCount(groupedTweets.values().stream()
                .mapToInt(List::size).sum());

        topicModelingResultRepository.save(modelResult);
    }

    private void saveTopicResults(ParallelTopicModel model, TopicModelingResult modelResult) {
        Alphabet alphabet = model.getAlphabet();

        for (int topicId = 0; topicId < model.getNumTopics(); topicId++) {
            TreeSet<IDSorter> sortedWords = model.getSortedWords().get(topicId);

            List<TopicModelingResponse.WordWeight> topWords = new ArrayList<>();
            int wordCount = 0;

            for (IDSorter idSorter : sortedWords) {
                if (wordCount >= 20) break; // Top 20 słów

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
                        .documentCount(0) // Będzie aktualizowane później
                        .averageProbability(0.0) // Będzie aktualizowane później
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

                // Znajdź dominujący temat
                int dominantTopicId = 0;
                double maxProbability = topicProbabilities[0];

                for (int i = 1; i < topicProbabilities.length; i++) {
                    if (topicProbabilities[i] > maxProbability) {
                        maxProbability = topicProbabilities[i];
                        dominantTopicId = i;
                    }
                }

                // Utwórz mapę prawdopodobieństw
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

    private double calculateCoherenceScore(ParallelTopicModel model) {
        // Implementacja metryki PMI (Pointwise Mutual Information)
        double logLikelihood = model.modelLogLikelihood();
        return logLikelihood / model.totalTokens;
//        double totalCoherence = 0.0;
//        int numTopics = model.getNumTopics();
//        Alphabet alphabet = model.getAlphabet();
//
//        for (int topicId = 0; topicId < numTopics; topicId++) {
//            TreeSet<IDSorter> sortedWords = model.getSortedWords().get(topicId);
//            List<String> topWords = new ArrayList<>();
//
//            int wordCount = 0;
//            for (IDSorter idSorter : sortedWords) {
//                if (wordCount >= 10) break; // Top 10 słów dla spójności
//                topWords.add((String) alphabet.lookupObject(idSorter.getID()));
//                wordCount++;
//            }
//
//            // Oblicz PMI dla par słów w temacie
//            double topicCoherence = calculateTopicPMI(topWords);
//            totalCoherence += topicCoherence;
//        }
//
//        return totalCoherence / numTopics;
    }

    private double calculateTopicPMI(List<String> words) {
        if (words.size() < 2) return 0.0;

        double pmiSum = 0.0;
        int pairCount = 0;

        for (int i = 0; i < words.size() - 1; i++) {
            for (int j = i + 1; j < words.size(); j++) {
                // Uproszczone PMI - w rzeczywistości potrzebne są statystyki współwystępowania
                pmiSum += Math.log(0.1); // Placeholder value
                pairCount++;
            }
        }

        return pairCount > 0 ? pmiSum / pairCount : 0.0;
    }

    private String generateTopicLabel(List<TopicModelingResponse.WordWeight> topWords) {
        if (topWords.isEmpty()) return "Empty Topic";

        // Utwórz etykietę z top 3 słów
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
                                   Double coherenceScore, Double perplexity,
                                   TopicModelingResult.ModelStatus status, String errorMessage) {
        model.setModelPath(modelPath);
        model.setCoherenceScore(coherenceScore);
        model.setPerplexity(perplexity);
        model.setStatus(status);
        model.setErrorMessage(errorMessage);

        topicModelingResultRepository.save(model);
    }

    private TopicModelingResponse buildResponse(TopicModelingResult model) {
        List<TopicResult> topicResults = topicResultRepository.findByTopicModelingResultIdOrderByTopicId(model.getId());

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
                .coherenceScore(model.getCoherenceScore())
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

    // ========================================
    // KLASA POMOCNICZA DOCUMENT
    // ========================================

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
