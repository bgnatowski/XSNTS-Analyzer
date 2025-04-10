package pl.bgnat.master.xscrapper.dto;

public record AdsPowerResponse<T> (int code, T data, String message) {
}
