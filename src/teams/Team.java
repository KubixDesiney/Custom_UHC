package teams;

public class Team {
    private String name;
    private String color;
    private int size;

    public Team(String name, String color, int size) {
        this.name = name;
        this.color = color;
        this.size = size;
    }

    public String getName() {
        return name;
    }

    public String getColor() {
        return color;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }
}
