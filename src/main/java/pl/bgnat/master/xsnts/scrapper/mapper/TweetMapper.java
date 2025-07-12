package pl.bgnat.master.xsnts.scrapper.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import pl.bgnat.master.xsnts.scrapper.dto.TweetDTO;
import pl.bgnat.master.xsnts.scrapper.model.Tweet;

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
    @Mapping(target = "views", source = "views")
    @Mapping(target = "postDate", source = "postDate")
    TweetDTO toDto(Tweet tweet);
}