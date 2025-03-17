package pl.bgnat.master.xscrapper.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties
@Entity(name = "Tweet")
@Table(name = "tweet")
public class Tweet {
    @Id
    @SequenceGenerator(
            name = "tweet_id_generator",
            sequenceName = "tweet_id_generator",
            allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "tweet_id_generator")
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "username", nullable = false)
    private String username;        // @nazwa_uzytkownika
    @Column(name = "content", length = 4000,  nullable = true)
    private String content;         // treść (tekst) tweetu
    @Column(name = "link", nullable = false)
    private String link;            // link do posta

    @Column(name = "like_count")
    private Long likeCount;
    @Column(name = "repost_count")
    private Long repostCount;
    @Column(name = "comment_count")
    private Long commentCount;

    @Column(name = "media_links", nullable = true)
    private String mediaLinks;      // linki do zdjęć/gifów/nagrań (opcjonalnie w formie JSON lub rozbijać w osobnej tabeli do przemyslenia)

    @Column(name = "post_date", nullable = false)
    private LocalDateTime postDate;
}
