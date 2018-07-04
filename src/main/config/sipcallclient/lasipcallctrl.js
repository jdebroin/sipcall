var lautils = require('./lautils.js');
var util = require('util');
var request = require('request');
var url = require('url');

function LASipCallCtrl(params) {
    this.host = params.host;
    this.config = params.config;
    this.to = params.to;
    this.nbCallsToPlace = params.nbCallsToPlace;
    this.nbSimultaneousCallsToPlace = params.nbSimultaneousCallsToPlace;
    this.minDelayBeforeInvite = params.minDelayBeforeInvite;
    this.maxDelayBeforeInvite = params.maxDelayBeforeInvite;
    this.minDelayBeforeBye = params.minDelayBeforeBye;
    this.maxDelayBeforeBye = params.maxDelayBeforeBye;
    this.recordFileNamePrefix = params.recordFileNamePrefix;
    this.playFileName = params.playFileName;

    if (this.nbSimultaneousCallsToPlace > this.nbCallsToPlace)
        this.nbSimultaneousCallsToPlace = this.nbCallsToPlace;

    if (this.minDelayBeforeInvite < 0)
        this.minDelayBeforeInvite = 0;
    if (this.maxDelayBeforeInvite < 0)
        this.maxDelayBeforeInvite = 0;
    if (this.minDelayBeforeInvite > this.maxDelayBeforeInvite)
        this.minDelayBeforeInvite = this.maxDelayBeforeInvite;

    if (this.minDelayBeforeBye < 0)
        this.minDelayBeforeBye = 0;
    if (this.maxDelayBeforeBye < 0)
        this.maxDelayBeforeBye = 0;
    if (this.minDelayBeforeBye > this.maxDelayBeforeBye)
        this.minDelayBeforeBye = this.maxDelayBeforeBye;

    this.inShutdown = true;
    this.sipcalls = [];
    this.timeouts = {};
    this.callData = {};
    this.nbCallsPlaced = 0;
    this.nbCallsSucceeded = 0;
    this.nbCallsFinished = 0;

    this.headers = {};
    this.headers['accept'] = 'application/json';
    this.headers['user-agent'] = 'request-json/1.0';
}

LASipCallCtrl.prototype.log = function(message) {
    lautils.log(message);
};

LASipCallCtrl.prototype.setCallsPlacedCB = function(callsPlacedCB) {
    this.callsPlacedCB = callsPlacedCB;
};

LASipCallCtrl.prototype.setCallsSucceededCB = function(callsSucceededCB) {
    this.callsSucceededCB = callsSucceededCB;
};

LASipCallCtrl.prototype.setShutdownCompletedCB = function(shutdownCompletedCB) {
    this.shutdownCompletedCB = shutdownCompletedCB;
};

LASipCallCtrl.prototype.get = function(urlSuffix, callback) {
    var self = this;
    var options = {};
    options.method = 'GET'
    options.uri = url.resolve(self.host, urlSuffix);
    options.headers = self.headers;
    //util.print('*');
    request(options, function(error, response, body) {
        if (error) {
            callback(error);
            return;
        }
        if (response.statusCode == 204 || response.statusCode == 200) {
            callback(null, lautils.parseBody(body));
        } else {
            callback("non ok: " + response.statusCode);
        }
    });
};

LASipCallCtrl.prototype.post = function(urlSuffix, data, callback) {
    var self = this;
    var options = {};
    options.method = 'POST'
    options.uri = url.resolve(self.host, urlSuffix);
    options.json = data;
    options.headers = self.headers;
    //util.print('*');
    request(options, function(error, response, body) {
        if (error) {
            callback(error);
            return;
        }
        if (response.statusCode == 204 || response.statusCode == 200) {
            callback(null, lautils.parseBody(body));
        } else {
            callback("non ok: " + response.statusCode);
        }
    });
};

LASipCallCtrl.prototype.poll = function() {
    var self = this;
    var now = new Date().getTime();
    self.get('/ws/sipcall/poll?timeoutMs=30000', function(error, data) {
        if (error) {
            self.errorMessage = "poll failed, error=" + error;
            throw "fatal, poll: " + error;
        }
        var elapsed = (new Date().getTime()) - now;
        if (elapsed > 35000) {
            throw "poll elapsed: " + elapsed;
        }
        if (!self.inShutdown) {
            if (data.response != "TIMEOUT") {
                var responseString = ", response=" + data.response;
                if (data.reason) {
                    responseString += ", reason=" + data.reason;
                }
                if (data.code) {
                    responseString += ", code=" + data.code;
                }
                self.log(data.callId + " poll success" + responseString);
                var mine = false;
                for (var i = 0; i < self.sipcalls.length; i++) {
                    if (self.sipcalls[i].callId == data.callId) {
                        mine = true;
                        self.sipcalls[i].response = data.response;
                        self.sipcalls[i].reason = data.reason;
                        self.sipcalls[i].code = data.code;
                    }
                }
                if (mine) {
                    if (data.response == 'INVITE_OK') {
                        if (data.callId in self.timeouts && 'inviteOkTimeout' in self.timeouts[data.callId]) {
                            clearTimeout(self.timeouts[data.callId]['inviteOkTimeout']);
                            delete self.timeouts[data.callId]['inviteOkTimeout'];
                        }
                        self.inviteOk(data.callId);
                    } else if (data.response == 'PLAY_DONE') {
                        self.playDone(data.callId);
                    } else if (data.response == 'CALL_FINISHED') {
                        if (data.callId in self.timeouts && 'inviteOkTimeout' in self.timeouts[data.callId]) {
                            clearTimeout(self.timeouts[data.callId]['inviteOkTimeout']);
                            delete self.timeouts[data.callId]['inviteOkTimeout'];
                        }
                        if (data.callId in self.timeouts && 'startPlayTimeout' in self.timeouts[data.callId]) {
                            clearTimeout(self.timeouts[data.callId]['startPlayTimeout']);
                            delete self.timeouts[data.callId]['startPlayTimeout'];
                        }
                        if (data.callId in self.timeouts && 'sendByeTimeout' in self.timeouts[data.callId]) {
                            clearTimeout(self.timeouts[data.callId]['sendByeTimeout']);
                            delete self.timeouts[data.callId]['sendByeTimeout'];
                        }
                        if (data.callId in self.callData) {
                            var callDatum = self.callData[data.callId];
                            if (callDatum.state === 'PLAY_STARTED' && data.reason === 'BYE_RECEIVED') {
                                callDatum.state = 'NORMAL_DISCONNECT';
                            }
                        }
                        self.callFinished(null, data.callId);
                    }
                }
            }

            self.poll();
        }
    });
};
    
LASipCallCtrl.prototype.init = function(callback) {
    var self = this;
    var initParams = {
        config : self.config
    };
    self.post('/ws/sipcall/init', initParams, function(error, result) {
        if (error) {
            self.log("init failed, error=" + error);
            throw "fatal, init: " + error;
        }
        self.log("init succeeded");
        self.inShutdown = false;
        self.poll();
        callback(null);
    });
};

LASipCallCtrl.prototype.shutdown = function() {
    var self = this;
    if (!self.inShutdown) {
        self.inShutdown = true;
        self.post('/ws/sipcall/shutdown', null, function(error) {
            if (error) {
                self.log("shutdown failed, error=" + error);
                // continue
            } else {
                self.log("shutdown succeeded");
            }
            self.sipcalls = [];
            if (self.shutdownCompletedCB()) {
                self.shutdownCompletedCB();
            }
        });
    }
};

LASipCallCtrl.prototype.sendInvite = function(fromUser, callback) {
    var self = this;
    var sendInviteParams = {
        fromUser : fromUser,
        to : self.to
    };
    self.post('/ws/sipcall/sendInvite', sendInviteParams, function(error, data) {
        if (error) {
            self.log("sendInvite(fromUser=" + sendInviteParams.fromUser + ", to=" + sendInviteParams.to + ") failed, error=" + error);
            callback(error);
            return;
        }
        self.log(data.callId + " sendInvite(fromUser=" + sendInviteParams.fromUser + ", to=" + sendInviteParams.to + ") succeeded");
        self.sipcalls.unshift({
            callId : data.callId,
            timeouts : {}
        });
        var callDatum = {};
        callDatum.state = 'SENDING_INVITE';
        callDatum.fromUser = sendInviteParams.fromUser;
        self.callData[data.callId] = callDatum;
        var timeout = 36000;
        var timeoutId = setTimeout(function(callId) {
            self.log(callId + " timeout waiting for invite(fromUser=" + sendInviteParams.fromUser + ", to=" + sendInviteParams.to + ")");
            callback('timeout');
            return;
        }, timeout, data.callId);
        self.setTimeoutId(data.callId, 'inviteOkTimeout', timeoutId);
        callback(null);
    });
};

LASipCallCtrl.prototype.sendBye = function(callId, callback) {
    var self = this;
    var sendByeParams = {
        callId : callId
    };
    self.post('/ws/sipcall/sendBye', sendByeParams, function(error, data) {
        if (error) {
            self.log(callId + " sendBye failed, error=" + error);
            callback(error);
            return;
        }
        self.log(callId + " sendBye succeeded");
        callback(null);
    });
};

LASipCallCtrl.prototype.record = function(callId, fileName, callback) {
    var self = this;
    var recordParams = {
        callId : callId,
        fileName : fileName
    };
    self.post('/ws/sipcall/record', recordParams, function(error, data) {
        if (error) {
            self.log(callId + " record failed, error=" + error);
            callback(error);
            return;
        }
        self.log(callId + " record succeeded");
        callback(null);
    });
};

LASipCallCtrl.prototype.play = function(callId, fileName, callback) {
    var self = this;
    var playParams = {
        callId : callId,
        fileName : fileName
    };
    self.post('/ws/sipcall/play', playParams, function(error, data) {
        if (error) {
            self.log(callId + " play failed, error=" + error);
            callback(error);
            return;
        }
        self.log(callId + " play succeeded");
        callback(null);
    });
};

LASipCallCtrl.prototype.inviteOk = function(callId) {
    var self = this;
    if (callId in self.callData) {
        var callDatum = self.callData[callId];
        if (callDatum.state === 'SENDING_INVITE') {
            callDatum.state = 'CONNECTED';
        }
    }
    var recordFileName = self.recordFileNamePrefix + "in-" + callId + ".ul";
    self.record(callId, recordFileName, function(error, result) {
        if (error) {
            self.inCallError(error, callId);
            return;
        }
        var timeout = 2000;
        var timeoutId = setTimeout(function(callId) {
            self.play(callId, self.playFileName, function(error, result) {
                if (error) {
                    self.inCallError(error, callId);
                    return;
                }
                if (callId in self.callData) {
                    var callDatum = self.callData[callId];
                    if (callDatum.state === 'CONNECTED') {
                        callDatum.state = 'PLAY_STARTED';
                    }
                }
            });
        }, timeout, callId);
        self.setTimeoutId(callId, 'startPlayTimeout', timeoutId);
    });
};

LASipCallCtrl.prototype.playDone = function(callId) {
    var self = this;
    self.finishCall(callId);
};
    
LASipCallCtrl.prototype.callFinished = function(error, callId) {
    var self = this;
    self.nbCallsFinished++;
    if (callId && callId in self.callData) {
        var callDatum = self.callData[callId];
        if (callDatum.state === 'NORMAL_DISCONNECT') {
            self.nbCallsSucceeded++;
            if (self.callsSucceededCB) {
                self.callsSucceededCB();
            }
        } else {
            self.log(callId + " from=" + callDatum.fromUser + ", state=" + callDatum.state);
        }
        delete self.callData[callId];
    }
    self.log("callsPlaced=" + self.nbCallsPlaced + ", callsToPlace=" + self.nbCallsToPlace);
    if (!error && self.nbCallsPlaced < self.nbCallsToPlace) {
        self.placeCall();
    } else if (self.nbCallsFinished >= self.nbCallsPlaced) {
        self.shutdown();
    }
};
    
LASipCallCtrl.prototype.inCallError = function(error, callId) {
    var self = this;
    self.sendBye(callId, function(error) {
        if (error) {
            self.callFinished(error, callId);
            return;
        }
    });
};

LASipCallCtrl.prototype.run = function() {
    var self = this;
    self.init(function(error) {
        if (error) {
            self.log('failed: ' + error);
            return;
        }
        var i;
        for (i = 0; i < self.nbSimultaneousCallsToPlace; i++) {
            self.placeCall();
        }
    });
};

LASipCallCtrl.prototype.placeCall = function() {
    var self = this;
    var fromUser = self.nbCallsPlaced;
    self.nbCallsPlaced++;
    if (self.callsPlacedCB) {
        self.callsPlacedCB();
    }
    var timeout = Math.floor(self.minDelayBeforeInvite + Math.random() * (self.maxDelayBeforeInvite - self.minDelayBeforeInvite));
    setTimeout(function() {
        self.sendInvite(fromUser, function(error) {
            if (error) {
                self.callFinished(error);
                return;
            }
        });
    }, timeout);
};

LASipCallCtrl.prototype.finishCall = function(callId) {
    var self = this;
    var timeout = Math.floor(self.minDelayBeforeBye + Math.random() * (self.maxDelayBeforeBye - self.minDelayBeforeBye));
    var timeoutId = setTimeout(function(callId) {
        self.sendBye(callId, function(error) {
            if (error) {
                self.callFinished(error, callId);
                return;
            }
        });
    }, timeout, callId);
    self.setTimeoutId(callId, 'sendByeTimeout', timeoutId);
};

LASipCallCtrl.prototype.setTimeoutId = function(callId, timeoutName, timeoutId) {
    var self = this;
    if (!(callId in self.timeouts)) {
        self.timeouts[callId] = {};
    }
    self.timeouts[callId][timeoutName] = timeoutId;
};

module.exports = LASipCallCtrl;
