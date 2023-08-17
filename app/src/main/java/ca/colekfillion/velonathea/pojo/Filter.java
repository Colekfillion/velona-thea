package ca.colekfillion.velonathea.pojo;

public class Filter {
    private String type;
    private boolean include;
    private boolean isOr;
    private String arg;
    public Filter(String type, boolean include, boolean isOr, String arg) {
        this.type = type;
        this.include = include;
        this.arg = arg;
    }

    public String getType() {
        return type;
    }

    public boolean isInclude() {
        return include;
    }

    public void setInclude(boolean include) {
        this.include = include;
    }

    public String getArg() {
        return arg;
    }

    public boolean isOr() {
        return isOr;
    }

    public void setIsOr(boolean or) {
        isOr = or;
    }
}
