package pl.bgnat.master.xscrapper.scheduler;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import pl.bgnat.master.xscrapper.utils.CookieUtils;

@Slf4j
@Data
public class CookieCleanupScheduler {

    // Co 24 godziny (86,400,000 ms)
    @Scheduled(cron = "0 0 0 */2 * *")
    public void cleanCookiesDaily() {
        log.info("Rozpoczynam czyszczenie plików ciasteczek...");
        CookieUtils.deleteAllCookiesFiles();
    }
}
