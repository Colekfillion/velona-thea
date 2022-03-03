package ca.quadrexium.velonathea.python;

import java.util.Arrays;

public class ImageResult {
    private String title;
    private String author;
    private float similarity;
    private String[] sources;
    private int indexId;
    private String[] tags;

    public ImageResult(String title, String author, float similarity,
                       String[] sources, int indexId) {
        this.title = title;
        this.author = author;
        this.similarity = similarity;
        this.sources = sources;
        this.indexId = indexId;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public float getSimilarity() {
        return similarity;
    }

    public String[] getSources() {
        return sources;
    }

    public int getIndexId() {
        return indexId;
    }

    public String[] getTags() {
        return tags;
    }

    @Override
    public String toString() {
        return "ImageResult{" +
                "title='" + title + '\'' +
                ", author='" + author + '\'' +
                ", similarity=" + similarity +
                ", sources=" + Arrays.toString(sources) +
                ", indexId=" + indexId +
                '}';
    }

    public void setTags(String[] tags) {
        this.tags = tags;
    }
}
