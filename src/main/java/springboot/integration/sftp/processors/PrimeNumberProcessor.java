package springboot.integration.sftp.processors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PrimeNumberProcessor implements Processor {
    private static final Logger LOG = LoggerFactory.getLogger(PrimeNumberProcessor.class);


    @Override
    public void process(String line) {
        int n = Integer.parseInt(line.trim());
        LOG.info("Number {} is prime: {}", n, isPrime(n));
    }

    private boolean isPrime(int n) {
        if (n < 2) return false;
        int sqrt = (int) Math.sqrt(n);
        for (int i = 2; i <= sqrt; i++) {
            if (n % i == 0) return false;
        }
        return true;
    }

}
