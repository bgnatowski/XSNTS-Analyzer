package pl.bgnat.master.xscrapper.dto.topicmodeling;

import lombok.Builder;

import java.util.List;

/**
 * DTO Documentu dla topic modelingu
 */
@Builder
public record Document(String id, String text, List<Long> tweetIds, int tweetsCount) { }