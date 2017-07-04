package ru.speech.gateway.asterisk;

import java.io.File;
import java.util.List;
import java.util.UUID;
import org.asteriskjava.fastagi.AgiChannel;
import org.asteriskjava.fastagi.AgiException;
import org.asteriskjava.fastagi.AgiRequest;
import org.asteriskjava.fastagi.BaseAgiScript;
import org.asteriskjava.live.ChannelState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.speech.gateway.yandex.Yandex;

/**
 *
 * @author ivan
 */
public class AgiRecv extends BaseAgiScript{
    private static final Logger LOG = LoggerFactory.getLogger(AgiRecv.class);
    
    @Override
    public void service(AgiRequest ar, AgiChannel ac) throws AgiException {
        try{
            Double maxSilence = 2.0;            
            Thread.currentThread().setName(ar.getCallerIdNumber());
            
            String[] keyResult = ar.getParameterValues("key");
            if(keyResult.length <= 0){
                LOG.error("yandex KEY is not defined");
                return;
            }
            String key = keyResult[0];
            
            String[] maxSilenceResult = ar.getParameterValues("maxSilence");
            if(maxSilenceResult.length > 0){
                maxSilence = new Double(maxSilenceResult[0]);
            }
            
            //Answer channel if not already answered
            int channelStatus = getChannelStatus();            
            if(channelStatus == ChannelState.RING.getStatus()){
                answer();
                LOG.info("Channel answer");
            }
            
//            Format format = detectFormat();
//            if(format == null){
//                LOG.error("Can't define channel format");
//                return;
//            }
//            LOG.info("Channel format {}",format.toString());
            
            String fileName = "/tmp/"+UUID.randomUUID().toString();
            LOG.info("Record to file {}", fileName);
            recordFile(fileName, "wav", "#", 10000, 0, false, maxSilence.intValue());

            LOG.info("Send file to yandex");
            File recordFfile = new File(fileName+".wav");
            if(recordFfile.length() > 0){
                List<String> speechToTextList = Yandex.speechToTextList(key, fileName+".wav");
                if(speechToTextList.isEmpty() == false){
                    StringBuilder sb = new StringBuilder();
                    for(int i=0; i<speechToTextList.size(); i++){
                        String text = speechToTextList.get(i);
                        sb.append(text);
                        if(i < speechToTextList.size()-1){
                            sb.append(",");
                        }
                    }
                    setVariable("RESUL_ARRAY", sb.toString());
                }else {
                    LOG.error("Yandex speech result is empty");
                }
            }else {
                LOG.error("Record file is empty");
            }
        }catch(AgiException ex){
            LOG.error("Exception ", ex);
        }
    }
    
    @Deprecated
    private Format detectFormat(){
        try {            
            String audionativeformat = getFullVariable("${CHANNEL(audionativeformat)}");
            LOG.debug("audionativeformat: {}", audionativeformat);
            if(audionativeformat.matches("(silk|sln)12")){
                return new Format("sln12", 12000);
            }
            if(audionativeformat.matches("(speex|slin|silk)16|g722|siren7")){
                return new Format("sln16", 16000);
            }
            if(audionativeformat.matches("(speex|slin|celt)32|siren14")){
                return new Format("sln32", 32000);
            }
            if(audionativeformat.matches("(celt|slin)44")){
                return new Format("sln44", 44100);
            }
            if(audionativeformat.matches("(celt|slin)48")){
                return new Format("sln48", 48000);
            }
            return new Format("sln", 8000);
        } catch (AgiException ex) {
            LOG.error("Exception {}",ex);
        }
        return null;
    }
    
    private class Format{
        String name;
        Integer freq;

        public Format(String name, Integer freq) {
            this.name = name;
            this.freq = freq;
        }

        @Override
        public String toString() {
            return name+"/"+freq;
        }   
    }
}
