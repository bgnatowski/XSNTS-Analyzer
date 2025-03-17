package pl.bgnat.master.xscrapper.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import pl.bgnat.master.xscrapper.dto.TweetDto;
import pl.bgnat.master.xscrapper.model.Tweet;

@Mapper
public interface TweetMapper {
    TweetMapper INSTANCE = Mappers.getMapper(TweetMapper.class);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "username", source = "username")
    @Mapping(target = "content", source = "content")
    @Mapping(target = "link", source = "link")
    @Mapping(target = "likeCount", source = "likeCount")
    @Mapping(target = "repostCount", source = "repostCount")
    @Mapping(target = "commentCount", source = "commentCount")
    @Mapping(target = "postDate", source = "postDate")
    TweetDto toDto(Tweet tweet);
}