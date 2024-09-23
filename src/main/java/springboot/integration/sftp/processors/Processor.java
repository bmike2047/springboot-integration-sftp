package springboot.integration.sftp.processors;

/**
 * Generic processor type.
 * @param <T1> input type to process
 * @param <T2> output processed type
 */
public interface Processor<T1,T2> {
    T2 process(T1 line);
}
