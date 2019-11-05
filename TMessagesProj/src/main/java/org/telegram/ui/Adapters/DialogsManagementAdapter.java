package org.telegram.ui.Adapters;

import android.content.Context;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.R;
import org.telegram.ui.Cells2.ManagementTabCell;
import org.telegram.ui.Components.RecyclerListView;

public class DialogsManagementAdapter extends RecyclerListView.SelectionAdapter {

    private Context context;

    private int favoritesCell;      // Избранное
    private int undeliveredCell;    // Недоставленные
    private int ownerCell;          // Владелец
    private int administratorCell;  // Администратор
    private int createdBotsCell;    // Созданные боты
    private int surveysCell;        // Опросы
    private int remindersCell;      // Напоминания
    private int geoCell;            // Геолокации

    private int mainRowCount = 0;

    public DialogsManagementAdapter(Context context) {
        this.context = context;

        favoritesCell = mainRowCount++;
        undeliveredCell = mainRowCount++;
        ownerCell = mainRowCount++;
        administratorCell = mainRowCount++;
//        createdBotsCell = mainRowCount++;
//        surveysCell = mainRowCount++;
//        remindersCell = mainRowCount++;
//        geoCell = mainRowCount++;
    }

    @Override
    public int getItemCount() {
        return mainRowCount;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean isEnabled(RecyclerView.ViewHolder holder) {
        return true;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        ManagementTabCell cell = new ManagementTabCell(context);
        cell.setLayoutParams(new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT));
        return new RecyclerListView.Holder(cell);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        switch (holder.getItemViewType()) {
            case 1:
                if (position == favoritesCell) {
                    ((ManagementTabCell) holder.itemView).setIconAndText(R.drawable.ic_management_favorites, R.string.tabManagementFavorites, true);
                }
                break;
            case 2:
                if (position == undeliveredCell) {
                    ((ManagementTabCell) holder.itemView).setIconAndText(R.drawable.ic_management_undelivered, R.string.tabManagementUndelivered, true);
                }
                break;
            case 3:
                if (position == ownerCell) {
                    ((ManagementTabCell) holder.itemView).setIconAndText(R.drawable.ic_management_owner, R.string.tabManagementOwner, true);
                }
            case 4:
                if (position == administratorCell) {
                    ((ManagementTabCell) holder.itemView).setIconAndText(R.drawable.ic_management_admin, R.string.tabManagementAdministrator, true);
                }
                break;
            case 5:
                if (position == createdBotsCell) {
                    ((ManagementTabCell) holder.itemView).setIconAndText(R.drawable.ic_management_created_bots, R.string.tabManagementCreatedBots, true);
                }
                break;
            case 6:
                if (position == surveysCell) {
                    ((ManagementTabCell) holder.itemView).setIconAndText(R.drawable.ic_management_survey, R.string.tabManagementSurveys, true);
                }
                break;
            case 7:
                if (position == remindersCell) {
                    ((ManagementTabCell) holder.itemView).setIconAndText(R.drawable.ic_management_reminder, R.string.tabManagementReminders, true);
                }
                break;
            case 8:
                if (position == geoCell) {
                    ((ManagementTabCell) holder.itemView).setIconAndText(R.drawable.ic_management_geo, R.string.tabManagementGeolocation, true);
                }
                break;
        }
    }

    @Override
    public int getItemViewType(int i) {
        if (i == favoritesCell) {
            return 1;
        } else if (i == undeliveredCell) {
            return 2;
        } else if (i == ownerCell) {
            return 3;
        } else if (i == administratorCell) {
            return 4;
        } else if (i == createdBotsCell) {
            return 5;
        } else if (i == surveysCell) {
            return 6;
        } else if (i == remindersCell) {
            return 7;
        } else if (i == geoCell) {
            return 8;
        }

        return 0;
    }
}
