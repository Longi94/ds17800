package nl.vu.ds17800.core.model;

/**
 * Different request types for the
 * nodes to send to the server.
 *
 * @author Pieter Anemaet, Boaz Pat-El
 */
public enum MessageRequest {
    SPAWN_UNIT,
    GET_UNIT,
    MOVE_UNIT,
    PUT_UNIT,
    REMOVE_UNIT,
    GET_TYPE,
    DEAL_DAMAGE,
    HEAL_DAMAGE
}
