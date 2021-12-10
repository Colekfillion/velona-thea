package ca.quadrexium.velonathea.pojo;

import java.util.ArrayList;

public class Constants {
    public static final ArrayList<String> IMAGE_EXTENSIONS = new ArrayList<String>() {{
        add(".webp");
        add(".png");
        add(".jpg");
        add(".jpeg");
        add(".bmp");
    }};

    public static final ArrayList<String> VIDEO_EXTENSIONS = new ArrayList<String>() {{
        add(".mp4");
        add(".mkv");
        add(".webm");
    }};
}
