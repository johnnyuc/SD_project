package Server.IndexStorageBarrel.Objects;

public record SearchData(String url, String title, String description, double tfIdf, int refCount) {
}