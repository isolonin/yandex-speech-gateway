package ru.speech.gateway.monitor;

import org.asteriskjava.fastagi.AgiServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author ivan
 */
public class Terminator extends Thread{
    private static final Logger LOG = LoggerFactory.getLogger(Terminator.class);
    private AgiServer agiServer;

    public Terminator(AgiServer agiServer) {
        setName("SIGNAL CATCH");
        this.agiServer = agiServer;
    }

    @Override
    public void run() {
        LOG.info("Process terminate!");
        agiServer.shutdown();
    }
}
