package com.bjzjns.hxplugin.fragment;

import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.bjzjns.hxplugin.ZJNSHXPlugin;
import com.bjzjns.hxplugin.activity.ContextMenuActivity;
import com.bjzjns.hxplugin.activity.ImageGridActivity;
import com.bjzjns.hxplugin.manager.HXManager;
import com.bjzjns.hxplugin.tools.GsonUtils;
import com.bjzjns.hxplugin.view.chatrow.EaseChatRowProduct;
import com.hyphenate.chat.EMMessage;
import com.hyphenate.chat.EMTextMessageBody;
import com.hyphenate.easeui.EaseConstant;
import com.hyphenate.easeui.model.MessageExtModel;
import com.hyphenate.easeui.ui.EaseChatFragment;
import com.hyphenate.easeui.ui.EaseChatFragment.EaseChatFragmentHelper;
import com.hyphenate.easeui.widget.chatrow.EaseChatRow;
import com.hyphenate.easeui.widget.chatrow.EaseCustomChatRowProvider;
import com.hyphenate.util.PathUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatFragment extends EaseChatFragment implements EaseChatFragmentHelper {

    // constant start from 11 to avoid conflict with constant in base class
    private static final int ITEM_VIDEO = 11;
    private static final int ITEM_FILE = 12;
    private static final int MESSAGE_TYPE_SENT_PRODUCT = 1;
    private static final int MESSAGE_TYPE_RECV_PRODUCT = 2;
    private static final int REQUEST_CODE_SELECT_VIDEO = 11;
    private static final int REQUEST_CODE_SELECT_FILE = 12;
    private static final int REQUEST_CODE_GROUP_DETAIL = 13;
    private static final int REQUEST_CODE_CONTEXT_MENU = 14;
    private static final int REQUEST_CODE_SELECT_AT_USER = 15;

    /**
     * if it is chatBot
     */
    private boolean isRobot;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    protected void setUpView() {
        setChatFragmentListener(this);
        super.setUpView();
        // set click listener
        titleBar.setLeftLayoutClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
//        if(chatType == EaseConstant.CHATTYPE_GROUP){
//            inputMenu.getPrimaryMenu().getEditText().addTextChangedListener(new TextWatcher() {
//
//                @Override
//                public void onTextChanged(CharSequence s, int start, int before, int count) {
//                    if(count == 1 && "@".equals(String.valueOf(s.charAt(start)))){
//                        startActivityForResult(new Intent(getActivity(), PickAtUserActivity.class).
//                                putExtra("groupId", toChatUsername), REQUEST_CODE_SELECT_AT_USER);
//                    }
//                }
//                @Override
//                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
//
//                }
//                @Override
//                public void afterTextChanged(Editable s) {
//
//                }
//            });
//        }
    }

    @Override
    protected void registerExtendMenuItem() {
        //use the menu in base class
        super.registerExtendMenuItem();
        //extend menu items
        inputMenu.registerExtendMenuItem(getResources().getIdentifier("attach_video", "string", getActivity().getPackageName()), getResources().getIdentifier("em_chat_video_selector", "drawable", getActivity().getPackageName()), ITEM_VIDEO, extendMenuItemClickListener);
//        inputMenu.registerExtendMenuItem(R.string.attach_file, R.drawable.em_chat_file_selector, ITEM_FILE, extendMenuItemClickListener);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_CONTEXT_MENU) {
            switch (resultCode) {
                case ContextMenuActivity.RESULT_CODE_COPY: // copy
                    clipboard.setPrimaryClip(ClipData.newPlainText(null,
                            ((EMTextMessageBody) contextMenuMessage.getBody()).getMessage()));
                    break;
                case ContextMenuActivity.RESULT_CODE_DELETE: // delete
                    conversation.removeMessage(contextMenuMessage.getMsgId());
                    messageList.refresh();
                    break;

                case ContextMenuActivity.RESULT_CODE_FORWARD: // forward
//                Intent intent = new Intent(getActivity(), ForwardMessageActivity.class);
//                intent.putExtra("forward_msg_id", contextMenuMessage.getMsgId());
//                startActivity(intent);

                    break;

                default:
                    break;
            }
        }
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case REQUEST_CODE_SELECT_VIDEO: //send the video
                    if (data != null) {
                        int duration = data.getIntExtra("dur", 0);
                        String videoPath = data.getStringExtra("path");
                        File file = new File(PathUtil.getInstance().getImagePath(), "thvideo" + System.currentTimeMillis());
                        try {
                            FileOutputStream fos = new FileOutputStream(file);
                            Bitmap ThumbBitmap = ThumbnailUtils.createVideoThumbnail(videoPath, 3);
                            ThumbBitmap.compress(CompressFormat.JPEG, 100, fos);
                            fos.close();
                            sendVideoMessage(videoPath, file.getAbsolutePath(), duration);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    break;
                case REQUEST_CODE_SELECT_FILE: //send the file
                    if (data != null) {
                        Uri uri = data.getData();
                        if (uri != null) {
                            sendFileByUri(uri);
                        }
                    }
                    break;
                case REQUEST_CODE_SELECT_AT_USER:
                    if (data != null) {
                        String username = data.getStringExtra("username");
                        inputAtUsername(username, false);
                    }
                    break;
                default:
                    break;
            }
        }

    }

    @Override
    public void onSetMessageAttributes(EMMessage message) {
        String extContent = message.getStringAttribute(EaseConstant.MESSAGE_ATTR_EXT, "");
        MessageExtModel model;
        if (TextUtils.isEmpty(extContent)) {
            model = new MessageExtModel();
            model.is_extend_message_content = false;
        } else {
            model = GsonUtils.fromJson(extContent, MessageExtModel.class);
        }
        if (null != extModel) {
            model.user = extModel.user;
            model.touser = extModel.touser;
            model.message_scene = extModel.message_scene;
        }
        message.setAttribute(EaseConstant.MESSAGE_ATTR_EXT, GsonUtils.toJson(model));
    }

    @Override
    public EaseCustomChatRowProvider onSetCustomChatRowProvider() {
        return new CustomChatRowProvider();
    }


    @Override
    public void onEnterToChatDetails() {
//        if (chatType == EaseConstant.CHATTYPE_GROUP) {
//            EMGroup group = EMClient.getInstance().groupManager().getGroup(toChatUsername);
//            if (group == null) {
//                Toast.makeText(getActivity(), R.string.gorup_not_found, Toast.LENGTH_SHORT).show();
//                return;
//            }
//            startActivityForResult(
//                    (new Intent(getActivity(), GroupDetailsActivity.class).putExtra("groupId", toChatUsername)),
//                    REQUEST_CODE_GROUP_DETAIL);
//        }
    }

    @Override
    public void onAvatarClick(EMMessage message) {
        //handling when user click avatar
        String extContent = message.getStringAttribute(EaseConstant.MESSAGE_ATTR_EXT, "");
        MessageExtModel model = GsonUtils.fromJson(extContent, MessageExtModel.class);
        if (null != model && null != model.user) {
            if (!TextUtils.isEmpty(HXManager.getInstance().getUserHXId()) && HXManager.getInstance().getUserHXId().equals(model.user.easemobile_id)) {
                ZJNSHXPlugin.gotoUserDetail(model.user.username);
                getActivity().finish();
            } else {
                switch (model.message_scene) {
                    case MessageExtModel.MESSAGE_SCENE_DESIGNER:
                        ZJNSHXPlugin.gotoDesignerDeatil(model.user.username);
                        getActivity().finish();
                        break;
                    case MessageExtModel.MESSAGE_SCENE_CUSTOMER_SERVICE:
                        break;
                    default:
                        ZJNSHXPlugin.gotoUserDetail(model.user.username);
                        getActivity().finish();
                        break;
                }
            }
        }

    }

    @Override
    public void onAvatarLongClick(EMMessage message) {
        String extContent = message.getStringAttribute(EaseConstant.MESSAGE_ATTR_EXT, "");
        MessageExtModel model = GsonUtils.fromJson(extContent, MessageExtModel.class);
        inputAtUsername(model.user.easemobile_id);
    }


    @Override
    public boolean onMessageBubbleClick(EMMessage message) {
        //消息框点击事件，demo这里不做覆盖，如需覆盖，return true
        String extContent = message.getStringAttribute(EaseConstant.MESSAGE_ATTR_EXT, "");
        MessageExtModel model = GsonUtils.fromJson(extContent, MessageExtModel.class);
        if (null != model && MessageExtModel.MESSAGE_SCENE_DESIGNER == model.message_scene
                && EMMessage.Type.TXT == message.getType()) {
            EMTextMessageBody textMessageBody = (EMTextMessageBody) message.getBody();
            String messageContent = textMessageBody.getMessage();
            Pattern p = Pattern.compile("^((13[0-9])|(14[0-9])|(15[0-9])|(17[0-9])|(18[0-9])|(9{3}))\\d{8}$");
            Matcher m = p.matcher(messageContent);
            if (m.matches()) {
                String productId = "";
                ZJNSHXPlugin.gotoProductDetail(productId);
                getActivity().finish();
                return true;
            }
        }
        return false;
    }

    @Override
    public void onCmdMessageReceived(List<EMMessage> messages) {
        super.onCmdMessageReceived(messages);
    }

    @Override
    public void onMessageBubbleLongClick(EMMessage message) {
        // no message forward when in chat room
        startActivityForResult((new Intent(getActivity(), ContextMenuActivity.class)).putExtra("message", message),
                REQUEST_CODE_CONTEXT_MENU);
    }

    @Override
    public boolean onExtendMenuItemClick(int itemId, View view) {
        switch (itemId) {
            case ITEM_VIDEO:
                Intent intent = new Intent(getActivity(), ImageGridActivity.class);
                startActivityForResult(intent, REQUEST_CODE_SELECT_VIDEO);
                break;
            case ITEM_FILE: //file
                selectFileFromLocal();
                break;
            default:
                break;
        }
        //keep exist extend menu
        return false;
    }

    /**
     * select file
     */
    protected void selectFileFromLocal() {
        Intent intent = null;
        if (Build.VERSION.SDK_INT < 19) { //api 19 and later, we can't use this way, demo just select from images
            intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);

        } else {
            intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        }
        startActivityForResult(intent, REQUEST_CODE_SELECT_FILE);
    }

    /**
     * chat row provider
     */
    private final class CustomChatRowProvider implements EaseCustomChatRowProvider {
        @Override
        public int getCustomChatRowTypeCount() {
            //here the number is the message type in EMMessage::Type
            //which is used to count the number of different chat row
            return 6;
        }

        @Override
        public int getCustomChatRowType(EMMessage message) {
            if (message.getType() == EMMessage.Type.TXT) {
                String messageExt = message.getStringAttribute(EaseConstant.MESSAGE_ATTR_EXT, "");
                MessageExtModel messageExtModel = GsonUtils.fromJson(messageExt, MessageExtModel.class);

                if (null != messageExtModel && messageExtModel.is_extend_message_content) {
                    if (MessageExtModel.EXT_TYPE_SINGLE_PRODUCT.equalsIgnoreCase(messageExtModel.message_type)) {
                        // 商品类型
                        return message.direct() == EMMessage.Direct.RECEIVE ? MESSAGE_TYPE_RECV_PRODUCT : MESSAGE_TYPE_SENT_PRODUCT;
                    }
                }
            }
            return 0;
        }

        @Override
        public EaseChatRow getCustomChatRow(EMMessage message, int position, BaseAdapter adapter) {
            if (message.getType() == EMMessage.Type.TXT) {
                String messageExt = message.getStringAttribute(EaseConstant.MESSAGE_ATTR_EXT, "");
                MessageExtModel messageExtModel = GsonUtils.fromJson(messageExt, MessageExtModel.class);

                if (null != messageExtModel && messageExtModel.is_extend_message_content) {
                    if (MessageExtModel.EXT_TYPE_SINGLE_PRODUCT.equalsIgnoreCase(messageExtModel.message_type)) {
                        // 商品类型
                        return new EaseChatRowProduct(getActivity(), message, position, adapter);
                    }
                }
            }
            return null;
        }

    }

}