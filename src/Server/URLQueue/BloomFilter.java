package Server.URLQueue;

import java.util.BitSet;

/**
 * The BloomFilter class represents a probabilistic data structure used to test
 * whether an element is a member of a set.
 * It uses multiple hash functions and a BitSet to store the data.
 */
public class BloomFilter {
    // Seeds for the hash functions
    private static final int[] seeds = new int[] { 5, 7, 11, 13, 31, 37, 61 };

    // BitSet to store the Bloom filter's data
    private final BitSet bits;

    // Array of hash functions
    private final SimpleHash[] func = new SimpleHash[seeds.length];

    /**
     * Constructs a BloomFilter object with the specified size.
     * 
     * @param size The size of the Bloom filter.
     */
    public BloomFilter(int size) {
        bits = new BitSet(size);
        for (int i = 0; i < seeds.length; i++) {
            func[i] = new SimpleHash(size, seeds[i]);
        }
    }

    /**
     * Adds an element to the Bloom filter.
     * 
     * @param value The value to be added.
     */
    public void add(String value) {
        for (SimpleHash f : func)
            bits.set(f.hash(value), true);
    }

    /**
     * Checks if an element is in the Bloom filter.
     * 
     * @param value The value to be checked.
     * @return true if the value is possibly in the Bloom filter, false otherwise.
     */
    public boolean contains(String value) {
        // If the value is null, it means it's not in the Bloom filter
        if (value == null)
            return false;
        boolean ret = true;
        // Check if the hash value of the element is in the BitSet
        for (SimpleHash f : func)
            ret = ret && bits.get(f.hash(value));
        return ret;
    }

    /**
     * The SimpleHash class represents a hash function used by the Bloom filter.
     */
    public static class SimpleHash {
        // Capacity and seed for the hash function
        private final int cap;
        private final int seed;

        /**
         * Constructs a SimpleHash object with the specified capacity and seed.
         * 
         * @param cap  The capacity of the hash function.
         * @param seed The seed of the hash function.
         */
        public SimpleHash(int cap, int seed) {
            this.cap = cap;
            this.seed = seed;
        }

        /**
         * Calculates the hash value of a string.
         * 
         * @param value The string to be hashed.
         * @return The hash value of the string.
         */
        public int hash(String value) {
            int result = 0;
            int len = value.length();
            // Horner's method to calculate the hash value
            for (int i = 0; i < len; i++)
                result = seed * result + value.charAt(i);
            return Math.abs(result % cap);
        }
    }
}