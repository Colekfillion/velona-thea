package ca.colekfillion.velonathea.pojo;

import androidx.annotation.NonNull;

import java.util.Set;

public class Media {
    protected int id;
    protected String name;
    protected String filePath;
    protected String author;
    protected String link;
    protected Set<String> tags;

    public Media(Media.Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.filePath = builder.filePath;
        this.author = builder.author;
        this.link = builder.link;
        this.tags = builder.tags;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getFileName() { return filePath.substring(filePath.lastIndexOf("/")+1); }
    public String getFilePath() { return filePath; }
    public String getAuthor() { return author; }
    public String getLink() { return link; }
    public Set<String> getTags() { return tags; }
    public String getTagsAsString() {
        if (tags == null) { return ""; }
        StringBuilder sTags = new StringBuilder();
        for (String tag : tags) {
            sTags.append(tag).append(" ");
        }
        sTags = new StringBuilder(sTags.substring(0, sTags.length()));
        return sTags.toString();
    }

    public void setId(int id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public void setAuthor(String author) { this.author = author; }
    public void setLink(String link) { this.link = link; }
    public void setTags(@NonNull Set<String> tags) { this.tags = tags; }

    public void addTag(String tag) { tags.add(tag); }

    public void removeTag(String tag) { tags.remove(tag); }

    public static class Builder {
        private int id = 0;
        private String name;
        private String filePath;
        private String author;
        private String link;
        private Set<String> tags;

        public Builder id(int id) {
            this.id = id;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder filePath(String filePath) {
            this.filePath = filePath;
            return this;
        }

        public Builder author(String author) {
            this.author = author;
            return this;
        }

        public Builder link(String link) {
            this.link = link;
            return this;
        }

        public Builder tags(Set<String> tags) {
            this.tags = tags;
            return this;
        }

        private void validate(Media media) {
            if (media.id == 0) {
                throw new IllegalStateException("Media must have an ID");
            }
            if (media.filePath == null) {
                throw new IllegalStateException("Media must have a file pointer");
            }
        }

        public Media build() {
            Media media = new Media(this);
            validate(media);
            return media;
        }
    }

    @NonNull
    @Override
    public String toString() {
        return "Media{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", filePath='" + filePath + '\'' +
                ", author='" + author + '\'' +
                ", link='" + link + '\'' +
                ", tags=" + tags +
                '}';
    }
}
