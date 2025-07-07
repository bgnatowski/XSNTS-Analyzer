package pl.bgnat.master.xscrapper.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Objects;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties
@Entity(name = "Tweet")
@Table(name = "tweet",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "tweet_link_constraint",
                        columnNames = "link"
                )
        })
public class Tweet {
    public static final Tweet POISON_PILL = new Tweet();
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
    @Column(name = "content", length = 4000)
    private String content;
    @Column(name = "link", nullable = false, unique = true)
    private String link;

    @Column(name = "like_count")
    private Long likeCount;
    @Column(name = "repost_count")
    private Long repostCount;
    @Column(name = "comment_count")
    private Long commentCount;
    @Column(name = "views")
    private Long views;

    @Column(name = "media_links")
    private String mediaLinks;      // linki do zdjęć/gifów/nagrań (opcjonalnie w formie JSON lub rozbijać w osobnej tabeli do tbd przemyslenia)

    @Column(name = "post_date", nullable = false)
    private LocalDateTime postDate;
    @Column(name = "creation_date", nullable = false)
    private LocalDateTime creationDate;
    @Column(name = "update_date", nullable = false)
    private LocalDateTime updateDate;
    @Column(name = "needs_refresh", nullable = false)
    private boolean needsRefresh = true;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Tweet)) return false;
        Tweet tweet = (Tweet) o;
        return Objects.equals(link, tweet.link);
    }

    @Override
    public int hashCode() {
        return Objects.hash(link);
    }
}
