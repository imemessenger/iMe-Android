/*
 * This is the source code of Telegram for Android v. 5.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 */

package org.telegram.ui.Adapters;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.DialogFolderCell;
import org.telegram.ui.Cells.DialogsEmptyCell;
import org.telegram.ui.Cells.ShadowSectionCell;
import org.telegram.ui.Components.CombinedDrawable;
import org.telegram.ui.Components.DialogsFolder;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.DialogsActivity;

import java.util.ArrayList;

public class DialogsFolderAdapter extends RecyclerListView.SelectionAdapter {

    private Context mContext;
    private int currentAccount = UserConfig.selectedAccount;

    public DialogsFolderAdapter(Context context) {
        mContext = context;
    }

    @Override
    public int getItemCount() {
        ArrayList<DialogsFolder> array = DialogsActivity.getDialogsFoldersArray(currentAccount);
        if (array.isEmpty()) {
            return 1;
        }

        return array.size();
    }

    @Override
    public boolean isEnabled(RecyclerView.ViewHolder holder) {
        return true;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        View view;
        switch (viewType) {
            case 0:
                view = new DialogFolderCell(mContext);
                break;
            case 5:
                view = new DialogsEmptyCell(mContext, DialogsActivity.Tab.FOLDERS);
                break;
            default:
                view = new ShadowSectionCell(mContext);
                Drawable drawable = Theme.getThemedDrawable(mContext, R.drawable.greydivider, Theme.key_windowBackgroundGrayShadow);
                CombinedDrawable combinedDrawable = new CombinedDrawable(new ColorDrawable(Theme.getColor(Theme.key_windowBackgroundGray)), drawable);
                combinedDrawable.setFullsize(true);
                view.setBackgroundDrawable(combinedDrawable);
                break;
        }
        view.setLayoutParams(new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, viewType == 5 ? RecyclerView.LayoutParams.MATCH_PARENT : RecyclerView.LayoutParams.WRAP_CONTENT));
        return new RecyclerListView.Holder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int i) {
        if (holder.getItemViewType() == 0) {
            DialogFolderCell cell = (DialogFolderCell) holder.itemView;
            ArrayList<DialogsFolder> folders = DialogsActivity.getDialogsFoldersArray(currentAccount);
            if (i < 0 || i > folders.size()) {
                return;
            }

            TLRPC.Dialog dialog = folders.get(i).getTopDialog();
            DialogsFolder folder = folders.get(i);
            cell.useSeparator = (i != getItemCount() - 1);
            cell.fullSeparator = folder.isPinned() && i + 1 < folders.size() && !folders.get(i + 1).isPinned();
            cell.setDialog(dialog, folder);
        }
    }

    @Override
    public int getItemViewType(int i) {
        if (i == DialogsActivity.getDialogsFoldersArray(currentAccount).size()) {
            return 5;
        }
        return 0;
    }
}
