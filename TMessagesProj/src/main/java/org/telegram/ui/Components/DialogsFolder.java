package org.telegram.ui.Components;

import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;

public class DialogsFolder {

    private long id;
    private String name;
    private boolean pinned;
    private int unread;
    private int mentions;
    private int background;
    private ArrayList<TLRPC.Dialog> dialogs;

    public DialogsFolder(long id, String name, boolean pinned, int unread, int mentions, int background, ArrayList<TLRPC.Dialog> dialogs) {
        this.id = id;
        this.name = name;
        this.pinned = pinned;
        this.unread = unread;
        this.mentions = mentions;
        this.background = background;
        this.dialogs = dialogs;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public boolean isPinned() {
        return pinned;
    }

    public int getUnread() {
        return unread;
    }

    public int getMentions() {
        return mentions;
    }

    public int getBackground() {
        return background;
    }

    public ArrayList<TLRPC.Dialog> getDialogs() {
        return dialogs;
    }

    public TLRPC.Dialog getTopDialog() {
        return dialogs.get(0);
    }
}
