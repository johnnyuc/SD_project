package URLQueue;

import java.util.BitSet;

public class BloomFilter {
    // Seeds for the hash functions (using prime numbers)
    private static final int[] seeds = new int[]{5, 7, 11, 13, 31, 37, 61};

    // BitSet to store the Bloom filter's data
    private BitSet bits;

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
        for (SimpleHash f : func) {
            int hash = f.hash(value);
            if (hash >= bits.size()) {
                // Create a new BitSet with a larger size
                BitSet newBits = new BitSet(hash + 1);
                newBits.or(bits); // Copy the values from the old BitSet
                bits = newBits; // Replace the old BitSet with the new one
            }
            bits.set(hash, true);
        }
    }

    // Method to check if an element is in the Bloom filter
    public boolean contains(String value) {
        if (value == null) {
            return false;
        }
        boolean ret = true;
        for (SimpleHash f : func) {
            ret = ret && bits.get(f.hash(value));
        }
        return ret;
    }

    // Inner class for the hash functions
    public static class SimpleHash {
        private final int cap;
        private final int seed;

        // Constructor that initializes the capacity and seed of the hash function
        public SimpleHash(int cap, int seed) {
            this.cap = cap;
            this.seed = seed;
        }

        // Method to calculate the hash value of a string based on the seeds
        public int hash(String value) {
            int result = 0;
            int len = value.length();
            for (int i = 0; i < len; i++) {
                result = seed * result + value.charAt(i);
            }
            return Math.abs(result % cap);
        }
    }
}