package ru.speech.gateway.asterisk;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.apache.commons.codec.digest.DigestUtils;
import org.asteriskjava.fastagi.AgiChannel;
import org.asteriskjava.fastagi.AgiException;
import org.asteriskjava.fastagi.AgiRequest;
import org.asteriskjava.fastagi.BaseAgiScript;
import org.asteriskjava.live.ChannelState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static ru.speech.gateway.asterisk.AGITextToSpeech.getValueByParameters;
import ru.speech.gateway.exceptions.KeyNotFound;
import ru.speech.gateway.yandex.Yandex;
import ru.vehicleutils.models.VehicleNumber;
import static ru.vehicleutils.utils.Utils.getVehicleNumberByText;

/**
 *
 * @author ivan
 */
public class AGISpeechToText extends BaseAgiScript{
    private static final Logger LOG = LoggerFactory.getLogger(AGISpeechToText.class);
    
    @Override
    public void service(AgiRequest ar, AgiChannel ac) throws AgiException {
        try{
            Thread.currentThread().setName(ar.getCallerIdNumber());
            
            String key = getValueByParameters(ar, "key", true);
            String sayWaitText = getValueByParameters(ar, "say_wait_text", false);
            Double maxSilence = new Double(getValueByParameters(ar, "maxSilence", false, "2"));
            
            //Answer channel if not already answered
            int channelStatus = getChannelStatus();     
            if(channelStatus == ChannelState.RING.getStatus()){
                answer();
                LOG.info("Channel answer");
            }
            
            String fileName = "/tmp/"+UUID.randomUUID().toString();
            LOG.info("Record to file {}", fileName);
            recordFile(fileName, "wav", "#", 10000, 0, false, maxSilence.intValue());
            
            if(sayWaitText != null){
                String sayWaitTextPath = Yandex.textToSpeech(DigestUtils.md5Hex(sayWaitText)+".wav", key, sayWaitText, "wav", "oksana", "good");
                int result = exec("Playback", sayWaitTextPath.replaceFirst("\\.(.*)$", ",$1"));
                LOG.info("exec return {}", result);
            }

            LOG.info("Send file to yandex");
            File recordFfile = new File(fileName+".wav");
            if(recordFfile.length() > 0){
                List<String> speechToTextList = Yandex.speechToTextList(key, fileName+".wav");
                if(speechToTextList.isEmpty() == false){
                    //fill RESUL_ARRAY
                    StringBuilder sb = new StringBuilder();
                    for(int i=0; i<speechToTextList.size(); i++){
                        String text = speechToTextList.get(i);
                        sb.append(text);
                        if(i < speechToTextList.size()-1){
                            sb.append(",");
                        }
                    }
                    setVariable("RESUL_ARRAY", sb.toString());
                    
                    //Get alpha-numeric number
                    List<VehicleNumber> vehicleNumberList = new ArrayList<>();
                    for(String text:speechToTextList){
                        VehicleNumber vehicleNumber = getVehicleNumberByText(text);
                        if(vehicleNumber != null){
                            vehicleNumberList.add(vehicleNumber);
                        }
                    }
                    if(vehicleNumberList.isEmpty() == false){
                        Collections.sort(vehicleNumberList);
                        VehicleNumber vehicleNumber = vehicleNumberList.get(0);
                        setVariable("TRANSPORT_CHARS", vehicleNumber.getTransportChars());
                        if(vehicleNumber.getTransportId() != null){
                            setVariable("TRANSPORT_ID", vehicleNumber.getTransportId().toString());
                        }
                        if(vehicleNumber.getTransportReg() != null){
                            setVariable("TRANSPORT_REG", vehicleNumber.getTransportReg().toString());
                        }
                    }
                }else {
                    LOG.error("Yandex speech result is empty");
                }
            }else {
                LOG.error("Record file is empty");
            }
        }catch(KeyNotFound ex){
            LOG.error(ex.getMessage());
        }catch(AgiException ex){
            LOG.error("Exception ", ex);
        }
    }
}
