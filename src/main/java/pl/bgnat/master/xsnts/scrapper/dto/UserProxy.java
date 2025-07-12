package pl.bgnat.master.xsnts.scrapper.dto;

/**
 * DTO dla proxy per user
 */
public record UserProxy(String host,
                        String port,
                        String username,
                        String password) {
}
