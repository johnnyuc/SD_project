package Server.Common;

import java.util.ArrayList;
import java.util.Collections;

public class VectorClock {
    private int id;
    private int[] values;
    private int dimensions;

    public VectorClock(int id, int dimensions) {
        this.id = id;
        this.dimensions = dimensions;

        this.values = new int[dimensions];

        for (int i = 0; i < dimensions; i++)
            this.values[i] = 0;
    }

    /**
     * Checks whether the time of the current clock is greater than the given clock
     * If for each component j, VT1[j] >= VT2[j],
     * and for some component k, VT1[k] > VT2[k], VT1 > VT2
     * 
     * @param vClock Clock to compare
     * @return whether the time of the current clock is greater than the given clock
     */
    public boolean greater(VectorClock vClock) {
        boolean hasGreater = false;
        for (int i = 0; i < this.dimensions; i++) {
            // If any value is lower, this clock is not greater
            if (this.values[i] < vClock.values[i])
                return false;
            // Atleast one value must be greater
            if (this.values[i] > vClock.values[i])
                hasGreater = true;
        }
        // If atleast one number was greater, returns true. Else returns false
        return hasGreater;
    }

    /**
     * Checks whether the time of the current clock is equal to the given clock
     * 
     * @param vClock
     * @return whether the time of the current clock is equal to the given clock
     */
    public boolean equal(VectorClock vClock) {
        for (int i = 0; i < this.dimensions; i++)
            // If any value is not equal, this clock is not equal
            if (this.values[i] != vClock.values[i])
                return false;

        return true;
    }

    /**
     * Update the current clock according to a message sender given clock
     * 
     * @param vClock clock from sender of message
     */
    public void update(VectorClock vClock) {
        // Increment current clock
        values[id]++;
        // And for every value in the given clock that is higher, update current clock
        // with it
        for (int i = 0; i < this.dimensions; i++)
            if (vClock.values[i] > values[i])
                values[i] = vClock.values[i];

    }
}
