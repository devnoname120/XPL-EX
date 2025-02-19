package eu.faircode.xlua.api.xlua.call;

import android.content.Context;
import android.os.Bundle;

import eu.faircode.xlua.XGlobalCore;
import eu.faircode.xlua.api.XProxyContent;
import eu.faircode.xlua.api.standard.CallCommandHandler;
import eu.faircode.xlua.api.standard.command.CallPacket;
import eu.faircode.xlua.utilities.BundleUtil;

public class GetGroupsCommand extends CallCommandHandler {
    public static GetGroupsCommand create() { return new GetGroupsCommand(); }
    public GetGroupsCommand() {
        name = "getGroups";
        requiresPermissionCheck = false;
    }

    @Override
    public Bundle handle(CallPacket commandData) throws Throwable {
        throwOnPermissionCheck(commandData.getContext());
        return BundleUtil.
                createFromStringArray(
                        "groups",
                        XGlobalCore.getGroups(commandData.getDatabase()));
    }

    public static Bundle invoke(Context context) {
        return XProxyContent.luaCall(
                context,
                "getGroups");
    }
}
