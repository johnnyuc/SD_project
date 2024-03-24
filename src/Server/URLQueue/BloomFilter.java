package Server.URLQueue;

import java.util.BitSet;

public class BloomFilter {
    // Seeds for the hash functions
    private static final int[] seeds = new int[]{5, 7, 11, 13, 31, 37, 61};

    // BitSet to store the Bloom filter's data
    private final BitSet bits;

    // Array of hash functions
    private final SimpleHash[] func = new SimpleHash[seeds.length];

    // Constructor that initializes the BitSet and hash functions
    public BloomFilter(int size) {
        bits = new BitSet(size);
        for (int i = 0; i < seeds.length; i++) {
            func[i] = new SimpleHash(size, seeds[i]);
        }
    }

    // Method to add an element to the Bloom filter
    public void add(String value) {
        for (SimpleHash f : func)
            bits.set(f.hash(value), true);
    }

    // Method to check if an element is in the Bloom filter
    public boolean contains(String value) {
        // If the value is null, it means it's not in the Bloom filter
        if (value == null) return false;
        boolean ret = true;
        // Check if the hash value of the element is in the BitSet
        for (SimpleHash f : func)
            ret = ret && bits.get(f.hash(value));
        return ret;
    }

    // Inner class for the hash functions
    public static class SimpleHash {
        // Capacity and seed for the hash function
        private final int cap;
        private final int seed;

        // Constructor that initializes the capacity and seed of the hash function
        public SimpleHash(int cap, int seed) {
            this.cap = cap;
            this.seed = seed;
        }

        // Method to calculate the hash value of a string
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