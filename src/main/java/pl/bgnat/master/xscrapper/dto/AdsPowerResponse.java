package pl.bgnat.master.xscrapper.dto;

import lombok.Builder;

/**
 * DTO zawierające wynik operacji otworzenia browsera
 */
@Builder
public record AdsPowerResponse<T> (int code, T data, String msg) {
}
