## About

lasipcallctrl.js is a sample node.js module to demonstrate how to use SipCallServer to do load tests.

## Install

Download these files to some empty directory and run:
    
    npm install

## Configuration:

Edit:

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

    node lasipcallctrl.js
