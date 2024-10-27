package ceb;

public abstract class OSFamily {

    static public class Unix extends OSFamily {
    }

    static public final Unix Mac = new Unix();
    static public final Unix Linux = new Unix();
    static public final Unix FreeBSD = new Unix();
    static public final OSFamily Windows = new OSFamily() {
    };

    static private final OSFamily INSTANCE = getCurrentOS();

    // perhaps create an OS class to which we pass the os.* properties
    static private final OSFamily getCurrentOS() {
        final String os = System.getProperty("os.name");
        if (os.startsWith("Windows")) {
            return Windows;
        } else if (os.startsWith("Mac OS")) {
            return Mac;
        } else if (os.startsWith("Linux")) {
            return Linux;
        } else if (os.startsWith("FreeBSD")) {
            return FreeBSD;
        } else {
            System.err.println("Unsupported OS " + os);
            return null;
        }
    }

    public final static OSFamily getInstance() {
        return INSTANCE;
    }
}
