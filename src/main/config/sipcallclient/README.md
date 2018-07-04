## About

lasipcallctrl.js is a sample node.js module to demonstrate how to use SipCallServer to do load tests.

laagentctrl.js is a sample node.js module to simulate Nuance Automation Assist agents.

latest.js combines these 2 modules to do an end-to-end load test.

## Install

Download these files to some empty directory and run:
    
    npm install

## Configuration:

Edit the latest.js file:

```javascript
var agentCtrlOptions = {
    nbAgents : 10,
    agentOptions : {
        host : 'https://mtl-mrcp16-vm01:8443/',
        minDelayBeforeSendOutcome : 3000,
        maxDelayBeforeSendOutcome : 6000
    }
};

var sipCallCtrlOptions = {
    //host : 'http://localhost:8084/',
    host : 'http://mtl-da55-vm6:8084/',
    to : 'sip:4628@mtl-mrcp16-vm02:5060',
    nbCallsToPlace : 200,
    nbSimultaneousCallsToPlace : 10,
    minDelayBeforeInvite : 1000,
    maxDelayBeforeInvite : 3000,
    minDelayBeforeBye : 30000,
    maxDelayBeforeBye : 30000,
    recordFileNamePrefix : '/tmp/',
    playFileName : '03-rambling-caller-input-ulaw.ul'
};
```

## Run

    node latest.js
