package adris.altoclef.dpc;

import adris.altoclef.AltoClef;
import adris.altoclef.util.helpers.ConfigHelper;
import net.dv8tion.jda.api.entities.Member;

public class UserAuth {
    private static final String BLACKLIST_PATH = "altoclef_dpc_blacklist.txt";
    private static final String WHITELIST_PATH = "altoclef_dpc_whitelist.txt";
    private final AltoClef _mod;
    private UserListFile _blacklist;
    private UserListFile _whitelist;

    public UserAuth(AltoClef mod) {
        _mod = mod;

        ConfigHelper.ensureCommentedListFileExists(BLACKLIST_PATH, """
                Add Dpc blacklisted players here.
                Make sure useDpcBlacklist is set to true in the settings file.
                Anything after a pound sign (#) will be ignored.""");
        ConfigHelper.ensureCommentedListFileExists(WHITELIST_PATH, """
                Add Dpc whitelisted players here.
                Make sure useDpcWhitelist is set to true in the settings file.
                Anything after a pound sign (#) will be ignored.""");

        UserListFile.load(BLACKLIST_PATH, newList -> _blacklist = newList);
        UserListFile.load(WHITELIST_PATH, newList -> _whitelist = newList);
    }

    public boolean isUserAuthorized(Member member) {

        String name = member.getId().toString();

        // Blacklist gets first priority.
        if (DpcConfig.getInstance().useDpcBlacklist && _blacklist.containsUser(name)) {
            return false;
        }
        if (DpcConfig.getInstance().useDpcWhitelist) {
            return _whitelist.containsUser(name);
        }

        // By default accept everyone.
        return true;
    }

}
