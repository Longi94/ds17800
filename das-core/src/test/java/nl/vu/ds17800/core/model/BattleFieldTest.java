package nl.vu.ds17800.core.model;

import nl.vu.ds17800.core.model.units.Dragon;
import nl.vu.ds17800.core.model.units.Player;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

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
                    "ID:   1 | TYPE:    DRAGON | COORDS:(21,12) | HEALTH: 62/ 62 | ATTACK: 18\n" +
                    "ID:   2 | TYPE:    PLAYER | COORDS:( 3, 3) | HEALTH: 19/ 19 | ATTACK:  1\n" +
                    "ID:   3 | TYPE:    PLAYER | COORDS:(10, 9) | HEALTH:  5/ 15 | ATTACK:  7\n";

    @Test
    public void testToString() throws Exception {
        BattleField battleField = new BattleField();

        Map<String, Object> action = new HashMap<>();

        action.put("request", MessageRequest.spawnUnit);
        action.put("x", 21);
        action.put("y", 12);
        action.put("unit", new Dragon(battleField.getNewUnitID(), 0, 0));

        battleField.apply(action);

        action.put("x", 3);
        action.put("y", 3);
        action.put("unit", new Player(battleField.getNewUnitID(), 0, 0));

        battleField.apply(action);

        action.put("x", 10);
        action.put("y", 9);
        action.put("unit", new Player(battleField.getNewUnitID(), 0, 0));

        battleField.apply(action);

        action.put("request", MessageRequest.dealDamage);
        action.put("damage", 10);

        battleField.apply(action);

        System.out.println(battleField);

        // TODO random fucks this up, need seed
        // Assert.assertEquals(TO_STRING_EXPECTED, battleField.toString());
    }

}