package pl.bgnat.master.xscrapper.dto;

import lombok.Data;

@Data
public class Metrics {
    private final long replies;
    private final long reposts;
    private final long likes;
    private final long views;

    public static Metrics empty() {
        return new Metrics(0, 0, 0, 0);
    }
}