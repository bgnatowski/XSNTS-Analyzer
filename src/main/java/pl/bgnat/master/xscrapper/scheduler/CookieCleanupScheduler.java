package pl.bgnat.master.xscrapper.scheduler;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import pl.bgnat.master.xscrapper.utils.CookieUtils;

@Slf4j
@Data
public class CookieCleanupScheduler {

//    @Scheduled(cron = "0 0 0 */2 * *")
    public void cleanCookiesDaily() {
        log.info("Rozpoczynam czyszczenie plików ciasteczek...");
        CookieUtils.deleteAllCookiesFiles();
    }
}
