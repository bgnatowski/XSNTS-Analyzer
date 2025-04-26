package pl.bgnat.master.xscrapper.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TweetDto(
        Long id,
        String username,
        String content,
        String link,
        Long likeCount,
        Long repostCount,
        Long commentCount,
        Long views,
        LocalDateTime postDate
) {
}
