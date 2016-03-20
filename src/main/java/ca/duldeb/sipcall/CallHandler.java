package ca.duldeb.sipcall;

public interface CallHandler {
    void handleInviteOk(CallLegData leg);

    void handleInviteFailure(CallLegData leg, int code);

    void handleBye(CallLegData leg);

    void handleByeOk(CallLegData leg);

    void handleByeFailure(CallLegData leg, int code);

    void handlePauseDone(CallLegData leg);

    void handlePlayDone(CallLegData leg);

}
