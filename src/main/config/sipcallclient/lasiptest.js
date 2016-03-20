var LASipCallCtrl = require('./lasipcallctrl.js');
var util = require('util');

if (process.argv.length < (2 + 3)) {
    console.log("Usage: node lasiptest.js nbCallsToPlace nbSimultaneousCallsToPlace sip:4617@mtl-mrcp16-vm02:5060");
    return 1;
}

var sipCallCtrlOptions = {
    //host : 'http://localhost:8085/',
    //host : 'http://mtl-da55-vm6:8084/',
    host : 'http://mtl-blade20-vm205:8085/',
    config : {
        localSipAddress : "",
        localSipPort : 5062
    },
    to : process.argv[4],
    // SPRINT_POC
    //to : 'sip:4622@mtl-mrcp16-vm02:5060',
    //to : 'sip:4627@mtl-da45-vm1:5060',
    nbCallsToPlace : parseInt(process.argv[2]),
    nbSimultaneousCallsToPlace : parseInt(process.argv[3]),
    minDelayBeforeInvite : 3000,
    maxDelayBeforeInvite : 3000,
    minDelayBeforeBye : 12000,
    maxDelayBeforeBye : 12000,
    recordFileNamePrefix : '/tmp/',
    playFileName : '03-rambling-caller-input-ulaw.ul'
};

var nbCallsPlaced = 0;
var nbCallsSucceeded = 0;

function logStatus() {
    util.log('STATUS: nbCallsPlaced=' + nbCallsPlaced + ', nbCallsSucceeded=' + nbCallsSucceeded);
}

var sipCallCtrl = new LASipCallCtrl(sipCallCtrlOptions);
sipCallCtrl.setCallsPlacedCB(function() {
    nbCallsPlaced++;
    logStatus();
});
sipCallCtrl.setCallsSucceededCB(function() {
    nbCallsSucceeded++;
    logStatus();
});
sipCallCtrl.setShutdownCompletedCB(function() {
});

setTimeout(function() {
    sipCallCtrl.run();
}, 1000);

