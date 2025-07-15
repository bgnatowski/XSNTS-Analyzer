package pl.bgnat.master.xsnts.scrapper.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;

import java.util.Date;

/**
 * DTO zawierające opis cookiesa
 */
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public record CookieDTO(String name,
                        String value,
                        String domain,
                        String path,
                        Date expiry,
                        boolean secure,
                        boolean httpOnly){
}
