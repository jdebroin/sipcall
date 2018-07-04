// Place SIP calls
//

if (process.argv.length < 5) {
    console.log('usage: nbCallsToPlace nbSimultaneousCallsToPlace sipCallServerHost sipTo');
    console.log('example: 100 10 http://mtl-blade20-vm205:8085/ sip:4622@mtl-mrcp16-vm02:5060');
    return;
}
var LASipCallCtrl = require('./lasipcallctrl.js');
var util = require('util');

var sipCallCtrlOptions = {
    nbCallsToPlace : parseInt(process.argv[2]),
    nbSimultaneousCallsToPlace : parseInt(process.argv[3]),
    //host : 'http://localhost:8085/',
    //host : 'http://mtl-da55-vm6:8084/',
    //host : 'http://mtl-blade20-vm205:8085/',
    host: process.argv[4],
    //to : 'sip:4638@mtl-mrcp16-vm02:5060',
    // SPRINT_POC
    //to : 'sip:4622@mtl-mrcp16-vm02:5060',
    //to : 'sip:4627@mtl-da45-vm1:5060',
    to : process.argv[5],
    config : {
        localSipAddress : "",
        localSipPort : 5062
    },
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

