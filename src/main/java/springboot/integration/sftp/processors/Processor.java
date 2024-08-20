package springboot.integration.sftp.processors;

public interface Processor {
    void process(String line);
}
