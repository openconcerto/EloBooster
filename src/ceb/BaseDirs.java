package ceb;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import ceb.BaseDirs.GroupAndOthers;

public abstract class BaseDirs {

    private static final String DATA = "Data";
    private static final String PREFERENCES = "Preferences";
    private static final String CACHES = "Caches";

    /**
     * Return the first value that is non-empty.
     *
     * @param values values to test for emptiness.
     * @return the first value that is neither <code>null</code> nor {@link String#isEmpty() empty},
     *         <code>null</code> if none.
     */
    public static String coalesce(final String... values) {
        return coalesce(false, values);
    }

    public static boolean isEmpty(final String s, final boolean trim) {
        return s == null || (trim ? s.trim() : s).isEmpty();
    }

    public static String coalesce(final boolean trim, final String... values) {
        for (final String s : values) {
            if (!isEmpty(s, trim))
                return s;
        }
        return null;
    }

    // https://specifications.freedesktop.org/basedir-spec/basedir-spec-latest.html
    public static final class XDG extends BaseDirs {
        protected XDG(final String info, final String subdir) {
            super(info, subdir);
        }

        private String getHomePath() {
            // ATTN on FreeBSD man 8 service : "service command sets HOME to /" thus one should
            // probably not use this class (perhaps Portable).
            return coalesce(System.getenv("HOME"), System.getProperty("user.home"));
        }

        private String getInHomePath(final String subPath) {
            final String homePath = getHomePath();
            // If no home, use current working directory
            return homePath == null ? subPath : homePath + '/' + subPath;
        }

        @Override
        protected File _getAppDataFolder() {
            /*
             * $XDG_DATA_HOME defines the base directory relative to which user specific data files
             * should be stored. If $XDG_DATA_HOME is either not set or empty, a default equal to
             * $HOME/.local/share should be used.
             */
            return new File(coalesce(System.getenv("XDG_DATA_HOME"), getInHomePath(".local/share")), this.getAppID());
        }

        @Override
        protected File _getPreferencesFolder() {
            /*
             * $XDG_CONFIG_HOME defines the base directory relative to which user specific
             * configuration files should be stored. If $XDG_CONFIG_HOME is either not set or empty,
             * a default equal to $HOME/.config should be used.
             */
            return new File(coalesce(System.getenv("XDG_CONFIG_HOME"), getInHomePath(".config")), this.getAppID());
        }

        @Override
        protected File _getCacheFolder() {
            /*
             * $XDG_CACHE_HOME defines the base directory relative to which user specific
             * non-essential data files should be stored. If $XDG_CACHE_HOME is either not set or
             * empty, a default equal to $HOME/.cache should be used.
             */
            return new File(coalesce(System.getenv("XDG_CACHE_HOME"), getInHomePath(".cache")), this.getAppID());
        }
    }

    public static final class Unknown extends BaseDirs {

        protected Unknown(final String info, final String subdir) {
            super(info, subdir);
        }

    }

    public static final class Windows extends BaseDirs {
        private final String path;

        protected Windows(final String info, final String subdir) {
            super(info, subdir);
            final String appID = this.getAppName();
            // handle missing org and avoid OpenConcerto/OpenConcerto
            this.path = appID;
            // ProductInfo test emptiness
            assert this.path.charAt(0) != File.separatorChar && this.path.charAt(this.path.length() - 1) != File.separatorChar : "Separator not in between : " + this.path;
        }

        protected final String getPath() {
            return this.path;
        }

        @Override
        protected File _getAppDataFolder() {
            // do not use LOCALAPPDATA as the user needs its data synchronised
            return new File(System.getenv("APPDATA"), this.getPath() + File.separatorChar + DATA);
        }

        @Override
        protected File _getPreferencesFolder() {
            // do not use LOCALAPPDATA as configuration should be small enough to be synchronised on
            // the network
            return new File(System.getenv("APPDATA"), this.getPath() + File.separatorChar + PREFERENCES);
        }

        @Override
        protected File _getCacheFolder() {
            // use LOCALAPPDATA as caches can be quite big and don't need to be synchronised
            return new File(System.getenv("LOCALAPPDATA"), this.getPath() + File.separatorChar + CACHES);
        }
    }

    // https://developer.apple.com/library/mac/qa/qa1170/_index.html
    public static final class Mac extends BaseDirs {

        protected Mac(final String info, final String subdir) {
            super(info, subdir);
        }

        @Override
        protected File _getAppDataFolder() {
            // NOTE : "Application Support" directory is reserved for non-essential application
            // resources
            return new File(System.getProperty("user.home") + "/Library/" + this.getAppName());
        }

        @Override
        protected File _getPreferencesFolder() {
            return new File(System.getProperty("user.home") + "/Library/Preferences/" + this.getAppName());
        }

        @Override
        protected File _getCacheFolder() {
            return new File(System.getProperty("user.home") + "/Library/Caches/" + this.getAppName());
        }
    }

    public static final class Portable extends BaseDirs {

        private final File rootDir;

        protected Portable(final File rootDir, final String info, final String subdir) {
            super(info, subdir);
            this.rootDir = rootDir;
        }

        public final File getRootDir() {
            return this.rootDir;
        }

        @Override
        protected File _getAppDataFolder() {
            return new File(this.getRootDir(), DATA);
        }

        @Override
        protected File _getPreferencesFolder() {
            return new File(this.getRootDir(), PREFERENCES);
        }

        @Override
        protected File _getCacheFolder() {
            return new File(this.getRootDir(), CACHES);
        }
    }

    public static final BaseDirs createPortable(final File rootDir, final String info, final String subdir) {
        return new Portable(rootDir, info, subdir);
    }

    public static final BaseDirs create(final String info) {
        return create(info, null);
    }

    public static final BaseDirs create(final String info, final String subdir) {
        final OSFamily os = OSFamily.getInstance();
        if (os == OSFamily.Windows)
            return new Windows(info, subdir);
        else if (os == OSFamily.Mac)
            return new Mac(info, subdir);
        else if (os instanceof OSFamily.Unix)
            return new XDG(info, subdir);
        else
            return new Unknown(info, subdir);
    }

    private final String info;
    private final String subdir;

    protected BaseDirs(final String info, final String subdir) {
        this.info = info;
        this.subdir = subdir == null ? null : subdir;
    }

    // should use other methods to avoid invalid characters
    private final String getInfo() {
        return this.info;
    }

    protected final String getAppName() {
        return this.getInfo();
    }

    protected final String getAppID() {
        return "1";
    }

    public static final File getFolderToWrite(final File dir) throws IOException {
        // MAYBE test for symlink
        if (dir.isDirectory() && dir.canWrite())
            return dir;
        if (dir.exists())
            throw new IOException((dir.isDirectory() ? "Not writable: " : "Not a directory: ") + dir);
        // create with 0700 mode (from § Referencing this specification)
        final String perms = "rwx------";
        try {
            Files.createDirectories(dir.toPath(), PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString(perms)));
        } catch (final UnsupportedOperationException e) {
            // e.g. this is Windows
            Files.createDirectories(dir.toPath());
            setFilePermissionsFromPOSIX(dir, perms);
        }
        return dir;
    }

    protected final File getSubDir(final File dir) {
        return this.subdir == null ? dir : new File(dir, this.subdir);
    }

    protected File _getAppDataFolder() {
        return new File(System.getProperty("user.home"), "." + this.getAppName());
    }

    // where to write user-hidden data files (e.g. mbox files, DB files)
    public final File getAppDataFolder() {
        return getSubDir(_getAppDataFolder());
    }

    public final File getAppDataFolderToWrite() throws IOException {
        return getFolderToWrite(this.getAppDataFolder());
    }

    protected File _getPreferencesFolder() {
        return this._getAppDataFolder();
    }

    // where to write configuration
    public final File getPreferencesFolder() {
        return getSubDir(_getPreferencesFolder());
    }

    public final File getPreferencesFolderToWrite() throws IOException {
        return getFolderToWrite(this.getPreferencesFolder());
    }

    protected File _getCacheFolder() {
        return new File(System.getProperty("java.io.tmpdir"), this.getAppName());
    }

    // where to write data that can be re-created
    public final File getCacheFolder() {
        return getSubDir(_getCacheFolder());
    }

    public final File getCacheFolderToWrite() throws IOException {
        return getFolderToWrite(this.getCacheFolder());
    }

    protected File _getStateFolder() {
        return this._getCacheFolder();
    }

    // where to write data that is non-essential but cannot be recreated
    // - logfiles
    // - state of application windows on exit
    // - recently opened files
    // See STATE directory in https://wiki.debian.org/XDGBaseDirectorySpecification
    public final File getStateFolder() {
        return getSubDir(_getStateFolder());
    }

    public final File getStateFolderToWrite() throws IOException {
        return getFolderToWrite(this.getStateFolder());
    }

    @Override
    public String toString() {
        return BaseDirs.class.getSimpleName() + " " + this.getClass().getSimpleName();
    }

    /**
     * How to merge the group and others portion of permissions for non-POSIX FS.
     *
     * @author sylvain
     * @see FileUtils#setFilePermissionsFromPOSIX(File, String, GroupAndOthers)
     */
    public enum GroupAndOthers {
        REQUIRE_SAME {
            @Override
            protected Set<Permission> getNonEqual(final Set<Permission> groupPerms, final Set<Permission> otherPerms) {
                throw new IllegalArgumentException("Different permissions : " + groupPerms + " != " + otherPerms);
            }
        },
        PERMISSIVE {
            @Override
            protected Set<Permission> getNonEqual(final Set<Permission> groupPerms, final Set<Permission> otherPerms) {
                final EnumSet<Permission> res = EnumSet.noneOf(Permission.class);
                res.addAll(groupPerms);
                res.addAll(otherPerms);
                return res;
            }
        },
        OTHERS {
            @Override
            protected Set<Permission> getNonEqual(final Set<Permission> groupPerms, final Set<Permission> otherPerms) {
                return otherPerms;
            }
        },
        RESTRICTIVE {
            @Override
            protected Set<Permission> getNonEqual(final Set<Permission> groupPerms, final Set<Permission> otherPerms) {
                final EnumSet<Permission> res = EnumSet.allOf(Permission.class);
                res.retainAll(groupPerms);
                res.retainAll(otherPerms);
                return res;
            }
        };

        public final Set<Permission> getPermissions(final Set<Permission> groupPerms, final Set<Permission> otherPerms) {
            if (groupPerms.equals(otherPerms)) {
                return groupPerms;
            } else {
                return getNonEqual(groupPerms, otherPerms);
            }
        }

        public final Set<Permission> getPermissions(final String posixPerms) {
            final Set<Permission> groupPerms = Permission.fromString(posixPerms.substring(3, 6));
            final Set<Permission> otherPerms = Permission.fromString(posixPerms.substring(6, 9));
            return this.getPermissions(groupPerms, otherPerms);
        }

        protected abstract Set<Permission> getNonEqual(final Set<Permission> groupPerms, final Set<Permission> otherPerms);

    }

    public static final String setPermissions(final Path p, final String posixPerms) throws IOException {
        return setPermissions(p, posixPerms, GroupAndOthers.RESTRICTIVE);
    }

    /**
     * Use {@link PosixFileAttributeView#setPermissions(Set)} if possible, otherwise use
     * {@link #setFilePermissionsFromPOSIX(File, String, GroupAndOthers)}.
     *
     * @param p the path to change.
     * @param posixPerms the new permissions to apply.
     * @param groupAndOthers only for non-POSIX FS, how to merge group and others portion.
     * @return the permission applied, 9 characters for POSIX, 6 for non-POSIX (i.e. 3 for owner, 3
     *         for the rest), <code>null</code> if some permissions couldn't be applied (only on
     *         non-POSIX).
     * @throws IOException if permissions couldn't be applied.
     */
    public static final String setPermissions(final Path p, final String posixPerms, final GroupAndOthers groupAndOthers) throws IOException {
        final String res;
        final PosixFileAttributeView view = Files.getFileAttributeView(p, PosixFileAttributeView.class);
        if (view != null) {
            view.setPermissions(PosixFilePermissions.fromString(posixPerms));
            res = posixPerms;
        } else {
            // final Set<Permission> notOwnerPerms = setFilePermissions(p.toFile(), pfp,
            // groupAndOthers);
            final Set<Permission> notOwnerPerms = setFilePermissionsFromPOSIX(p.toFile(), posixPerms, groupAndOthers);
            res = notOwnerPerms == null ? null : posixPerms.substring(0, 3) + Permission.get3chars(notOwnerPerms);
        }
        return res;
    }

    public static final Set<Permission> setFilePermissionsFromPOSIX(final File f, final String posixPerms) {
        return setFilePermissionsFromPOSIX(f, posixPerms, GroupAndOthers.RESTRICTIVE);
    }

    public static final Set<Permission> setFilePermissionsFromPOSIX(final File f, final String posixPerms, final GroupAndOthers groupAndOthers) {
        if (posixPerms.length() != 9)
            throw new IllegalArgumentException("Invalid mode : " + posixPerms);
        final Set<Permission> ownerPerms = Permission.fromString(posixPerms.substring(0, 3));
        final Set<Permission> notOwnerPerms = groupAndOthers.getPermissions(posixPerms);
        assert notOwnerPerms != null;
        final boolean success = setFilePermissions(f, ownerPerms, notOwnerPerms);
        return success ? notOwnerPerms : null;
    }

    /**
     * Use {@link File} methods to set permissions. This works everywhere but group and others are
     * treated as the same.
     *
     * @param f the file to change.
     * @param owner the permissions for the owner.
     * @param notOwner the permissions for not the owner.
     * @return <code>true</code> if all asked permissions were set.
     * @see File#setReadable(boolean, boolean)
     * @see File#setWritable(boolean, boolean)
     * @see File#setExecutable(boolean, boolean)
     */
    public static final boolean setFilePermissions(final File f, final Set<Permission> owner, final Set<Permission> notOwner) {
        boolean res = setFilePermissions(f, notOwner, false);
        if (!owner.equals(notOwner)) {
            res &= setFilePermissions(f, owner, true);
        }
        return res;
    }

    public static final boolean setFilePermissions(final File f, final Set<Permission> perms, final boolean ownerOnly) {
        boolean res = f.setReadable(perms.contains(Permission.READ), ownerOnly);
        res &= f.setWritable(perms.contains(Permission.WRITE), ownerOnly);
        res &= f.setExecutable(perms.contains(Permission.EXECUTE), ownerOnly);
        return res;
    }

    public enum Permission {
        READ, WRITE, EXECUTE;

        public static final Permission R = READ;
        public static final Permission W = WRITE;
        public static final Permission X = EXECUTE;
        protected static final Map<String, Set<Permission>> FROM_STRING = new HashMap<>();
        public static final Pattern MINUS_PATTERN = Pattern.compile("-+");
        static {
            putString("---", Collections.<Permission> emptySet());
            putString("--x", Collections.singleton(EXECUTE));
            putString("-w-", Collections.singleton(WRITE));
            putString("-wx", EnumSet.of(WRITE, EXECUTE));
            putString("r--", Collections.singleton(READ));
            putString("r-x", EnumSet.of(READ, EXECUTE));
            putString("rw-", EnumSet.of(READ, WRITE));
            putString("rwx", EnumSet.allOf(Permission.class));
        }

        private static final void putString(final String str, final EnumSet<Permission> set) {
            putString(str, Collections.unmodifiableSet(set));
        }

        private static final void putString(final String str, final Set<Permission> unmodifiableSet) {
            FROM_STRING.put(str, unmodifiableSet);
            FROM_STRING.put(MINUS_PATTERN.matcher(str).replaceAll(""), unmodifiableSet);
        }

        public static final Set<Permission> fromString(final String str) {
            final Set<Permission> res = FROM_STRING.get(str);
            if (res == null)
                throw new IllegalArgumentException("Invalid string : " + str);
            return res;
        }

        public static final String get3chars(final Set<Permission> perms) {
            return get3chars(perms.contains(READ), perms.contains(WRITE), perms.contains(EXECUTE));
        }

        private static final String get3chars(final boolean read, final boolean write, final boolean exec) {
            final StringBuilder sb = new StringBuilder(3);
            sb.append(read ? 'r' : '-');
            sb.append(write ? 'w' : '-');
            sb.append(exec ? 'x' : '-');
            return sb.toString();
        }
    }

    public static void main(final String[] args) throws IOException {

        final BaseDirs instance = create("test");
        System.out.println(instance);
        System.out.println("app data : " + instance.getAppDataFolder());
        System.out.println("preferences : " + instance.getPreferencesFolder());
        System.out.println("cache : " + instance.getCacheFolder());
        // test creation and permission
        if (Boolean.getBoolean("createCacheDir")) {
            final File createdCache = instance.getCacheFolderToWrite();
            System.out.println("cache dir created : " + createdCache);
            if (Boolean.getBoolean("deleteCacheDir")) {
                Files.delete(createdCache.toPath());
                System.out.println("cache dir deleted");
            }
        }
    }
}
