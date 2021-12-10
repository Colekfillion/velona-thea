package ca.quadrexium.velonathea.pojo;

import android.os.Parcel;
import android.os.Parcelable;

public class Media implements Parcelable {
    protected int id;
    protected String name;
    protected String fileName;
    protected String author;
    protected String link;

    public Media(Media.Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.fileName = builder.fileName;
        this.author = builder.author;
        this.link = builder.link;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getFileName() { return fileName; }
    public String getAuthor() { return author; }
    public String getLink() { return link; }

    public void setId(int id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public void setAuthor(String author) { this.author = author; }
    public void setLink(String link) { this.link = link; }

    public static class Builder {
        private int id = 0;
        private String name;
        private String fileName;
        private String author = "unknown";
        private String link;

        public Builder id(int id) {
            this.id = id;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder fileName(String fileName) {
            this.fileName = fileName;
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

        private void validate(Media media) {
            if (media.id == 0) {
                throw new IllegalStateException("Media must have an ID");
            }
            if (media.fileName == null) {
                throw new IllegalStateException("Media must have a file name");
            }
        }

        public Media build() {
            Media media = new Media(this);
            validate(media);
            return media;
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.id);
        dest.writeString(this.name);
        dest.writeString(this.fileName);
        dest.writeString(this.author);
        dest.writeString(this.link);
    }

    public void readFromParcel(Parcel source) {
        this.id = source.readInt();
        this.name = source.readString();
        this.fileName = source.readString();
        this.author = source.readString();
        this.link = source.readString();
    }

    protected Media(Parcel in) {
        this.id = in.readInt();
        this.name = in.readString();
        this.fileName = in.readString();
        this.author = in.readString();
        this.link = in.readString();
    }

    public static final Parcelable.Creator<Media> CREATOR = new Parcelable.Creator<Media>() {
        @Override
        public Media createFromParcel(Parcel source) {
            return new Media(source);
        }

        @Override
        public Media[] newArray(int size) {
            return new Media[size];
        }
    };
}
