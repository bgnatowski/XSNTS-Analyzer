# XSNTS-Analyzer by Bartosz Gnatowski

### PL ([English version below > click me](#en))

XSNTS-Analyzer jest aplikacją w ramach projektu do pracy magisterskiej na Uniwersytecie Ekonomicznym w Krakowie.

**Tytuł:**  
* PL: *Implementacja aplikacji do analizy danych z platformy X.com: analiza tematyki i sentymentu w badaniu opinii publicznej*
* EN: *Design and Implementation of an X.com Data-Analysis Application: Topic Modelling and Sentiment Analysis for Public-Opinion Research*

**Autor:** Bartosz Gnatowski\
**Promotor pracy:** dr hab. inż. Janusz Morajda\
**Kolegium:** Kolegium Nauk o Zarządzaniu i Jakości\
**Instytut:** Instytut Informatyki, Rachunkowości i Controllingu\
**Kierunek:** Informatyka Stosowana\
**Specjalność:** Systemy inteligentne



## Spis treści
1. [Opis projektu](#opis-projektu)
2. [Najważniejsze funkcjonalności](#najwa%C5%BCniejsze-funkcjonalno%C5%9Bci)
3. [Stos technologiczny](#stos-technologiczny)
4. [Wymagania wstępne](#wymagania-wst%C4%99pne)
5. [Szybki start](#szybki-start)
    * [Uruchomienie w IDE](#uruchomienie-w-ide)
    * [Uruchomienie przez Docker Compose](#uruchomienie-przez-docker-compose)
6. [REST API](#rest-api)
7. [Struktura katalogów](#struktura-katalog%C3%B3w)
8. [Przykładowe scenariusze użycia](#przyk%C5%82adowe-scenariusze-u%C5%BCycia)
9. [Plany rozwoju](#plany-rozwoju)
10. [Licencja](#licencja)

## Opis projektu

XSNTS-Analyzer to mikrousługa **back-end** do pozyskiwania, oczyszczania i analizy treści publikowanych na platformie **X.com** (dawniej Twitter).
Celem aplikacji jest:
* zaplanowane, automatyczne zbieranie tweetów i zapis do bazy danych (selenium + ADSPower Global)
* selekcja i normalizacja danych w języku polskim 
* automatyczne **zgrupowanie** tweetów w dokumenty (np. hashtagiem lub oknem czasowym),
* wytrenowanie modeli **LDA (MALLET)** w celu identyfikacji kluczowych tematów,
* przypisanie **sentymentu** do tweetów (prosty słownik PL lub model językowy),
* eksport wyników do plików **CSV** celem dalszej eksploracji w narzędziach statystycznych.

## Najważniejsze funkcjonalności

| Moduł | Opis                                                                                            |
| :-- |:------------------------------------------------------------------------------------------------|
| **Scraper** | Pobiera publiczne tweety (selenium + ADSPower Global)                                           |
| **Normalization** | normalizacja, anonimizacja @mention, tokenizacja, lematyzacja PL                                |
| **Topic modeling** | MALLET ParallelTopicModel (LDA, α/β optymalizowane co 10 iteracji)                              |
| **Sentiment** | regułowy słownik PL (SlownikWydzwieku) lub model `tabularisai/multilingual-sentiment-analysis`. |
| **Export** | Eksport danych do CSV → `output/csv/…`                                                          |
| **REST API** | Endpointy do sterowania całym pipeline’em.                                                      |

## Stos technologiczny
| Warstwa | Biblioteki / Wersje                                                                                                                  |
|---------|--------------------------------------------------------------------------------------------------------------------------------------|
| Język  | **Java 17**                                                                                                                          |
| Framework | Spring Boot 3.4.1 (`starter-web`, `starter-data-jpa`, `starter-webflux`)                                                             |
| Baza | PostgreSQL 15 (kontener dockera)                                                                                                     |
| ORM  | Hibernate 6 / Spring Data JPA                                                                                                        |
| Scraping | Selenium 4.27 + JSoup 1.16 + Apache HttpClient 4.5                                                                                   |
| NLP  | MALLET 2.0.8, Morfologik 2.1.9, Lingua 1.2.2, serwis pythonowy na dockerze z modelem: `tabularisai/multilingual-sentiment-analysis`. |
| Statystyka | Apache Commons-Math3 3.6.1                                                                                                           |
| Inne | Lombok 1.18.36, MapStruct 1.5.5                                                                                                      |
| Build | **Maven** + `spring-boot-maven-plugin`                                                                                               |
| Conteneryzacja | Docker / Docker Compose                                                                                                              |

W POM znajdują się również biblioteki **Kafka** – obecnie **nieaktywne** (placeholder pod przyszły streaming tweetów).

## Wymagania wstępne

* **JDK 17+**
* **Docker 20.10+**
* min. 4 GB RAM
## Szybki start

### Uruchomienie w IDE 
* np IntellijIDEA Community

### Uruchomienie przez Docker Compose

```bash
git clone <repo>
cd xsnts-analyzer
docker compose up -d --build
# Aplikacja będzie dostępna na http://localhost:8080
```

## REST API

```
GET    /api/export/processed
GET    /api/export/sentiment
GET    /api/export/topic-results/{modelId}
GET    /api/export/topic-sentiment/{modelId}

DELETE /api/processing/cleanup-empty
GET    /api/processing/empty-count
GET    /api/processing/empty-records
POST   /api/processing/process-all
GET    /api/processing/stats

POST   /api/sentiment/analyze-all
DELETE /api/sentiment

POST   /api/topic-modeling/lda/train
GET    /api/topic-modeling/models
GET    /api/topic-modeling/models/{modelId}

```

_Pełna dokumentacja Swagger dostępna pod `/swagger-ui.html`._ (TBD)

## Struktura katalogów

```
xsnts-analyzer/
└── src/main/java/pl/bgnat/master/xsnts
    ├── config/ # globalny konfig
    ├── exporter/ # eksport CSV
    ├── kafka/ # configuracja kafki
    ├── scrapper/ # moduł scrappera
    ├── normalization/ # moduł normalizacji
    ├── sentiment/ # moduł analizy sentymentu
    └──topicmodeling/ # moduł analizy tematycznej
├── sentiment-hf # aplikacja wystawiająca endpoint do obsługi modelu językowego analizy sentymentu
├── docker-compose.yml
├── .gitignore
├── LICENSE
├── THIRD_PARTY_LICENSES
├── NOTICE
├── README.md
└── pom.xml

```

## Przykładowe scenariusze użycia

1. **Pobierz i przetwórz wszystkie tweety**

```
POST /api/processing/process-all
```

2. **Wytrenuj model LDA (20 tematów, lematyzacja, bez mentions)**

```POST /api/topic-modeling/lda/train```

``` json
{
  "tokenStrategy": "lemmatized",
  "topicModel": "LDA",
  "isUseBigrams": false,
  "numberOfTopics": 10,
  "poolingStrategy": "hashtag",
  "minDocumentSize": 10,
  "maxIterations": 3000,
  "modelName": "LDA_lemmatized_hashtag_v1_2025",
  "startDate": "2025-01-01T00:00:00",
  "endDate": "2025-12-31T23:59:59",
  "skipMentions": true
}
```

3. **Przypisz każdemu tweetowi sentyment**

```
POST /api/sentiment/analyze-all
```
``` json
{
    "tokenStrategy": "LEMMATIZED",
    "sentimentModelStrategy": "STANDARD"
} 
```

4. **Zbierz dane model tematycznej-sentyment**
```GET /api/sentiment/{{modelId}}/stats```

5. **Wyekportuj dane**
```GET /api/export/topic-sentiment/{{modelId}}```

## Plany rozwoju

* integracja **Kafka Streams** → automatyczne update tweetów
* frontend

## Licencja

Kod źródłowy udostępniony na licencji **MIT**.
Nazwy, logotypy i znaki towarowe platformy **X.com** są własnością odpowiednich właścicieli.

### EN

XSNTS-Analyzer is an application developed for a master’s thesis at the Cracow University of Economics.

**Title:**
* EN: *Implementation of an Application for X.com Data Analysis: Topic Modeling and Sentiment Analysis in Public Opinion Research*
* PL (org): *Implementacja aplikacji do analizy danych z platformy X.com: analiza tematyki i sentymentu w badaniu opinii publicznej*

**Author:** Bartosz Gnatowski\
**Thesis Advisor:** Dr Hab. Inż. Janusz Morajda\
**College:** College of Management and Quality Sciences
**Institute:** Institute of Informatics, Accounting and Controlling\
**Field of Study:** Applied Informatics\
**Specialization:** Intelligent Systems

## Table of Contents

1. [Project Overview](#project-overview)
2. [Key Features](#key-features)
3. [Technology Stack](#technology-stack)
4. [Prerequisites](#prerequisites)
5. [Quick Start](#quick-start)
    * [Running in an IDE](#running-in-an-ide)
    * [Running with Docker Compose](#running-with-docker-compose)
6. [REST API](#rest-api)
7. [Directory Structure](#directory-structure)
8. [Sample Use-Cases](#sample-use-cases)
9. [Roadmap](#roadmap)
10. [License](#license)

## Project Overview

XSNTS-Analyzer is a **back-end microservice** for scrapping, cleansing and analysing content published on **X.com** (formerly Twitter).

Main goals:

* Scheduled, automated harvesting of tweets and database storage (Selenium + ADSPower Global).
* Selection and normalization of Polish-language data.
* Automatic **grouping** of tweets into documents (e.g., by hashtag or time window).
* Training **LDA models (MALLET)** to identify key topics.
* Assigning **sentiment** to tweets (simple Polish lexicon or language model).
* Exporting results to **CSV** for further exploration in statistical tools.


## Key Features

| Module | Description |
| :-- | :-- |
| **Scraper** | Fetches public tweets (Selenium + ADSPower Global). |
| **Normalization** | Normalization, @mention anonymization, tokenization, Polish lemmatization. |
| **Topic Modeling** | MALLET `ParallelTopicModel` (LDA, α/β optimized every 10 iterations). |
| **Sentiment** | Rule-based Polish lexicon (SlownikWydzwieku) or model `tabularisai/multilingual-sentiment-analysis`. |
| **Export** | Data export to CSV → `output/csv/…`. |
| **REST API** | Endpoints controlling the entire pipeline. |

## Technology Stack

| Layer | Libraries / Versions |
| :-- | :-- |
| Language | **Java 17** |
| Framework | Spring Boot 3.4.1 (`starter-web`, `starter-data-jpa`, `starter-webflux`) |
| Database | PostgreSQL 15 (Docker container) |
| ORM | Hibernate 6 / Spring Data JPA |
| Scraping | Selenium 4.27, JSoup 1.16, Apache HttpClient 4.5 |
| NLP | MALLET 2.0.8, Morfologik 2.1.9, Lingua 1.2.2, Python service with model `tabularisai/multilingual-sentiment-analysis` |
| Statistics | Apache Commons-Math3 3.6.1 |
| Other | Lombok 1.18.36, MapStruct 1.5.5 |
| Build | **Maven** + `spring-boot-maven-plugin` |
| Containerisation | Docker / Docker Compose |

Kafka libraries are present in the **POM** but currently **inactive** (placeholder for future tweet streaming).

## Prerequisites

* **JDK 17+**
* **Docker 20.10+**
* At least 4 GB RAM


## Quick Start

### Running in an IDE

* e.g., IntelliJ IDEA Community


### Running with Docker Compose

```bash
git clone <repo>
cd xsnts-analyzer
docker compose up -d --build
# The application will be available at http://localhost:8080
```


## REST API

```
GET    /api/export/processed
GET    /api/export/sentiment
GET    /api/export/topic-results/{modelId}
GET    /api/export/topic-sentiment/{modelId}

DELETE /api/processing/cleanup-empty
GET    /api/processing/empty-count
GET    /api/processing/empty-records
POST   /api/processing/process-all
GET    /api/processing/stats

POST   /api/sentiment/analyze-all
DELETE /api/sentiment

POST   /api/topic-modeling/lda/train
GET    /api/topic-modeling/models
GET    /api/topic-modeling/models/{modelId}
```

*Full Swagger documentation will be available at `/swagger-ui.html` (TBD).*

## Directory Structure

```
xsnts-analyzer/
└── src/main/java/pl/bgnat/master/xsnts
    ├── config/           # global configuration
    ├── exporter/         # CSV export
    ├── kafka/            # Kafka config
    ├── scrapper/         # scraper module
    ├── normalization/    # normalization module
    ├── sentiment/        # sentiment analysis module
    └── topicmodeling/    # topic-model module
├── sentiment-hf          # Python service exposing sentiment model
├── docker-compose.yml
├── .gitignore
├── LICENSE
├── THIRD_PARTY_LICENSES
├── NOTICE
├── README.md
└── pom.xml
```


## Sample Use-Cases

1. **Fetch and process all tweets**
```
POST /api/processing/process-all
```

2. **Train an LDA model (10 topics, lemmatized, no mentions)**
```
POST /api/topic-modeling/lda/train
```

```json
{
  "tokenStrategy": "lemmatized",
  "topicModel": "LDA",
  "isUseBigrams": false,
  "numberOfTopics": 10,
  "poolingStrategy": "hashtag",
  "minDocumentSize": 10,
  "maxIterations": 3000,
  "modelName": "LDA_lemmatized_hashtag_v1_2025",
  "startDate": "2025-01-01T00:00:00",
  "endDate": "2025-12-31T23:59:59",
  "skipMentions": true
}
```

3. **Assign sentiment to every tweet**
```
POST /api/sentiment/analyze-all
```

```json
{
  "tokenStrategy": "LEMMATIZED",
  "sentimentModelStrategy": "STANDARD"
}
```

4. **Collect topic-sentiment data**
```
GET /api/sentiment/{{modelId}}/stats
```

5. **Export results**
```
GET /api/export/topic-sentiment/{{modelId}}
```


## Roadmap

* Integrate **Kafka Streams** → automated tweet updates
* Front-end UI


## License

Source code released under the **MIT License**.
All names, logos and trademarks of **X.com** belong to their respective owners.