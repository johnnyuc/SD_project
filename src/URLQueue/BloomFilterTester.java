package URLQueue;

public class BloomFilterTester {
    public static void main(String[] args) {
        BloomFilter bloomFilter = new BloomFilter(9585058);

        // Add some elements to the filter
        bloomFilter.add("https://example1.com");
        bloomFilter.add("https://example2.com");
        bloomFilter.add("https://example3.com");

        // Check if the elements are in the filter
        System.out.println(bloomFilter.contains("https://example1.com")); // Should print: true
        System.out.println(bloomFilter.contains("https://example2.com")); // Should print: true
        System.out.println(bloomFilter.contains("https://example3.com")); // Should print: true

        // Check for an element that was not added to the filter
        System.out.println(bloomFilter.contains("https://notadded.com")); // Should print: false
    }
}