package gamemodes;

public class gamemode { // 0:MOLE 1:LG 3:CLASSIC
    private static int currentmode = -1; // Static to persist across all instances

    public static int getMode() {
        
        return currentmode;
    }

    public static void setMode(int n) { 
        System.out.println("[DEBUG] Gamemode set to: " + n);
        currentmode = n;
    }
}
