package ru.speech.gateway;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.log4j.PropertyConfigurator;
import org.asteriskjava.fastagi.AgiScript;
import org.asteriskjava.fastagi.DefaultAgiServer;
import org.asteriskjava.fastagi.SimpleMappingStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.speech.gateway.asterisk.AgiRecv;
import ru.speech.gateway.monitor.Terminator;
import ru.speech.gateway.yandex.Yandex;

/**
 *
 * @author ivan
 */
public class Run{
    private static final Logger LOG = LoggerFactory.getLogger(Run.class);
    
    public static void main(String[] args) {
        try {
            PropertyConfigurator.configure("log4j.properties");            
            LOG.info("start");
            
            Map<String, AgiScript> map = new HashMap<>();
            map.put("yandex-speech.agi", new AgiRecv());
            
            SimpleMappingStrategy mappingStrategy = new SimpleMappingStrategy();
            mappingStrategy.setMappings(map);
            
            DefaultAgiServer agiServer = new DefaultAgiServer(mappingStrategy);
            agiServer.setMaximumPoolSize(20);
            
            Runtime.getRuntime().addShutdownHook(new Terminator(agiServer));
            agiServer.startup();
        } catch (IOException | IllegalStateException ex) {
            LOG.error("Exception ", ex);
        }
    }
    
    
}
