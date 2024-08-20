package springboot.integration.sftp.utils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility class used by transaction manager to record the status for each file.
 */
public class TransactionManagerOps {
    private Map<String, Boolean> records = new ConcurrentHashMap<>();

    public Boolean success(String file) {
        return Boolean.TRUE.equals(records.put(file, true));
    }

    public Boolean error(String file) {
        return Boolean.FALSE.equals(records.put(file, false));
    }

    public Boolean inspect(String file) {
        return records.get(file);
    }


}