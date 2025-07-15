package pl.bgnat.master.xscrapper.dto.scrapper;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;

import java.time.LocalDateTime;
/**
 * DTO Tweeta
 */
@Builder
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
