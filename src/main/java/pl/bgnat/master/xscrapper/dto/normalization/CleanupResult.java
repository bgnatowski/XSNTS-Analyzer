package pl.bgnat.master.xscrapper.dto.normalization;

import lombok.Builder;
import lombok.Data;

/**
 * DTO zawierające wynik operacji czyszczenia
 */
@Data
@Builder
public class CleanupResult {
    private Integer deletedRecords;
    private Boolean success;
    private String message;
}
