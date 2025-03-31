package gamemodes;

public class Gamestatus {
    private static int currentstatus = 0; // Static to persist across all instances

    public static int getStatus() {
        return currentstatus;
    }

    public static void setStatus(int newstatus) { 
        System.out.println("[DEBUG] Gamestatus set to: " + newstatus);
        currentstatus = newstatus;
    }
    private static int currentalive = 0;
    public static int getAlive() {
    	return currentalive;
    }
    public static void setAlive(int newalive) {
    	currentalive = newalive;
    }
}

