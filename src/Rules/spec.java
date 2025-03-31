package Rules;

public class spec {
    private static boolean spect = true ; // Static to persist across all instances

    public static boolean getspect() {
        return spect;
    }

    public static void setspect(boolean status) { 
    	spect = status;
    }

}
