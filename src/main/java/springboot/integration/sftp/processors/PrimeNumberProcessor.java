package springboot.integration.sftp.processors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Prime number processor applied to an incoming file lines.
 */
public class PrimeNumberProcessor implements Processor<String, Boolean> {
    private static final Logger LOG = LoggerFactory.getLogger(PrimeNumberProcessor.class);

    /**
     * Process logic.
     *
     * @param line input line
     */
    @Override
    public Boolean process(String line) {
        int n = Integer.parseInt(line.trim());
        boolean prime = isPrime(n);
        LOG.info("Number {} is prime: {}", n, prime);
        return prime;
    }

    /**
     * Prime number check algorithm.
     *
     * @param n input number
     * @return true if a number is prime
     */
    private boolean isPrime(int n) {
        if (n < 2) return false;
        int sqrt = (int) Math.sqrt(n);
        for (int i = 2; i <= sqrt; i++) {
            if (n % i == 0) return false;
        }
        return true;
    }

}
