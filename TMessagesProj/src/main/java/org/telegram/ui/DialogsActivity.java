/*
 * This is the source code of Telegram for Android v. 5.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 */

package org.telegram.ui;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.StateListAnimator;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScrollerMiddle;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.airbnb.lottie.LottieDrawable;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.tabs.TabLayout;
import com.smedialink.channels.IMeChannelsActivity;
import com.smedialink.storage.data.repository.StorageRepository;

import org.telegram.messenger.AccountInstance;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BuildVars;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.ContactsController;
import org.telegram.messenger.DataQuery;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.ImageLoader;
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.NotificationsController;
import org.telegram.messenger.R;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.UserObject;
import org.telegram.messenger.XiaomiUtilities;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.ActionBarMenuSubItem;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BackDrawable;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.MenuDrawable;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.Adapters.DialogsAdapter;
import org.telegram.ui.Adapters.DialogsFolderAdapter;
import org.telegram.ui.Adapters.DialogsSearchAdapter;
import org.telegram.ui.Cells.AccountSelectCell;
import org.telegram.ui.Cells.ArchiveHintInnerCell;
import org.telegram.ui.Cells.DialogCell;
import org.telegram.ui.Cells.DialogsEmptyCell;
import org.telegram.ui.Cells.DividerCell;
import org.telegram.ui.Cells.DrawerActionCell;
import org.telegram.ui.Cells.DrawerAddCell;
import org.telegram.ui.Cells.DrawerProfileCell;
import org.telegram.ui.Cells.DrawerUserCell;
import org.telegram.ui.Cells.GraySectionCell;
import org.telegram.ui.Cells.HashtagSearchCell;
import org.telegram.ui.Cells.HintDialogCell;
import org.telegram.ui.Cells.LoadingCell;
import org.telegram.ui.Cells.ProfileSearchCell;
import org.telegram.ui.Cells.ShadowSectionCell;
import org.telegram.ui.Cells.UserCell;
import org.telegram.ui.Components.AlertsCreator;
import org.telegram.ui.Components.AvatarDrawable;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.ChatActivityEnterView;
import org.telegram.ui.Components.CombinedDrawable;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Components.CustomTabView;
import org.telegram.ui.Components.DialogsFolder;
import org.telegram.ui.Components.DialogsItemAnimator;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.EmptyTextProgressView;
import org.telegram.ui.Components.FragmentContextView;
import org.telegram.ui.Components.JoinGroupAlert;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.NumberTextView;
import org.telegram.ui.Components.PacmanAnimation;
import org.telegram.ui.Components.ProxyDrawable;
import org.telegram.ui.Components.RadialProgressView;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.SizeNotifierFrameLayout;
import org.telegram.ui.Components.StickersAlert;
import org.telegram.ui.Components.UndoView;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class DialogsActivity extends BaseFragment implements NotificationCenter.NotificationCenterDelegate {

    private interface DialogPositiveButtonCallback {
        void run(String enteredText);
    }

    // Виды вкладок для чатов
    public enum Tab {
        ALL(R.string.tabAll),
        UNREAD(R.string.tabUnread),
        CHATS(R.string.tabChats),
        GROUPS(R.string.tabGroups),
        CHANNELS(R.string.tabChannels),
        BOTS(R.string.tabBots),
        FOLDERS(R.string.tabFolders);

        Tab(int stringRes) {
            this.stringRes = stringRes;
        }

        public int getStringRes() {
            return stringRes;
        }

        @IdRes
        private int stringRes;
    }

    // Режим отображения вкладок
    public enum TabsMode {
        // Только вкладка "Все чаты"
        SINGLE,
        // Набор вкладок, в которые разрешена пересылка сообщений
        MINIMAL,
        // Все вкладки
        ALL
    }

    // Вью, соответствующие каждой из вкладок
    private Map<Tab, RecyclerListView> tabViews = new EnumMap<>(Tab.class);
    // Адаптеры, соответствующие каждой из вкладок
    private Map<Tab, RecyclerListView.SelectionAdapter> tabAdapters = new EnumMap<>(Tab.class);
    // Иконки, соответствующие каждой из вкладок
    private Map<Tab, Integer> tabIcons = new EnumMap<>(Tab.class);
    // Менеджер вкладок
    private DialogsTabManager tabsManager = DialogsTabManager.getInstance();

    // Храним текущую вкладку и текущий режим
    private Tab currentTab;
    private TabsMode currentTabsMode;

    private DialogsSearchAdapter dialogsSearchAdapter;

    private CoordinatorLayout tabsContainer;
    private TabLayout tabsLayout;
    private ViewPager tabsViewPager;
    private ViewPager.OnPageChangeListener tabsViewPagerListener;
    private TabLayout.OnTabSelectedListener tabsLayoutSelectedListener;

    // Храним имя папки, используется при просмотре контента папки
    private static String originalFolderName = "";
    // Добавляются ли диалоги в уже существующую папку (true) или в новую (false)
    private static boolean editExistingFolder = false;
    // айди существующей папки
    private static long existingFolderId = -1;

    private RecyclerListView listViewAll;
    private RecyclerListView listViewUnread;
    private RecyclerListView listViewGroups;
    private RecyclerListView listViewChats;
    private RecyclerListView listViewChannels;
    private RecyclerListView listViewBots;
    private RecyclerListView listViewFavorites;

    private DialogsAdapter dialogsAdapterAll;
    private DialogsAdapter dialogsAdapterUnread;
    private DialogsAdapter dialogsAdapterGroups;
    private DialogsAdapter dialogsAdapterChats;
    private DialogsAdapter dialogsAdapterChannels;
    private DialogsAdapter dialogsAdapterBots;
    private DialogsFolderAdapter dialogsAdapterFavorites;

    private LinearLayoutManager layoutManagerAll;
    private LinearLayoutManager layoutManagerGroups;
    private LinearLayoutManager layoutManagerChats;
    private LinearLayoutManager layoutManagerChannels;
    private LinearLayoutManager layoutManagerBots;
    private LinearLayoutManager layoutManagerFavorites;

    private PagerAdapter tabsAdapter;

    // Флаг определяющий можно ли переключать вкладки свайпом,
    // в некоторых режимах свайп необходимо блокировать
    private boolean canSwipeHorizontally = true;

    private StorageRepository storage =
            MessagesController.getInstance(currentAccount).Storage;

    private CompositeDisposable disposables = new CompositeDisposable();

    private EmptyTextProgressView searchEmptyView;
    private RadialProgressView progressView;
    private ActionBarMenuItem passcodeItem;
    private ActionBarMenuItem proxyItem;
    private ActionBarMenuItem channelsItem;

    private ProxyDrawable proxyDrawable;
    private ImageView floatingButton;
    private FrameLayout floatingButtonContainer;
    private UndoView[] undoView = new UndoView[2];
    private SwipeController swipeController;
    private ItemTouchHelper itemTouchhelper;

    private int lastItemsCount;

    private PacmanAnimation pacmanAnimation;

    private DialogCell slidingView;
    private DialogCell movingView;
    private boolean allowMoving;
    private boolean movingWas;
    private boolean waitingForScrollFinished;
    private boolean allowSwipeDuringCurrentTouch;

    private MenuDrawable menuDrawable;
    private BackDrawable backDrawable;

    private NumberTextView selectedDialogsCountTextView;
    private ArrayList<View> actionModeViews = new ArrayList<>();
    private ActionBarMenuItem deleteItem;
    private ActionBarMenuItem pinItem;
    private ActionBarMenuItem archiveItem;
    private ActionBarMenuItem hideArchiveItem;
    private ActionBarMenuSubItem clearItem;
    private ActionBarMenuSubItem readItem;
    private ActionBarMenuSubItem muteItem;
    private ActionBarMenuItem otherItem;
    private ActionBarMenuSubItem folderItem;

    private float additionalFloatingTranslation;

    private RecyclerView sideMenu;
    private ChatActivityEnterView commentView;
    private ActionBarMenuItem switchItem;

    private static ArrayList<TLRPC.Dialog> frozenDialogsList;
    private boolean dialogsListFrozen;
    private int dialogRemoveFinished;
    private int dialogInsertFinished;
    private int dialogChangeFinished;
    private DialogsItemAnimator dialogsItemAnimator;

    private AlertDialog permissionDialog;
    private boolean askAboutContacts = true;

    private boolean proxyItemVisible;
    private boolean closeSearchFieldOnHide;
    private long searchDialogId;
    private TLObject searchObject;

    private int prevPosition;
    private int prevTop;
    private boolean scrollUpdated;
    private boolean floatingHidden;
    private final AccelerateDecelerateInterpolator floatingInterpolator = new AccelerateDecelerateInterpolator();

    private boolean checkPermission = true;

    private int currentConnectionState;

    private String selectAlertString;
    private String selectAlertStringGroup;
    private String addToGroupAlertString;

    // Добавлены новые dialogsType:
    //  8 - просмотр содержимого папки,
    //  9 - список диалогов для добавления в папку
    private int dialogsType;

    public static boolean[] dialogsLoaded = new boolean[UserConfig.MAX_ACCOUNT_COUNT];
    private boolean searching;
    private boolean searchWas;
    private boolean onlySelect;
    private String searchString;
    private long openedDialogId;
    private boolean cantSendToChannels;
    private boolean allowSwitchAccount;
    private boolean checkCanWrite;

    private DialogsActivityDelegate delegate;

    private int canReadCount;
    private int canPinCount;
    private int canMuteCount;
    private int canUnmuteCount;
    private int canClearCacheCount;

    private int folderId;

    private final static int pin = 100;
    private final static int read = 101;
    private final static int delete = 102;
    private final static int clear = 103;
    private final static int mute = 104;
    private final static int archive = 105;
    private final static int folder = 106;
    private final static int hideArchive = 107;


    private boolean allowScrollToHiddenView;
    private boolean scrollingManually;
    private int totalConsumedAmount;
    private boolean startedScrollAtTop;

    private class ContentView extends SizeNotifierFrameLayout {

        private int inputFieldHeight;

        public ContentView(Context context) {
            super(context);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            int widthSize = View.MeasureSpec.getSize(widthMeasureSpec);
            int heightSize = View.MeasureSpec.getSize(heightMeasureSpec);

            setMeasuredDimension(widthSize, heightSize);
            heightSize -= getPaddingTop();

            measureChildWithMargins(actionBar, widthMeasureSpec, 0, heightMeasureSpec, 0);

            int keyboardSize = getKeyboardHeight();
            int childCount = getChildCount();

            if (commentView != null) {
                measureChildWithMargins(commentView, widthMeasureSpec, 0, heightMeasureSpec, 0);
                Object tag = commentView.getTag();
                if (tag != null && tag.equals(2)) {
                    if (keyboardSize <= AndroidUtilities.dp(20) && !AndroidUtilities.isInMultiwindow) {
                        heightSize -= commentView.getEmojiPadding();
                    }
                    inputFieldHeight = commentView.getMeasuredHeight();
                } else {
                    inputFieldHeight = 0;
                }
            }

            for (int i = 0; i < childCount; i++) {
                View child = getChildAt(i);
                if (child == null || child.getVisibility() == GONE || child == commentView || child == actionBar) {
                    continue;
                }
                if (child == tabViews.get(currentTab) || child == progressView || child == searchEmptyView) {
                    int contentWidthSpec = View.MeasureSpec.makeMeasureSpec(widthSize, View.MeasureSpec.EXACTLY);
                    int contentHeightSpec = View.MeasureSpec.makeMeasureSpec(Math.max(AndroidUtilities.dp(10), heightSize - inputFieldHeight + AndroidUtilities.dp(2)), View.MeasureSpec.EXACTLY);
                    child.measure(contentWidthSpec, contentHeightSpec);
                } else if (commentView != null && commentView.isPopupView(child)) {
                    if (AndroidUtilities.isInMultiwindow) {
                        if (AndroidUtilities.isTablet()) {
                            child.measure(View.MeasureSpec.makeMeasureSpec(widthSize, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(Math.min(AndroidUtilities.dp(320), heightSize - inputFieldHeight - AndroidUtilities.statusBarHeight + getPaddingTop()), View.MeasureSpec.EXACTLY));
                        } else {
                            child.measure(View.MeasureSpec.makeMeasureSpec(widthSize, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(heightSize - inputFieldHeight - AndroidUtilities.statusBarHeight + getPaddingTop(), View.MeasureSpec.EXACTLY));
                        }
                    } else {
                        child.measure(View.MeasureSpec.makeMeasureSpec(widthSize, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(child.getLayoutParams().height, View.MeasureSpec.EXACTLY));
                    }
                } else {
                    measureChildWithMargins(child, widthMeasureSpec, 0, heightMeasureSpec, 0);
                }
            }
        }

        @Override
        protected void onLayout(boolean changed, int l, int t, int r, int b) {
            final int count = getChildCount();

            int paddingBottom;
            Object tag = commentView != null ? commentView.getTag() : null;
            if (tag != null && tag.equals(2)) {
                paddingBottom = getKeyboardHeight() <= AndroidUtilities.dp(20) && !AndroidUtilities.isInMultiwindow ? commentView.getEmojiPadding() : 0;
            } else {
                paddingBottom = 0;
            }
            setBottomClip(paddingBottom);

            for (int i = 0; i < count; i++) {
                final View child = getChildAt(i);
                if (child.getVisibility() == GONE) {
                    continue;
                }
                final FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) child.getLayoutParams();

                final int width = child.getMeasuredWidth();
                final int height = child.getMeasuredHeight();

                int childLeft;
                int childTop;

                int gravity = lp.gravity;
                if (gravity == -1) {
                    gravity = Gravity.TOP | Gravity.LEFT;
                }

                final int absoluteGravity = gravity & Gravity.HORIZONTAL_GRAVITY_MASK;
                final int verticalGravity = gravity & Gravity.VERTICAL_GRAVITY_MASK;

                switch (absoluteGravity & Gravity.HORIZONTAL_GRAVITY_MASK) {
                    case Gravity.CENTER_HORIZONTAL:
                        childLeft = (r - l - width) / 2 + lp.leftMargin - lp.rightMargin;
                        break;
                    case Gravity.RIGHT:
                        childLeft = r - width - lp.rightMargin;
                        break;
                    case Gravity.LEFT:
                    default:
                        childLeft = lp.leftMargin;
                }

                switch (verticalGravity) {
                    case Gravity.TOP:
                        childTop = lp.topMargin + getPaddingTop();
                        break;
                    case Gravity.CENTER_VERTICAL:
                        childTop = ((b - paddingBottom) - t - height) / 2 + lp.topMargin - lp.bottomMargin;
                        break;
                    case Gravity.BOTTOM:
                        childTop = ((b - paddingBottom) - t) - height - lp.bottomMargin;
                        break;
                    default:
                        childTop = lp.topMargin;
                }

                if (commentView != null && commentView.isPopupView(child)) {
                    if (AndroidUtilities.isInMultiwindow) {
                        childTop = commentView.getTop() - child.getMeasuredHeight() + AndroidUtilities.dp(1);
                    } else {
                        childTop = commentView.getBottom();
                    }
                }
                child.layout(childLeft, childTop, childLeft + width, childTop + height);
            }

            notifyHeightChanged();
        }

        @Override
        public boolean onInterceptTouchEvent(MotionEvent ev) {
            int action = ev.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                if (action == MotionEvent.ACTION_DOWN) {
                    RecyclerListView recycler = tabViews.get(currentTab);
                    if (recycler != null) {
                        LinearLayoutManager lm = (LinearLayoutManager) recycler.getLayoutManager();
                        if (lm != null) {
                            int currentPosition = lm.findFirstVisibleItemPosition();
                            startedScrollAtTop = currentPosition <= 1;
                        }
                    }
                } else {
                    if (actionBar.isActionModeShowed()) {
                        allowMoving = true;
                    }
                }
                totalConsumedAmount = 0;
                allowScrollToHiddenView = false;
            }
            return super.onInterceptTouchEvent(ev);
        }
    }

    class SwipeController extends ItemTouchHelper.Callback {

        private RectF buttonInstance;
        private RecyclerView.ViewHolder currentItemViewHolder;
        private boolean swipingFolder;
        private boolean swipeFolderBack;

        @Override
        public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            return 0;
        }

        @Override
        public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder source, RecyclerView.ViewHolder target) {
            return false;
        }

        @Override
        public int convertToAbsoluteDirection(int flags, int layoutDirection) {
            if (swipeFolderBack) {
                return 0;
            }
            return super.convertToAbsoluteDirection(flags, layoutDirection);
        }

        @Override
        public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
        }

        @Override
        public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
            if (viewHolder != null) {
                RecyclerListView tabView = tabViews.get(currentTab);
                if (tabView != null) {
                    tabView.hideSelector();
                }

            }
            super.onSelectedChanged(viewHolder, actionState);
        }

        @Override
        public long getAnimationDuration(RecyclerView recyclerView, int animationType, float animateDx, float animateDy) {
            if (animationType == ItemTouchHelper.ANIMATION_TYPE_SWIPE_CANCEL) {
                return 200;
            } else if (animationType == ItemTouchHelper.ANIMATION_TYPE_DRAG) {
                if (movingView != null) {
                    View view = movingView;
                    AndroidUtilities.runOnUIThread(() -> view.setBackgroundDrawable(null), dialogsItemAnimator.getMoveDuration());
                    movingView = null;
                }
            }
            return super.getAnimationDuration(recyclerView, animationType, animateDx, animateDy);
        }

        @Override
        public float getSwipeThreshold(RecyclerView.ViewHolder viewHolder) {
            return 0.3f;
        }

        @Override
        public float getSwipeEscapeVelocity(float defaultValue) {
            return 3500;
        }

        @Override
        public float getSwipeVelocityThreshold(float defaultValue) {
            return Float.MAX_VALUE;
        }
    }

    public interface DialogsActivityDelegate {
        void didSelectDialogs(DialogsActivity fragment, ArrayList<Long> dids, CharSequence message, boolean param);
    }

    public DialogsActivity(Bundle args) {
        super(args);
    }

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();

        if (getArguments() != null) {
            onlySelect = arguments.getBoolean("onlySelect", false);
            editExistingFolder = arguments.getBoolean("editExistingFolder", false);
            existingFolderId = arguments.getLong("existingFolderId", 0);
            cantSendToChannels = arguments.getBoolean("cantSendToChannels", false);
            dialogsType = arguments.getInt("dialogsType", 0);
            selectAlertString = arguments.getString("selectAlertString");
            selectAlertStringGroup = arguments.getString("selectAlertStringGroup");
            addToGroupAlertString = arguments.getString("addToGroupAlertString");
            allowSwitchAccount = arguments.getBoolean("allowSwitchAccount");
            checkCanWrite = arguments.getBoolean("checkCanWrite", true);
            folderId = arguments.getInt("folderId", 0);
        }

        if (dialogsType == 8 || dialogsType == 9) {
            originalFolderName = arguments.getString("folderName");
        }

        if (dialogsType == 0 || dialogsType == 8) {
            askAboutContacts = MessagesController.getGlobalNotificationsSettings().getBoolean("askAboutContacts", true);
            SharedConfig.loadProxyList();
        }

        if (searchString == null) {
            currentConnectionState = getConnectionsManager().getConnectionState();

            getNotificationCenter().addObserver(this, NotificationCenter.dialogsNeedReload);
            NotificationCenter.getGlobalInstance().addObserver(this, NotificationCenter.emojiDidLoad);
            if (!onlySelect) {
                NotificationCenter.getGlobalInstance().addObserver(this, NotificationCenter.closeSearchByActiveAction);
                NotificationCenter.getGlobalInstance().addObserver(this, NotificationCenter.proxySettingsChanged);
            }
            getNotificationCenter().addObserver(this, NotificationCenter.updateInterfaces);
            getNotificationCenter().addObserver(this, NotificationCenter.encryptedChatUpdated);
            getNotificationCenter().addObserver(this, NotificationCenter.contactsDidLoad);
            getNotificationCenter().addObserver(this, NotificationCenter.appDidLogout);
            getNotificationCenter().addObserver(this, NotificationCenter.openedChatChanged);
            getNotificationCenter().addObserver(this, NotificationCenter.notificationsSettingsUpdated);
            getNotificationCenter().addObserver(this, NotificationCenter.messageReceivedByAck);
            getNotificationCenter().addObserver(this, NotificationCenter.messageReceivedByServer);
            getNotificationCenter().addObserver(this, NotificationCenter.messageSendError);
            getNotificationCenter().addObserver(this, NotificationCenter.needReloadRecentDialogsSearch);
            getNotificationCenter().addObserver(this, NotificationCenter.replyMessagesDidLoad);
            getNotificationCenter().addObserver(this, NotificationCenter.reloadHints);
            getNotificationCenter().addObserver(this, NotificationCenter.didUpdateConnectionState);
            getNotificationCenter().addObserver(this, NotificationCenter.dialogsUnreadCounterChanged);
            getNotificationCenter().addObserver(this, NotificationCenter.needDeleteDialog);
            getNotificationCenter().addObserver(this, NotificationCenter.refreshTabIcons);
            getNotificationCenter().addObserver(this, NotificationCenter.refreshTabContent);
            getNotificationCenter().addObserver(this, NotificationCenter.refreshTabState);
            getNotificationCenter().addObserver(this, NotificationCenter.folderBecomeEmpty);


            NotificationCenter.getGlobalInstance().addObserver(this, NotificationCenter.didSetPasscode);
        }

        if (!dialogsLoaded[currentAccount]) {
            getMessagesController().loadGlobalNotificationsSettings();
            getMessagesController().loadDialogs(folderId, 0, 100, true);
            getMessagesController().loadHintDialogs();
            getContactsController().checkInviteText();
            getDataQuery().loadRecents(DataQuery.TYPE_FAVE, false, true, false);
            getDataQuery().checkFeaturedStickers();
            dialogsLoaded[currentAccount] = true;
        }

        int userId = UserConfig.getInstance(currentAccount).getClientUserId();
        boolean botsAlreadyInstalled = MessagesController.getMainSettings(currentAccount).getBoolean("", false);

        if (userId != 0) {
            if (!botsAlreadyInstalled) {
                ApplicationLoader.smartBotsManager.sendAppInstalledEvent(userId, () ->
                        MessagesController.getMainSettings(currentAccount).edit().putBoolean("BotsInstalled", true).apply());
            }

            ApplicationLoader.smartBotsManager.fetchVotes(userId);

            if (dialogsType == 0) {
                MessagesController.getInstance(currentAccount).startDialogFoldersObserving(userId);
            }
        }

        // Слушаем инфу из коллекции Firebase, на апдейтах обновляем кэш инфы о покупках
        ApplicationLoader.smartBotsManager.listenForRemoteBotUpdates(() ->
                ApplicationLoader.purchaseHelper.preloadPurchasesInfo());

        if (dialogsType == 8 || dialogsType == 9 || folderId != 0) {
            currentTabsMode = TabsMode.SINGLE;
        } else if (dialogsType == 3) {
            currentTabsMode = TabsMode.MINIMAL;
        } else {
            currentTabsMode = TabsMode.ALL;
        }

        ApplicationLoader.smartBotsManager.setBotDisableCallback(() ->
                NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.botsListChanged));

        getMessagesController().loadPinnedDialogs(folderId, 0, null);
        return true;
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        if (searchString == null) {
            getNotificationCenter().removeObserver(this, NotificationCenter.dialogsNeedReload);
            NotificationCenter.getGlobalInstance().removeObserver(this, NotificationCenter.emojiDidLoad);
            if (!onlySelect) {
                NotificationCenter.getGlobalInstance().removeObserver(this, NotificationCenter.closeSearchByActiveAction);
                NotificationCenter.getGlobalInstance().removeObserver(this, NotificationCenter.proxySettingsChanged);
            }
            getNotificationCenter().removeObserver(this, NotificationCenter.updateInterfaces);
            getNotificationCenter().removeObserver(this, NotificationCenter.encryptedChatUpdated);
            getNotificationCenter().removeObserver(this, NotificationCenter.contactsDidLoad);
            getNotificationCenter().removeObserver(this, NotificationCenter.appDidLogout);
            getNotificationCenter().removeObserver(this, NotificationCenter.openedChatChanged);
            getNotificationCenter().removeObserver(this, NotificationCenter.notificationsSettingsUpdated);
            getNotificationCenter().removeObserver(this, NotificationCenter.messageReceivedByAck);
            getNotificationCenter().removeObserver(this, NotificationCenter.messageReceivedByServer);
            getNotificationCenter().removeObserver(this, NotificationCenter.messageSendError);
            getNotificationCenter().removeObserver(this, NotificationCenter.needReloadRecentDialogsSearch);
            getNotificationCenter().removeObserver(this, NotificationCenter.replyMessagesDidLoad);
            getNotificationCenter().removeObserver(this, NotificationCenter.reloadHints);
            getNotificationCenter().removeObserver(this, NotificationCenter.didUpdateConnectionState);
            getNotificationCenter().removeObserver(this, NotificationCenter.dialogsUnreadCounterChanged);
            getNotificationCenter().removeObserver(this, NotificationCenter.needDeleteDialog);
            getNotificationCenter().removeObserver(this, NotificationCenter.refreshTabIcons);
            getNotificationCenter().removeObserver(this, NotificationCenter.refreshTabContent);
            getNotificationCenter().removeObserver(this, NotificationCenter.refreshTabState);
            getNotificationCenter().removeObserver(this, NotificationCenter.folderBecomeEmpty);


            NotificationCenter.getGlobalInstance().removeObserver(this, NotificationCenter.didSetPasscode);
        }
        if (commentView != null) {
            commentView.onDestroy();
        }
        if (undoView[0] != null) {
            undoView[0].hide(true, 0);
        }
        delegate = null;
        disposables.clear();
        if (dialogsType == 0) {
            MessagesController.getInstance(currentAccount).stopDialogFoldersObserving();
        }
    }

    @Override
    public View createView(final Context context) {
        searching = false;
        searchWas = false;
        pacmanAnimation = null;

        AndroidUtilities.runOnUIThread(() -> Theme.createChatResources(context, false));

        ActionBarMenu menu = actionBar.createMenu();
        if (!onlySelect && searchString == null && dialogsType != 8 && folderId == 0) {
            proxyDrawable = new ProxyDrawable(context);
            proxyItem = menu.addItem(3, proxyDrawable);
            channelsItem = menu.addItem(2, R.drawable.ic_channels);
            passcodeItem = menu.addItem(1, R.drawable.lock_close);
            updatePasscodeButton();
            updateProxyButton(false);
        }
        if (dialogsType == 9) {
            menu.addItem(9, R.drawable.ic_done).setOnClickListener(v -> {
                if (dialogsAdapterAll != null) {
                    ArrayList<Long> selectedDialogs = dialogsAdapterAll.getSelectedDialogs();
                    if (selectedDialogs.isEmpty()) {
                        Toast.makeText(getParentActivity(), String.format(Locale.getDefault(), LocaleController.getInternalString(R.string.SelectDialogsToAdd), originalFolderName), Toast.LENGTH_SHORT).show();
                    } else {
                        saveDialogsToFolder(originalFolderName, selectedDialogs);
                    }
                }
            });
        } else if (dialogsType != 8) {
            ActionBarMenuItem searchItem = menu.addItem(0, R.drawable.ic_ab_search).setIsSearchField(true).setActionBarMenuItemSearchListener(new ActionBarMenuItem.ActionBarMenuItemSearchListener() {

                TabsMode savedMode;

                @Override
                public void onSearchExpand() {
                    savedMode = currentTabsMode;
                    currentTabsMode = TabsMode.SINGLE;
                    tabsAdapter = buildNewTabsAdapter();
                    tabsViewPager.setAdapter(tabsAdapter);
                    tabsAdapter.notifyDataSetChanged();
                    tabsLayout.setEnabled(false);
                    tabsLayout.setVisibility(View.GONE);
                    searching = true;
                    if (switchItem != null) {
                        switchItem.setVisibility(View.GONE);
                    }
                    if (proxyItem != null && proxyItemVisible) {
                        proxyItem.setVisibility(View.GONE);
                    }
                    if (channelsItem != null) {
                        channelsItem.setVisibility(View.GONE);
                    }
                    if (listViewAll != null) {
                        if (searchString != null) {
                            listViewAll.setEmptyView(searchEmptyView);
                            progressView.setVisibility(View.GONE);
                        }
                        if (!onlySelect) {
                            floatingButtonContainer.setVisibility(View.GONE);
                            //unreadFloatingButtonContainer.setVisibility(View.GONE);
                        }
                    }
                    updatePasscodeButton();
                    canSwipeHorizontally = false;

                    updatePasscodeButton();
                    actionBar.setBackButtonContentDescription(LocaleController.getString("AccDescrGoBack", R.string.AccDescrGoBack));
                }

                @Override
                public boolean canCollapseSearch() {
                    if (switchItem != null) {
                        switchItem.setVisibility(View.VISIBLE);
                    }
                    if (proxyItem != null && proxyItemVisible) {
                        proxyItem.setVisibility(View.VISIBLE);
                    }

                    if (channelsItem != null) {
                        channelsItem.setVisibility(View.VISIBLE);

                    }
                    if (searchString != null) {
                        finishFragment();
                        return false;
                    }
                    return true;
                }

                @Override
                public void onSearchCollapse() {
                    currentTabsMode = savedMode;
                    tabsAdapter = buildNewTabsAdapter();
                    tabsViewPager.setAdapter(tabsAdapter);
                    tabsViewPager.setCurrentItem(tabsManager.getPositionByTab(currentTab, currentTabsMode), false);
                    tabsAdapter.notifyDataSetChanged();
                    tabsLayout.setEnabled(true);
                    setupTabsBehavior();
                    setupTabsAppearance();
                    searching = false;
                    searchWas = false;
                    if (listViewAll != null) {
                        listViewAll.setEmptyView(folderId == 0 ? progressView : null);
                        searchEmptyView.setVisibility(View.GONE);
                        if (!onlySelect) {
                            floatingButtonContainer.setVisibility(View.VISIBLE);
                            floatingHidden = true;
                            floatingButtonContainer.setTranslationY(AndroidUtilities.dp(100));
                            hideFloatingButton(false);
                        }
                        if (listViewAll != null && listViewAll.getAdapter() != dialogsAdapterAll) {
                            listViewAll.setAdapter(dialogsAdapterAll);
                            dialogsAdapterAll.notifyDataSetChanged();
                        }
                    }
                    if (dialogsSearchAdapter != null) {
                        dialogsSearchAdapter.searchDialogs(null);
                    }
                    updatePasscodeButton();
                    canSwipeHorizontally = true;
                    if (menuDrawable != null) {
                        actionBar.setBackButtonContentDescription(LocaleController.getString("AccDescrOpenMenu", R.string.AccDescrOpenMenu));
                    }
                }

                @Override
                public void onTextChanged(EditText editText) {
                    String text = editText.getText().toString();
                    if (text.length() != 0 || dialogsSearchAdapter != null && dialogsSearchAdapter.hasRecentRearch()) {
                        searchWas = true;
                        if (dialogsSearchAdapter != null && listViewAll.getAdapter() != dialogsSearchAdapter) {
                            listViewAll.setAdapter(dialogsSearchAdapter);
                            dialogsSearchAdapter.notifyDataSetChanged();
                        }
                        if (searchEmptyView != null && listViewAll.getEmptyView() != searchEmptyView) {
                            progressView.setVisibility(View.GONE);
                            searchEmptyView.showTextView();
                            listViewAll.setEmptyView(searchEmptyView);
                        }
                    }
                    if (dialogsSearchAdapter != null) {
                        dialogsSearchAdapter.searchDialogs(text);
                    }
                }

            });
            searchItem.setSearchFieldHint(LocaleController.getString("Search", R.string.Search));
            searchItem.setContentDescription(LocaleController.getString("Search", R.string.Search));


        }

        if (onlySelect || dialogsType == 8) {
            actionBar.setBackButtonImage(R.drawable.ic_ab_back);
            if (dialogsType == 3 && selectAlertString == null) {
                actionBar.setTitle(LocaleController.getString("ForwardTo", R.string.ForwardTo));
            } else if (dialogsType == 9) {
                actionBar.setTitle(LocaleController.getInternalString(R.string.SelectChats));
            } else if (dialogsType == 8) {
                actionBar.setTitle(originalFolderName);
            }
        } else {
            if (searchString != null || folderId != 0) {
                actionBar.setBackButtonDrawable(backDrawable = new BackDrawable(false));
            } else {
                actionBar.setBackButtonDrawable(menuDrawable = new MenuDrawable());
                actionBar.setBackButtonContentDescription(LocaleController.getString("AccDescrOpenMenu", R.string.AccDescrOpenMenu));
            }

            if (folderId != 0) {
                actionBar.setTitle(LocaleController.getString("ArchivedChats", R.string.ArchivedChats));
            } else {
                if (BuildVars.DEBUG_VERSION) {
                    actionBar.setTitle("iMe"/*LocaleController.getString("AppNameBeta", R.string.AppNameBeta)*/);
                } else {
                    actionBar.setTitle(LocaleController.getString("AppName", R.string.AppName));
                }
            }
            actionBar.setSupportsHolidayImage(true);
        }
        actionBar.setTitleActionRunnable(() -> {
            hideFloatingButton(false);
            RecyclerListView list = tabViews.get(currentTab);
            if (list != null) {
                list.smoothScrollToPosition(hasHiddenArchive() ? 1 : 0);
            }
        });

        if (allowSwitchAccount && UserConfig.getActivatedAccountsCount() > 1) {
            switchItem = menu.addItemWithWidth(1, 0, AndroidUtilities.dp(56));
            AvatarDrawable avatarDrawable = new AvatarDrawable();
            avatarDrawable.setTextSize(AndroidUtilities.dp(12));

            BackupImageView imageView = new BackupImageView(context);
            imageView.setRoundRadius(AndroidUtilities.dp(18));
            switchItem.addView(imageView, LayoutHelper.createFrame(36, 36, Gravity.CENTER));


            TLRPC.User user = getUserConfig().getCurrentUser();
            avatarDrawable.setInfo(user);
            imageView.getImageReceiver().setCurrentAccount(currentAccount);
            imageView.setImage(ImageLocation.getForUser(user, false), "50_50", avatarDrawable, user);

            for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
                TLRPC.User u = AccountInstance.getInstance(a).getUserConfig().getCurrentUser();
                if (u != null) {
                    AccountSelectCell cell = new AccountSelectCell(context);
                    cell.setAccount(a, true);
                    switchItem.addSubItem(10 + a, cell, AndroidUtilities.dp(230), AndroidUtilities.dp(48));
                }
            }
        }
        actionBar.setAllowOverlayTitle(true);

        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    if (actionBar.isActionModeShowed()) {
                        hideActionMode(true);
                    } else if (onlySelect || folderId != 0 || dialogsType == 8) {
                        finishFragment();
                    } else if (parentLayout != null) {
                        parentLayout.getDrawerLayoutContainer().openDrawer(false);
                    }
                } else if (id == 1) {
                    SharedConfig.appLocked = !SharedConfig.appLocked;
                    SharedConfig.saveConfig();
                    updatePasscodeButton();
                } else if (id == 2) {
                    presentFragment(new IMeChannelsActivity());
                } else if (id == 3) {
                    presentFragment(new ProxyListActivity());
                } else if (id >= 10 && id < 10 + UserConfig.MAX_ACCOUNT_COUNT) {
                    if (getParentActivity() == null) {
                        return;
                    }
                    DialogsActivityDelegate oldDelegate = delegate;
                    LaunchActivity launchActivity = (LaunchActivity) getParentActivity();
                    launchActivity.switchToAccount(id - 10, true);

                    DialogsActivity dialogsActivity = new DialogsActivity(arguments);
                    dialogsActivity.setDelegate(oldDelegate);
                    launchActivity.presentFragment(dialogsActivity, false, true);
                } else if (id == pin || id == read || id == delete || id == clear || id == mute || id == archive || id == folder || id == hideArchive) {
                    perfromSelectedDialogsAction(id, true);
                }
            }
        });

        if (sideMenu != null) {
            sideMenu.setBackgroundColor(Theme.getColor(Theme.key_chats_menuBackground));
            sideMenu.setGlowColor(Theme.getColor(Theme.key_chats_menuBackground));
            sideMenu.getAdapter().notifyDataSetChanged();
        }

        final ActionBarMenu actionMode = actionBar.createActionMode();

        selectedDialogsCountTextView = new NumberTextView(actionMode.getContext());
        selectedDialogsCountTextView.setTextSize(18);
        selectedDialogsCountTextView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        selectedDialogsCountTextView.setTextColor(Theme.getColor(Theme.key_actionBarActionModeDefaultIcon));
        actionMode.addView(selectedDialogsCountTextView, LayoutHelper.createLinear(0, LayoutHelper.MATCH_PARENT, 1.0f, 72, 0, 0, 0));
        selectedDialogsCountTextView.setOnTouchListener((v, event) -> true);

        pinItem = actionMode.addItemWithWidth(pin, R.drawable.msg_pin, AndroidUtilities.dp(54));
        archiveItem = actionMode.addItemWithWidth(archive, R.drawable.msg_archive, AndroidUtilities.dp(54));
        hideArchiveItem = actionMode.addItemWithWidth(hideArchive, R.drawable.chats_archive_hide, AndroidUtilities.dp(54));

        deleteItem = actionMode.addItemWithWidth(delete, R.drawable.msg_delete, AndroidUtilities.dp(54), LocaleController.getString("Delete", R.string.Delete));
        otherItem = actionMode.addItemWithWidth(0, R.drawable.ic_ab_other, AndroidUtilities.dp(54), LocaleController.getString("AccDescrMoreOptions", R.string.AccDescrMoreOptions));
        muteItem = otherItem.addSubItem(mute, R.drawable.msg_mute, LocaleController.getString("ChatsMute", R.string.ChatsMute));
        folderItem = otherItem.addSubItem(folder, R.drawable.send_to_folder, dialogsType == 8 ? LocaleController.getInternalString(R.string.RemoveFromFolder) : LocaleController.getInternalString(R.string.SendToFolder));
        readItem = otherItem.addSubItem(read, R.drawable.msg_markread, LocaleController.getString("MarkAsRead", R.string.MarkAsRead));
        clearItem = otherItem.addSubItem(clear, R.drawable.msg_clear, LocaleController.getString("ClearHistory", R.string.ClearHistory));

        actionModeViews.add(pinItem);
        actionModeViews.add(archiveItem);
        actionModeViews.add(deleteItem);
        actionModeViews.add(otherItem);

        ContentView contentView = new ContentView(context);
        setupTabs(contentView);
        fragmentView = contentView;

        //TODO ALL
        listViewAll = new DialogsRecyclerListView(context, Tab.ALL);
        dialogsItemAnimator = new DialogsItemAnimator() {
            @Override
            public void onRemoveFinished(RecyclerView.ViewHolder item) {
                if (dialogRemoveFinished == 2) {
                    dialogRemoveFinished = 1;
                }
            }

            @Override
            public void onAddFinished(RecyclerView.ViewHolder item) {
                if (dialogInsertFinished == 2) {
                    dialogInsertFinished = 1;
                }
            }

            @Override
            public void onChangeFinished(RecyclerView.ViewHolder item, boolean oldItem) {
                if (dialogChangeFinished == 2) {
                    dialogChangeFinished = 1;
                }
            }

            @Override
            protected void onAllAnimationsDone() {
                if (dialogRemoveFinished == 1 || dialogInsertFinished == 1 || dialogChangeFinished == 1) {
                    onDialogAnimationFinished();
                }
            }
        };
        listViewAll.setItemAnimator(dialogsItemAnimator);
        listViewAll.setVerticalScrollBarEnabled(true);
        listViewAll.setInstantClick(true);
        listViewAll.setTag(4);
        layoutManagerAll = new DialogsLayoutManager(context, Tab.ALL);
        layoutManagerAll.setOrientation(LinearLayoutManager.VERTICAL);
        listViewAll.setLayoutManager(layoutManagerAll);
        listViewAll.setVerticalScrollbarPosition(LocaleController.isRTL ? RecyclerListView.SCROLLBAR_POSITION_LEFT : RecyclerListView.SCROLLBAR_POSITION_RIGHT);
        listViewAll.setOnItemClickListener(new OnItemClickListener(Tab.ALL));
        listViewAll.setOnItemLongClickListener(new OnItemLongClickListener(Tab.ALL));

        swipeController = new SwipeController();

        itemTouchhelper = new ItemTouchHelper(swipeController);
        itemTouchhelper.attachToRecyclerView(listViewAll);

        searchEmptyView = new EmptyTextProgressView(context);
        searchEmptyView.setVisibility(View.GONE);
        searchEmptyView.setShowAtCenter(true);
        searchEmptyView.setTopImage(R.drawable.settings_noresults);
        searchEmptyView.setText(LocaleController.getString("SettingsNoResults", R.string.SettingsNoResults));
        contentView.addView(searchEmptyView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        progressView = new RadialProgressView(context);
        progressView.setVisibility(View.GONE);
        contentView.addView(progressView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER));

        floatingButtonContainer = new FrameLayout(context);
        floatingButtonContainer.setVisibility(onlySelect || folderId != 0 ? View.GONE : View.VISIBLE);
        contentView.addView(floatingButtonContainer, LayoutHelper.createFrame((Build.VERSION.SDK_INT >= 21 ? 56 : 60) + 20, (Build.VERSION.SDK_INT >= 21 ? 56 : 60) + 14, (LocaleController.isRTL ? Gravity.LEFT : Gravity.RIGHT) | Gravity.BOTTOM, LocaleController.isRTL ? 4 : 0, 0, LocaleController.isRTL ? 0 : 4, 0));
        floatingButtonContainer.setOnClickListener(v -> {
            if (currentTab == Tab.FOLDERS) {
                onFabClickedFavorites();
            } else if (currentTab == Tab.UNREAD) {
                onFabClickedUnread();
            } else {
                onFabClickedNormal();
            }
        });

        floatingButton = new ImageView(context);
        floatingButton.setScaleType(ImageView.ScaleType.CENTER);
        Drawable drawable = Theme.createSimpleSelectorCircleDrawable(AndroidUtilities.dp(56), Theme.getColor(Theme.key_chats_actionBackground), Theme.getColor(Theme.key_chats_actionPressedBackground));
        if (Build.VERSION.SDK_INT < 21) {
            Drawable shadowDrawable = context.getResources().getDrawable(R.drawable.floating_shadow).mutate();
            shadowDrawable.setColorFilter(new PorterDuffColorFilter(0xff000000, PorterDuff.Mode.MULTIPLY));
            CombinedDrawable combinedDrawable = new CombinedDrawable(shadowDrawable, drawable, 0, 0);
            combinedDrawable.setIconSize(AndroidUtilities.dp(56), AndroidUtilities.dp(56));
            drawable = combinedDrawable;
        }
        floatingButton.setBackgroundDrawable(drawable);
        floatingButton.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_chats_actionIcon), PorterDuff.Mode.MULTIPLY));
        floatingButton.setImageResource(R.drawable.floating_pencil);
        if (Build.VERSION.SDK_INT >= 21) {
            StateListAnimator animator = new StateListAnimator();
            animator.addState(new int[]{android.R.attr.state_pressed}, ObjectAnimator.ofFloat(floatingButton, View.TRANSLATION_Z, AndroidUtilities.dp(2), AndroidUtilities.dp(4)).setDuration(200));
            animator.addState(new int[]{}, ObjectAnimator.ofFloat(floatingButton, View.TRANSLATION_Z, AndroidUtilities.dp(4), AndroidUtilities.dp(2)).setDuration(200));
            floatingButton.setStateListAnimator(animator);
            floatingButton.setOutlineProvider(new ViewOutlineProvider() {
                @SuppressLint("NewApi")
                @Override
                public void getOutline(View view, Outline outline) {
                    outline.setOval(0, 0, AndroidUtilities.dp(56), AndroidUtilities.dp(56));
                }
            });
        }
        floatingButtonContainer.setContentDescription(LocaleController.getString("NewMessageTitle", R.string.NewMessageTitle));
        floatingButtonContainer.addView(floatingButton, LayoutHelper.createFrame((Build.VERSION.SDK_INT >= 21 ? 56 : 60), (Build.VERSION.SDK_INT >= 21 ? 56 : 60), Gravity.LEFT | Gravity.TOP, 10, 0, 10, 0));


        tabViews.put(Tab.ALL, listViewAll);
        tabIcons.put(Tab.ALL, R.drawable.ic_tab_all);

        //TODO UNREAD
        listViewUnread = new RecyclerListView(context);
        listViewUnread.setVerticalScrollBarEnabled(true);
        listViewUnread.setItemAnimator(null);
        listViewUnread.setInstantClick(true);
        listViewUnread.setLayoutAnimation(null);
        listViewUnread.setTag(4);
        LinearLayoutManager layoutManagerUnread = new DialogsLayoutManager(context, Tab.UNREAD);
        layoutManagerUnread.setOrientation(LinearLayoutManager.VERTICAL);
        listViewUnread.setLayoutManager(layoutManagerUnread);
        listViewUnread.setVerticalScrollbarPosition(LocaleController.isRTL ? RecyclerListView.SCROLLBAR_POSITION_LEFT : RecyclerListView.SCROLLBAR_POSITION_RIGHT);
        listViewUnread.setOnItemClickListener(new OnItemClickListener(Tab.UNREAD));
        listViewUnread.setOnItemLongClickListener(new OnItemLongClickListener(Tab.UNREAD));
        tabViews.put(Tab.UNREAD, listViewUnread);
        tabIcons.put(Tab.UNREAD, R.drawable.ic_tab_unread);

        //TODO GROUPS
        listViewGroups = new RecyclerListView(context);
        listViewGroups.setVerticalScrollBarEnabled(true);
        listViewGroups.setItemAnimator(null);
        listViewGroups.setInstantClick(true);
        listViewGroups.setLayoutAnimation(null);
        listViewGroups.setTag(4);
        layoutManagerGroups = new DialogsLayoutManager(context, Tab.GROUPS);
        layoutManagerGroups.setOrientation(LinearLayoutManager.VERTICAL);
        listViewGroups.setLayoutManager(layoutManagerGroups);
        listViewGroups.setVerticalScrollbarPosition(LocaleController.isRTL ? RecyclerListView.SCROLLBAR_POSITION_LEFT : RecyclerListView.SCROLLBAR_POSITION_RIGHT);
        listViewGroups.setOnItemClickListener(new OnItemClickListener(Tab.GROUPS));
        listViewGroups.setOnItemLongClickListener(new OnItemLongClickListener(Tab.GROUPS));
        tabViews.put(Tab.GROUPS, listViewGroups);
        tabIcons.put(Tab.GROUPS, R.drawable.ic_tab_groups);

        //TODO CHATS
        listViewChats = new RecyclerListView(context);
        listViewChats.setVerticalScrollBarEnabled(true);
        listViewChats.setItemAnimator(null);
        listViewChats.setInstantClick(true);
        listViewChats.setLayoutAnimation(null);
        listViewChats.setTag(4);
        layoutManagerChats = new DialogsLayoutManager(context, Tab.CHATS);
        layoutManagerChats.setOrientation(LinearLayoutManager.VERTICAL);
        listViewChats.setLayoutManager(layoutManagerChats);
        listViewChats.setVerticalScrollbarPosition(LocaleController.isRTL ? RecyclerListView.SCROLLBAR_POSITION_LEFT : RecyclerListView.SCROLLBAR_POSITION_RIGHT);
        listViewChats.setOnItemClickListener(new OnItemClickListener(Tab.CHATS));
        listViewChats.setOnItemLongClickListener(new OnItemLongClickListener(Tab.CHATS));
        tabViews.put(Tab.CHATS, listViewChats);
        tabIcons.put(Tab.CHATS, R.drawable.ic_tab_chats);

        //TODO CHANNELS
        listViewChannels = new RecyclerListView(context);
        listViewChannels.setVerticalScrollBarEnabled(true);
        listViewChannels.setItemAnimator(null);
        listViewChannels.setInstantClick(true);
        listViewChannels.setLayoutAnimation(null);
        listViewChannels.setTag(4);
        layoutManagerChannels = new DialogsLayoutManager(context, Tab.CHANNELS);
        layoutManagerChannels.setOrientation(LinearLayoutManager.VERTICAL);
        listViewChannels.setLayoutManager(layoutManagerChannels);
        listViewChannels.setVerticalScrollbarPosition(LocaleController.isRTL ? RecyclerListView.SCROLLBAR_POSITION_LEFT : RecyclerListView.SCROLLBAR_POSITION_RIGHT);
        listViewChannels.setOnItemClickListener(new OnItemClickListener(Tab.CHANNELS));
        listViewChannels.setOnItemLongClickListener(new OnItemLongClickListener(Tab.CHANNELS));
        tabViews.put(Tab.CHANNELS, listViewChannels);
        tabIcons.put(Tab.CHANNELS, R.drawable.ic_tab_channels);

        //TODO BOTS
        listViewBots = new RecyclerListView(context);
        listViewBots.setVerticalScrollBarEnabled(true);
        listViewBots.setItemAnimator(null);
        listViewBots.setInstantClick(true);
        listViewBots.setLayoutAnimation(null);
        listViewBots.setTag(4);
        layoutManagerBots = new DialogsLayoutManager(context, Tab.BOTS);
        layoutManagerBots.setOrientation(LinearLayoutManager.VERTICAL);
        listViewBots.setLayoutManager(layoutManagerBots);
        listViewBots.setVerticalScrollbarPosition(LocaleController.isRTL ? RecyclerListView.SCROLLBAR_POSITION_LEFT : RecyclerListView.SCROLLBAR_POSITION_RIGHT);
        listViewBots.setOnItemClickListener(new OnItemClickListener(Tab.BOTS));
        listViewBots.setOnItemLongClickListener(new OnItemLongClickListener(Tab.BOTS));

        tabViews.put(Tab.BOTS, listViewBots);
        tabIcons.put(Tab.BOTS, R.drawable.ic_tab_bots);

        //TODO FAVORITES
        listViewFavorites = new RecyclerListView(context);
        listViewFavorites.setVerticalScrollBarEnabled(true);
        listViewFavorites.setItemAnimator(null);
        listViewFavorites.setInstantClick(true);
        listViewFavorites.setLayoutAnimation(null);
        listViewFavorites.setTag(4);
        layoutManagerFavorites = new DialogsLayoutManager(context, Tab.FOLDERS);
        layoutManagerFavorites.setOrientation(LinearLayoutManager.VERTICAL);
        listViewFavorites.setLayoutManager(layoutManagerFavorites);
        listViewFavorites.setVerticalScrollbarPosition(LocaleController.isRTL ? RecyclerListView.SCROLLBAR_POSITION_LEFT : RecyclerListView.SCROLLBAR_POSITION_RIGHT);
        listViewFavorites.setOnItemClickListener((view, position) -> {
            if (listViewFavorites == null || listViewFavorites.getAdapter() == null || getParentActivity() == null) {
                return;
            }

            ArrayList<DialogsFolder> folders = getDialogsFoldersArray(currentAccount);

            if (position > folders.size()) {
                return;
            }

            Bundle args = new Bundle();
            args.putInt("dialogsType", 8);
            args.putString("folderName", folders.get(position).getName());
            args.putLong("existingFolderId", folders.get(position).getId());
            presentFragment(new DialogsActivity(args));
        });
        listViewFavorites.setOnItemLongClickListener((view, position) -> {
            if (getParentActivity() == null) {
                return false;
            }

            ArrayList<DialogsFolder> folders = MessagesController.getInstance(currentAccount).dialogFoldersAll;
            if (folders.isEmpty() || position > folders.size()) {
                return false;
            }

            showFolderContextMenu(folders.get(position));
            return true;
        });
        tabViews.put(Tab.FOLDERS, listViewFavorites);
        tabIcons.put(Tab.FOLDERS, R.drawable.ic_tab_favorites);


        listViewAll.setOnScrollListener(new OnDialogsScrollListener(Tab.ALL));
        listViewUnread.setOnScrollListener(new OnDialogsScrollListener(Tab.UNREAD));
        listViewGroups.setOnScrollListener(new OnDialogsScrollListener(Tab.GROUPS));
        listViewChats.setOnScrollListener(new OnDialogsScrollListener(Tab.CHATS));
        listViewChannels.setOnScrollListener(new OnDialogsScrollListener(Tab.CHANNELS));
        listViewBots.setOnScrollListener(new OnDialogsScrollListener(Tab.BOTS));

        tabsAdapter = buildNewTabsAdapter();
        tabsViewPager.setAdapter(tabsAdapter);
        tabsAdapter.notifyDataSetChanged();
        setupTabsBehavior();
        setupTabsAppearance();

        searchEmptyView = new EmptyTextProgressView(context);
        searchEmptyView.setVisibility(View.GONE);
        searchEmptyView.setShowAtCenter(true);
        searchEmptyView.setText(LocaleController.getString("NoResult", R.string.NoResult));
        contentView.addView(searchEmptyView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        progressView = new RadialProgressView(context);
        progressView.setVisibility(View.GONE);
        contentView.addView(progressView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER));


        if (searchString == null) {
            dialogsAdapterAll = new DialogsAdapter(context, dialogsType, folderId, onlySelect, Tab.ALL);
            tabAdapters.put(Tab.ALL, dialogsAdapterAll);
            dialogsAdapterUnread = new DialogsAdapter(context, dialogsType, folderId, onlySelect, Tab.UNREAD);
            tabAdapters.put(Tab.UNREAD, dialogsAdapterUnread);
            dialogsAdapterGroups = new DialogsAdapter(context, dialogsType, folderId, onlySelect, Tab.GROUPS);
            tabAdapters.put(Tab.GROUPS, dialogsAdapterGroups);
            dialogsAdapterChats = new DialogsAdapter(context, dialogsType, folderId, onlySelect, Tab.CHATS);
            tabAdapters.put(Tab.CHATS, dialogsAdapterChats);
            dialogsAdapterChannels = new DialogsAdapter(context, dialogsType, folderId, onlySelect, Tab.CHANNELS);
            tabAdapters.put(Tab.CHANNELS, dialogsAdapterChannels);
            dialogsAdapterBots = new DialogsAdapter(context, dialogsType, folderId, onlySelect, Tab.BOTS);
            tabAdapters.put(Tab.BOTS, dialogsAdapterBots);
            dialogsAdapterFavorites = new DialogsFolderAdapter(context);
            tabAdapters.put(Tab.FOLDERS, dialogsAdapterFavorites);

            if (AndroidUtilities.isTablet() && openedDialogId != 0) {
                dialogsAdapterAll.setOpenedDialogId(openedDialogId);
                dialogsAdapterUnread.setOpenedDialogId(openedDialogId);
                dialogsAdapterGroups.setOpenedDialogId(openedDialogId);
                dialogsAdapterChats.setOpenedDialogId(openedDialogId);
                dialogsAdapterChannels.setOpenedDialogId(openedDialogId);
                dialogsAdapterBots.setOpenedDialogId(openedDialogId);
            }
            listViewAll.setAdapter(dialogsAdapterAll);
            listViewUnread.setAdapter(dialogsAdapterUnread);
            listViewGroups.setAdapter(dialogsAdapterGroups);
            listViewChats.setAdapter(dialogsAdapterChats);
            listViewChannels.setAdapter(dialogsAdapterChannels);
            listViewBots.setAdapter(dialogsAdapterBots);
            listViewFavorites.setAdapter(dialogsAdapterFavorites);
        }
        int type = 0;
        if (searchString != null) {
            type = 2;
        } else if (!onlySelect) {
            type = 1;
        }
        dialogsSearchAdapter = new DialogsSearchAdapter(context, type, dialogsType);
        dialogsSearchAdapter.setDelegate(new DialogsSearchAdapter.DialogsSearchAdapterDelegate() {
            @Override
            public void searchStateChanged(boolean search) {
                if (searching && searchWas && searchEmptyView != null) {
                    if (search) {
                        searchEmptyView.showProgress();
                    } else {
                        searchEmptyView.showTextView();
                    }
                }
            }

            @Override
            public void didPressedOnSubDialog(long did) {
                if (onlySelect) {
                    if (dialogsAdapterAll.hasSelectedDialogs()) {
                        dialogsAdapterAll.addOrRemoveSelectedDialog(did, null);
                        updateSelectedCount();
                        closeSearch();
                    } else {
                        didSelectResult(did, true, false);
                    }
                } else {
                    int lower_id = (int) did;
                    Bundle args = new Bundle();
                    if (lower_id > 0) {
                        args.putInt("user_id", lower_id);
                    } else {
                        args.putInt("chat_id", -lower_id);
                    }
                    closeSearch();
                    if (AndroidUtilities.isTablet()) {
                        if (dialogsAdapterAll != null) {
                            dialogsAdapterAll.setOpenedDialogId(openedDialogId = did);
                            updateVisibleRows(MessagesController.UPDATE_MASK_SELECT_DIALOG);
                        }
                    }
                    if (searchString != null) {
                        if (getMessagesController().checkCanOpenChat(args, DialogsActivity.this)) {
                            getNotificationCenter().postNotificationName(NotificationCenter.closeChats);
                            presentFragment(new ChatActivity(args));
                        }
                    } else {
                        if (getMessagesController().checkCanOpenChat(args, DialogsActivity.this)) {
                            presentFragment(new ChatActivity(args));
                        }
                    }
                }
            }

            @Override
            public void needRemoveHint(final int did) {
                if (getParentActivity() == null) {
                    return;
                }
                TLRPC.User user = getMessagesController().getUser(did);
                if (user == null) {
                    return;
                }
                AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                builder.setTitle(LocaleController.getString("ChatHintsDeleteAlertTitle", R.string.ChatHintsDeleteAlertTitle));
                builder.setMessage(AndroidUtilities.replaceTags(LocaleController.formatString("ChatHintsDeleteAlert", R.string.ChatHintsDeleteAlert, ContactsController.formatName(user.first_name, user.last_name))));
                builder.setPositiveButton(LocaleController.getString("StickersRemove", R.string.StickersRemove), (dialogInterface, i) -> getDataQuery().removePeer(did));
                builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
                AlertDialog dialog = builder.create();
                showDialog(dialog);
                TextView button = (TextView) dialog.getButton(DialogInterface.BUTTON_POSITIVE);
                if (button != null) {
                    button.setTextColor(Theme.getColor(Theme.key_dialogTextRed2));
                }
            }

            @Override
            public void needClearList() {
                AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                builder.setTitle(LocaleController.getString("ClearSearchAlertTitle", R.string.ClearSearchAlertTitle));
                builder.setMessage(LocaleController.getString("ClearSearchAlert", R.string.ClearSearchAlert));
                builder.setPositiveButton(LocaleController.getString("ClearButton", R.string.ClearButton).toUpperCase(), (dialogInterface, i) -> {
                    if (dialogsSearchAdapter.isRecentSearchDisplayed()) {
                        dialogsSearchAdapter.clearRecentSearch();
                    } else {
                        dialogsSearchAdapter.clearRecentHashtags();
                    }
                });
                builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
                AlertDialog dialog = builder.create();
                showDialog(dialog);
                TextView button = (TextView) dialog.getButton(DialogInterface.BUTTON_POSITIVE);
                if (button != null) {
                    button.setTextColor(Theme.getColor(Theme.key_dialogTextRed2));
                }
            }
        });

        listViewAll.setEmptyView(folderId == 0 ? progressView : null);
        if (searchString != null) {
            actionBar.openSearchField(searchString, false);
        }

        if (!onlySelect && dialogsType == 0) {
            FragmentContextView fragmentLocationContextView = new FragmentContextView(context, this, true);
            contentView.addView(fragmentLocationContextView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 39, Gravity.TOP | Gravity.LEFT, 0, -36, 0, 0));

            FragmentContextView fragmentContextView = new FragmentContextView(context, this, false);
            contentView.addView(fragmentContextView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 39, Gravity.TOP | Gravity.LEFT, 0, -36, 0, 0));

            fragmentContextView.setAdditionalContextView(fragmentLocationContextView);
            fragmentLocationContextView.setAdditionalContextView(fragmentContextView);
        } else if (dialogsType == 3 && selectAlertString == null) {
            if (commentView != null) {
                commentView.onDestroy();
            }
            commentView = new ChatActivityEnterView(getParentActivity(), contentView, null, false);
            commentView.setAllowStickersAndGifs(false, false);
            commentView.setForceShowSendButton(true, false);
            commentView.setVisibility(View.GONE);
            contentView.addView(commentView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.BOTTOM));
            commentView.setDelegate(new ChatActivityEnterView.ChatActivityEnterViewDelegate() {
                @Override
                public void onMessageSend(CharSequence message) {
                    if (delegate == null) {
                        return;
                    }
                    ArrayList<Long> selectedDialogs = dialogsAdapterAll.getSelectedDialogs();
                    if (selectedDialogs.isEmpty()) {
                        return;
                    }
                    delegate.didSelectDialogs(DialogsActivity.this, selectedDialogs, message, false);
                }

                @Override
                public void onSwitchRecordMode(boolean video) {

                }

                @Override
                public void onTextSelectionChanged(int start, int end) {

                }


                @Override
                public void onStickersExpandedChange() {

                }


                @Override
                public void onPreAudioVideoRecord() {

                }

                @Override
                public void onTextChanged(final CharSequence text, boolean bigChange) {

                }

                @Override
                public void onTextSpansChanged(CharSequence text) {

                }

                @Override
                public void needSendTyping() {

                }

                @Override
                public void onAttachButtonHidden() {

                }

                @Override
                public void onAttachButtonShow() {

                }

                @Override
                public void onMessageEditEnd(boolean loading) {

                }

                @Override
                public void onWindowSizeChanged(int size) {

                }

                @Override
                public void onStickersTab(boolean opened) {

                }

                @Override
                public void didPressedAttachButton() {

                }


                @Override
                public void needStartRecordVideo(int state) {

                }

                @Override
                public void needChangeVideoPreviewState(int state, float seekProgress) {

                }

                @Override
                public void needStartRecordAudio(int state) {

                }

                @Override
                public void needShowMediaBanHint() {

                }
            });
        }

        for (int a = 0; a < 2; a++) {
            undoView[a] = new UndoView(context) {
                @Override
                public void setTranslationY(float translationY) {
                    super.setTranslationY(translationY);
                    if (this == undoView[0] && undoView[1].getVisibility() != VISIBLE) {
                        float diff = getMeasuredHeight() + AndroidUtilities.dp(8) - translationY;
                        if (!floatingHidden) {
                            floatingButtonContainer.setTranslationY(floatingButtonContainer.getTranslationY() + additionalFloatingTranslation - diff);
                        }
                        additionalFloatingTranslation = diff;
                    }
                }

                @Override
                protected boolean canUndo() {
                    return !dialogsItemAnimator.isRunning();
                }
            };
            contentView.addView(undoView[a], LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.BOTTOM | Gravity.LEFT, 8, 0, 8, 8));
        }


        currentTab = tabsManager.getTabByPosition(0, currentTabsMode);
        tabsViewPager.setCurrentItem(0, false);


        if (folderId != 0) {
            actionBar.setBackgroundColor(Theme.getColor(Theme.key_actionBarDefaultArchived));
            listViewAll.setGlowColor(Theme.getColor(Theme.key_actionBarDefaultArchived));
            actionBar.setTitleColor(Theme.getColor(Theme.key_actionBarDefaultArchivedTitle));
            actionBar.setItemsColor(Theme.getColor(Theme.key_actionBarDefaultArchivedIcon), false);
            actionBar.setItemsBackgroundColor(Theme.getColor(Theme.key_actionBarDefaultArchivedSelector), false);
            actionBar.setSearchTextColor(Theme.getColor(Theme.key_actionBarDefaultArchivedSearch), false);
            actionBar.setSearchTextColor(Theme.getColor(Theme.key_actionBarDefaultArchivedSearchPlaceholder), true);
        }

        return fragmentView;
    }


    public void setupTabs(FrameLayout container) {
        Context context = getParentActivity();
        tabsContainer = new CoordinatorLayout(context);

        container.addView(tabsContainer, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.TOP));

        tabsLayout = new TabLayout(context);
        if (Build.VERSION.SDK_INT >= 21) {
            tabsLayout.setElevation(6f);
            tabsLayout.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
            tabsLayout.setSelectedTabIndicatorColor(Theme.getColor(Theme.key_actionBarDefault));
        }

        tabsViewPager = new ViewPager(context) {
            @Override
            public boolean onInterceptTouchEvent(MotionEvent ev) {
                int tab = tabsViewPager.getCurrentItem();
                if (tab != 0 && getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                } else if (tab == 0 && getParent() != null) {
                    if (ev.getAction() == MotionEvent.ACTION_DOWN) {
                        float x = ev.getX();
                        int measuredWidth = tabsContainer.getMeasuredWidth();
                        float maxPossibleX = measuredWidth / 3f;

                        if (x > maxPossibleX && dialogsType != 8) {
                            getParent().requestDisallowInterceptTouchEvent(true);
                        } else {
                            getParent().requestDisallowInterceptTouchEvent(false);
                        }
                    }
                }

                return canSwipeHorizontally && super.onInterceptTouchEvent(ev);
            }

            @Override
            public boolean onTouchEvent(MotionEvent ev) {
                return canSwipeHorizontally && super.onTouchEvent(ev);
            }
        };
        tabsLayout.setupWithViewPager(tabsViewPager);

        tabsLayoutSelectedListener = new TabLayout.ViewPagerOnTabSelectedListener(tabsViewPager) {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                super.onTabSelected(tab);
                View view = tab.getCustomView();
                if (view instanceof CustomTabView) {
                    ((CustomTabView) view).highlightIcon(true);
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                super.onTabUnselected(tab);
                View view = tab.getCustomView();
                if (view instanceof CustomTabView) {
                    ((CustomTabView) view).highlightIcon(false);
                }
            }
        };

        tabsLayout.addOnTabSelectedListener(tabsLayoutSelectedListener);

        tabsViewPagerListener = new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                currentTab = tabsManager.getTabByPosition(position, currentTabsMode);
                RecyclerListView.SelectionAdapter adapter = tabAdapters.get(currentTab);
                if (adapter != null) {
                    adapter.notifyDataSetChanged();
                }
                refreshTabIndicators();
                displayActualFloatingButton(position);

                if (currentTab == Tab.UNREAD &&
                        MessagesController.getInstance(currentAccount).dialogsUnread.size() == 0) {
                    hideFloatingButton(true);
                } else {
                    hideFloatingButton(false);
                }
            }
        };

        tabsViewPager.addOnPageChangeListener(tabsViewPagerListener);

        AppBarLayout tabsAppbar = new AppBarLayout(getParentActivity());
        tabsAppbar.addView(tabsLayout, LayoutHelper.createAppBar(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        AppBarLayout.LayoutParams params =
                (AppBarLayout.LayoutParams) tabsLayout.getLayoutParams();
        params.setScrollFlags(AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL
                | AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS
                | AppBarLayout.LayoutParams.SCROLL_FLAG_SNAP);

        tabsContainer.addView(tabsAppbar, LayoutHelper.createCoordinator(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        tabsContainer.addView(tabsViewPager, LayoutHelper.createCoordinator(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        CoordinatorLayout.LayoutParams tabsViewPagerParams =
                (CoordinatorLayout.LayoutParams) tabsViewPager.getLayoutParams();
        tabsViewPagerParams.setBehavior(new AppBarLayout.ScrollingViewBehavior());
        tabsViewPager.requestLayout();

    }

    @Override
    public void onResume() {
        super.onResume();
        RecyclerListView.SelectionAdapter adapter = tabAdapters.get(currentTab);
        if (adapter != null && !dialogsListFrozen) {
            adapter.notifyDataSetChanged();
            refreshTabIndicators();
        }

        if (commentView != null) {
            commentView.onResume();
        }
        if (dialogsSearchAdapter != null) {
            dialogsSearchAdapter.notifyDataSetChanged();
        }
        if (dialogsAdapterFavorites != null) {
            dialogsAdapterFavorites.notifyDataSetChanged();
        }
        if (checkPermission && !onlySelect && Build.VERSION.SDK_INT >= 23) {
            Activity activity = getParentActivity();
            if (activity != null) {
                checkPermission = false;
                boolean hasNotContactsPermission = activity.checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED;
                boolean hasNotStoragePermission = activity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED;
                if (hasNotContactsPermission || hasNotStoragePermission) {
                    if (hasNotContactsPermission && askAboutContacts && getUserConfig().syncContacts && activity.shouldShowRequestPermissionRationale(Manifest.permission.READ_CONTACTS)) {
                        AlertDialog.Builder builder = AlertsCreator.createContactsPermissionDialog(activity, param -> {
                            askAboutContacts = param != 0;
                            MessagesController.getGlobalNotificationsSettings().edit().putBoolean("askAboutContacts", askAboutContacts).commit();
                            askForPermissons(false);
                        });
                        showDialog(permissionDialog = builder.create());
                    } else if (hasNotStoragePermission && activity.shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                        builder.setTitle(LocaleController.getString("AppName", R.string.AppName));
                        builder.setMessage(LocaleController.getString("PermissionStorage", R.string.PermissionStorage));
                        builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), null);
                        showDialog(permissionDialog = builder.create());
                    } else {
                        askForPermissons(true);
                    }
                }
            }
        } else if (!onlySelect && XiaomiUtilities.isMIUI() && Build.VERSION.SDK_INT >= 19 && !XiaomiUtilities.isCustomPermissionGranted(XiaomiUtilities.OP_SHOW_WHEN_LOCKED)) {
            if (getParentActivity() == null) {
                return;
            }
            if (MessagesController.getGlobalNotificationsSettings().getBoolean("askedAboutMiuiLockscreen", false)) {
                return;
            }
            showDialog(new AlertDialog.Builder(getParentActivity())
                    .setTitle(LocaleController.getString("AppName", R.string.AppName))
                    .setMessage(LocaleController.getString("PermissionXiaomiLockscreen", R.string.PermissionXiaomiLockscreen))
                    .setPositiveButton(LocaleController.getString("PermissionOpenSettings", R.string.PermissionOpenSettings), (dialog, which) -> {
                        Intent intent = XiaomiUtilities.getPermissionManagerIntent();
                        if (intent != null) {
                            try {
                                getParentActivity().startActivity(intent);
                            } catch (Exception x) {
                                try {
                                    intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                    intent.setData(Uri.parse("package:" + ApplicationLoader.applicationContext.getPackageName()));
                                    getParentActivity().startActivity(intent);
                                } catch (Exception xx) {
                                    FileLog.e(xx);
                                }
                            }
                        }
                    })
                    .setNegativeButton(LocaleController.getString("ContactsPermissionAlertNotNow", R.string.ContactsPermissionAlertNotNow), (dialog, which) -> MessagesController.getGlobalNotificationsSettings().edit().putBoolean("askedAboutMiuiLockscreen", true).commit())
                    .create());
        }

        tabsViewPager.post(() -> {
            tabsViewPagerListener.onPageSelected(tabsManager.getPositionByTab(currentTab, currentTabsMode));
            tabsLayoutSelectedListener.onTabSelected(tabsLayout.getTabAt(tabsManager.getPositionByTab(currentTab, currentTabsMode)));
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        if (commentView != null) {
            commentView.onResume();
        }
        if (undoView[0] != null) {
            undoView[0].hide(true, 0);
        }
    }

    @Override
    public boolean onBackPressed() {
        if (actionBar != null && actionBar.isActionModeShowed()) {
            hideActionMode(true);
            return false;
        } else if (commentView != null && commentView.isPopupShowing()) {
            commentView.hidePopup(true);
            return false;
        }
        return super.onBackPressed();
    }

    @Override
    protected void onBecomeFullyHidden() {
        if (closeSearchFieldOnHide) {
            if (actionBar != null) {
                actionBar.closeSearchField();
            }
            if (searchObject != null) {
                dialogsSearchAdapter.putRecentSearch(searchDialogId, searchObject);
                searchObject = null;
            }
            closeSearchFieldOnHide = false;
        }
        if (undoView[0] != null) {
            undoView[0].hide(true, 0);
        }
    }

    private boolean hasHiddenArchive() {
        RecyclerListView list = tabViews.get(currentTab);
        if (list != null) {
            return list.getAdapter() == tabAdapters.get(currentTab) && !onlySelect && dialogsType == 0 && folderId == 0 && getMessagesController().hasHiddenArchive();
        }return false;
    }

    private boolean waitingForDialogsAnimationEnd() {
        return dialogsItemAnimator.isRunning() || dialogRemoveFinished != 0 || dialogInsertFinished != 0 || dialogChangeFinished != 0;
    }

    private void onDialogAnimationFinished() {
        dialogRemoveFinished = 0;
        dialogInsertFinished = 0;
        dialogChangeFinished = 0;
        AndroidUtilities.runOnUIThread(() -> {
            if (folderId != 0 && frozenDialogsList.isEmpty()) {
                RecyclerListView list = tabViews.get(currentTab);
                if (list != null) {
                    list.setEmptyView(null);
                }

                progressView.setVisibility(View.INVISIBLE);
                finishFragment();
            }
            setDialogsListFrozen(false);
            updateDialogIndices();
        });
    }

    private void hideActionMode(boolean animateCheck) {
        if (!(tabAdapters.get(currentTab) instanceof DialogsAdapter)) {
            return;
        }
        actionBar.hideActionMode();
        if (menuDrawable != null) {
            actionBar.setBackButtonContentDescription(LocaleController.getString("AccDescrOpenMenu", R.string.AccDescrOpenMenu));
        }
        ((DialogsAdapter) tabAdapters.get(currentTab)).getSelectedDialogs().clear();
        ((DialogsAdapter) tabAdapters.get(currentTab)).notifyDataSetChanged();
        if (menuDrawable != null) {
            menuDrawable.setRotation(0, true);
        } else if (backDrawable != null) {
            backDrawable.setRotation(0, true);
        }
        allowMoving = false;
        if (movingWas) {
            getMessagesController().reorderPinnedDialogs(folderId, null, 0);
            movingWas = false;
        }
        updateCounters(true, false);
        ((DialogsAdapter) tabAdapters.get(currentTab)).onReorderStateChanged(false);
        updateVisibleRows(MessagesController.UPDATE_MASK_REORDER | MessagesController.UPDATE_MASK_CHECK | (animateCheck ? MessagesController.UPDATE_MASK_CHAT : 0));
    }

    private int getPinnedCount() {
        int pinnedCount = 0;
        ArrayList<TLRPC.Dialog> dialogs = getMessagesController().getDialogs(folderId);
        for (int a = 0, N = dialogs.size(); a < N; a++) {
            TLRPC.Dialog dialog = dialogs.get(a);
            if (dialog instanceof TLRPC.TL_dialogFolder) {
                continue;
            }
            int lower_id = (int) dialog.id;
            if (dialog.pinned) {
                pinnedCount++;
            } else {
                break;
            }
        }
        return pinnedCount;
    }

    private void perfromSelectedDialogsAction(int action, boolean alert) {
        if (getParentActivity() == null) {
            return;
        }
        if (!(tabAdapters.get(currentTab) instanceof DialogsAdapter)) {
            return;
        }

        ArrayList<Long> selectedDialogs = ((DialogsAdapter) tabAdapters.get(currentTab)).getSelectedDialogs();
        int count = selectedDialogs.size();
        if (action == archive) {
            ArrayList<Long> copy = new ArrayList<>(selectedDialogs);
            getMessagesController().addDialogToFolder(copy, folderId == 0 ? 1 : 0, -1, null, 0);
            hideActionMode(false);
            if (folderId == 0) {
                SharedPreferences preferences = MessagesController.getGlobalMainSettings();
                boolean hintShowed = preferences.getBoolean("archivehint_l", false);
                preferences.edit().putBoolean("archivehint_l", true).commit();
                int undoAction;
                if (hintShowed) {
                    undoAction = copy.size() > 1 ? UndoView.ACTION_ARCHIVE_FEW : UndoView.ACTION_ARCHIVE;
                } else {
                    undoAction = copy.size() > 1 ? UndoView.ACTION_ARCHIVE_FEW_HINT : UndoView.ACTION_ARCHIVE_HINT;
                }
                getUndoView().showWithAction(0, undoAction, null, () -> getMessagesController().addDialogToFolder(copy, folderId == 0 ? 0 : 1, -1, null, 0));
            } else {
                ArrayList<TLRPC.Dialog> dialogs = getMessagesController().getDialogs(folderId);
                if (dialogs.isEmpty()) {
                    RecyclerListView tabView = tabViews.get(currentTab);
                    if (tabView != null) {
                        tabView.setEmptyView(null);
                    }
                    progressView.setVisibility(View.INVISIBLE);
                    finishFragment();
                }
            }
            return;
        } else if (action == pin && canPinCount != 0) {
            int pinnedCount = 0;
            int pinnedSecretCount = 0;
            int newPinnedCount = 0;
            int newPinnedSecretCount = 0;
            ArrayList<TLRPC.Dialog> dialogs = getMessagesController().getDialogs(folderId);
            for (int a = 0, N = dialogs.size(); a < N; a++) {
                TLRPC.Dialog dialog = dialogs.get(a);
                if (dialog instanceof TLRPC.TL_dialogFolder) {
                    continue;
                }
                int lower_id = (int) dialog.id;
                if (dialog.pinned) {
                    if (lower_id == 0) {
                        pinnedSecretCount++;
                    } else {
                        pinnedCount++;
                    }
                } else {
                    break;
                }
            }
            for (int a = 0; a < count; a++) {
                long selectedDialog = selectedDialogs.get(a);
                TLRPC.Dialog dialog = getMessagesController().dialogs_dict.get(selectedDialog);
                if (dialog == null || dialog.pinned) {
                    continue;
                }
                int lower_id = (int) selectedDialog;
                if (lower_id == 0) {
                    newPinnedSecretCount++;
                } else {
                    newPinnedCount++;
                }
            }
            int maxPinnedCount;
            if (folderId != 0) {
                maxPinnedCount = getMessagesController().maxFolderPinnedDialogsCount;
            } else {
                maxPinnedCount = getMessagesController().maxPinnedDialogsCount;
            }
            if (newPinnedSecretCount + pinnedSecretCount > maxPinnedCount || newPinnedCount + pinnedCount > maxPinnedCount) {
                AlertsCreator.showSimpleToast(DialogsActivity.this, LocaleController.formatString("PinToTopLimitReached", R.string.PinToTopLimitReached, LocaleController.formatPluralString("Chats", maxPinnedCount)));
                AndroidUtilities.shakeView(pinItem, 2, 0);
                Vibrator v = (Vibrator) getParentActivity().getSystemService(Context.VIBRATOR_SERVICE);
                if (v != null) {
                    v.vibrate(200);
                }
                return;
            }
        } else if ((action == delete || action == clear) && count > 1 && alert) {
            if (alert) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                if (action == delete) {
                    builder.setTitle(LocaleController.formatString("DeleteFewChatsTitle", R.string.DeleteFewChatsTitle, LocaleController.formatPluralString("ChatsSelected", count)));
                    builder.setMessage(LocaleController.getString("AreYouSureDeleteFewChats", R.string.AreYouSureDeleteFewChats));
                    builder.setPositiveButton(LocaleController.getString("Delete", R.string.Delete), (dialog1, which) -> {
                        getMessagesController().setDialogsInTransaction(true);
                        perfromSelectedDialogsAction(action, false);
                        getMessagesController().setDialogsInTransaction(false);
                        MessagesController.getInstance(currentAccount).checkIfFolderEmpty(folderId);
                        if (folderId != 0 && getDialogsArray(currentAccount, dialogsType, folderId, false, currentTab).size() == 0) {
                            RecyclerListView tabView = tabViews.get(currentTab);
                            if (tabView != null) {
                                tabView.setEmptyView(null);
                            }
                              progressView.setVisibility(View.INVISIBLE);
                            finishFragment();
                        }
                    });
                } else {
                    if (canClearCacheCount != 0) {
                        builder.setTitle(LocaleController.formatString("ClearCacheFewChatsTitle", R.string.ClearCacheFewChatsTitle, LocaleController.formatPluralString("ChatsSelectedClearCache", count)));
                        builder.setMessage(LocaleController.getString("AreYouSureClearHistoryCacheFewChats", R.string.AreYouSureClearHistoryCacheFewChats));
                        builder.setPositiveButton(LocaleController.getString("ClearHistoryCache", R.string.ClearHistoryCache), (dialog1, which) -> perfromSelectedDialogsAction(action, false));
                    } else {
                        builder.setTitle(LocaleController.formatString("ClearFewChatsTitle", R.string.ClearFewChatsTitle, LocaleController.formatPluralString("ChatsSelectedClear", count)));
                        builder.setMessage(LocaleController.getString("AreYouSureClearHistoryFewChats", R.string.AreYouSureClearHistoryFewChats));
                        builder.setPositiveButton(LocaleController.getString("ClearHistory", R.string.ClearHistory), (dialog1, which) -> perfromSelectedDialogsAction(action, false));
                    }
                }
                builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
                AlertDialog alertDialog = builder.create();
                showDialog(alertDialog);
                TextView button = (TextView) alertDialog.getButton(DialogInterface.BUTTON_POSITIVE);
                if (button != null) {
                    button.setTextColor(Theme.getColor(Theme.key_dialogTextRed2));
                }
                return;
            }
        } else if (action == folder) {
            ArrayList<Long> selectedIds = new ArrayList<>(selectedDialogs);
            if (dialogsType == 8) {
                removeSelectedDialogFromFolder(selectedIds);
            } else {
                showFoldersListSheet(selectedIds);
            }
        } else if (action == hideArchive) {
            SharedConfig.toggleArchiveHidden();
            if (SharedConfig.archiveHidden) {
                tabViews.get(Tab.UNREAD).smoothScrollBy(0, 260, CubicBezierInterpolator.EASE_OUT);
                tabViews.get(Tab.ALL).smoothScrollBy(0, 260, CubicBezierInterpolator.EASE_OUT);
                getUndoView().showWithAction(0, UndoView.ACTION_ARCHIVE_HIDDEN, null, null);
            }
        }
        boolean scrollToTop = false;
        for (int a = 0; a < count; a++) {
            long selectedDialog = selectedDialogs.get(a);
            TLRPC.Dialog dialog = getMessagesController().dialogs_dict.get(selectedDialog);
            if (dialog == null) {
                continue;
            }
            TLRPC.Chat chat;
            TLRPC.User user = null;
            int lower_id = (int) selectedDialog;
            int high_id = (int) (selectedDialog >> 32);
            if (lower_id != 0) {
                if (lower_id > 0) {
                    user = getMessagesController().getUser(lower_id);
                    chat = null;
                } else {
                    chat = getMessagesController().getChat(-lower_id);
                }
            } else {
                TLRPC.EncryptedChat encryptedChat = getMessagesController().getEncryptedChat(high_id);
                chat = null;
                if (encryptedChat != null) {
                    user = getMessagesController().getUser(encryptedChat.user_id);
                } else {
                    user = new TLRPC.TL_userEmpty();
                }
            }
            if (chat == null && user == null) {
                continue;
            }
            boolean isBot = user != null && user.bot && !MessagesController.isSupportUser(user);
            if (action == pin) {
                if (canPinCount != 0) {
                    if (dialog.pinned) {
                        continue;
                    }
                    if (getMessagesController().pinDialog(selectedDialog, true, null, -1)) {
                        scrollToTop = true;
                    }
                } else {
                    if (!dialog.pinned) {
                        continue;
                    }
                    if (getMessagesController().pinDialog(selectedDialog, false, null, -1)) {
                        scrollToTop = true;
                    }
                }
            } else if (action == read) {
                if (canReadCount != 0) {
                    getMessagesController().markMentionsAsRead(selectedDialog);
                    getMessagesController().markDialogAsRead(selectedDialog, dialog.top_message, dialog.top_message, dialog.last_message_date, false, 0, true);
                } else {
                    getMessagesController().markDialogAsUnread(selectedDialog, null, 0);
                }
            } else if (action == delete || action == clear) {
                if (count == 1) {
                    AlertsCreator.createClearOrDeleteDialogAlert(DialogsActivity.this, action == clear, chat, user, lower_id == 0, (param) -> {
                        hideActionMode(false);
                        if (action == clear && ChatObject.isChannel(chat) && (!chat.megagroup || !TextUtils.isEmpty(chat.username))) {
                            getMessagesController().deleteDialog(selectedDialog, 2, param);
                        } else {
                            if (action == delete && folderId != 0 && getDialogsArray(currentAccount, dialogsType, folderId, false, currentTab).size() == 1) {
                                progressView.setVisibility(View.INVISIBLE);
                            }
                            getUndoView().showWithAction(selectedDialog, action == clear ? UndoView.ACTION_CLEAR : UndoView.ACTION_DELETE, () -> {
                                if (action == clear) {
                                    getMessagesController().deleteDialog(selectedDialog, 1, param);
                                } else {
                                    if (chat != null) {
                                        if (ChatObject.isNotInChat(chat)) {
                                            getMessagesController().deleteDialog(selectedDialog, 0, param);
                                        } else {
                                            TLRPC.User currentUser = getMessagesController().getUser(getUserConfig().getClientUserId());
                                            getMessagesController().deleteUserFromChat((int) -selectedDialog, currentUser, null);
                                        }
                                    } else {
                                        getMessagesController().deleteDialog(selectedDialog, 0, param);
                                        if (isBot) {
                                            getMessagesController().blockUser((int) selectedDialog);
                                        }
                                    }
                                    if (AndroidUtilities.isTablet()) {
                                        getNotificationCenter().postNotificationName(NotificationCenter.closeChats, selectedDialog);
                                    }
                                    MessagesController.getInstance(currentAccount).checkIfFolderEmpty(folderId);
                                }
                            });
                        }
                    });
                    return;
                } else {
                    if (action == clear && canClearCacheCount != 0) {
                        getMessagesController().deleteDialog(selectedDialog, 2, false);
                    } else {
                        if (action == clear) {
                            getMessagesController().deleteDialog(selectedDialog, 1, false);
                        } else {
                            if (chat != null) {
                                if (ChatObject.isNotInChat(chat)) {
                                    getMessagesController().deleteDialog(selectedDialog, 0, false);
                                } else {
                                    TLRPC.User currentUser = getMessagesController().getUser(getUserConfig().getClientUserId());
                                    getMessagesController().deleteUserFromChat((int) -selectedDialog, currentUser, null);
                                }
                            } else {
                                getMessagesController().deleteDialog(selectedDialog, 0, false);
                                if (isBot) {
                                    getMessagesController().blockUser((int) selectedDialog);
                                }
                            }
                            if (AndroidUtilities.isTablet()) {
                                getNotificationCenter().postNotificationName(NotificationCenter.closeChats, selectedDialog);
                            }
                        }
                    }
                }
            } else if (action == mute) {
                if (count == 1 && canMuteCount == 1) {
                    showDialog(AlertsCreator.createMuteAlert(getParentActivity(), selectedDialog), dialog12 -> hideActionMode(true));
                    return;
                } else {
                    if (canUnmuteCount != 0) {
                        if (!getMessagesController().isDialogMuted(selectedDialog)) {
                            continue;
                        }
                        getNotificationsController().setDialogNotificationsSettings(selectedDialog, NotificationsController.SETTING_MUTE_UNMUTE);
                    } else {
                        if (getMessagesController().isDialogMuted(selectedDialog)) {
                            continue;
                        }
                        getNotificationsController().setDialogNotificationsSettings(selectedDialog, NotificationsController.SETTING_MUTE_FOREVER);
                    }
                }
            }
        }
        if (action == pin) {
            getMessagesController().reorderPinnedDialogs(folderId, null, 0);
        }
        if (scrollToTop) {
            hideFloatingButton(false);
            RecyclerListView tabView = tabViews.get(currentTab);
            if (tabView != null) {
                tabView.smoothScrollToPosition(hasHiddenArchive() ? 1 : 0);
            }

        }
        hideActionMode(action != pin && action != delete);
    }

    private void updateCounters(boolean hide, boolean isArchive) {
        if (!(tabAdapters.get(currentTab) instanceof DialogsAdapter)) {
            return;
        }
        int canClearHistoryCount = 0;
        int canDeleteCount = 0;
        int canUnpinCount = 0;
        int canArchiveCount = 0;
        int canUnarchiveCount = 0;
        canUnmuteCount = 0;
        canMuteCount = 0;
        canPinCount = 0;
        canReadCount = 0;
        canClearCacheCount = 0;
        if (hide) {
            return;
        }

        ArrayList<Long> selectedDialogs = ((DialogsAdapter) tabAdapters.get(currentTab)).getSelectedDialogs();
        int count = selectedDialogs.size();
        for (int a = 0; a < count; a++) {
            TLRPC.Dialog dialog = getMessagesController().dialogs_dict.get(selectedDialogs.get(a));
            if (dialog == null) {
                continue;
            }

            long selectedDialog = dialog.id;
            boolean pinned = dialog.pinned;
            boolean hasUnread = dialog.unread_count != 0 || dialog.unread_mark;
            if (getMessagesController().isDialogMuted(selectedDialog)) {
                canUnmuteCount++;
            } else {
                canMuteCount++;
            }

            if (hasUnread) {
                canReadCount++;
            }

            if (folderId == 1) {
                canUnarchiveCount++;
            } else if (selectedDialog != getUserConfig().getClientUserId() && selectedDialog != 777000 && !getMessagesController().isProxyDialog(selectedDialog, false)) {
                canArchiveCount++;
            }

            int lower_id = (int) selectedDialog;
            int high_id = (int) (selectedDialog >> 32);

            if (DialogObject.isChannel(dialog)) {
                final TLRPC.Chat chat = getMessagesController().getChat(-lower_id);
                CharSequence[] items;
                if (getMessagesController().isProxyDialog(dialog.id, true)) {
                    canClearCacheCount++;
                } else {
                    if (pinned) {
                        canUnpinCount++;
                    } else {
                        canPinCount++;
                    }
                    if (chat != null && chat.megagroup) {
                        if (TextUtils.isEmpty(chat.username)) {
                            canClearHistoryCount++;
                        } else {
                            canClearCacheCount++;
                        }
                        canDeleteCount++;
                    } else {
                        canClearCacheCount++;
                        canDeleteCount++;
                    }
                }
            } else {
                final boolean isChat = lower_id < 0 && high_id != 1;
                TLRPC.User user;
                TLRPC.Chat chat = isChat ? getMessagesController().getChat(-lower_id) : null;
                if (lower_id == 0) {
                    TLRPC.EncryptedChat encryptedChat = getMessagesController().getEncryptedChat(high_id);
                    if (encryptedChat != null) {
                        user = getMessagesController().getUser(encryptedChat.user_id);
                    } else {
                        user = new TLRPC.TL_userEmpty();
                    }
                } else {
                    user = !isChat && lower_id > 0 && high_id != 1 ? getMessagesController().getUser(lower_id) : null;
                }
                final boolean isBot = user != null && user.bot && !MessagesController.isSupportUser(user);

                if (pinned) {
                    canUnpinCount++;
                } else {
                    canPinCount++;
                }
                canClearHistoryCount++;
                canDeleteCount++;
            }
        }
        if (canDeleteCount != count) {
            deleteItem.setVisibility(View.GONE);
        } else {
            deleteItem.setVisibility(View.VISIBLE);
        }
        if (canClearCacheCount != 0 && canClearCacheCount != count || canClearHistoryCount != 0 && canClearHistoryCount != count) {
            clearItem.setVisibility(View.GONE);
        } else {
            clearItem.setVisibility(View.VISIBLE);
            if (canClearCacheCount != 0) {
                clearItem.setText(LocaleController.getString("ClearHistoryCache", R.string.ClearHistoryCache));
            } else {
                clearItem.setText(LocaleController.getString("ClearHistory", R.string.ClearHistory));
            }
        }
        if (canUnarchiveCount != 0) {
            archiveItem.setIcon(R.drawable.msg_unarchive);
            archiveItem.setContentDescription(LocaleController.getString("Unarchive", R.string.Unarchive));
        } else {
            archiveItem.setIcon(R.drawable.msg_archive);
            archiveItem.setContentDescription(LocaleController.getString("Archive", R.string.Archive));
            archiveItem.setEnabled(canArchiveCount != 0);
            archiveItem.setAlpha(canArchiveCount != 0 ? 1.0f : 0.5f);
        }
        if (canPinCount + canUnpinCount != count) {
            pinItem.setVisibility(View.GONE);
        } else {
            pinItem.setVisibility(View.VISIBLE);
        }
        final boolean foldersAvailable = !MessagesController.getInstance(currentAccount).dialogFoldersAll.isEmpty();
        if (foldersAvailable) {
            folderItem.setVisibility(View.VISIBLE);
        } else {
            folderItem.setVisibility(View.GONE);
        }
        if (canUnmuteCount != 0) {
            muteItem.setTextAndIcon(LocaleController.getString("ChatsUnmute", R.string.ChatsUnmute), R.drawable.msg_unmute);
        } else {
            muteItem.setTextAndIcon(LocaleController.getString("ChatsMute", R.string.ChatsMute), R.drawable.msg_mute);
        }
        if (canReadCount != 0) {
            readItem.setTextAndIcon(LocaleController.getString("MarkAsRead", R.string.MarkAsRead), R.drawable.msg_markread);
        } else {
            readItem.setTextAndIcon(LocaleController.getString("MarkAsUnread", R.string.MarkAsUnread), R.drawable.msg_markunread);
        }
        if (canPinCount != 0) {
            pinItem.setIcon(R.drawable.msg_pin);
            pinItem.setContentDescription(LocaleController.getString("PinToTop", R.string.PinToTop));
        } else {
            pinItem.setIcon(R.drawable.msg_unpin);
            pinItem.setContentDescription(LocaleController.getString("UnpinFromTop", R.string.UnpinFromTop));
        }

        if (isArchive) {
            muteItem.setVisibility(View.GONE);
            pinItem.setVisibility(View.GONE);
            deleteItem.setVisibility(View.GONE);
            archiveItem.setVisibility(View.GONE);
            otherItem.setVisibility(View.GONE);
            hideArchiveItem.setVisibility(View.VISIBLE);
            if (SharedConfig.archiveHidden) {
                hideArchiveItem.setIcon(R.drawable.chats_archive_show);
            } else {
                hideArchiveItem.setIcon(R.drawable.chats_archive_hide);
            }
        } else {
            muteItem.setVisibility(View.VISIBLE);
            pinItem.setVisibility(View.VISIBLE);
            deleteItem.setVisibility(View.VISIBLE);
            archiveItem.setVisibility(View.VISIBLE);
            otherItem.setVisibility(View.VISIBLE);
            hideArchiveItem.setVisibility(View.GONE);
        }
    }

    private void showOrUpdateActionMode(TLRPC.Dialog dialog, View cell) {
        if (!(tabAdapters.get(currentTab) instanceof DialogsAdapter)) {
            return;
        }


        DialogsAdapter adapter = (DialogsAdapter) tabAdapters.get(currentTab);
        if (adapter == null) {
            return;
        }
        adapter.addOrRemoveSelectedDialog(dialog.id, cell);
        ArrayList<Long> selectedDialogs = adapter.getSelectedDialogs();
        boolean updateAnimated = false;
        if (actionBar.isActionModeShowed()) {
            if (selectedDialogs.isEmpty()) {
                hideActionMode(true);
                return;
            }
            updateAnimated = true;
        } else {
            actionBar.showActionMode();
            if (menuDrawable != null) {
                actionBar.setBackButtonContentDescription(LocaleController.getString("AccDescrGoBack", R.string.AccDescrGoBack));
            }
            if (getPinnedCount() > 1) {
                adapter.onReorderStateChanged(true);
                updateVisibleRows(MessagesController.UPDATE_MASK_REORDER);
            }

            AnimatorSet animatorSet = new AnimatorSet();
            ArrayList<Animator> animators = new ArrayList<>();
            for (int a = 0; a < actionModeViews.size(); a++) {
                View view = actionModeViews.get(a);
                view.setPivotY(ActionBar.getCurrentActionBarHeight() / 2);
                AndroidUtilities.clearDrawableAnimation(view);
                animators.add(ObjectAnimator.ofFloat(view, View.SCALE_Y, 0.1f, 1.0f));
            }
            animatorSet.playTogether(animators);
            animatorSet.setDuration(250);
            animatorSet.start();
            if (menuDrawable != null) {
                menuDrawable.setRotateToBack(false);
                menuDrawable.setRotation(1, true);
            } else if (backDrawable != null) {
                backDrawable.setRotation(1, true);
            }
        }
        updateCounters(false, dialog instanceof TLRPC.TL_dialogFolder && ((TLRPC.TL_dialogFolder) dialog).folder.id == 1);
        selectedDialogsCountTextView.setNumber(selectedDialogs.size(), updateAnimated);
    }

    private void closeSearch() {
        if (AndroidUtilities.isTablet()) {
            if (actionBar != null) {
                actionBar.closeSearchField();
            }
            if (searchObject != null) {
                dialogsSearchAdapter.putRecentSearch(searchDialogId, searchObject);
                searchObject = null;
            }
        } else {
            closeSearchFieldOnHide = true;
        }
    }


    private UndoView getUndoView() {
        if (undoView[0].getVisibility() == View.VISIBLE) {
            UndoView old = undoView[0];
            undoView[0] = undoView[1];
            undoView[1] = old;
            old.hide(true, 2);
            ContentView contentView = (ContentView) fragmentView;
            contentView.removeView(undoView[0]);
            contentView.addView(undoView[0]);
        }
        return undoView[0];
    }


    private void updateProxyButton(boolean animated) {
        if (proxyDrawable == null) {
            return;
        }
        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
        String proxyAddress = preferences.getString("proxy_ip", "");
        boolean proxyEnabled;
        if ((proxyEnabled = preferences.getBoolean("proxy_enabled", false) && !TextUtils.isEmpty(proxyAddress)) || getMessagesController().blockedCountry && !SharedConfig.proxyList.isEmpty()) {
            if (!actionBar.isSearchFieldVisible()) {
                proxyItem.setVisibility(View.VISIBLE);
            }
            proxyDrawable.setConnected(proxyEnabled, currentConnectionState == ConnectionsManager.ConnectionStateConnected || currentConnectionState == ConnectionsManager.ConnectionStateUpdating, animated);
            proxyItemVisible = true;
        } else {
            proxyItem.setVisibility(View.GONE);
            proxyItemVisible = false;
        }
    }

    private void updateSelectedCount() {
        if (commentView == null && dialogsType != 9) {
            return;
        }
        if (!dialogsAdapterAll.hasSelectedDialogs()) {
            if (dialogsType == 3 && selectAlertString == null) {
                actionBar.setTitle(LocaleController.getString("ForwardTo", R.string.ForwardTo));
            } else if (dialogsType == 9) {
                actionBar.setTitle(LocaleController.getInternalString(R.string.SelectChats));
            } else {
                actionBar.setTitle(LocaleController.getString("SelectChat", R.string.SelectChat));
            }
            if (dialogsType != 9 && commentView.getTag() != null) {
                commentView.hidePopup(false);
                commentView.closeKeyboard();
                AnimatorSet animatorSet = new AnimatorSet();
                animatorSet.playTogether(ObjectAnimator.ofFloat(commentView, View.TRANSLATION_Y, 0, commentView.getMeasuredHeight()));
                animatorSet.setDuration(180);
                animatorSet.setInterpolator(new DecelerateInterpolator());
                animatorSet.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        commentView.setVisibility(View.GONE);
                    }
                });
                animatorSet.start();
                commentView.setTag(null);
                listViewAll.requestLayout();
            }
        } else {
            if (dialogsType != 9 && commentView.getTag() == null) {
                commentView.setFieldText("");
                commentView.setVisibility(View.VISIBLE);
                AnimatorSet animatorSet = new AnimatorSet();
                animatorSet.playTogether(ObjectAnimator.ofFloat(commentView, View.TRANSLATION_Y, commentView.getMeasuredHeight(), 0));
                animatorSet.setDuration(180);
                animatorSet.setInterpolator(new DecelerateInterpolator());
                animatorSet.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        commentView.setTag(2);
                        commentView.requestLayout();
                    }
                });
                animatorSet.start();
                commentView.setTag(1);
            }
            int selectedDialogs = dialogsAdapterAll.getSelectedDialogs().size();
            if (dialogsType == 9) {
                actionBar.setTitle(String.format(Locale.getDefault(), LocaleController.getInternalString(R.string.Selected), selectedDialogs));
            } else {
                actionBar.setTitle(LocaleController.formatPluralString("Recipient", selectedDialogs));
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void askForPermissons(boolean alert) {
        Activity activity = getParentActivity();
        if (activity == null) {
            return;
        }
        ArrayList<String> permissons = new ArrayList<>();
        if (getUserConfig().syncContacts && askAboutContacts && activity.checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            if (alert) {
                AlertDialog.Builder builder = AlertsCreator.createContactsPermissionDialog(activity, param -> {
                    askAboutContacts = param != 0;
                    MessagesController.getGlobalNotificationsSettings().edit().putBoolean("askAboutContacts", askAboutContacts).commit();
                    askForPermissons(false);
                });
                showDialog(permissionDialog = builder.create());
                return;
            }
            permissons.add(Manifest.permission.READ_CONTACTS);
            permissons.add(Manifest.permission.WRITE_CONTACTS);
            permissons.add(Manifest.permission.GET_ACCOUNTS);
        }
        if (activity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissons.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            permissons.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (permissons.isEmpty()) {
            return;
        }
        String[] items = permissons.toArray(new String[0]);
        try {
            activity.requestPermissions(items, 1);
        } catch (Exception ignore) {
        }
    }

    @Override
    protected void onDialogDismiss(Dialog dialog) {
        super.onDialogDismiss(dialog);
        if (permissionDialog != null && dialog == permissionDialog && getParentActivity() != null) {
            askForPermissons(false);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (!onlySelect && floatingButtonContainer != null) {
            floatingButtonContainer.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    floatingButtonContainer.setTranslationY((floatingHidden ? AndroidUtilities.dp(100) : -additionalFloatingTranslation));
                    //unreadFloatingButtonContainer.setTranslationY(floatingHidden ? AndroidUtilities.dp(74) : 0);
                    floatingButtonContainer.setClickable(!floatingHidden);
                    if (floatingButtonContainer != null) {
                        floatingButtonContainer.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    }
                }
            });
        }
    }

    @Override
    public void onRequestPermissionsResultFragment(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == 1) {
            for (int a = 0; a < permissions.length; a++) {
                if (grantResults.length <= a) {
                    continue;
                }
                switch (permissions[a]) {
                    case Manifest.permission.READ_CONTACTS:
                        if (grantResults[a] == PackageManager.PERMISSION_GRANTED) {
                            getContactsController().forceImportContacts();
                        } else {
                            MessagesController.getGlobalNotificationsSettings().edit().putBoolean("askAboutContacts", askAboutContacts = false).commit();
                        }
                        break;
                    case Manifest.permission.WRITE_EXTERNAL_STORAGE:
                        if (grantResults[a] == PackageManager.PERMISSION_GRANTED) {
                            ImageLoader.getInstance().checkMediaPaths();
                        }
                        break;
                }
            }
        }
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.dialogsNeedReload) {
            if (dialogsListFrozen) {
                return;
            }
            //checkUnreadCount(true);
            RecyclerListView.SelectionAdapter currentPageAdapter = tabAdapters.get(currentTab);
            if (currentPageAdapter instanceof DialogsAdapter) {
                if (((DialogsAdapter) currentPageAdapter).isDataSetChanged() || args.length > 0) {
                    currentPageAdapter.notifyDataSetChanged();
                } else {
                    updateVisibleRows(MessagesController.UPDATE_MASK_NEW_MESSAGE);
                }
            }
            RecyclerListView tabView = tabViews.get(currentTab);
            RecyclerListView.SelectionAdapter tabAdapter = tabAdapters.get(currentTab);
            if (tabView != null && tabAdapter != null) {
                try {
                    if (tabView.getAdapter() == tabAdapter) {
                        searchEmptyView.setVisibility(View.GONE);
                        tabView.setEmptyView(folderId == 0 ? progressView : null);

                    } else {
                        if (searching && searchWas) {
                            tabView.setEmptyView(searchEmptyView);
                        } else {
                            searchEmptyView.setVisibility(View.GONE);
                            tabView.setEmptyView(null);
                        }
                        progressView.setVisibility(View.GONE);
                    }
                } catch (Exception e) {
                    FileLog.e(e);
                }
            }
        } else if (id == NotificationCenter.emojiDidLoad) {
            updateVisibleRows(0);
        } else if (id == NotificationCenter.closeSearchByActiveAction) {
            if (actionBar != null) {
                actionBar.closeSearchField();
            }
        } else if (id == NotificationCenter.proxySettingsChanged) {
            updateProxyButton(false);
        } else if (id == NotificationCenter.updateInterfaces) {
            Integer mask = (Integer) args[0];
            updateVisibleRows(mask);
            /*if ((mask & MessagesController.UPDATE_MASK_NEW_MESSAGE) != 0 || (mask & MessagesController.UPDATE_MASK_READ_DIALOG_MESSAGE) != 0) {
                checkUnreadCount(true);
            }*/
        } else if (id == NotificationCenter.appDidLogout) {
            dialogsLoaded[currentAccount] = false;
        } else if (id == NotificationCenter.encryptedChatUpdated) {
            updateVisibleRows(0);
        } else if (id == NotificationCenter.contactsDidLoad) {
            if (dialogsListFrozen) {
                return;
            }
            if (dialogsType == 0 && getMessagesController().getDialogs(folderId).isEmpty()) {
                if (tabAdapters.get(currentTab) != null) {
                    tabAdapters.get(currentTab).notifyDataSetChanged();
                }
            } else {
                updateVisibleRows(0);
            }
        } else if (id == NotificationCenter.openedChatChanged) {
            if (dialogsType == 0 && AndroidUtilities.isTablet()) {
                boolean close = (Boolean) args[1];
                long dialog_id = (Long) args[0];
                if (close) {
                    if (dialog_id == openedDialogId) {
                        openedDialogId = 0;
                    }
                } else {
                    openedDialogId = dialog_id;
                }
                RecyclerListView.SelectionAdapter adapter = tabAdapters.get(currentTab);
                if (adapter instanceof DialogsAdapter) {
                    ((DialogsAdapter) adapter).setOpenedDialogId(openedDialogId);
                }
                updateVisibleRows(MessagesController.UPDATE_MASK_SELECT_DIALOG);
            }
        } else if (id == NotificationCenter.notificationsSettingsUpdated) {
            updateVisibleRows(0);
        } else if (id == NotificationCenter.messageReceivedByAck || id == NotificationCenter.messageReceivedByServer || id == NotificationCenter.messageSendError) {
            updateVisibleRows(MessagesController.UPDATE_MASK_SEND_STATE);
        } else if (id == NotificationCenter.didSetPasscode) {
            updatePasscodeButton();
        } else if (id == NotificationCenter.needReloadRecentDialogsSearch) {
            if (dialogsSearchAdapter != null) {
                dialogsSearchAdapter.loadRecentSearch();
            }
        } else if (id == NotificationCenter.replyMessagesDidLoad) {
            updateVisibleRows(MessagesController.UPDATE_MASK_MESSAGE_TEXT);
        } else if (id == NotificationCenter.reloadHints) {
            if (dialogsSearchAdapter != null) {
                dialogsSearchAdapter.notifyDataSetChanged();
            }
        } else if (id == NotificationCenter.didUpdateConnectionState) {
            int state = AccountInstance.getInstance(account).getConnectionsManager().getConnectionState();
            if (currentConnectionState != state) {
                currentConnectionState = state;
                updateProxyButton(true);
            }
        } else if (id == NotificationCenter.dialogsUnreadCounterChanged) {
            /*if (!onlySelect) {
                int count = (Integer) args[0];
                currentUnreadCount = count;
                if (count != 0) {
                    unreadFloatingButtonCounter.setText(String.format("%d", count));
                    unreadFloatingButtonContainer.setVisibility(View.VISIBLE);
                    unreadFloatingButtonContainer.setTag(1);
                    unreadFloatingButtonContainer.animate().alpha(1.0f).setDuration(200).setInterpolator(new DecelerateInterpolator()).setDelegate(null).start();
                } else {
                    unreadFloatingButtonContainer.setTag(null);
                    unreadFloatingButtonContainer.animate().alpha(0.0f).setDuration(200).setInterpolator(new DecelerateInterpolator()).setDelegate(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            unreadFloatingButtonContainer.setVisibility(View.INVISIBLE);
                        }
                    }).start();
                }
            }*/
        } else if (id == NotificationCenter.needDeleteDialog) {
            if (fragmentView == null) {
                return;
            }
            long dialogId = (Long) args[0];
            TLRPC.User user = (TLRPC.User) args[1];
            TLRPC.Chat chat = (TLRPC.Chat) args[2];
            boolean revoke = (Boolean) args[3];
            Runnable deleteRunnable = () -> {
                if (chat != null) {
                    if (ChatObject.isNotInChat(chat)) {
                        getMessagesController().deleteDialog(dialogId, 0, revoke);
                    } else {
                        getMessagesController().deleteUserFromChat((int) -dialogId, getMessagesController().getUser(getUserConfig().getClientUserId()), null, false, revoke);
                    }
                } else {
                    getMessagesController().deleteDialog(dialogId, 0, revoke);
                }
            };
            if (undoView[0] != null) {
                getUndoView().showWithAction(dialogId, UndoView.ACTION_DELETE, deleteRunnable);
            } else {
                deleteRunnable.run();
            }
        } else if (id == NotificationCenter.folderBecomeEmpty) {
            int fid = (Integer) args[0];
            if (folderId == fid && folderId != 0) {
                finishFragment();
            }
        } else if (id == NotificationCenter.refreshTabIcons) {
            refreshTabIndicators();
        } else if (id == NotificationCenter.refreshTabContent) {
            MessagesController.getInstance(currentAccount).refreshFolderDialogs();
            if (dialogsType == 8) {
                ArrayList<TLRPC.Dialog> folderDialogs = MessagesController.getInstance(currentAccount).chosenDialogsByFolder.get(existingFolderId);
                if (folderDialogs == null || (folderDialogs != null && folderDialogs.isEmpty())) {
                    finishFragment();
                    return;
                }
            }
            MessagesController.getInstance(currentAccount).refreshUnreadDialogs();
            if (dialogsAdapterFavorites != null) {
                dialogsAdapterFavorites.notifyDataSetChanged();
            }
            if (dialogsType == 8 && dialogsAdapterAll != null) {
                dialogsAdapterAll.notifyDataSetChanged();
            }
            if (dialogsAdapterUnread != null) {
                dialogsAdapterUnread.notifyDataSetChanged();
            }
            if (currentTab == Tab.UNREAD) {
                hideFloatingButton(MessagesController.getInstance(currentAccount).dialogsUnread.size() == 0);
            }
        } else if (id == NotificationCenter.refreshTabState) {
            tabsAdapter = buildNewTabsAdapter();
            tabsViewPager.setAdapter(tabsAdapter);
            tabsAdapter.notifyDataSetChanged();
            setupTabsBehavior();
            setupTabsAppearance();
            currentTab = tabsManager.getTabByPosition(0, currentTabsMode);
            tabsViewPager.setCurrentItem(0, false);
        }
    }

    // Настройка поведения вкладок чатов, скрываем табы и отключаем свайп если вкладка одна
    private void setupTabsBehavior() {
        if (dialogsType == 0 || dialogsType == 3) {
            if (tabsManager.getAvailableTabsCount(currentTabsMode) == 1) {
                tabsLayout.setVisibility(View.GONE);
                canSwipeHorizontally = false;
            } else {
                tabsLayout.setVisibility(View.VISIBLE);
                canSwipeHorizontally = true;
            }
        } else {
            tabsLayout.setVisibility(View.GONE);
            canSwipeHorizontally = false;
        }
    }

    // Настройка параметров вкладок чатов
    // Если включены все вкладки, то табы получают режим scrollable
    private void setupTabsAppearance() {
        if (tabsManager.getAvailableTabsCount(currentTabsMode) != tabsManager.allTabs.size()) {
            tabsLayout.setTabMode(TabLayout.MODE_FIXED);
        } else {
            tabsLayout.setTabMode(TabLayout.MODE_SCROLLABLE);
        }

        for (int i = 0; i < tabsManager.getAvailableTabsCount(currentTabsMode); i++) {
            CustomTabView view = new CustomTabView(getParentActivity());
            Integer iconRes = tabIcons.get(tabsManager.getTabByPosition(i, currentTabsMode));
            if (iconRes != null) {
                view.setIcon(iconRes);
            }
            TabLayout.Tab tab = tabsLayout.getTabAt(i);
            if (tab != null) {
                tab.setCustomView(view);
            }
        }
    }

    // Обновление индикатора непрочитанных сообщений у всех вкладок
    private void refreshTabIndicators() {
        if (tabsLayout == null) {
            return;
        }

        for (int index = 0; index < tabsManager.getAvailableTabsCount(currentTabsMode); index++) {
            boolean tabHasUnread = false;

            if (tabsManager.getTabByPosition(index, currentTabsMode) == Tab.FOLDERS) {
                ArrayList<DialogsFolder> folders = getDialogsFoldersArray(currentAccount);
                for (DialogsFolder folder : folders) {
                    if (folder.getUnread() > 0) {
                        tabHasUnread = true;
                        break;
                    }
                }
            } else {
                ArrayList<TLRPC.Dialog> dialogs = getDialogsArray(currentAccount, dialogsType, folderId, dialogsListFrozen, tabsManager.getTabByPosition(index, currentTabsMode));
                if (dialogs == null) {
                    return;
                }
                for (TLRPC.Dialog dialog : dialogs) {
                    if (dialog.unread_count != 0 || dialog.unread_mark) {
                        tabHasUnread = true;
                        break;
                    }
                }
            }

            highlightTab(index, tabHasUnread);
        }
    }

    /**
     * Управляет видмостью индикатора непрочитанных сообщений на вкладке
     *
     * @param number    Номер вкладки
     * @param highlight Показать/скрыть
     */
    private void highlightTab(int number,
                              boolean highlight) {
        TabLayout.Tab tab = tabsLayout.getTabAt(number);
        if (tab != null) {
            View customView = tab.getCustomView();
            if (customView instanceof CustomTabView) {
                ((CustomTabView) customView).showIndicator(highlight);
            }
        }
    }

    // При изменении количества активных вкладок каждый раз генерируем новый адаптер
    private PagerAdapter buildNewTabsAdapter() {
        for (Tab tab : Tab.values()) {
            View view = tabViews.get(tab);
            if (view == null) {
                continue;
            }
            ViewGroup viewGroup = (ViewGroup) view.getParent();
            if (viewGroup != null) {
                viewGroup.removeView(view);
            }
        }

        return new PagerAdapter() {
            @Override
            public int getCount() {
                return tabsManager.getAvailableTabsCount(currentTabsMode);
            }

            @Override
            public boolean isViewFromObject(@NonNull View view, @NonNull Object o) {
                return view == o;
            }

            @NonNull
            @Override
            public Object instantiateItem(@NonNull ViewGroup container, int position) {
                Tab newTab = tabsManager.getTabByPosition(position, currentTabsMode);
                View view = tabViews.get(newTab);
                container.addView(view);
                return view;
            }

            @Override
            public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
                container.removeView((View) object);
            }

            @Nullable
            @Override
            public CharSequence getPageTitle(int position) {
                return null;
            }
        };
    }

    private void setDialogsListFrozen(boolean frozen) {
        if (!(tabAdapters.get(currentTab) instanceof DialogsAdapter)) {
            return;
        }
        if (dialogsListFrozen == frozen) {
            return;
        }
        if (frozen) {
            frozenDialogsList = new ArrayList<>(getDialogsArray(currentAccount, dialogsType, folderId, false, currentTab));
        } else {
            frozenDialogsList = null;
        }
        dialogsListFrozen = frozen;

        DialogsAdapter dialogAdapter = (DialogsAdapter) tabAdapters.get(currentTab);
        if (dialogAdapter != null) {
            dialogAdapter.setDialogsListFrozen(frozen);
            if (!frozen) {
                dialogAdapter.notifyDataSetChanged();
            }
        }

    }

    // Добавлены новые массивы для соответствующих вкладок
    public static ArrayList<TLRPC.Dialog> getDialogsArray(int currentAccount, int dialogsType, int folderId, boolean frozen, Tab tab) {
        if (frozen && frozenDialogsList != null) {
            return frozenDialogsList;
        }
        MessagesController messagesController = AccountInstance.getInstance(currentAccount).getMessagesController();
        if (folderId != 0) {
            return messagesController.getDialogs(folderId);
        } else if (dialogsType == 0 || dialogsType == 3) {
            switch (tab) {
                case ALL:
                    if (dialogsType == 3) {
                        return MessagesController.getInstance(currentAccount).dialogsForward;
                    } else {
                        return MessagesController.getInstance(currentAccount).getDialogs(0);
                    }
                case UNREAD:
                    return MessagesController.getInstance(currentAccount).dialogsUnread;
                case GROUPS:
                    return MessagesController.getInstance(currentAccount).dialogsGroupsOnly;
                case CHATS:
                    return MessagesController.getInstance(currentAccount).dialogsSingleChatsOnly;
                case CHANNELS:
                    return MessagesController.getInstance(currentAccount).dialogsChannelsOnly;
                case BOTS:
                    return MessagesController.getInstance(currentAccount).dialogsBotsOnly;
                case FOLDERS:
                    return new ArrayList<>();
                default:
                    return MessagesController.getInstance(currentAccount).getDialogs(0);
            }
        } else if (dialogsType == 1) {
            return messagesController.dialogsServerOnly;
        } else if (dialogsType == 2) {
            return messagesController.dialogsCanAddUsers;
        } else if (dialogsType == 4) {
            return messagesController.dialogsUsersOnly;
        } else if (dialogsType == 5) {
            return messagesController.dialogsChannelsOnly;
        } else if (dialogsType == 6) {
            return messagesController.dialogsGroupsOnly;
        } else if (dialogsType == 8) {
            ArrayList<TLRPC.Dialog> dialogs = MessagesController.getInstance(currentAccount).chosenDialogsByFolder.get(existingFolderId);
            if (dialogs != null) {
                return dialogs;
            } else return new ArrayList<>();
        } else if (dialogsType == 9) {
            ArrayList<TLRPC.Dialog> dialogs;
            if (editExistingFolder) {
                dialogs = MessagesController.getInstance(currentAccount).unchosenDialogsByFolder.get(existingFolderId);
            } else {
                dialogs = MessagesController.getInstance(currentAccount).getAllDialogs();
            }
            if (dialogs != null) {
                return dialogs;
            } else return new ArrayList<>();
        }

        return new ArrayList<>();
    }


    public static ArrayList<DialogsFolder> getDialogsFoldersArray(int currentAccount) {
        return MessagesController.getInstance(currentAccount).dialogFoldersAll;
    }

    public void setSideMenu(RecyclerView recyclerView) {
        sideMenu = recyclerView;
        sideMenu.setBackgroundColor(Theme.getColor(Theme.key_chats_menuBackground));
        sideMenu.setGlowColor(Theme.getColor(Theme.key_chats_menuBackground));
    }

    /**
     * Отображает разные иконки на FAB в зависимости от текущей вкладки
     *
     * @param position Номер вкладки
     */
    private void displayActualFloatingButton(
            int position) {
        Tab tab = tabsManager.getTabByPosition(position, currentTabsMode);

        if (tab == Tab.FOLDERS) {
            floatingButton.setImageResource(R.drawable.floating_plus);
        } else if (tab == Tab.UNREAD) {
            floatingButton.setImageResource(R.drawable.floating_read_all);
        } else {
            floatingButton.setImageResource(R.drawable.floating_pencil);
        }
    }

    private void onFabClickedNormal() {
        Bundle args = new Bundle();
        args.putBoolean("destroyAfterSelect", true);
        presentFragment(new ContactsActivity(args));
    }

    private void onFabClickedFavorites() {
        showFolderNameInputField(null, enteredText -> {
            showDialogsPicker(enteredText, false);
        });
    }


    private void onFabClickedUnread() {
        MessagesController.getInstance(currentAccount).markAllDialogsAsRead();
    }

    /**
     * Отображает контекстное меню диалога для отправки его в имеющиеся папки
     *
     * @param dialogIds айди диалога
     */
    private void showFoldersListSheet(ArrayList<Long> dialogIds) {
        long userId = UserConfig.getInstance(currentAccount).getClientUserId();
        disposables.add(
                storage.getUserFolders(userId)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(folders -> {
                            BottomSheet.Builder builder = new BottomSheet.Builder(getParentActivity());
                            int size = folders.size();
                            int icons[] = new int[size];
                            CharSequence items[] = new CharSequence[size];
                            for (int i = 0; i < size; i++) {
                                icons[i] = R.drawable.ic_folder_icon;
                                items[i] = folders.get(i).getName();
                            }

                            builder.setItems(items, icons, (d, which) -> {
                                long folderId = folders.get(which).getId();
                                saveSelectedDialogToFolder(folderId, userId, dialogIds);
                            });

                            BottomSheet sheet = builder.create();
                            showDialog(sheet);
                        }, Throwable::printStackTrace)
        );
    }


    private void showFolderHideSheet() {
        BottomSheet.Builder builder = new BottomSheet.Builder(getParentActivity());
        int size = 1;
        int icons[] = new int[size];
        CharSequence items[] = new CharSequence[size];
        icons[0] = R.drawable.chats_archive_hide;
        items[0] = LocaleController.getString("HideOnTop", R.string.HideOnTop);


        builder.setItems(items, icons, (d, which) -> {
        });

        BottomSheet sheet = builder.create();
        showDialog(sheet);
    }

    /**
     * Отображает контекстное меню папки
     *
     * @param folder Выбранная папка
     */
    private void showFolderContextMenu
    (DialogsFolder folder) {
        long userId = UserConfig.getInstance(currentAccount).getClientUserId();
        long folderId = folder.getId();
        String folderName = folder.getName();
        boolean isPinned = folder.isPinned();

        BottomSheet.Builder builder = new BottomSheet.Builder(getParentActivity());

        CharSequence items[] = new CharSequence[]{
                isPinned ? LocaleController.getString("UnpinFromTop", R.string.UnpinFromTop) : LocaleController.getString("PinToTop", R.string.PinToTop),
                LocaleController.getInternalString(R.string.FolderEdit),
                LocaleController.getInternalString(R.string.FolderRename),
                LocaleController.getInternalString(R.string.FolderDelete)
        };

        int icons[] = new int[]{
                isPinned ? R.drawable.chats_unpin : R.drawable.chats_pin,
                R.drawable.ic_folder_add_chat1,
                R.drawable.ic_folder_rename,
                R.drawable.chats_delete
        };

        builder.setItems(items, icons, (d, which) -> {
            if (which == 0) {
                pinChosenFolder(folderId, userId, !isPinned);
            } else if (which == 1) {
                existingFolderId = folderId;
                showDialogsPicker(folderName, true);
            } else if (which == 2) {
                renameChosenFolder(folderId, userId, folderName);
            } else {
                deleteChosenFolder(folderId, userId);
            }
        });

        BottomSheet sheet = builder.create();
        showDialog(sheet);
    }

    /**
     * Изменяет pin выбранной папки (прикреплена или нет)
     *
     * @param folderId идентификатор папки
     * @param userId   идентификатор пользователя
     * @param pin      пинить или нет
     */
    private void pinChosenFolder(long folderId, long userId, boolean pin) {
        disposables.add(
                storage.pinFolder(folderId, userId, pin)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(() -> {
                                    NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.refreshTabContent);
                                    NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.refreshTabIcons);
                                    Log.d("Storage", "Folder " + folderId + " - pinned state changed");
                                }, Throwable::printStackTrace
                        )
        );
    }

    /**
     * Переименовывает выбранную папку
     *
     * @param folderId         идентификатор папки
     * @param userId           идентификатор пользователя
     * @param chosenFolderName Текущее имя папки
     */
    private void renameChosenFolder(long folderId, long userId, String chosenFolderName) {
        showFolderNameInputField(chosenFolderName, enteredText ->
                saveNewFolderName(folderId, userId, enteredText));
    }

    /**
     * Удаляет выбранную папку
     *
     * @param folderId идентификатор папки
     * @param userId   идентификатор пользователя
     */
    private void deleteChosenFolder(long folderId,
                                    long userId) {
        disposables.add(
                storage.deleteFolder(folderId, userId)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(() -> {
                                    NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.refreshTabContent);
                                    NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.refreshTabIcons);
                                    Log.d("Storage", "Folder " + folderId + " deleted");
                                }, Throwable::printStackTrace
                        )
        );
    }

    /**
     * Сохраняет новое имя папки
     *
     * @param folderId      идентификатор папки
     * @param userId        идентификатор пользователя
     * @param newFolderName новое имя папки
     */
    private void saveNewFolderName(long folderId, long userId, String newFolderName) {
        disposables.add(
                storage.renameFolder(folderId, userId, newFolderName)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(() -> {
                                    NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.refreshTabContent);
                                    NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.refreshTabIcons);
                                    Log.d(" Storage", "Folder with id " + folderId + " renamed to " + newFolderName);
                                }, Throwable::printStackTrace
                        )
        );
    }

    /**
     * Сохраняет диалог в папку
     *
     * @param folderId идентификатор папки
     * @param userId   идентификатор пользователя
     * @param dialogId идентификатор выбранного диалога
     */
    private void saveSelectedDialogToFolder(long folderId, long userId, long dialogId) {
        disposables.add(
                storage.saveSingleDialogToFolder(folderId, userId, dialogId)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(() -> {
                                    NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.refreshTabIcons);
                                    NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.refreshTabContent);
                                    Log.d("Storage", "Dialog " + dialogId + " saved to folder " + folderId);
                                }, Throwable::printStackTrace
                        )
        );
    }

    /**
     * Сохраняет диалоги в папку
     *
     * @param folderId  идентификатор папки
     * @param userId    идентификатор пользователя
     * @param dialogIds идентификаторы выбранного диалога
     */
    private void saveSelectedDialogToFolder(long folderId, long userId, ArrayList<Long> dialogIds) {
        disposables.add(
                storage.saveDialogsToFolder(folderId, userId, dialogIds)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(() -> {
                                    NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.refreshTabIcons);
                                    NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.refreshTabContent);
                                    Log.d("Storage", "Dialog " + dialogIds + " saved to folder " + folderId);
                                }, Throwable::printStackTrace
                        )
        );
    }

    /**
     * Удаляет диалоги из папки, идентификатор папки берем из статик поля existingFolderId
     *
     * @param dialogIds ids диалогов
     */
    private void removeSelectedDialogFromFolder(ArrayList<Long> dialogIds) {
        long userId = UserConfig.getInstance(currentAccount).getClientUserId();
        disposables.add(
                storage.removeDialogsFromFolder(existingFolderId, userId, dialogIds)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(() -> {
                                    NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.refreshTabContent);
                                    NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.refreshTabIcons);
                                    Log.d("Storage", "Dialog " + dialogIds + " removed from folder " + originalFolderName);
                                }, Throwable::printStackTrace
                        )
        );
    }


    /**
     * Сохранение выбранных диалогов в новую созданную папку
     *
     * @param createdFolderName имя новой папки
     * @param selectedDialogs   выбранные диалоги
     */
    private void saveDialogsToFolder(@NonNull String createdFolderName, ArrayList<Long> selectedDialogs) {
        if (createdFolderName.isEmpty() || selectedDialogs.isEmpty()) {
            return;
        }

        long userId = UserConfig.getInstance(currentAccount).getClientUserId();

        if (editExistingFolder && existingFolderId != -1) {
            disposables.add(
                    storage.addDialogsToFolder(existingFolderId, userId, selectedDialogs)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .doOnComplete(this::finishFragment)
                            .subscribe(() -> {
                                        NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.refreshTabContent);
                                        NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.refreshTabIcons);
                                        Log.d("Storage", "Folder " + createdFolderName + " created");
                                    }, Throwable::printStackTrace
                            )
            );
        } else {
            disposables.add(
                    storage.saveDialogsToNewFolder(userId, createdFolderName, selectedDialogs)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .doOnComplete(this::finishFragment)
                            .subscribe(() -> {
                                        NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.refreshTabContent);
                                        NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.refreshTabIcons);
                                        Log.d("Storage", "Folder " + createdFolderName + " created");
                                    }, Throwable::printStackTrace
                            )
            );
        }
    }

    /**
     * Отображает диалог для ввода имя папки
     *
     * @param prefillText Начальный текст для поля ввода
     * @param callback    Коллбек для кнопки ок
     */
    private void showFolderNameInputField(String prefillText, DialogPositiveButtonCallback callback) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getInternalString(R.string.FolderName));
        EditTextBoldCursor editText = new EditTextBoldCursor(getParentActivity());

        editText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
        editText.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        editText.setSingleLine(true);
        editText.setFocusable(true);
        editText.setInputType(InputType.TYPE_CLASS_TEXT);
        editText.setImeOptions(EditorInfo.IME_ACTION_DONE);
        editText.requestFocus();
        editText.setPadding(32, 32, 32, 32);

        if (prefillText != null) {
            editText.setText(prefillText);
            editText.setSelection(prefillText.length());
        }

        int maxLength = 24;
        InputFilter[] filters = new InputFilter[1];
        filters[0] = new InputFilter.LengthFilter(maxLength);
        editText.setFilters(filters);
        builder.setView(editText);

        builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), (dialogInterface, i) -> {
            callback.run(editText.getText().toString());
        });

        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);

        builder.setAllowEmptyText(false);

        builder.show().setOnShowListener(dialog -> {
            editText.requestFocus();
            AndroidUtilities.showKeyboard(editText);
        });
    }

    /**
     * Отображает новое окно с выбором диалогов для добавления в папку
     *
     * @param folderName Имя папки
     * @param existing   Добавляются ли диалоги в существующую папку (true) или в новую (false)
     */
    private void showDialogsPicker(String folderName, boolean existing) {
        Bundle args = new Bundle();
        args.putBoolean("onlySelect", true);
        args.putString("folderName", folderName);
        args.putInt("dialogsType", 9);
        args.putBoolean("editExistingFolder", existing);
        args.putLong("existingFolderId", existingFolderId);
        presentFragment(new DialogsActivity(args));
    }

    private void updatePasscodeButton() {
        if (passcodeItem == null) {
            return;
        }
        if (SharedConfig.passcodeHash.length() != 0 && !searching) {
            passcodeItem.setVisibility(View.VISIBLE);
            if (SharedConfig.appLocked) {
                passcodeItem.setIcon(R.drawable.lock_close);
                passcodeItem.setContentDescription(LocaleController.getString("AccDescrPasscodeUnlock", R.string.AccDescrPasscodeUnlock));
            } else {
                passcodeItem.setIcon(R.drawable.lock_open);
                passcodeItem.setContentDescription(LocaleController.getString("AccDescrPasscodeLock", R.string.AccDescrPasscodeLock));
            }
        } else {
            passcodeItem.setVisibility(View.GONE);
        }
    }

    private void hideFloatingButton(boolean hide) {
        if (floatingHidden == hide) {
            return;
        }
        floatingHidden = hide;
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(ObjectAnimator.ofFloat(floatingButtonContainer, View.TRANSLATION_Y, (floatingHidden ? AndroidUtilities.dp(100) : -additionalFloatingTranslation))/*,
                ObjectAnimator.ofFloat(unreadFloatingButtonContainer, View.TRANSLATION_Y, floatingHidden ? AndroidUtilities.dp(74) : 0)*/);
        animatorSet.setDuration(300);
        animatorSet.setInterpolator(floatingInterpolator);
        floatingButtonContainer.setClickable(!hide);
        animatorSet.start();
    }

    private void updateDialogIndices() {
        RecyclerListView listView = tabViews.get(currentTab);
        if (listView == null || listView.getAdapter() != tabAdapters.get(currentTab)) {
            return;
        }
        ArrayList<TLRPC.Dialog> dialogs = getDialogsArray(currentAccount, dialogsType, folderId, false, currentTab);
        int count = listView.getChildCount();
        for (int a = 0; a < count; a++) {
            View child = listView.getChildAt(a);
            if (child instanceof DialogCell) {
                DialogCell dialogCell = (DialogCell) child;
                TLRPC.Dialog dialog = getMessagesController().dialogs_dict.get(dialogCell.getDialogId());
                if (dialog == null) {
                    continue;
                }
                int index = dialogs.indexOf(dialog);
                if (index < 0) {
                    continue;
                }
                dialogCell.setDialogIndex(index);
            }
        }
    }

    private void updateVisibleRows(int mask) {
        RecyclerListView currentTabListView = tabViews.get(currentTab);
        if (currentTabListView == null || dialogsListFrozen) {
            return;
        }
        int count = currentTabListView.getChildCount();
        for (int a = 0; a < count; a++) {
            View child = currentTabListView.getChildAt(a);
            if (child instanceof DialogCell) {
                if (currentTabListView.getAdapter() != dialogsSearchAdapter) {
                    DialogCell cell = (DialogCell) child;
                    if ((mask & MessagesController.UPDATE_MASK_NEW_MESSAGE) != 0) {
                        cell.checkCurrentDialogIndex(dialogsListFrozen, ((DialogsAdapter) currentTabListView.getAdapter()).getAssociatedTab());
                        if (dialogsType == 0 && AndroidUtilities.isTablet()) {
                            cell.setDialogSelected(cell.getDialogId() == openedDialogId);
                        }
                    } else if ((mask & MessagesController.UPDATE_MASK_SELECT_DIALOG) != 0) {
                        if (dialogsType == 0 && AndroidUtilities.isTablet()) {
                            cell.setDialogSelected(cell.getDialogId() == openedDialogId);
                        }
                    } else {
                        cell.update(mask);
                    }
                }
            } else if (child instanceof UserCell) {
                ((UserCell) child).update(mask);
            } else if (child instanceof ProfileSearchCell) {
                ((ProfileSearchCell) child).update(mask);
            } else if (child instanceof RecyclerListView) {
                RecyclerListView innerListView = (RecyclerListView) child;
                int count2 = innerListView.getChildCount();
                for (int b = 0; b < count2; b++) {
                    View child2 = innerListView.getChildAt(b);
                    if (child2 instanceof HintDialogCell) {
                        ((HintDialogCell) child2).update(mask);
                    }
                }
            }
        }
    }


    public void setDelegate(DialogsActivityDelegate dialogsActivityDelegate) {
        delegate = dialogsActivityDelegate;
    }

    public void setSearchString(String string) {
        searchString = string;
    }

    public boolean isMainDialogList() {
        return delegate == null && searchString == null;
    }

    private void didSelectResult(final long dialog_id, boolean useAlert, final boolean param) {
        if (addToGroupAlertString == null && checkCanWrite) {
            if ((int) dialog_id < 0) {
                TLRPC.Chat chat = getMessagesController().getChat(-(int) dialog_id);
                if (ChatObject.isChannel(chat) && !chat.megagroup && (cantSendToChannels || !ChatObject.isCanWriteToChannel(-(int) dialog_id, currentAccount))) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                    builder.setTitle(LocaleController.getString("AppName", R.string.AppName));
                    builder.setMessage(LocaleController.getString("ChannelCantSendMessage", R.string.ChannelCantSendMessage));
                    builder.setNegativeButton(LocaleController.getString("OK", R.string.OK), null);
                    showDialog(builder.create());
                    return;
                }
            }
        }
        if (useAlert && (selectAlertString != null && selectAlertStringGroup != null || addToGroupAlertString != null)) {
            if (getParentActivity() == null) {
                return;
            }
            AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
            builder.setTitle(LocaleController.getString("AppName", R.string.AppName));
            int lower_part = (int) dialog_id;
            int high_id = (int) (dialog_id >> 32);
            if (lower_part != 0) {
                if (high_id == 1) {
                    TLRPC.Chat chat = getMessagesController().getChat(lower_part);
                    if (chat == null) {
                        return;
                    }
                    builder.setMessage(LocaleController.formatStringSimple(selectAlertStringGroup, chat.title));
                } else {
                    if (lower_part == getUserConfig().getClientUserId()) {
                        builder.setMessage(LocaleController.formatStringSimple(selectAlertStringGroup, LocaleController.getString("SavedMessages", R.string.SavedMessages)));
                    } else if (lower_part > 0) {
                        TLRPC.User user = getMessagesController().getUser(lower_part);
                        if (user == null) {
                            return;
                        }
                        builder.setMessage(LocaleController.formatStringSimple(selectAlertString, UserObject.getUserName(user)));
                    } else if (lower_part < 0) {
                        TLRPC.Chat chat = getMessagesController().getChat(-lower_part);
                        if (chat == null) {
                            return;
                        }
                        if (addToGroupAlertString != null) {
                            builder.setMessage(LocaleController.formatStringSimple(addToGroupAlertString, chat.title));
                        } else {
                            builder.setMessage(LocaleController.formatStringSimple(selectAlertStringGroup, chat.title));
                        }
                    }
                }
            } else {
                TLRPC.EncryptedChat chat = getMessagesController().getEncryptedChat(high_id);
                TLRPC.User user = getMessagesController().getUser(chat.user_id);
                if (user == null) {
                    return;
                }
                builder.setMessage(LocaleController.formatStringSimple(selectAlertString, UserObject.getUserName(user)));
            }

            builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), (dialogInterface, i) -> didSelectResult(dialog_id, false, false));
            builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
            showDialog(builder.create());
        } else {
            if (delegate != null) {
                ArrayList<Long> dids = new ArrayList<>();
                dids.add(dialog_id);
                delegate.didSelectDialogs(DialogsActivity.this, dids, null, param);
                delegate = null;
            } else {
                finishFragment();
            }
        }
    }

    @Override
    public ThemeDescription[] getThemeDescriptions() {
        ThemeDescription.ThemeDescriptionDelegate cellDelegate = () -> {
            RecyclerListView currentTabListView = tabViews.get(currentTab);
            if (currentTabListView == null) {
                currentTabListView = tabViews.get(tabsManager.getTabByPosition(0, currentTabsMode));
            }
            if (tabViews != null && currentTabListView != null) {
                int count = currentTabListView.getChildCount();
                for (int a = 0; a < count; a++) {
                    View child = currentTabListView.getChildAt(a);
                    if (child instanceof ProfileSearchCell) {
                        ((ProfileSearchCell) child).update(0);
                    } else if (child instanceof DialogCell) {
                        ((DialogCell) child).update(0);
                    }
                }
            }

            if (dialogsSearchAdapter != null) {
                RecyclerListView recyclerListView = dialogsSearchAdapter.getInnerListView();
                if (recyclerListView != null) {
                    int count = recyclerListView.getChildCount();
                    for (int a = 0; a < count; a++) {
                        View child = recyclerListView.getChildAt(a);
                        if (child instanceof HintDialogCell) {
                            ((HintDialogCell) child).update();
                        }
                    }
                }
            }
            if (sideMenu != null) {
                View child = sideMenu.getChildAt(0);
                if (child instanceof DrawerProfileCell) {
                    DrawerProfileCell profileCell = (DrawerProfileCell) child;
                    profileCell.applyBackground();
                }
            }
        };

        ArrayList<ThemeDescription> arrayList = new ArrayList<>();

        arrayList.add(new ThemeDescription(fragmentView, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_windowBackgroundWhite));

        if (movingView != null) {
            arrayList.add(new ThemeDescription(movingView, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_windowBackgroundWhite));
        }

        if (folderId == 0) {
            arrayList.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_actionBarDefault));
            arrayList.add(new ThemeDescription(listViewAll, ThemeDescription.FLAG_LISTGLOWCOLOR, null, null, null, null, Theme.key_actionBarDefault));
            arrayList.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_ITEMSCOLOR, null, null, null, null, Theme.key_actionBarDefaultIcon));
            arrayList.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_TITLECOLOR, null, null, new Drawable[]{Theme.dialogs_holidayDrawable}, null, Theme.key_actionBarDefaultTitle));
            arrayList.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_SELECTORCOLOR, null, null, null, null, Theme.key_actionBarDefaultSelector));
            arrayList.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_SEARCH, null, null, null, null, Theme.key_actionBarDefaultSearch));
            arrayList.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_SEARCHPLACEHOLDER, null, null, null, null, Theme.key_actionBarDefaultSearchPlaceholder));
        } else {
            arrayList.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_actionBarDefaultArchived));
            arrayList.add(new ThemeDescription(listViewAll, ThemeDescription.FLAG_LISTGLOWCOLOR, null, null, null, null, Theme.key_actionBarDefaultArchived));
            arrayList.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_ITEMSCOLOR, null, null, null, null, Theme.key_actionBarDefaultArchivedIcon));
            arrayList.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_TITLECOLOR, null, null, new Drawable[]{Theme.dialogs_holidayDrawable}, null, Theme.key_actionBarDefaultArchivedTitle));
            arrayList.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_SELECTORCOLOR, null, null, null, null, Theme.key_actionBarDefaultArchivedSelector));
            arrayList.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_SEARCH, null, null, null, null, Theme.key_actionBarDefaultArchivedSearch));
            arrayList.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_SEARCHPLACEHOLDER, null, null, null, null, Theme.key_actionBarDefaultArchivedSearchPlaceholder));
        }

        arrayList.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_AM_ITEMSCOLOR, null, null, null, null, Theme.key_actionBarActionModeDefaultIcon));
        arrayList.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_AM_BACKGROUND, null, null, null, null, Theme.key_actionBarActionModeDefault));
        arrayList.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_AM_TOPBACKGROUND, null, null, null, null, Theme.key_actionBarActionModeDefaultTop));
        arrayList.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_AM_SELECTORCOLOR, null, null, null, null, Theme.key_actionBarActionModeDefaultSelector));
        arrayList.add(new ThemeDescription(selectedDialogsCountTextView, ThemeDescription.FLAG_TEXTCOLOR, null, null, null, null, Theme.key_actionBarActionModeDefaultIcon));

        arrayList.add(new ThemeDescription(listViewAll, ThemeDescription.FLAG_SELECTOR, null, null, null, null, Theme.key_listSelector));

        arrayList.add(new ThemeDescription(listViewAll, 0, new Class[]{View.class}, Theme.dividerPaint, null, null, Theme.key_divider));

        arrayList.add(new ThemeDescription(searchEmptyView, ThemeDescription.FLAG_TEXTCOLOR, null, null, null, null, Theme.key_emptyListPlaceholder));
        arrayList.add(new ThemeDescription(searchEmptyView, ThemeDescription.FLAG_PROGRESSBAR, null, null, null, null, Theme.key_progressCircle));

        arrayList.add(new ThemeDescription(listViewAll, ThemeDescription.FLAG_TEXTCOLOR, new Class[]{DialogsEmptyCell.class}, new String[]{"emptyTextView1"}, null, null, null, Theme.key_chats_nameMessage_threeLines));
        arrayList.add(new ThemeDescription(listViewAll, ThemeDescription.FLAG_TEXTCOLOR, new Class[]{DialogsEmptyCell.class}, new String[]{"emptyTextView2"}, null, null, null, Theme.key_chats_message));

        arrayList.add(new ThemeDescription(floatingButton, ThemeDescription.FLAG_IMAGECOLOR, null, null, null, null, Theme.key_chats_actionIcon));
        arrayList.add(new ThemeDescription(floatingButton, ThemeDescription.FLAG_BACKGROUNDFILTER, null, null, null, null, Theme.key_chats_actionBackground));
        arrayList.add(new ThemeDescription(floatingButton, ThemeDescription.FLAG_BACKGROUNDFILTER | ThemeDescription.FLAG_DRAWABLESELECTEDSTATE, null, null, null, null, Theme.key_chats_actionPressedBackground));

        /*new ThemeDescription(unreadFloatingButtonCounter, ThemeDescription.FLAG_BACKGROUNDFILTER, null, null, null, null, Theme.key_chat_goDownButtonCounterBackground));
        new ThemeDescription(unreadFloatingButtonCounter, ThemeDescription.FLAG_TEXTCOLOR, null, null, null, null, Theme.key_chat_goDownButtonCounter));
        new ThemeDescription(unreadFloatingButton, ThemeDescription.FLAG_IMAGECOLOR, null, null, null, null, Theme.key_chats_actionUnreadIcon));
        new ThemeDescription(unreadFloatingButton, ThemeDescription.FLAG_BACKGROUNDFILTER, null, null, null, null, Theme.key_chats_actionUnreadBackground));
        new ThemeDescription(unreadFloatingButton, ThemeDescription.FLAG_BACKGROUNDFILTER | ThemeDescription.FLAG_DRAWABLESELECTEDSTATE, null, null, null, null, Theme.key_chats_actionUnreadPressedBackground));*/

        arrayList.add(new ThemeDescription(listViewAll, 0, new Class[]{DialogCell.class, ProfileSearchCell.class}, null, new Drawable[]{Theme.avatar_broadcastDrawable, Theme.avatar_savedDrawable}, null, Theme.key_avatar_text));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, cellDelegate, Theme.key_avatar_backgroundRed));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, cellDelegate, Theme.key_avatar_backgroundOrange));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, cellDelegate, Theme.key_avatar_backgroundViolet));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, cellDelegate, Theme.key_avatar_backgroundGreen));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, cellDelegate, Theme.key_avatar_backgroundCyan));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, cellDelegate, Theme.key_avatar_backgroundBlue));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, cellDelegate, Theme.key_avatar_backgroundPink));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, cellDelegate, Theme.key_avatar_backgroundSaved));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, cellDelegate, Theme.key_avatar_backgroundArchived));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, cellDelegate, Theme.key_avatar_backgroundArchivedHidden));
        arrayList.add(new ThemeDescription(listViewAll, 0, new Class[]{DialogCell.class}, Theme.dialogs_countPaint, null, null, Theme.key_chats_unreadCounter));
        arrayList.add(new ThemeDescription(listViewAll, 0, new Class[]{DialogCell.class}, Theme.dialogs_countGrayPaint, null, null, Theme.key_chats_unreadCounterMuted));
        arrayList.add(new ThemeDescription(listViewAll, 0, new Class[]{DialogCell.class}, Theme.dialogs_countTextPaint, null, null, Theme.key_chats_unreadCounterText));
        arrayList.add(new ThemeDescription(listViewAll, 0, new Class[]{DialogCell.class, ProfileSearchCell.class}, null, new Paint[]{Theme.dialogs_namePaint, Theme.dialogs_searchNamePaint}, null, null, Theme.key_chats_name));
        arrayList.add(new ThemeDescription(listViewAll, 0, new Class[]{DialogCell.class, ProfileSearchCell.class}, null, new Paint[]{Theme.dialogs_nameEncryptedPaint, Theme.dialogs_searchNameEncryptedPaint}, null, null, Theme.key_chats_secretName));
        arrayList.add(new ThemeDescription(listViewAll, 0, new Class[]{DialogCell.class, ProfileSearchCell.class}, null, new Drawable[]{Theme.dialogs_lockDrawable}, null, Theme.key_chats_secretIcon));
        arrayList.add(new ThemeDescription(listViewAll, 0, new Class[]{DialogCell.class, ProfileSearchCell.class}, null, new Drawable[]{Theme.dialogs_groupDrawable, Theme.dialogs_broadcastDrawable, Theme.dialogs_botDrawable}, null, Theme.key_chats_nameIcon));
        arrayList.add(new ThemeDescription(listViewAll, 0, new Class[]{DialogCell.class, ProfileSearchCell.class}, null, new Drawable[]{Theme.dialogs_scamDrawable}, null, Theme.key_chats_draft));
        arrayList.add(new ThemeDescription(listViewAll, 0, new Class[]{DialogCell.class}, null, new Drawable[]{Theme.dialogs_pinnedDrawable, Theme.dialogs_reorderDrawable}, null, Theme.key_chats_pinnedIcon));
        if (SharedConfig.useThreeLinesLayout) {
            arrayList.add(new ThemeDescription(listViewAll, 0, new Class[]{DialogCell.class}, Theme.dialogs_messagePaint, null, null, Theme.key_chats_message_threeLines));
        } else {
            arrayList.add(new ThemeDescription(listViewAll, 0, new Class[]{DialogCell.class}, Theme.dialogs_messagePaint, null, null, Theme.key_chats_message));
        }
        arrayList.add(new ThemeDescription(listViewAll, 0, new Class[]{DialogCell.class}, Theme.dialogs_messageNamePaint, null, null, Theme.key_chats_nameMessage_threeLines));
        arrayList.add(new ThemeDescription(listViewAll, 0, new Class[]{DialogCell.class}, null, null, null, Theme.key_chats_draft));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, cellDelegate, Theme.key_chats_nameMessage));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, cellDelegate, Theme.key_chats_draft));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, cellDelegate, Theme.key_chats_attachMessage));

        arrayList.add(new ThemeDescription(null, 0, null, null, null, cellDelegate, Theme.key_chats_nameArchived));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, cellDelegate, Theme.key_chats_nameMessageArchived));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, cellDelegate, Theme.key_chats_nameMessageArchived_threeLines));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, cellDelegate, Theme.key_chats_messageArchived));

        arrayList.add(new ThemeDescription(listViewAll, 0, new Class[]{DialogCell.class}, Theme.dialogs_messagePrintingPaint, null, null, Theme.key_chats_actionMessage));
        arrayList.add(new ThemeDescription(listViewAll, 0, new Class[]{DialogCell.class}, Theme.dialogs_timePaint, null, null, Theme.key_chats_date));
        arrayList.add(new ThemeDescription(listViewAll, 0, new Class[]{DialogCell.class}, Theme.dialogs_pinnedPaint, null, null, Theme.key_chats_pinnedOverlay));
        arrayList.add(new ThemeDescription(listViewAll, 0, new Class[]{DialogCell.class}, Theme.dialogs_tabletSeletedPaint, null, null, Theme.key_chats_tabletSelectedOverlay));
        arrayList.add(new ThemeDescription(listViewAll, 0, new Class[]{DialogCell.class}, null, new Drawable[]{Theme.dialogs_checkDrawable, Theme.dialogs_halfCheckDrawable}, null, Theme.key_chats_sentCheck));
        arrayList.add(new ThemeDescription(listViewAll, 0, new Class[]{DialogCell.class}, null, new Drawable[]{Theme.dialogs_clockDrawable}, null, Theme.key_chats_sentClock));
        arrayList.add(new ThemeDescription(listViewAll, 0, new Class[]{DialogCell.class}, Theme.dialogs_errorPaint, null, null, Theme.key_chats_sentError));
        arrayList.add(new ThemeDescription(listViewAll, 0, new Class[]{DialogCell.class}, null, new Drawable[]{Theme.dialogs_errorDrawable}, null, Theme.key_chats_sentErrorIcon));
        arrayList.add(new ThemeDescription(listViewAll, 0, new Class[]{DialogCell.class, ProfileSearchCell.class}, null, new Drawable[]{Theme.dialogs_verifiedCheckDrawable}, null, Theme.key_chats_verifiedCheck));
        arrayList.add(new ThemeDescription(listViewAll, 0, new Class[]{DialogCell.class, ProfileSearchCell.class}, null, new Drawable[]{Theme.dialogs_verifiedDrawable}, null, Theme.key_chats_verifiedBackground));
        arrayList.add(new ThemeDescription(listViewAll, 0, new Class[]{DialogCell.class}, null, new Drawable[]{Theme.dialogs_muteDrawable}, null, Theme.key_chats_muteIcon));
        arrayList.add(new ThemeDescription(listViewAll, 0, new Class[]{DialogCell.class}, null, new Drawable[]{Theme.dialogs_mentionDrawable}, null, Theme.key_chats_mentionIcon));

        arrayList.add(new ThemeDescription(listViewAll, 0, new Class[]{DialogCell.class}, null, null, null, Theme.key_chats_archivePinBackground));
        arrayList.add(new ThemeDescription(listViewAll, 0, new Class[]{DialogCell.class}, null, null, null, Theme.key_chats_archiveBackground));

        if (SharedConfig.archiveHidden) {
            arrayList.add(new ThemeDescription(listViewAll, 0, new Class[]{DialogCell.class}, new LottieDrawable[]{Theme.dialogs_archiveAvatarDrawable}, "Arrow1", Theme.key_avatar_backgroundArchivedHidden));
            arrayList.add(new ThemeDescription(listViewAll, 0, new Class[]{DialogCell.class}, new LottieDrawable[]{Theme.dialogs_archiveAvatarDrawable}, "Arrow2", Theme.key_avatar_backgroundArchivedHidden));
        } else {
            arrayList.add(new ThemeDescription(listViewAll, 0, new Class[]{DialogCell.class}, new LottieDrawable[]{Theme.dialogs_archiveAvatarDrawable}, "Arrow1", Theme.key_avatar_backgroundArchived));
            arrayList.add(new ThemeDescription(listViewAll, 0, new Class[]{DialogCell.class}, new LottieDrawable[]{Theme.dialogs_archiveAvatarDrawable}, "Arrow2", Theme.key_avatar_backgroundArchived));
        }
        arrayList.add(new ThemeDescription(listViewAll, 0, new Class[]{DialogCell.class}, new LottieDrawable[]{Theme.dialogs_archiveAvatarDrawable}, "Box2", Theme.key_avatar_text));
        arrayList.add(new ThemeDescription(listViewAll, 0, new Class[]{DialogCell.class}, new LottieDrawable[]{Theme.dialogs_archiveAvatarDrawable}, "Box1", Theme.key_avatar_text));

        if (Theme.dialogs_pinArchiveDrawable instanceof LottieDrawable) {
            LottieDrawable lottieDrawable = (LottieDrawable) Theme.dialogs_pinArchiveDrawable;
            arrayList.add(new ThemeDescription(listViewAll, 0, new Class[]{DialogCell.class}, new LottieDrawable[]{lottieDrawable}, "Arrow", Theme.key_chats_archiveIcon));
            arrayList.add(new ThemeDescription(listViewAll, 0, new Class[]{DialogCell.class}, new LottieDrawable[]{lottieDrawable}, "Line", Theme.key_chats_archiveIcon));
        } else {
            arrayList.add(new ThemeDescription(listViewAll, 0, new Class[]{DialogCell.class}, null, new Drawable[]{Theme.dialogs_pinArchiveDrawable}, null, Theme.key_chats_archiveIcon));
        }

        if (Theme.dialogs_unpinArchiveDrawable instanceof LottieDrawable) {
            LottieDrawable lottieDrawable = (LottieDrawable) Theme.dialogs_unpinArchiveDrawable;
            arrayList.add(new ThemeDescription(listViewAll, 0, new Class[]{DialogCell.class}, new LottieDrawable[]{lottieDrawable}, "Arrow", Theme.key_chats_archiveIcon));
            arrayList.add(new ThemeDescription(listViewAll, 0, new Class[]{DialogCell.class}, new LottieDrawable[]{lottieDrawable}, "Line", Theme.key_chats_archiveIcon));
        } else {
            arrayList.add(new ThemeDescription(listViewAll, 0, new Class[]{DialogCell.class}, null, new Drawable[]{Theme.dialogs_unpinArchiveDrawable}, null, Theme.key_chats_archiveIcon));
        }

        if (Theme.dialogs_archiveDrawable instanceof LottieDrawable) {
            LottieDrawable lottieDrawable = (LottieDrawable) Theme.dialogs_archiveDrawable;
            arrayList.add(new ThemeDescription(listViewAll, 0, new Class[]{DialogCell.class}, new LottieDrawable[]{lottieDrawable}, "Arrow", Theme.key_chats_archiveBackground));
            arrayList.add(new ThemeDescription(listViewAll, 0, new Class[]{DialogCell.class}, new LottieDrawable[]{lottieDrawable}, "Box2", Theme.key_chats_archiveIcon));
            arrayList.add(new ThemeDescription(listViewAll, 0, new Class[]{DialogCell.class}, new LottieDrawable[]{lottieDrawable}, "Box1", Theme.key_chats_archiveIcon));
        } else {
            arrayList.add(new ThemeDescription(listViewAll, 0, new Class[]{DialogCell.class}, null, new Drawable[]{Theme.dialogs_archiveDrawable}, null, Theme.key_chats_archiveIcon));
        }

        if (Theme.dialogs_unarchiveDrawable instanceof LottieDrawable) {
            LottieDrawable lottieDrawable = (LottieDrawable) Theme.dialogs_unarchiveDrawable;
            arrayList.add(new ThemeDescription(listViewAll, 0, new Class[]{DialogCell.class}, new LottieDrawable[]{lottieDrawable}, "Arrow1", Theme.key_chats_archiveIcon));
            arrayList.add(new ThemeDescription(listViewAll, 0, new Class[]{DialogCell.class}, new LottieDrawable[]{lottieDrawable}, "Arrow2", Theme.key_chats_archivePinBackground));
            arrayList.add(new ThemeDescription(listViewAll, 0, new Class[]{DialogCell.class}, new LottieDrawable[]{lottieDrawable}, "Box2", Theme.key_chats_archiveIcon));
            arrayList.add(new ThemeDescription(listViewAll, 0, new Class[]{DialogCell.class}, new LottieDrawable[]{lottieDrawable}, "Box1", Theme.key_chats_archiveIcon));
        } else {
            arrayList.add(new ThemeDescription(listViewAll, 0, new Class[]{DialogCell.class}, null, new Drawable[]{Theme.dialogs_unarchiveDrawable}, null, Theme.key_chats_archiveIcon));
        }

        arrayList.add(new ThemeDescription(sideMenu, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_chats_menuBackground));
        arrayList.add(new ThemeDescription(sideMenu, 0, new Class[]{DrawerProfileCell.class}, null, null, null, Theme.key_chats_menuName));
        arrayList.add(new ThemeDescription(sideMenu, 0, new Class[]{DrawerProfileCell.class}, null, null, null, Theme.key_chats_menuPhone));
        arrayList.add(new ThemeDescription(sideMenu, 0, new Class[]{DrawerProfileCell.class}, null, null, null, Theme.key_chats_menuPhoneCats));
        arrayList.add(new ThemeDescription(sideMenu, 0, new Class[]{DrawerProfileCell.class}, null, null, null, Theme.key_chats_menuCloudBackgroundCats));
        arrayList.add(new ThemeDescription(sideMenu, 0, new Class[]{DrawerProfileCell.class}, null, null, null, Theme.key_chat_serviceBackground));
        arrayList.add(new ThemeDescription(sideMenu, 0, new Class[]{DrawerProfileCell.class}, null, null, null, Theme.key_chats_menuTopShadow));
        arrayList.add(new ThemeDescription(sideMenu, 0, new Class[]{DrawerProfileCell.class}, null, null, null, Theme.key_chats_menuTopShadowCats));
        arrayList.add(new ThemeDescription(sideMenu, ThemeDescription.FLAG_CELLBACKGROUNDCOLOR | ThemeDescription.FLAG_CHECKTAG, new Class[]{DrawerProfileCell.class}, null, null, cellDelegate, Theme.key_chats_menuTopBackgroundCats));
        arrayList.add(new ThemeDescription(sideMenu, ThemeDescription.FLAG_CELLBACKGROUNDCOLOR | ThemeDescription.FLAG_CHECKTAG, new Class[]{DrawerProfileCell.class}, null, null, cellDelegate, Theme.key_chats_menuTopBackground));

        arrayList.add(new ThemeDescription(sideMenu, ThemeDescription.FLAG_IMAGECOLOR, new Class[]{DrawerActionCell.class}, new String[]{"textView"}, null, null, null, Theme.key_chats_menuItemIcon));
        arrayList.add(new ThemeDescription(sideMenu, 0, new Class[]{DrawerActionCell.class}, new String[]{"textView"}, null, null, null, Theme.key_chats_menuItemText));

        arrayList.add(new ThemeDescription(sideMenu, 0, new Class[]{DrawerUserCell.class}, new String[]{"textView"}, null, null, null, Theme.key_chats_menuItemText));
        arrayList.add(new ThemeDescription(sideMenu, ThemeDescription.FLAG_TEXTCOLOR, new Class[]{DrawerUserCell.class}, new String[]{"checkBox"}, null, null, null, Theme.key_chats_unreadCounterText));
        arrayList.add(new ThemeDescription(sideMenu, ThemeDescription.FLAG_TEXTCOLOR, new Class[]{DrawerUserCell.class}, new String[]{"checkBox"}, null, null, null, Theme.key_chats_unreadCounter));
        arrayList.add(new ThemeDescription(sideMenu, ThemeDescription.FLAG_TEXTCOLOR, new Class[]{DrawerUserCell.class}, new String[]{"checkBox"}, null, null, null, Theme.key_chats_menuBackground));
        arrayList.add(new ThemeDescription(sideMenu, ThemeDescription.FLAG_IMAGECOLOR, new Class[]{DrawerAddCell.class}, new String[]{"textView"}, null, null, null, Theme.key_chats_menuItemIcon));
        arrayList.add(new ThemeDescription(sideMenu, 0, new Class[]{DrawerAddCell.class}, new String[]{"textView"}, null, null, null, Theme.key_chats_menuItemText));

        arrayList.add(new ThemeDescription(sideMenu, 0, new Class[]{DividerCell.class}, Theme.dividerPaint, null, null, Theme.key_divider));

        arrayList.add(new ThemeDescription(listViewAll, 0, new Class[]{LoadingCell.class}, new String[]{"progressBar"}, null, null, null, Theme.key_progressCircle));

        arrayList.add(new ThemeDescription(listViewAll, 0, new Class[]{ProfileSearchCell.class}, Theme.dialogs_offlinePaint, null, null, Theme.key_windowBackgroundWhiteGrayText3));
        arrayList.add(new ThemeDescription(listViewAll, 0, new Class[]{ProfileSearchCell.class}, Theme.dialogs_onlinePaint, null, null, Theme.key_windowBackgroundWhiteBlueText3));

        arrayList.add(new ThemeDescription(listViewAll, 0, new Class[]{GraySectionCell.class}, new String[]{"textView"}, null, null, null, Theme.key_graySectionText));
        arrayList.add(new ThemeDescription(listViewAll, ThemeDescription.FLAG_CELLBACKGROUNDCOLOR, new Class[]{GraySectionCell.class}, null, null, null, Theme.key_graySection));

        arrayList.add(new ThemeDescription(listViewAll, ThemeDescription.FLAG_TEXTCOLOR, new Class[]{HashtagSearchCell.class}, null, null, null, Theme.key_windowBackgroundWhiteBlackText));

        arrayList.add(new ThemeDescription(progressView, ThemeDescription.FLAG_PROGRESSBAR, null, null, null, null, Theme.key_progressCircle));

        arrayList.add(new ThemeDescription(dialogsAdapterAll != null ? dialogsAdapterAll.getArchiveHintCellPager() : null, 0, new Class[]{ArchiveHintInnerCell.class}, new String[]{"imageView"}, null, null, null, Theme.key_chats_nameMessage_threeLines));
        arrayList.add(new ThemeDescription(dialogsAdapterAll != null ? dialogsAdapterAll.getArchiveHintCellPager() : null, 0, new Class[]{ArchiveHintInnerCell.class}, new String[]{"imageView2"}, null, null, null, Theme.key_chats_unreadCounter));
        arrayList.add(new ThemeDescription(dialogsAdapterAll != null ? dialogsAdapterAll.getArchiveHintCellPager() : null, 0, new Class[]{ArchiveHintInnerCell.class}, new String[]{"headerTextView"}, null, null, null, Theme.key_chats_nameMessage_threeLines));
        arrayList.add(new ThemeDescription(dialogsAdapterAll != null ? dialogsAdapterAll.getArchiveHintCellPager() : null, 0, new Class[]{ArchiveHintInnerCell.class}, new String[]{"messageTextView"}, null, null, null, Theme.key_chats_message));
        arrayList.add(new ThemeDescription(dialogsAdapterAll != null ? dialogsAdapterAll.getArchiveHintCellPager() : null, ThemeDescription.FLAG_LISTGLOWCOLOR, null, null, null, null, Theme.key_actionBarDefaultArchived));

        arrayList.add(new ThemeDescription(listViewAll, ThemeDescription.FLAG_BACKGROUNDFILTER, new Class[]{ShadowSectionCell.class}, null, null, null, Theme.key_windowBackgroundGrayShadow));
        arrayList.add(new ThemeDescription(listViewAll, ThemeDescription.FLAG_BACKGROUNDFILTER | ThemeDescription.FLAG_CELLBACKGROUNDCOLOR, new Class[]{ShadowSectionCell.class}, null, null, null, Theme.key_windowBackgroundGray));

        arrayList.add(new ThemeDescription(dialogsSearchAdapter != null ? dialogsSearchAdapter.getInnerListView() : null, 0, new Class[]{HintDialogCell.class}, Theme.dialogs_countPaint, null, null, Theme.key_chats_unreadCounter));
        arrayList.add(new ThemeDescription(dialogsSearchAdapter != null ? dialogsSearchAdapter.getInnerListView() : null, 0, new Class[]{HintDialogCell.class}, Theme.dialogs_countGrayPaint, null, null, Theme.key_chats_unreadCounterMuted));
        arrayList.add(new ThemeDescription(dialogsSearchAdapter != null ? dialogsSearchAdapter.getInnerListView() : null, 0, new Class[]{HintDialogCell.class}, Theme.dialogs_countTextPaint, null, null, Theme.key_chats_unreadCounterText));
        arrayList.add(new ThemeDescription(dialogsSearchAdapter != null ? dialogsSearchAdapter.getInnerListView() : null, 0, new Class[]{HintDialogCell.class}, Theme.dialogs_archiveTextPaint, null, null, Theme.key_chats_archiveText));
        arrayList.add(new ThemeDescription(dialogsSearchAdapter != null ? dialogsSearchAdapter.getInnerListView() : null, 0, new Class[]{HintDialogCell.class}, new String[]{"nameTextView"}, null, null, null, Theme.key_windowBackgroundWhiteBlackText));
        arrayList.add(new ThemeDescription(dialogsSearchAdapter != null ? dialogsSearchAdapter.getInnerListView() : null, 0, new Class[]{HintDialogCell.class}, null, null, null, Theme.key_chats_onlineCircle));

        arrayList.add(new ThemeDescription(fragmentView, ThemeDescription.FLAG_BACKGROUND | ThemeDescription.FLAG_CHECKTAG, new Class[]{FragmentContextView.class}, new String[]{"frameLayout"}, null, null, null, Theme.key_inappPlayerBackground));
        arrayList.add(new ThemeDescription(fragmentView, ThemeDescription.FLAG_IMAGECOLOR, new Class[]{FragmentContextView.class}, new String[]{"playButton"}, null, null, null, Theme.key_inappPlayerPlayPause));
        arrayList.add(new ThemeDescription(fragmentView, ThemeDescription.FLAG_TEXTCOLOR | ThemeDescription.FLAG_CHECKTAG, new Class[]{FragmentContextView.class}, new String[]{"titleTextView"}, null, null, null, Theme.key_inappPlayerTitle));
        arrayList.add(new ThemeDescription(fragmentView, ThemeDescription.FLAG_TEXTCOLOR | ThemeDescription.FLAG_FASTSCROLL, new Class[]{FragmentContextView.class}, new String[]{"titleTextView"}, null, null, null, Theme.key_inappPlayerPerformer));
        arrayList.add(new ThemeDescription(fragmentView, ThemeDescription.FLAG_IMAGECOLOR, new Class[]{FragmentContextView.class}, new String[]{"closeButton"}, null, null, null, Theme.key_inappPlayerClose));

        arrayList.add(new ThemeDescription(fragmentView, ThemeDescription.FLAG_BACKGROUND | ThemeDescription.FLAG_CHECKTAG, new Class[]{FragmentContextView.class}, new String[]{"frameLayout"}, null, null, null, Theme.key_returnToCallBackground));
        arrayList.add(new ThemeDescription(fragmentView, ThemeDescription.FLAG_TEXTCOLOR | ThemeDescription.FLAG_CHECKTAG, new Class[]{FragmentContextView.class}, new String[]{"titleTextView"}, null, null, null, Theme.key_returnToCallText));

        for (int a = 0; a < undoView.length; a++) {
            arrayList.add(new ThemeDescription(undoView[a], ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_undo_background));
            arrayList.add(new ThemeDescription(undoView[a], 0, new Class[]{UndoView.class}, new String[]{"undoImageView"}, null, null, null, Theme.key_undo_cancelColor));
            arrayList.add(new ThemeDescription(undoView[a], 0, new Class[]{UndoView.class}, new String[]{"undoTextView"}, null, null, null, Theme.key_undo_cancelColor));
            arrayList.add(new ThemeDescription(undoView[a], 0, new Class[]{UndoView.class}, new String[]{"infoTextView"}, null, null, null, Theme.key_undo_infoColor));
            arrayList.add(new ThemeDescription(undoView[a], 0, new Class[]{UndoView.class}, new String[]{"subinfoTextView"}, null, null, null, Theme.key_undo_infoColor));
            arrayList.add(new ThemeDescription(undoView[a], 0, new Class[]{UndoView.class}, new String[]{"textPaint"}, null, null, null, Theme.key_undo_infoColor));
            arrayList.add(new ThemeDescription(undoView[a], 0, new Class[]{UndoView.class}, new String[]{"progressPaint"}, null, null, null, Theme.key_undo_infoColor));
            arrayList.add(new ThemeDescription(undoView[a], 0, new Class[]{UndoView.class}, new String[]{"leftImageView"}, "info1", Theme.key_undo_background));
            arrayList.add(new ThemeDescription(undoView[a], 0, new Class[]{UndoView.class}, new String[]{"leftImageView"}, "info2", Theme.key_undo_background));
            arrayList.add(new ThemeDescription(undoView[a], 0, new Class[]{UndoView.class}, new String[]{"leftImageView"}, "luc12", Theme.key_undo_infoColor));
            arrayList.add(new ThemeDescription(undoView[a], 0, new Class[]{UndoView.class}, new String[]{"leftImageView"}, "luc11", Theme.key_undo_infoColor));
            arrayList.add(new ThemeDescription(undoView[a], 0, new Class[]{UndoView.class}, new String[]{"leftImageView"}, "luc10", Theme.key_undo_infoColor));
            arrayList.add(new ThemeDescription(undoView[a], 0, new Class[]{UndoView.class}, new String[]{"leftImageView"}, "luc9", Theme.key_undo_infoColor));
            arrayList.add(new ThemeDescription(undoView[a], 0, new Class[]{UndoView.class}, new String[]{"leftImageView"}, "luc8", Theme.key_undo_infoColor));
            arrayList.add(new ThemeDescription(undoView[a], 0, new Class[]{UndoView.class}, new String[]{"leftImageView"}, "luc7", Theme.key_undo_infoColor));
            arrayList.add(new ThemeDescription(undoView[a], 0, new Class[]{UndoView.class}, new String[]{"leftImageView"}, "luc6", Theme.key_undo_infoColor));
            arrayList.add(new ThemeDescription(undoView[a], 0, new Class[]{UndoView.class}, new String[]{"leftImageView"}, "luc5", Theme.key_undo_infoColor));
            arrayList.add(new ThemeDescription(undoView[a], 0, new Class[]{UndoView.class}, new String[]{"leftImageView"}, "luc4", Theme.key_undo_infoColor));
            arrayList.add(new ThemeDescription(undoView[a], 0, new Class[]{UndoView.class}, new String[]{"leftImageView"}, "luc3", Theme.key_undo_infoColor));
            arrayList.add(new ThemeDescription(undoView[a], 0, new Class[]{UndoView.class}, new String[]{"leftImageView"}, "luc2", Theme.key_undo_infoColor));
            arrayList.add(new ThemeDescription(undoView[a], 0, new Class[]{UndoView.class}, new String[]{"leftImageView"}, "luc1", Theme.key_undo_infoColor));
            arrayList.add(new ThemeDescription(undoView[a], 0, new Class[]{UndoView.class}, new String[]{"leftImageView"}, "Oval", Theme.key_undo_infoColor));
        }

        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogBackground));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogBackgroundGray));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogTextBlack));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogTextLink));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogLinkSelection));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogTextBlue));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogTextBlue2));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogTextBlue3));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogTextBlue4));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogTextRed));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogTextRed2));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogTextGray));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogTextGray2));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogTextGray3));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogTextGray4));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogIcon));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogRedIcon));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogTextHint));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogInputField));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogInputFieldActivated));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogCheckboxSquareBackground));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogCheckboxSquareCheck));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogCheckboxSquareUnchecked));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogCheckboxSquareDisabled));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogRadioBackground));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogRadioBackgroundChecked));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogProgressCircle));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogButton));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogButtonSelector));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogScrollGlow));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogRoundCheckBox));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogRoundCheckBoxCheck));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogBadgeBackground));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogBadgeText));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogLineProgress));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogLineProgressBackground));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogGrayLine));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialog_inlineProgressBackground));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialog_inlineProgress));

        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogSearchBackground));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogSearchHint));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogSearchIcon));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogSearchText));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogFloatingButton));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogFloatingIcon));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogShadowLine));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_sheet_scrollUp));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_sheet_other));

        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_player_actionBar));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_player_actionBarSelector));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_player_actionBarTitle));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_player_actionBarTop));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_player_actionBarSubtitle));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_player_actionBarItems));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_player_background));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_player_time));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_player_progressBackground));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_player_progressCachedBackground));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_player_progress));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_player_placeholder));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_player_placeholderBackground));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_player_button));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, Theme.key_player_buttonActive));

        return arrayList.toArray(new ThemeDescription[0]);
    }

    private class DialogsRecyclerListView extends RecyclerListView {

        private boolean firstLayout = true;
        private boolean ignoreLayout;
        private Tab tab;


        public DialogsRecyclerListView(Context context) {
            super(context);
        }

        public DialogsRecyclerListView(Context context, Tab tab) {
            super(context);
            this.tab = tab;
        }


        @Override
        protected void dispatchDraw(Canvas canvas) {
            super.dispatchDraw(canvas);
            if (slidingView != null && pacmanAnimation != null) {
                pacmanAnimation.draw(canvas, slidingView.getTop() + slidingView.getMeasuredHeight() / 2);
            }
        }

        @Override
        public void setAdapter(Adapter adapter) {
            super.setAdapter(adapter);
            firstLayout = true;
        }

        private void checkIfAdapterValid() {
            if (tabViews.get(tab) != null && tabAdapters.get(tab) != null && tabViews.get(tab).getAdapter() == tabAdapters.get(tab) && lastItemsCount != tabAdapters.get(tab).getItemCount()) {
                ignoreLayout = true;
                tabAdapters.get(tab).notifyDataSetChanged();
                ignoreLayout = false;
            }
        }


        @Override
        protected void onMeasure(int widthSpec, int heightSpec) {
            if (firstLayout && getMessagesController().dialogsLoaded) {
                if (hasHiddenArchive()) {
                    ignoreLayout = true;
                    layoutManagerAll.scrollToPositionWithOffset(1, 0);
                    ignoreLayout = false;
                }
                firstLayout = false;
            }
            checkIfAdapterValid();

            super.onMeasure(widthSpec, heightSpec);
        }

        @Override
        protected void onLayout(boolean changed, int l, int t, int r, int b) {
            super.onLayout(changed, l, t, r, b);
            if ((dialogRemoveFinished != 0 || dialogInsertFinished != 0 || dialogChangeFinished != 0) && !dialogsItemAnimator.isRunning()) {
                onDialogAnimationFinished();
            }
        }

        @Override
        public void requestLayout() {
            if (ignoreLayout) {
                return;
            }
            super.requestLayout();
        }

        @Override
        public boolean onTouchEvent(MotionEvent e) {
            if (waitingForScrollFinished || dialogRemoveFinished != 0 || dialogInsertFinished != 0 || dialogChangeFinished != 0) {
                return false;
            }
            int action = e.getAction();
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                if (!itemTouchhelper.isIdle() && swipeController.swipingFolder) {
                    swipeController.swipeFolderBack = true;
                    if (itemTouchhelper.checkHorizontalSwipe(null, ItemTouchHelper.LEFT) != 0) {
                        SharedConfig.toggleArchiveHidden();
                        getUndoView().showWithAction(0, UndoView.ACTION_ARCHIVE_PINNED, null, null);
                    }
                }
            }
            boolean result = super.onTouchEvent(e);
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                if (allowScrollToHiddenView) {
                    int currentPosition = layoutManagerAll.findFirstVisibleItemPosition();
                    if (currentPosition == 0) {
                        View view = layoutManagerAll.findViewByPosition(currentPosition);
                        int height = AndroidUtilities.dp(SharedConfig.useThreeLinesLayout ? 78 : 72) / 4 * 3;
                        int diff = view.getTop() + view.getMeasuredHeight();
                        if (view != null) {
                            if (diff < height) {
                                tabViews.get(tab).smoothScrollBy(0, diff, CubicBezierInterpolator.EASE_OUT_QUINT);
                            } else {
                                tabViews.get(tab).smoothScrollBy(0, view.getTop(), CubicBezierInterpolator.EASE_OUT_QUINT);
                            }
                        }
                    }
                    allowScrollToHiddenView = false;
                }
            }
            return result;
        }

        @Override
        public boolean onInterceptTouchEvent(MotionEvent e) {
            if (waitingForScrollFinished || dialogRemoveFinished != 0 || dialogInsertFinished != 0 || dialogChangeFinished != 0) {
                return false;
            }
            if (e.getAction() == MotionEvent.ACTION_DOWN) {
                allowSwipeDuringCurrentTouch = !actionBar.isActionModeShowed();
                checkIfAdapterValid();
            }
            return super.onInterceptTouchEvent(e);
        }

    }

    private class DialogsLayoutManager extends LinearLayoutManager {

        private Tab tab;

        public DialogsLayoutManager(Context context, Tab tab) {
            super(context);
            this.tab = tab;
        }

        @Override
        public void smoothScrollToPosition(RecyclerView
                                                   recyclerView, RecyclerView.State state, int position) {
            if (hasHiddenArchive() && position == 1) {
                super.smoothScrollToPosition(recyclerView, state, position);
            } else {
                LinearSmoothScrollerMiddle linearSmoothScroller = new LinearSmoothScrollerMiddle(recyclerView.getContext());
                linearSmoothScroller.setTargetPosition(position);
                startSmoothScroll(linearSmoothScroller);
            }
        }

        @Override
        public int scrollVerticallyBy(int dy, RecyclerView.
                Recycler recycler, RecyclerView.State state) {
            RecyclerListView list = tabViews.get(currentTab);
            if (list != null && list.getAdapter() == tabAdapters.get(currentTab) && dialogsType == 0 && !onlySelect && !allowScrollToHiddenView && folderId == 0 && dy < 0 && getMessagesController().hasHiddenArchive()) {
                int currentPosition = findFirstVisibleItemPosition();
                if (currentPosition == 0) {
                    View view = findViewByPosition(currentPosition);
                    if (view != null && view.getBottom() <= AndroidUtilities.dp(1)) {
                        currentPosition = 1;
                    }
                }
                if (currentPosition != 0 && currentPosition != RecyclerView.NO_POSITION) {
                    View view = findViewByPosition(currentPosition);
                    if (view != null) {
                        int dialogHeight = AndroidUtilities.dp(SharedConfig.useThreeLinesLayout ? 78 : 72) + 1;
                        int canScrollDy = -view.getTop() + (currentPosition - 1) * dialogHeight;
                        int positiveDy = Math.abs(dy);
                        if (canScrollDy < positiveDy) {
                            totalConsumedAmount += Math.abs(dy);
                            dy = -canScrollDy;
                            if (startedScrollAtTop && totalConsumedAmount >= AndroidUtilities.dp(150)) {
                                allowScrollToHiddenView = true;
                                try {
                                    list.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
                                } catch (Exception ignore) {

                                }
                            }
                        }
                    }
                }
            }
            return super.scrollVerticallyBy(dy, recycler, state);
        }


    }


    private class OnItemClickListener implements RecyclerListView.OnItemClickListener {

        private Tab tab;

        public OnItemClickListener(Tab tab) {
            this.tab = tab;
        }


        @Override
        public void onItemClick(View view, int position) {
            RecyclerListView list = tabViews.get(tab);
            if (list == null || !(list.getAdapter() instanceof RecyclerListView.SelectionAdapter)) {
                return;
            }
            RecyclerListView.SelectionAdapter adapter = (RecyclerListView.SelectionAdapter) list.getAdapter();
            if (adapter == null || getParentActivity() == null) {
                return;
            }

            if (dialogsType == 9) {
                final TLRPC.Dialog dialog;
                ArrayList<TLRPC.Dialog> dialogs = getDialogsArray(currentAccount, dialogsType, folderId, dialogsListFrozen, tab);
                if (position < 0 || position > dialogs.size()) {
                    return;
                }
                dialog = dialogs.get(position);
                if (onlySelect && adapter instanceof DialogsAdapter) {
                    ((DialogsAdapter) adapter).addOrRemoveSelectedDialog(dialog.id, view);
                    updateSelectedCount();
                }
                return;
            }

            long dialog_id = 0;
            int message_id = 0;
            boolean isGlobalSearch = false;
            if (adapter instanceof DialogsAdapter && adapter == tabAdapters.get(tab)) {
                DialogsAdapter dialogsAdapter = (DialogsAdapter) tabAdapters.get(tab);
                TLObject object = dialogsAdapter.getItem(position);
                if (object instanceof TLRPC.User) {
                    dialog_id = ((TLRPC.User) object).id;
                } else if (object instanceof TLRPC.Dialog) {
                    TLRPC.Dialog dialog = (TLRPC.Dialog) object;
                    if (dialog instanceof TLRPC.TL_dialogFolder) {
                        if (actionBar.isActionModeShowed()) {
                            ((DialogsAdapter) adapter).clearSelectedDialogs();
                            ((DialogsAdapter) adapter).notifyDataSetChanged();
                            hideActionMode(false);
                            updateSelectedCount();
                            return;
                        }
                        TLRPC.TL_dialogFolder dialogFolder = (TLRPC.TL_dialogFolder) dialog;
                        Bundle args = new Bundle();
                        args.putInt("folderId", dialogFolder.folder.id);
                        presentFragment(new DialogsActivity(args));
                        return;
                    }
                    dialog_id = dialog.id;
                    if (actionBar.isActionModeShowed()) {
                        if (((DialogsAdapter) tabAdapters.get(tab)).hasSelectedArchive()) {
                            ((DialogsAdapter) adapter).clearSelectedDialogs();
                            ((DialogsAdapter) adapter).notifyDataSetChanged();
                            hideActionMode(false);
                            updateSelectedCount();
                            return;
                        } else {
                            showOrUpdateActionMode(dialog, view);
                            return;
                        }
                    }
                } else if (object instanceof TLRPC.TL_recentMeUrlChat) {
                    dialog_id = -((TLRPC.TL_recentMeUrlChat) object).chat_id;
                } else if (object instanceof TLRPC.TL_recentMeUrlUser) {
                    dialog_id = ((TLRPC.TL_recentMeUrlUser) object).user_id;
                } else if (object instanceof TLRPC.TL_recentMeUrlChatInvite) {
                    TLRPC.TL_recentMeUrlChatInvite chatInvite = (TLRPC.TL_recentMeUrlChatInvite) object;
                    TLRPC.ChatInvite invite = chatInvite.chat_invite;
                    if (invite.chat == null && (!invite.channel || invite.megagroup) || invite.chat != null && (!ChatObject.isChannel(invite.chat) || invite.chat.megagroup)) {
                        String hash = chatInvite.url;
                        int index = hash.indexOf('/');
                        if (index > 0) {
                            hash = hash.substring(index + 1);
                        }
                        showDialog(new JoinGroupAlert(getParentActivity(), invite, hash, DialogsActivity.this));
                        return;
                    } else {
                        if (invite.chat != null) {
                            dialog_id = -invite.chat.id;
                        } else {
                            return;
                        }
                    }
                } else if (object instanceof TLRPC.TL_recentMeUrlStickerSet) {
                    TLRPC.StickerSet stickerSet = ((TLRPC.TL_recentMeUrlStickerSet) object).set.set;
                    TLRPC.TL_inputStickerSetID set = new TLRPC.TL_inputStickerSetID();
                    set.id = stickerSet.id;
                    set.access_hash = stickerSet.access_hash;
                    showDialog(new StickersAlert(getParentActivity(), DialogsActivity.this, set, null, null));
                    return;
                } else if (object instanceof TLRPC.TL_recentMeUrlUnknown) {
                    return;
                } else {
                    return;
                }
            } else if (adapter == dialogsSearchAdapter) {
                Object obj = dialogsSearchAdapter.getItem(position);
                isGlobalSearch = dialogsSearchAdapter.isGlobalSearch(position);
                if (obj instanceof TLRPC.User) {
                    dialog_id = ((TLRPC.User) obj).id;
                    if (!onlySelect) {
                        searchDialogId = dialog_id;
                        searchObject = (TLRPC.User) obj;
                    }
                } else if (obj instanceof TLRPC.Chat) {
                    if (((TLRPC.Chat) obj).id > 0) {
                        dialog_id = -((TLRPC.Chat) obj).id;
                    } else {
                        dialog_id = AndroidUtilities.makeBroadcastId(((TLRPC.Chat) obj).id);
                    }
                    if (!onlySelect) {
                        searchDialogId = dialog_id;
                        searchObject = (TLRPC.Chat) obj;
                    }
                } else if (obj instanceof TLRPC.EncryptedChat) {
                    dialog_id = ((long) ((TLRPC.EncryptedChat) obj).id) << 32;
                    if (!onlySelect) {
                        searchDialogId = dialog_id;
                        searchObject = (TLRPC.EncryptedChat) obj;
                    }
                } else if (obj instanceof MessageObject) {
                    MessageObject messageObject = (MessageObject) obj;
                    dialog_id = messageObject.getDialogId();
                    message_id = messageObject.getId();
                    dialogsSearchAdapter.addHashtagsFromMessage(dialogsSearchAdapter.getLastSearchString());
                } else if (obj instanceof String) {
                    actionBar.openSearchField((String) obj, false);
                }
            }

            if (dialog_id == 0) {
                return;
            }

            if (onlySelect) {
                if (adapter instanceof DialogsAdapter && ((DialogsAdapter) adapter).hasSelectedArchive()) {
                    ((DialogsAdapter) adapter).clearSelectedDialogs();
                    ((DialogsAdapter) adapter).notifyDataSetChanged();
                    hideActionMode(false);
                    updateSelectedCount();
                    return;
                }
                if (adapter instanceof DialogsAdapter && ((DialogsAdapter) adapter).hasSelectedDialogs()) {
                    ((DialogsAdapter) adapter).addOrRemoveSelectedDialog(dialog_id, view);
                    updateSelectedCount();
                } else {
                    didSelectResult(dialog_id, true, false);
                }
            } else {
                Bundle args = new Bundle();
                int lower_part = (int) dialog_id;
                int high_id = (int) (dialog_id >> 32);
                if (lower_part != 0) {
                    if (high_id == 1) {
                        args.putInt("chat_id", lower_part);
                    } else {
                        if (lower_part > 0) {
                            args.putInt("user_id", lower_part);
                        } else if (lower_part < 0) {
                            if (message_id != 0) {
                                TLRPC.Chat chat = getMessagesController().getChat(-lower_part);
                                if (chat != null && chat.migrated_to != null) {
                                    args.putInt("migrated_to", lower_part);
                                    lower_part = -chat.migrated_to.channel_id;
                                }
                            }
                            args.putInt("chat_id", -lower_part);
                        }
                    }
                } else {
                    args.putInt("enc_id", high_id);
                }
                if (message_id != 0) {
                    args.putInt("message_id", message_id);
                } else if (!isGlobalSearch) {
                    closeSearch();
                } else {
                    if (searchObject != null) {
                        dialogsSearchAdapter.putRecentSearch(searchDialogId, searchObject);
                        searchObject = null;
                    }
                }
                if (AndroidUtilities.isTablet()) {
                    if (openedDialogId == dialog_id && adapter != dialogsSearchAdapter) {
                        return;
                    }
                    if (adapter instanceof DialogsAdapter) {
                        ((DialogsAdapter) adapter).setOpenedDialogId(openedDialogId = dialog_id);
                        updateVisibleRows(MessagesController.UPDATE_MASK_SELECT_DIALOG);
                    }

                }
                if (searchString != null) {
                    if (getMessagesController().checkCanOpenChat(args, DialogsActivity.this)) {
                        getNotificationCenter().postNotificationName(NotificationCenter.closeChats);
                        presentFragment(new ChatActivity(args));
                    }
                } else {
                    if (getMessagesController().checkCanOpenChat(args, DialogsActivity.this)) {
                        presentFragment(new ChatActivity(args));
                    }
                }
            }
        }
    }

    private class OnDialogsScrollListener extends RecyclerView.OnScrollListener {

        private boolean scrollingManually;
        private Tab tab;

        public OnDialogsScrollListener(Tab tab) {
            this.tab = tab;
        }

        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                if (searching && searchWas) {
                    AndroidUtilities.hideKeyboard(getParentActivity().getCurrentFocus());
                }
                scrollingManually = true;
            } else {
                scrollingManually = false;
            }
        }

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            LinearLayoutManager lm = (LinearLayoutManager) tabViews.get(tab).getLayoutManager();
            int firstVisibleItem = lm.findFirstVisibleItemPosition();
            int visibleItemCount = Math.abs(layoutManagerAll.findLastVisibleItemPosition() - firstVisibleItem) + 1;
            int totalItemCount = recyclerView.getAdapter().getItemCount();

            if (searching && searchWas) {
                if (visibleItemCount > 0 && lm.findLastVisibleItemPosition() == totalItemCount - 1 && !dialogsSearchAdapter.isMessagesSearchEndReached()) {
                    dialogsSearchAdapter.loadMoreSearchMessages();
                }
                return;
            }
            if (visibleItemCount > 0) {
                if (layoutManagerAll.findLastVisibleItemPosition() >= getDialogsArray(currentAccount, dialogsType, folderId, dialogsListFrozen, tab).size() - 10) {
                    boolean fromCache = !getMessagesController().isDialogsEndReached(folderId);
                    if (fromCache || !getMessagesController().isServerDialogsEndReached(folderId)) {
                        AndroidUtilities.runOnUIThread(() -> getMessagesController().loadDialogs(folderId, -1, 100, fromCache));
                    }
                }
            }

            //checkUnreadButton(true);

            if (floatingButtonContainer.getVisibility() != View.GONE) {
                final View topChild = recyclerView.getChildAt(0);
                int firstViewTop = 0;
                if (topChild != null) {
                    firstViewTop = topChild.getTop();
                }
                boolean goingDown;
                boolean changed = true;
                if (prevPosition == firstVisibleItem) {
                    final int topDelta = prevTop - firstViewTop;
                    goingDown = firstViewTop < prevTop;
                    changed = Math.abs(topDelta) > 1;
                } else {
                    goingDown = firstVisibleItem > prevPosition;
                }
                if (changed && scrollUpdated && (goingDown || !goingDown && scrollingManually) && tab != Tab.FOLDERS) {
                    hideFloatingButton(goingDown);
                }
                prevPosition = firstVisibleItem;
                prevTop = firstViewTop;
                scrollUpdated = true;
            }
        }
    }

    private class OnItemLongClickListener implements RecyclerListView.OnItemLongClickListenerExtended {
        private Tab tab;

        public OnItemLongClickListener(Tab tab) {
            this.tab = tab;
        }

        @Override
        public boolean onItemClick(View view, int position, float x, float y) {
            if (getParentActivity() == null) {
                return false;
            }
            if (!actionBar.isActionModeShowed() && !AndroidUtilities.isTablet() && !onlySelect && view instanceof DialogCell) {
                DialogCell cell = (DialogCell) view;
                if (cell.isPointInsideAvatar(x, y)) {
                    long dialog_id = cell.getDialogId();
                    Bundle args = new Bundle();
                    int lower_part = (int) dialog_id;
                    int high_id = (int) (dialog_id >> 32);
                    int message_id = cell.getMessageId();
                    if (lower_part != 0) {
                        if (high_id == 1) {
                            args.putInt("chat_id", lower_part);
                        } else {
                            if (lower_part > 0) {
                                args.putInt("user_id", lower_part);
                            } else if (lower_part < 0) {
                                if (message_id != 0) {
                                    TLRPC.Chat chat = getMessagesController().getChat(-lower_part);
                                    if (chat != null && chat.migrated_to != null) {
                                        args.putInt("migrated_to", lower_part);
                                        lower_part = -chat.migrated_to.channel_id;
                                    }
                                }
                                args.putInt("chat_id", -lower_part);
                            }
                        }
                    } else {
                        return false;
                    }

                    if (message_id != 0) {
                        args.putInt("message_id", message_id);
                    }
                    if (searchString != null) {
                        if (getMessagesController().checkCanOpenChat(args, DialogsActivity.this)) {
                            getNotificationCenter().postNotificationName(NotificationCenter.closeChats);
                            presentFragmentAsPreview(new ChatActivity(args));
                        }
                    } else {
                        if (getMessagesController().checkCanOpenChat(args, DialogsActivity.this)) {
                            presentFragmentAsPreview(new ChatActivity(args));
                        }
                    }
                    return true;
                }
            }
            RecyclerView.Adapter adapter = tabViews.get(tab).getAdapter();
            if (adapter == dialogsSearchAdapter) {
                Object item = dialogsSearchAdapter.getItem(position);
                    /*if (item instanceof String || dialogsSearchAdapter.isRecentSearchDisplayed()) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                        builder.setTitle(LocaleController.getString("AppName", R.string.AppName));
                        builder.setMessage(LocaleController.getString("ClearSearch", R.string.ClearSearch));
                        builder.setPositiveButton(LocaleController.getString("ClearButton", R.string.ClearButton).toUpperCase(), (dialogInterface, i) -> {
                            if (dialogsSearchAdapter.isRecentSearchDisplayed()) {
                                dialogsSearchAdapter.clearRecentSearch();
                            } else {
                                dialogsSearchAdapter.clearRecentHashtags();
                            }
                        });
                        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
                        showDialog(builder.create());
                        return true;
                    }*/
                return false;
            }

            final TLRPC.Dialog dialog;
            ArrayList<TLRPC.Dialog> dialogs = getDialogsArray(currentAccount, dialogsType, folderId, dialogsListFrozen, tab);
            if (!(tabAdapters.get(tab) instanceof DialogsAdapter)) {
                return false;
            }
            DialogsAdapter dialogsAdapter = (DialogsAdapter) tabAdapters.get(tab);
            position = dialogsAdapter.fixPosition(position);
            if (position < 0 || position >= dialogs.size()) {
                return false;
            }
            dialog = dialogs.get(position);
            if (onlySelect) {
                if ((dialogsType != 3 && dialogsType != 9) || selectAlertString != null) {
                    return false;
                }
                dialogsAdapter.addOrRemoveSelectedDialog(dialog.id, view);
                updateSelectedCount();
            } else {
                if (actionBar.isActionModeShowed() && dialog.pinned) {
                    return false;
                }
                showOrUpdateActionMode(dialog, view);

            }
            return true;
        }

        @Override
        public void onLongClickRelease() {
            finishPreviewFragment();
        }

        @Override
        public void onMove(float dx, float dy) {
            movePreviewFragment(dy);
        }
    }
}
