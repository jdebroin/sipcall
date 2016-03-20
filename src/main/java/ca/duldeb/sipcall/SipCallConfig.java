package ca.duldeb.sipcall;

import java.util.List;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(using = SipCallConfigDeserializer.class)
public class SipCallConfig {
    private String localUserName;
    private String localSipAddress;
    private int localSipPort = 0;
    private int localRtpPort = 0;
    private int nbLegs;
    private int nbIterations;
    private String to;
    private List<SipCallTask> tasks;

    public String getLocalUserName() {
        return localUserName;
    }

    public void setLocalUserName(String localUserName) {
        this.localUserName = localUserName;
    }

    public String getLocalSipAddress() {
        return localSipAddress;
    }

    public void setLocalSipAddress(String localSipAddress) {
        this.localSipAddress = localSipAddress;
    }

    public int getLocalSipPort() {
        return localSipPort;
    }

    public void setLocalSipPort(int localSipPort) {
        this.localSipPort = localSipPort;
    }

    public int getLocalRtpPort() {
        return localRtpPort;
    }

    public void setLocalRtpPort(int localRtpPort) {
        this.localRtpPort = localRtpPort;
    }

    public int getNbLegs() {
        return nbLegs;
    }

    public void setNbLegs(int nbLegs) {
        this.nbLegs = nbLegs;
    }

    public int getNbIterations() {
        return nbIterations;
    }

    public void setNbIterations(int nbIterations) {
        this.nbIterations = nbIterations;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public List<SipCallTask> getTasks() {
        return tasks;
    }

    public void setTasks(List<SipCallTask> tasks) {
        this.tasks = tasks;
    }

    public String getLocalRtpAddress() {
        return localSipAddress;
    }

    public void merge(SipCallConfig extraConfig) {
        if (extraConfig.localUserName != null) {
            this.localUserName = extraConfig.localUserName;
        }
        if (extraConfig.localSipAddress != null) {
            this.localSipAddress = extraConfig.localSipAddress;
        }
        if (extraConfig.localSipPort != 0) {
            this.localSipPort = extraConfig.localSipPort;
        }
        if (extraConfig.localRtpPort != 0) {
            this.localRtpPort = extraConfig.localRtpPort;
        }
        if (extraConfig.nbLegs != 0) {
            this.nbLegs = extraConfig.nbLegs;
        }
        if (extraConfig.nbIterations != 0) {
            this.nbIterations = extraConfig.nbIterations;
        }
        if (extraConfig.to != null) {
            this.to = extraConfig.to;
        }
    }
}
