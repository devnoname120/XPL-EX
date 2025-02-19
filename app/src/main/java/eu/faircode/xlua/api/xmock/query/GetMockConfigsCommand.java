package eu.faircode.xlua.api.xmock.query;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import eu.faircode.xlua.api.XProxyContent;
import eu.faircode.xlua.api.standard.QueryCommandHandler;
import eu.faircode.xlua.api.standard.command.QueryPacket;
import eu.faircode.xlua.api.xmock.database.XMockConfigDatabase;
import eu.faircode.xlua.utilities.CursorUtil;

public class GetMockConfigsCommand extends QueryCommandHandler {
    public static GetMockConfigsCommand create(boolean marshall) { return new GetMockConfigsCommand(marshall); };

    private boolean marshall;
    public GetMockConfigsCommand(boolean marshall) {
        this.name = marshall ? "getMockConfigs2" : "getMockConfigs";
        this.marshall = marshall;
        this.requiresPermissionCheck = false;
    }

    @Override
    public Cursor handle(QueryPacket commandData) throws Throwable {
        throwOnPermissionCheck(commandData.getContext());
        return CursorUtil.toMatrixCursor(
                XMockConfigDatabase.getMockConfigs(
                        commandData.getContext(),
                        commandData.getDatabase()),
                marshall, 0);
    }

    public static Cursor invoke(Context context, boolean marshall) {
        return XProxyContent.mockQuery(
                context,
                marshall ? "getMockConfigs2" : "getMockConfigs");
    }
}
