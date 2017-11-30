package nl.vu.ds17800.core.model;

/**
 * Different request types for the
 * nodes to send to the server.
 *
 * @author Pieter Anemaet, Boaz Pat-El
 */
public enum MessageRequest {
    spawnUnit, moveUnit, putUnit, removeUnit, dealDamage, healDamage,
}
