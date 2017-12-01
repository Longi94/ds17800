package nl.vu.ds17800.core.model;

import nl.vu.ds17800.core.model.units.Dragon;
import nl.vu.ds17800.core.model.units.Player;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static org.junit.Assert.assertEquals;

/**
 * @author lngtr
 * @since 2017-12-01
 */
public class BattleFieldTest {

    private static final String TO_STRING_EXPECTED =
            ".........................\n" +
                    ".........................\n" +
                    ".........................\n" +
                    ".........................\n" +
                    ".........................\n" +
                    ".........................\n" +
                    ".........................\n" +
                    ".........................\n" +
                    ".........................\n" +
                    ".........................\n" +
                    ".........................\n" +
                    ".........................\n" +
                    ".....................1...\n" +
                    ".........................\n" +
                    ".........................\n" +
                    "..........3..............\n" +
                    ".........................\n" +
                    ".........................\n" +
                    ".........................\n" +
                    ".........................\n" +
                    ".........................\n" +
                    "...2.....................\n" +
                    ".........................\n" +
                    ".........................\n" +
                    ".........................\n" +
                    "ID:   1 | TYPE:    DRAGON | COORDS:(21,12) | HEALTH: 86/ 86 | ATTACK:  8\n" +
                    "ID:   2 | TYPE:    PLAYER | COORDS:( 3, 3) | HEALTH: 13/ 13 | ATTACK:  5\n" +
                    "ID:   3 | TYPE:    PLAYER | COORDS:(10, 9) | HEALTH:  4/ 14 | ATTACK:  3\n";

    @Test
    public void testToString() throws Exception {
        Random random = new Random(0L);
        BattleField battleField = new BattleField();

        Map<String, Object> action = new HashMap<>();

        action.put("request", MessageRequest.spawnUnit);
        action.put("x", 21);
        action.put("y", 12);
        action.put("unit", new Dragon(battleField.getNewUnitID(), 0, 0, random));

        battleField.apply(action);

        action.put("x", 3);
        action.put("y", 3);
        action.put("unit", new Player(battleField.getNewUnitID(), 0, 0, random));

        battleField.apply(action);

        action.put("x", 10);
        action.put("y", 9);
        action.put("unit", new Player(battleField.getNewUnitID(), 0, 0, random));

        battleField.apply(action);

        action.put("request", MessageRequest.dealDamage);
        action.put("damage", 10);

        battleField.apply(action);

        assertEquals(TO_STRING_EXPECTED, battleField.toString());
    }

}