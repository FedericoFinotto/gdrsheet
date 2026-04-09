package it.fin8.gdrsheet.service;

import org.springframework.stereotype.Component;

import java.util.Random;

/**
 * Utility class for handling dice rolls expressed as NdM (e.g. "30d12").
 */
@Component
public class DiceRoller {

    private static final Random RAND = new Random();

    /**
     * Parses a roll string and returns the result.
     *
     * @param roll       a string in the format NdM, where N is the number of dice
     *                   and M is the number of faces on each die (both > 0).
     * @param useAverage if true, returns the deterministic “average” value:
     *                   floor(M/2) * N  (as described in the request).
     *                   If false, performs a random roll and returns the sum.
     * @return the calculated or random total.
     * @throws IllegalArgumentException if the string does not match NdM or
     *                                  if N or M are not positive.
     */
    public static int roll(String roll, boolean useAverage) {
        if (roll == null || roll.isBlank()) {
            throw new IllegalArgumentException("Roll string must not be empty");
        }

        String[] parts = roll.toLowerCase().split("d");
        if (parts.length != 2) {
            throw new IllegalArgumentException(
                    "Invalid roll format. Expected NdM (e.g. 30d12), got: " + roll);
        }

        int numDice;
        int sides;
        try {
            numDice = Integer.parseInt(parts[0].trim());
            sides = Integer.parseInt(parts[1].trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "N and M must be positive integers. Roll: " + roll, e);
        }

        if (numDice <= 0 || sides <= 0) {
            throw new IllegalArgumentException(
                    "Number of dice and sides must be > 0. Got N=" + numDice
                            + ", M=" + sides);
        }

        if (useAverage) {
            // floor(sides/2) * numDice  (as per the request)
            int avgPerDie = sides / 2;          // integer division = floor
            return avgPerDie * numDice;
        } else {
            int total = 0;
            for (int i = 0; i < numDice; i++) {
                // nextInt(bound) returns [0, bound)
                total += RAND.nextInt(sides) + 1; // shift to [1, sides]
            }
            return total;
        }
    }

    // Simple demo
    public static void main(String[] args) {
        System.out.println("Random roll 30d12: " + roll("30d12", false));
        System.out.println("Average roll 30d12: " + roll("30d12", true));
    }
}