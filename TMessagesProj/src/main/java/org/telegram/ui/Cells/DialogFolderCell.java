/*
 * This is the source code of Telegram for Android v. 1.3.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 */

package org.telegram.ui.Cells;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.Emoji;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.ImageReceiver;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.UserObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.AvatarDrawable;
import org.telegram.ui.Components.DialogsFolder;

public class DialogFolderCell extends BaseCell {

    private int currentAccount = UserConfig.selectedAccount;

    private DialogsFolder currentFolder;
    private long currentDialogId;
    private boolean isDialogCell;
    private int lastMessageDate;
    private int unreadCount;
    private int mentionCount;
    private boolean lastUnreadState;
    private int lastSendState;
    private MessageObject message;
    private CharSequence lastMessageString;

    private TLRPC.User user;
    private TLRPC.Chat chat;
    private TLRPC.EncryptedChat encryptedChat;

    private ImageReceiver avatarImage = new ImageReceiver(this);
    private AvatarDrawable avatarDrawable = new AvatarDrawable();

    private CharSequence lastPrintString;

    public boolean useSeparator;
    public boolean fullSeparator;

    private int nameLeft;
    private int nameLockLeft;
    private int nameLockTop;
    private StaticLayout nameLayout;

    private int timeLeft;
    private int timeTop = AndroidUtilities.dp(17);
    private StaticLayout timeLayout;

    private int messageTop = AndroidUtilities.dp(40);
    private int messageLeft;
    private StaticLayout messageLayout;

    private boolean drawError;
    private int errorTop = AndroidUtilities.dp(39);
    private int errorLeft;

    private boolean drawPin;
    private int pinTop = AndroidUtilities.dp(39);
    private int pinLeft;

    private boolean drawCount;
    private int countTop = AndroidUtilities.dp(39);
    private int countLeft;
    private int countWidth;
    private StaticLayout countLayout;

    private boolean drawMention;
    private int mentionLeft;
    private int mentionWidth;

    private int avatarTop = AndroidUtilities.dp(10);

    private RectF rect = new RectF();

    public DialogFolderCell(Context context) {
        super(context);
        Theme.createDialogsResources(context);
        avatarImage.setRoundRadius(AndroidUtilities.dp(26));
    }

    public void setDialog(TLRPC.Dialog dialog, DialogsFolder folder) {
        currentDialogId = dialog.id;
        currentFolder = folder;
        isDialogCell = true;
        update(0);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        avatarImage.onDetachedFromWindow();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        avatarImage.onAttachedToWindow();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), AndroidUtilities.dp(72) + (useSeparator ? 1 : 0));
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (currentDialogId == 0) {
            return;
        }
        if (changed) {
            try {
                buildLayout();
            } catch (Exception e) {
                FileLog.e(e);
            }
        }
    }

    public void buildLayout() {
        String nameString = "";
        if (currentFolder != null) {
            nameString = currentFolder.getName();
            unreadCount = currentFolder.getUnread();
            mentionCount = currentFolder.getMentions();
        }
        String timeString = "";
        String countString = null;
        String mentionString = null;
        CharSequence messageString = "";
        CharSequence printingString = null;
        if (isDialogCell) {
            printingString = MessagesController.getInstance(currentAccount).printingStrings.get(currentDialogId);
        }
        TextPaint currentNamePaint = Theme.dialogs_namePaint;
        TextPaint currentMessagePaint = Theme.dialogs_messagePaint;

        boolean checkMessage = true;
        boolean drawTime = true;

        lastMessageString = message != null ? message.messageText : null;

        if (!LocaleController.isRTL) {
            nameLockLeft = AndroidUtilities.dp(AndroidUtilities.leftBaseline);
            nameLeft = AndroidUtilities.dp(AndroidUtilities.leftBaseline + 4) + Theme.dialogs_dialogGroupDrawable.getIntrinsicWidth();
        } else {
            nameLockLeft = getMeasuredWidth() - AndroidUtilities.dp(AndroidUtilities.leftBaseline) - Theme.dialogs_dialogGroupDrawable.getIntrinsicWidth();
            nameLeft = AndroidUtilities.dp(14);
        }

        if (printingString != null) {
            lastPrintString = messageString = printingString;
            currentMessagePaint = Theme.dialogs_messagePrintingPaint;
        } else {
            lastPrintString = null;
            if (message.messageOwner instanceof TLRPC.TL_messageService) {
                if (message.messageOwner.action instanceof TLRPC.TL_messageActionHistoryClear ||
                        message.messageOwner.action instanceof TLRPC.TL_messageActionChannelMigrateFrom) {
                    messageString = "";
                } else {
                    messageString = message.messageText;
                }
                currentMessagePaint = Theme.dialogs_messagePrintingPaint;
            } else {
                if (message.messageOwner.media instanceof TLRPC.TL_messageMediaPhoto && message.messageOwner.media.photo instanceof TLRPC.TL_photoEmpty && message.messageOwner.media.ttl_seconds != 0) {
                    messageString = LocaleController.getString("AttachPhotoExpired", R.string.AttachPhotoExpired);
                } else if (message.messageOwner.media instanceof TLRPC.TL_messageMediaDocument && message.messageOwner.media.document instanceof TLRPC.TL_documentEmpty && message.messageOwner.media.ttl_seconds != 0) {
                    messageString = LocaleController.getString("AttachVideoExpired", R.string.AttachVideoExpired);
                } else if (message.caption != null) {
                    messageString = message.caption;
                } else {
                    if (message.messageOwner.media instanceof TLRPC.TL_messageMediaGame) {
                        messageString = "\uD83C\uDFAE " + message.messageOwner.media.game.title;
                    } else if (message.type == 14) {
                        messageString = String.format("\uD83C\uDFA7 %s - %s", message.getMusicAuthor(), message.getMusicTitle());
                    } else {
                        messageString = message.messageText;
                    }
                    if (message.messageOwner.media != null && !message.isMediaEmpty()) {
                        currentMessagePaint = Theme.dialogs_messagePrintingPaint;
                    }
                }
            }
        }

        if (user != null) {
            if (user.first_name != null) {
                messageString = user.first_name + ": " + messageString;
            } else if (user.last_name != null) {
                messageString = user.last_name + ": " + messageString;
            }
        } else if (chat != null && chat.title != null) {
            messageString = chat.title + ": " + messageString;
        }

        if (lastMessageDate != 0) {
            timeString = LocaleController.stringForMessageListDate(lastMessageDate);
        } else if (message != null) {
            timeString = LocaleController.stringForMessageListDate(message.messageOwner.date);
        }

        if (message == null) {
            drawCount = false;
            drawMention = false;
            drawError = false;
        } else {
            if (unreadCount != 0 && (unreadCount != 1 || unreadCount != mentionCount || message == null || !message.messageOwner.mentioned)) {
                drawCount = true;
                countString = String.format("%d", unreadCount);
            } else {
                drawCount = false;
            }
            if (mentionCount != 0) {
                drawMention = true;
                mentionString = "@";
            } else {
                drawMention = false;
            }

            if (message.isOut()) {
                if (message.isSending()) {
                    drawError = false;
                } else if (message.isSendError()) {
                    drawError = true;
                    drawCount = false;
                    drawMention = false;
                } else if (message.isSent()) {
                    drawError = false;
                }
            } else {
                drawError = false;
            }
        }

        if (nameString.length() == 0) {
            nameString = LocaleController.getString("HiddenName", R.string.HiddenName);
        }

        int timeWidth;
        if (drawTime) {
            timeWidth = (int) Math.ceil(Theme.dialogs_timePaint.measureText(timeString));
            timeLayout = new StaticLayout(timeString, Theme.dialogs_timePaint, timeWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
            if (!LocaleController.isRTL) {
                timeLeft = getMeasuredWidth() - AndroidUtilities.dp(15) - timeWidth;
            } else {
                timeLeft = AndroidUtilities.dp(15);
            }
        } else {
            timeWidth = 0;
            timeLayout = null;
            timeLeft = 0;
        }

        int nameWidth;

        nameLockTop = AndroidUtilities.dp(18.5f);
        if (!LocaleController.isRTL) {
            nameWidth = getMeasuredWidth() - nameLeft - AndroidUtilities.dp(14) - timeWidth;
        } else {
            nameWidth = getMeasuredWidth() - nameLeft - AndroidUtilities.dp(AndroidUtilities.leftBaseline) - timeWidth;
            nameLeft += timeWidth;
        }
        nameWidth -= AndroidUtilities.dp(4) + Theme.dialogs_dialogGroupDrawable.getIntrinsicWidth();
        nameWidth = Math.max(AndroidUtilities.dp(12), nameWidth);

        try {
            CharSequence nameStringFinal = TextUtils.ellipsize(nameString.replace('\n', ' '), currentNamePaint, nameWidth - AndroidUtilities.dp(12), TextUtils.TruncateAt.END);
            nameLayout = new StaticLayout(nameStringFinal, currentNamePaint, nameWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
        } catch (Exception e) {
            FileLog.e(e);
        }

        int messageWidth = getMeasuredWidth() - AndroidUtilities.dp(AndroidUtilities.leftBaseline + 16);
        int avatarLeft;
        if (!LocaleController.isRTL) {
            messageLeft = AndroidUtilities.dp(AndroidUtilities.leftBaseline);
            avatarLeft = AndroidUtilities.dp(AndroidUtilities.isTablet() ? 13 : 9);
        } else {
            messageLeft = AndroidUtilities.dp(16);
            avatarLeft = getMeasuredWidth() - AndroidUtilities.dp(AndroidUtilities.isTablet() ? 65 : 61);
        }
        avatarImage.setImageCoords(avatarLeft, avatarTop, AndroidUtilities.dp(52), AndroidUtilities.dp(52));
        if (drawError) {
            int w = AndroidUtilities.dp(23 + 8);
            messageWidth -= w;
            if (!LocaleController.isRTL) {
                errorLeft = getMeasuredWidth() - AndroidUtilities.dp(23 + 11);
            } else {
                errorLeft = AndroidUtilities.dp(11);
                messageLeft += w;
            }
        } else if (countString != null || mentionString != null) {
            if (countString != null) {
                countWidth = Math.max(AndroidUtilities.dp(12), (int) Math.ceil(Theme.dialogs_countTextPaint.measureText(countString)));
                countLayout = new StaticLayout(countString, Theme.dialogs_countTextPaint, countWidth, Layout.Alignment.ALIGN_CENTER, 1.0f, 0.0f, false);
                int w = countWidth + AndroidUtilities.dp(18);
                messageWidth -= w;
                if (!LocaleController.isRTL) {
                    countLeft = getMeasuredWidth() - countWidth - AndroidUtilities.dp(19);
                } else {
                    countLeft = AndroidUtilities.dp(19);
                    messageLeft += w;
                }
                drawCount = true;
            } else {
                countWidth = 0;
            }
            if (mentionString != null) {
                mentionWidth = AndroidUtilities.dp(12);
                int w = mentionWidth + AndroidUtilities.dp(18);
                messageWidth -= w;
                if (!LocaleController.isRTL) {
                    mentionLeft = getMeasuredWidth() - mentionWidth - AndroidUtilities.dp(19) - (countWidth != 0 ? countWidth + AndroidUtilities.dp(18) : 0);
                } else {
                    mentionLeft = AndroidUtilities.dp(19) + (countWidth != 0 ? countWidth + AndroidUtilities.dp(18) : 0);
                    messageLeft += w;
                }
                drawMention = true;
            }
        } else {
            if (drawPin) {
                int w = Theme.dialogs_pinnedDrawable.getIntrinsicWidth() + AndroidUtilities.dp(8);
                messageWidth -= w;
                if (!LocaleController.isRTL) {
                    pinLeft = getMeasuredWidth() - Theme.dialogs_pinnedDrawable.getIntrinsicWidth() - AndroidUtilities.dp(14);
                } else {
                    pinLeft = AndroidUtilities.dp(14);
                    messageLeft += w;
                }
            }
            drawCount = false;
            drawMention = false;
        }

        if (checkMessage) {
            if (messageString == null) {
                messageString = "";
            }
            String mess = messageString.toString();
            if (mess.length() > 150) {
                mess = mess.substring(0, 150);
            }
            mess = mess.replace('\n', ' ');
            messageString = Emoji.replaceEmoji(mess, Theme.dialogs_messagePaint.getFontMetricsInt(), AndroidUtilities.dp(17), false);
        }
        messageWidth = Math.max(AndroidUtilities.dp(12), messageWidth);

        CharSequence messageStringFinal = TextUtils.ellipsize(messageString, currentMessagePaint, messageWidth - AndroidUtilities.dp(12), TextUtils.TruncateAt.END);
        try {
            messageLayout = new StaticLayout(messageStringFinal, currentMessagePaint, messageWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
        } catch (Exception e) {
            FileLog.e(e);
        }

        double widthpx;
        float left;
        if (LocaleController.isRTL) {
            if (nameLayout != null && nameLayout.getLineCount() > 0) {
                left = nameLayout.getLineLeft(0);
                widthpx = Math.ceil(nameLayout.getLineWidth(0));
                if (left == 0) {
                    if (widthpx < nameWidth) {
                        nameLeft += (nameWidth - widthpx);
                    }
                }
            }
            if (messageLayout != null && messageLayout.getLineCount() > 0) {
                left = messageLayout.getLineLeft(0);
                if (left == 0) {
                    widthpx = Math.ceil(messageLayout.getLineWidth(0));
                    if (widthpx < messageWidth) {
                        messageLeft += (messageWidth - widthpx);
                    }
                }
            }
        } else {
            if (nameLayout != null && nameLayout.getLineCount() > 0) {
                left = nameLayout.getLineRight(0);
                if (left == nameWidth) {
                    widthpx = Math.ceil(nameLayout.getLineWidth(0));
                    if (widthpx < nameWidth) {
                        nameLeft -= (nameWidth - widthpx);
                    }
                }
            }
            if (messageLayout != null && messageLayout.getLineCount() > 0) {
                left = messageLayout.getLineRight(0);
                if (left == messageWidth) {
                    widthpx = Math.ceil(messageLayout.getLineWidth(0));
                    if (widthpx < messageWidth) {
                        messageLeft -= (messageWidth - widthpx);
                    }
                }
            }
        }
    }

    public void update(int mask) {
        drawPin = currentFolder.isPinned();
        if (isDialogCell) {
            TLRPC.Dialog dialog = MessagesController.getInstance(currentAccount).dialogs_dict.get(currentDialogId);
            if (dialog != null && mask == 0) {
                message = MessagesController.getInstance(currentAccount).dialogMessage.get(dialog.id);
                lastUnreadState = message != null && message.isUnread();
                lastMessageDate = dialog.last_message_date;
                if (message != null) {
                    lastSendState = message.messageOwner.send_state;
                }
            }
        }

        if (mask != 0) {
            boolean continueUpdate = false;
            if (isDialogCell) {
                if ((mask & MessagesController.UPDATE_MASK_USER_PRINT) != 0) {
                    CharSequence printString = MessagesController.getInstance(currentAccount).printingStrings.get(currentDialogId);
                    if (lastPrintString != null && printString == null || lastPrintString == null && printString != null || lastPrintString != null && printString != null && !lastPrintString.equals(printString)) {
                        continueUpdate = true;
                    }
                }
            }
            if (!continueUpdate && (mask & MessagesController.UPDATE_MASK_MESSAGE_TEXT) != 0) {
                if (message != null && message.messageText != lastMessageString) {
                    continueUpdate = true;
                }
            }
            if (!continueUpdate && (mask & MessagesController.UPDATE_MASK_READ_DIALOG_MESSAGE) != 0) {
                if (message != null && lastUnreadState != message.isUnread()) {
                    lastUnreadState = message.isUnread();
                    continueUpdate = true;
                } else if (isDialogCell) {
                    TLRPC.Dialog dialog = MessagesController.getInstance(currentAccount).dialogs_dict.get(currentDialogId);
                    if (dialog != null && (unreadCount != dialog.unread_count || mentionCount != dialog.unread_mentions_count)) {
                        continueUpdate = true;
                    }
                }
            }
            if (!continueUpdate && (mask & MessagesController.UPDATE_MASK_SEND_STATE) != 0) {
                if (message != null && lastSendState != message.messageOwner.send_state) {
                    lastSendState = message.messageOwner.send_state;
                    continueUpdate = true;
                }
            }

            if (!continueUpdate) {
                invalidate();
                return;
            }
        }

        user = null;
        chat = null;
        encryptedChat = null;

        int lower_id = (int) currentDialogId;
        int high_id = (int) (currentDialogId >> 32);
        if (lower_id != 0) {
            if (high_id == 1) {
                chat = MessagesController.getInstance(currentAccount).getChat(lower_id);
            } else {
                if (lower_id < 0) {
                    chat = MessagesController.getInstance(currentAccount).getChat(-lower_id);
                    if (!isDialogCell && chat != null && chat.migrated_to != null) {
                        TLRPC.Chat chat2 = MessagesController.getInstance(currentAccount).getChat(chat.migrated_to.channel_id);
                        if (chat2 != null) {
                            chat = chat2;
                        }
                    }
                } else {
                    user = MessagesController.getInstance(currentAccount).getUser(lower_id);
                }
            }
        } else {
            encryptedChat = MessagesController.getInstance(currentAccount).getEncryptedChat(high_id);
            if (encryptedChat != null) {
                user = MessagesController.getInstance(currentAccount).getUser(encryptedChat.user_id);
            }
        }

        avatarDrawable.setInfo(currentFolder.getBackground(), currentFolder.getName(), null, false);
        avatarImage.setImage(null, "50_50", avatarDrawable, null, 0);

        if (getMeasuredWidth() != 0 || getMeasuredHeight() != 0) {
            buildLayout();
        } else {
            requestLayout();
        }

        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (currentDialogId == 0) {
            return;
        }
        if (drawPin) {
            canvas.drawRect(0, 0, getMeasuredWidth(), getMeasuredHeight(), Theme.dialogs_pinnedPaint);
        }

        setDrawableBounds(Theme.dialogs_dialogGroupDrawable, nameLockLeft, nameLockTop);
        Theme.dialogs_dialogGroupDrawable.draw(canvas);

        if (nameLayout != null) {
            canvas.save();
            canvas.translate(nameLeft, AndroidUtilities.dp(13));
            nameLayout.draw(canvas);
            canvas.restore();
        }

        if (timeLayout != null) {
            canvas.save();
            canvas.translate(timeLeft, timeTop);
            timeLayout.draw(canvas);
            canvas.restore();
        }

        if (messageLayout != null) {
            canvas.save();
            canvas.translate(messageLeft, messageTop);
            try {
                messageLayout.draw(canvas);
            } catch (Exception e) {
                FileLog.e(e);
            }
            canvas.restore();
        }

        if (drawError) {
            rect.set(errorLeft, errorTop, errorLeft + AndroidUtilities.dp(23), errorTop + AndroidUtilities.dp(23));
            canvas.drawRoundRect(rect, 11.5f * AndroidUtilities.density, 11.5f * AndroidUtilities.density, Theme.dialogs_errorPaint);
            setDrawableBounds(Theme.dialogs_errorDrawable, errorLeft + AndroidUtilities.dp(5.5f), errorTop + AndroidUtilities.dp(5));
            Theme.dialogs_errorDrawable.draw(canvas);
        } else if (drawCount || drawMention) {
            if (drawCount) {
                int x = countLeft - AndroidUtilities.dp(5.5f);
                rect.set(x, countTop, x + countWidth + AndroidUtilities.dp(11), countTop + AndroidUtilities.dp(23));
                canvas.drawRoundRect(rect, 11.5f * AndroidUtilities.density, 11.5f * AndroidUtilities.density, Theme.dialogs_countGrayPaint);
                canvas.save();
                canvas.translate(countLeft, countTop + AndroidUtilities.dp(4));
                if (countLayout != null) {
                    countLayout.draw(canvas);
                }
                canvas.restore();
            }
            if (drawMention) {
                int x = mentionLeft - AndroidUtilities.dp(5.5f);
                rect.set(x, countTop, x + mentionWidth + AndroidUtilities.dp(11), countTop + AndroidUtilities.dp(23));
                canvas.drawRoundRect(rect, 11.5f * AndroidUtilities.density, 11.5f * AndroidUtilities.density, Theme.dialogs_countPaint);
                setDrawableBounds(Theme.dialogs_mentionDrawable, mentionLeft - AndroidUtilities.dp(2), countTop + AndroidUtilities.dp(3.2f), AndroidUtilities.dp(16), AndroidUtilities.dp(16));
                Theme.dialogs_mentionDrawable.draw(canvas);
            }
        } else if (drawPin) {
            setDrawableBounds(Theme.dialogs_pinnedDrawable, pinLeft, pinTop);
            Theme.dialogs_pinnedDrawable.draw(canvas);
        }

        if (useSeparator) {
            int left;
            if (fullSeparator) {
                left = 0;
            } else {
                left = AndroidUtilities.dp(AndroidUtilities.leftBaseline);
            }
            if (LocaleController.isRTL) {
                canvas.drawLine(0, getMeasuredHeight() - 1, getMeasuredWidth() - left, getMeasuredHeight() - 1, Theme.dividerPaint);
            } else {
                canvas.drawLine(left, getMeasuredHeight() - 1, getMeasuredWidth(), getMeasuredHeight() - 1, Theme.dividerPaint);
            }
        }

        avatarImage.draw(canvas);
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }
}
