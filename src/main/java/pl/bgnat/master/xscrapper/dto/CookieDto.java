package pl.bgnat.master.xscrapper.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Date;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CookieDto(String name,
                        String value,
                        String domain,
                        String path,
                        Date expiry,
                        boolean secure,
                        boolean httpOnly){
}
