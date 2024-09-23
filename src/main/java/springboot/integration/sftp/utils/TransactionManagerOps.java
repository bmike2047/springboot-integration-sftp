package springboot.integration.sftp.utils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility class used by transaction manager to record the status for each file.
 */
public class TransactionManagerOps {
    private Map<String, Boolean> records = new ConcurrentHashMap<>();

    /**
     * Map success operation.
     *
     * @param file received file
     * @return map result
     */
    public Boolean success(String file) {
        return Boolean.TRUE.equals(records.put(file, true));
    }

    /**
     * Map error operation
     *
     * @param file received file
     * @return map result
     */
    public Boolean error(String file) {
        return Boolean.FALSE.equals(records.put(file, false));
    }

    /**
     * Retrieve file last operation status
     *
     * @param file received file
     * @return retrieve status
     */
    public Boolean inspect(String file) {
        return records.get(file);
    }


}