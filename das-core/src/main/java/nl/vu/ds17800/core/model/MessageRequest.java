package nl.vu.ds17800.core.model;

/**
 * Different request types for the
 * nodes to send to the server.
 *
 * @author Pieter Anemaet, Boaz Pat-El
 */
public enum MessageRequest {
    // these messages require extra field `messageStage`, which is one of `ask`, `accept`, `reject` or `commit`
    spawnUnit, moveUnit, putUnit, removeUnit, dealDamage, healDamage,

    clientConnect,
    clientDisconnect,

    serverConnect,
    serverDisconnect,
}
