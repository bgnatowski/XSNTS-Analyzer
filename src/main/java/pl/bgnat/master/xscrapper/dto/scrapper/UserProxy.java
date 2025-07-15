package pl.bgnat.master.xscrapper.dto.scrapper;

/**
 * DTO dla proxy per user
 */
public record UserProxy(String host,
                        String port,
                        String username,
                        String password) {
}
