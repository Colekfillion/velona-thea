package ca.colekfillion.velonathea.pojo;

import java.util.LinkedHashSet;
import java.util.Set;

public class Filter {
    private String type;
    private boolean include;
    private boolean isOr;
    private Set<String> args = new LinkedHashSet<>();

    public Filter(String type, boolean include, boolean isOr, Set<String> args) {
        this.type = type;
        this.include = include;
        this.isOr = isOr;
        this.args = args;
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

    public Set<String> getArgs() {
        return args;
    }

    public void removeArg(String arg) {
        args.remove(arg);
    }

    public void addArg(String arg) {
        args.add(arg);
    }

    public boolean isOr() {
        return isOr;
    }

    public void setIsOr(boolean or) {
        isOr = or;
    }
}
