function SipCallCtrl($scope, $http) {
    $scope.errorMessage = "";
    $scope.inShutdown = false;
    $scope.inInit = false;
    $scope.initialized = false;
    $scope.sipcalls = [];

    $scope.config = {
        localSipAddress : "",
        localSipPort : 0,
        localRtpPort : 0
    };
    $scope.fromUser = '1';
    $scope.to = 'sip:jacques@mt-jdebroin10:5060';
    $scope.sessionId = '';
    $scope.recordingDirectory = '';
    $scope.playFile = "03-rambling-caller-input-ulaw.ul";

    var poll = function() {
        $http.get('/ws/sipcall/poll?timeoutMs=30000').
            success(function(data) {
                if ($scope.initialized && !$scope.inShutdown) {
                    if (data.response != "TIMEOUT") {
                        console.log("poll success, response=" + data.response + ", callId=" + data.callId);
                        setSipCallReason(data.callId, data.response, data.reason, data.code);
                        if (data.response == "INVITE_OK") {
                            if ($scope.recordingDirectory) {
                                $scope.record(data.callId, $scope.recordingDirectory + "in-" + data.callId + ".ul");
                            }
                            if ($scope.playFile) {
                                $scope.play(data.callId, $scope.playFile);
                            }
                        }
                    }

                    poll();
                }
            }).
            error(function(data, status) {
                $scope.errorMessage = "poll failed, status=" + status;
            });
    };
    
    var setSipCallReason = function(callId, response, reason, code) {
        var c = findSipCall(callId);
        if (c) {
            c.response = response;
            c.reason = reason;
            c.code = code;
        }
    };
    
    var findSipCall = function(callId) {
        for (var i = 0; i < $scope.sipcalls.length; i++) {
            if ($scope.sipcalls[i].callId == callId) {
                return $scope.sipcalls[i];
            }
        }
    };

    $scope.init = function() {
        $scope.inInit = true;
        var initParams = {
            config : $scope.config
        };
        $scope.errorMessage = "";
        $http.post('/ws/sipcall/init', initParams).
            success(function(data) {
                $scope.inInit = false;
                $scope.initialized = true;
                poll();
            }).
            error(function(data, status) {
                $scope.errorMessage = "init failed, status=" + status;
            });
    };

    $scope.shutdown = function() {
        $scope.inShutdown = true;
        $http.post('/ws/sipcall/shutdown').
            success(function() {
                $scope.inShutdown = false;
                $scope.initialized = false;
                $scope.sipcalls = [];
            });
    };

    $scope.sendInvite = function() {
        var sendInviteParams = {
            fromUser : $scope.fromUser,
            to : $scope.to,
            sessionId : $scope.sessionId
        };
        var command;
        if ($scope.outboundTo) {
            command = '/ws/sipcall/sendInviteForOutbound';
            sendInviteParams.outboundTo = $scope.outboundTo;
        } else {
            command = '/ws/sipcall/sendInvite';
        }
        $http.post(command, sendInviteParams).
            success(function(data) {
                $scope.sipcalls.unshift({
                    callId : data.callId,
                    response : "sending INVITE",
                    fromUser : $scope.fromUser,
                    to : $scope.to,
                    recordFileName : ($scope.recordingDirectory ? $scope.recordingDirectory + "in-" + data.callId + ".ul" : ""),
                    playFileName : ($scope.playFile ? $scope.playFile : "")
                });
                if ($scope.recordingDirectory) {
                    $scope.record(data.callId, $scope.recordingDirectory + "in-" + data.callId + ".ul");
                }
                if ($scope.playFile) {
                    $scope.play(data.callId, $scope.playFile);
                }
            });
    };

    $scope.waitForInvite = function() {
        var waitForInviteParams = {
        };
        $http.post('/ws/sipcall/waitForInvite', waitForInviteParams).
            success(function(data) {
                $scope.sipcalls.unshift({
                    callId : data.callId,
                    response : "waiting for INVITE",
                    fromUser : "",
                    to : "",
                    recordFileName : ($scope.recordingDirectory ? $scope.recordingDirectory + "in-" + data.callId + ".ul" : ""),
                    playFileName : ($scope.playFile ? $scope.playFile : "")
                });
            });
    };

    $scope.sendBye = function(callId) {
        var sendByeParams = {
            callId : callId
        };
        setSipCallReason(callId, "sending BYE");
        $http.post('/ws/sipcall/sendBye', sendByeParams).
            success(function(data) {
            });
    };

    $scope.record = function(callId, fileName) {
        var sendRecordParams = {
            callId : callId,
            fileName : fileName
        };
        $http.post('/ws/sipcall/record', sendRecordParams).
            success(function(data) {
            });
    };

    $scope.play = function(callId, fileName) {
        var sendPlayParams = {
            callId : callId,
            fileName : fileName
        };
        setSipCallReason(callId, "playing");
        $http.post('/ws/sipcall/play', sendPlayParams).
            success(function(data) {
            });
    };

    $scope.sendDtmf = function(callId, dtmf) {
        var sendDtmfParams = {
            callId : callId,
            dtmf : dtmf
        };
        setSipCallReason(callId, "dtmf");
        $http.post('/ws/sipcall/sendDtmf', sendDtmfParams).
            success(function(data) {
            });
    };

    $scope.isCallActive = function(sipcall) {
        return sipcall.response && sipcall.response != 'CALL_FINISHED';
    };
    
    $scope.isCallFinished = function(sipcall) {
        return sipcall.response == 'CALL_FINISHED';
    };
    
    $scope.remove = function(callId) {
        var callIndex = -1;
        for (var i = 0; i < $scope.sipcalls.length; i++) {
            if ($scope.sipcalls[i].callId == callId) {
                callIndex = i;
            }
        }
        if (callIndex > -1) {
            $scope.sipcalls.splice(callIndex, 1);
        }
    };

}
