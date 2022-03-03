package ca.quadrexium.velonathea.python;

import java.util.Arrays;

public class ResultContainer {
    private ImageResult[] imageResults;
    private int shortRemaining;
    private int longRemaining;

    public ResultContainer(ImageResult[] imageResults, int shortRemaining, int longRemaining) {
        this.imageResults = imageResults;
        this.shortRemaining = shortRemaining;
        this.longRemaining = longRemaining;
    }

    public ImageResult[] getImageResults() {
        return imageResults;
    }

    public int getShortRemaining() {
        return shortRemaining;
    }

    public int getLongRemaining() {
        return longRemaining;
    }

    @Override
    public String toString() {
        return "ResultContainer{" +
                "imageResults=" + Arrays.toString(imageResults) +
                ", shortRemaining=" + shortRemaining +
                ", longRemaining=" + longRemaining +
                '}';
    }
}
