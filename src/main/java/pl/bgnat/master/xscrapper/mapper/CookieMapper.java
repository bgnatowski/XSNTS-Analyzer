package pl.bgnat.master.xscrapper.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ObjectFactory;
import org.mapstruct.factory.Mappers;
import org.openqa.selenium.Cookie;
import pl.bgnat.master.xscrapper.dto.scrapper.CookieDTO;

@Mapper
public interface CookieMapper {
    CookieMapper INSTANCE = Mappers.getMapper(CookieMapper.class);

    // Metoda do mapowania Selenium Cookie -> record CookieDto
    @Mapping(target = "name", source = "name")
    @Mapping(target = "value", source = "value")
    @Mapping(target = "domain", source = "domain")
    @Mapping(target = "path", source = "path")
    @Mapping(target = "expiry", source = "expiry")
    @Mapping(target = "secure", source = "secure")
    @Mapping(target = "httpOnly", source = "httpOnly")
    CookieDTO seleniumCookieToDto(org.openqa.selenium.Cookie cookie);

    @ObjectFactory
    default Cookie buildCookie(CookieDTO dto) {
        Cookie.Builder builder = new Cookie.Builder(dto.name(), dto.value())
                .domain(dto.domain())
                .path(dto.path())
                .isSecure(dto.secure())
                .isHttpOnly(dto.httpOnly());

        if (dto.expiry() != null) {
            builder.expiresOn(dto.expiry());
        }
        return builder.build();
    }
}