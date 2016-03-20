var LASipCallCtrl = require('./sinksipcallctrl.js');
var util = require('util');

var sipCallCtrlOptions = {
    host : 'http://localhost:8084/',
    //host : 'http://mtl-da55-vm6:8084/',
    config : {
        localSipAddress : "127.0.0.1",
        localSipPort : 5070
    },
    //to : 'sip:4628@mtl-mrcp16-vm02:5060',
    to : 'sip:4627@mtl-da45-vm1:5060',
    nbCallsToPlace : 10,
    nbSimultaneousCallsToPlace : 2,
    minDelayBeforeInvite : 3000,
    maxDelayBeforeInvite : 10000,
    minDelayBeforeBye : 60000,
    maxDelayBeforeBye : 60000,
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

