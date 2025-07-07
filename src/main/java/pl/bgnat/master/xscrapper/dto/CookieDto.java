package pl.bgnat.master.xscrapper.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;

import java.util.Date;

/**
 * DTO zawierające opis cookiesa
 */
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public record CookieDto(String name,
                        String value,
                        String domain,
                        String path,
                        Date expiry,
                        boolean secure,
                        boolean httpOnly){
}
