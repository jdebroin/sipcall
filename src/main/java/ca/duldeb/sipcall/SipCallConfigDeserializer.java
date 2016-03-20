package ca.duldeb.sipcall;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

public class SipCallConfigDeserializer extends JsonDeserializer<SipCallConfig> {

    @Override
    public SipCallConfig deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException,
            JsonProcessingException {
        SipCallConfig config = new SipCallConfig();

        JsonNode node = jp.readValueAsTree();
        JsonNode child = node.get("localUserName");
        if (child != null)
            config.setLocalUserName(child.asText());

        child = node.get("localSipAddress");
        if (child != null)
            config.setLocalSipAddress(child.asText());

        child = node.get("localSipPort");
        if (child != null)
            config.setLocalSipPort(child.asInt());

        child = node.get("localRtpPort");
        if (child != null)
            config.setLocalRtpPort(child.asInt());

        child = node.get("nbLegs");
        if (child != null)
            config.setNbLegs(child.asInt());

        child = node.get("nbIterations");
        if (child != null)
            config.setNbIterations(child.asInt());

        child = node.get("to");
        if (child != null)
            config.setTo(child.asText());

        JsonNode tasksNode = node.get("tasks");
        if (tasksNode != null) {
            List<SipCallTask> tasks = new ArrayList<SipCallTask>();
            for (Iterator<JsonNode> tasksIter = tasksNode.elements(); tasksIter.hasNext();) {
                JsonNode taskNode = tasksIter.next();
                String taskType = taskNode.get("type").asText();
                if (taskType.equals("pause")) {
                    int delay = taskNode.get("delay").asInt();
                    tasks.add(new SipCallPause(delay));
                } else if (taskType.equals("play")) {
                    String fileName = taskNode.get("fileName").asText();
                    tasks.add(new SipCallPlay(fileName));
                } else if (taskType.equals("record")) {
                    String fileName = taskNode.get("fileName").asText();
                    tasks.add(new SipCallRecord(fileName));
                } else if (taskType.equals("hangup")) {
                    tasks.add(new SipCallHangup());
                }
            }
            config.setTasks(tasks);
        }

        return config;
    }

}
