package pl.bgnat.master.xsnts.scrapper.dto;

import lombok.Builder;

/**
 * DTO zawierające wynik operacji czyszczenia status AdsBrowsera
 */
@Builder
public record BrowserStatusData(String status, WebSocketInfo ws, String webdriver){
}
