package br.ufrj.caronae.acts;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.squareup.otto.Subscribe;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import br.ufrj.caronae.App;
import br.ufrj.caronae.R;
import br.ufrj.caronae.SharedPref;
import br.ufrj.caronae.Util;
import br.ufrj.caronae.adapters.ChatMsgsAdapter;
import br.ufrj.caronae.comparators.ChatMsgComparator;
import br.ufrj.caronae.models.ChatAssets;
import br.ufrj.caronae.models.ChatMessageReceived;
import br.ufrj.caronae.models.ChatMessageReceivedFromJson;
import br.ufrj.caronae.models.ChatMessageSendResponse;
import br.ufrj.caronae.models.ModelReceivedFromChat;
import br.ufrj.caronae.models.NewChatMsgIndicator;
import br.ufrj.caronae.models.RideEndedEvent;
import br.ufrj.caronae.models.modelsforjson.ChatSendMessageForJson;
import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class ChatAct extends AppCompatActivity {

    @Bind(R.id.chatMsgs_rv)
    RecyclerView chatMsgs_rv;
    @Bind(R.id.send_bt)
    Button send_bt;
    @Bind(R.id.msg_et)
    EditText msg_et;
    @Bind(R.id.neighborhood_tv)
    TextView neighborhood_tv;
    @Bind(R.id.date_tv)
    TextView date_tv;
    @Bind(R.id.time_tv)
    TextView time_tv;
    @Bind(R.id.lay1)
    RelativeLayout lay1;
    @Bind(R.id.swipe_refresh_chat)
    SwipeRefreshLayout swipeRefreshLayout;

    private String rideId;
    private static List<ChatMessageReceived> chatMsgsList;
    static int color;
    Context context;
    static ChatMsgsAdapter chatMsgsAdapter;

    private final String MESSAGE_WITH_USER_BUNDLE_KEY         = "message";
    private final String SENDER_ID_BUNDLE_KEY                 = "senderId";
    private final String SENT_TIME_BUNDLE_KEY                 = "google.sent_time";
    private final String RIDE_ID_BUNDLE_KEY                 = "rideId";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        ButterKnife.bind(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
        }

        rideId = getIntent().getExtras().getString("rideId");
        List<ChatAssets> l = ChatAssets.find(ChatAssets.class, "ride_id = ?", rideId);
        if (l == null || l.isEmpty()) {
            finish();
            return;
        }

        ChatAssets chatAssets = l.get(0);

        context = this;
        color = chatAssets.getColor();
        lay1.setBackgroundColor(color);
        int bgRes = chatAssets.getBgRes();
        send_bt.setBackgroundResource(bgRes);
        String neighborhood = chatAssets.getLocation();
        neighborhood_tv.setText(neighborhood);
        String date = chatAssets.getDate();
        date_tv.setText(date);
        String time = chatAssets.getTime();
        time_tv.setText(time);

        chatMsgsList = ChatMessageReceived.find(ChatMessageReceived.class, "ride_id = ?", rideId);
        Collections.sort(chatMsgsList, new ChatMsgComparator());

        chatMsgsAdapter = new ChatMsgsAdapter(chatMsgsList, color);
        chatMsgs_rv.setAdapter(chatMsgsAdapter);
        chatMsgs_rv.setLayoutManager(new LinearLayoutManager(context));

        //TODO: Check multiples notifications
//        if (getIntent().getExtras().get(SENDER_ID_BUNDLE_KEY) != null){
//            ChatMessageReceived cmr = new ChatMessageReceived(
//                    getSenderNameFromNotification(getIntent().getExtras().getString(MESSAGE_WITH_USER_BUNDLE_KEY)),
//                    getIntent().getExtras().getString(SENDER_ID_BUNDLE_KEY),
//                    getMessageFromNotification(getIntent().getExtras().getString(MESSAGE_WITH_USER_BUNDLE_KEY)),
//                    getIntent().getExtras().getString(RIDE_ID_BUNDLE_KEY),
//                    getTimeFromNotification(getIntent().getExtras().getLong(SENT_TIME_BUNDLE_KEY)));
//
//            chatMsgsList.add(cmr);
//        }

        if (!chatMsgsList.isEmpty())
            chatMsgs_rv.scrollToPosition(chatMsgsList.size() - 1);

        swipeRefreshLayout.post(new Runnable() {
            @Override
            public void run() {
                swipeRefreshLayout.setRefreshing(true);
            }
        });

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                updateMsgsListWithServer(rideId);
            }
        });


//        String since = null;
//        if(chatMsgsList.size() != 0){
//            boolean lastMessageisMine = true;
//            int counter = chatMsgsList.size() - 1;
//            while (lastMessageisMine && counter >= 0) {
//                if(!chatMsgsList.get(counter).getSenderId().equals(String.valueOf(App.getUser().getDbId()))) {
//                    since = chatMsgsList.get(counter).getTime();
//                    lastMessageisMine = false;
//                }
//                counter--;
//            }
//        }
//
//        /************************************************************/
//        App.getChatService().requestChatMsgs(rideId, since, new Callback<ModelReceivedFromChat>() {
//            @Override
//            public void success(ModelReceivedFromChat chatMessagesReceived, Response response) {
//
//                if (chatMessagesReceived != null && chatMessagesReceived.getMessages().size() != 0){
//
//                    List<ChatMessageReceivedFromJson> listMessages = chatMessagesReceived.getMessages();
//                    for (int mensagesNum = 0; mensagesNum < listMessages.size(); mensagesNum++) {
//                        ChatMessageReceived cmr = new ChatMessageReceived(listMessages.get(mensagesNum).getUser().getName(),
//                                String.valueOf(listMessages.get(mensagesNum).getUser().getId()),
//                                listMessages.get(mensagesNum).getMessage(),
//                                rideId,
//                                listMessages.get(mensagesNum).getTime());
////                        chatMsgsList.add(cmr);
//
//                        cmr.setId(Long.parseLong(listMessages.get(mensagesNum).getMessageId()));
//
//                        cmr.save();
//
//                        updateMsgsList(cmr);
//                    }
////                    new NewChatMsgIndicator(Integer.valueOf(listMessages.get(0).getMessageId())).save();
////                    chatMsgsAdapter = new ChatMsgsAdapter(chatMsgsList, color);
////                    chatMsgs_rv.setAdapter(chatMsgsAdapter);
////                    chatMsgs_rv.setLayoutManager(new LinearLayoutManager(context));
////
////                    if (!chatMsgsList.isEmpty())
////                        chatMsgs_rv.scrollToPosition(chatMsgsList.size() - 1);
//
//                }
//
//                swipeRefreshLayout.post(new Runnable() {
//                    @Override
//                    public void run() {
//                        swipeRefreshLayout.setRefreshing(false);
//                    }
//                });
//            }
//
//            @Override
//            public void failure(RetrofitError error) {
//                Util.toast("Erro ao Recuperar mensagem de chat");
//                try {
//                    Log.e("GetMessages", error.getMessage());
//                } catch (Exception e) {
//                    Log.e("GetMessages", e.getMessage());
//                }
//                swipeRefreshLayout.post(new Runnable() {
//                    @Override
//                    public void run() {
//                        swipeRefreshLayout.setRefreshing(false);
//                    }
//                });            }
//        });
//         /************************************************************/
//
////        chatMsgs_rv.setAdapter(new ChatMsgsAdapter(chatMsgsList, color));
////        chatMsgs_rv.setLayoutManager(new LinearLayoutManager(this));
////
////        if (!chatMsgsList.isEmpty())
////            chatMsgs_rv.scrollToPosition(chatMsgsList.size() - 1);
////
        updateMsgsListWithServer(rideId);

        App.getBus().register(this);
    }

    @OnClick(R.id.send_bt)
    public void sendBt() {
        final String message = msg_et.getText().toString();
        if (message.isEmpty())
            return;

        String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date());
        final ChatMessageReceived msg = new ChatMessageReceived(App.getUser().getName(), App.getUser().getDbId() + "", message, rideId, time);

        App.getChatService().sendChatMsg(rideId, new ChatSendMessageForJson(message), new Callback<ChatMessageSendResponse>() {
            @Override
            public void success(ChatMessageSendResponse chatMessageSendResponse, Response response) {
                Log.i("Message Sent", "Sulcefully Send Chat Messages");
                msg_et.setText("");
                msg.save();
                updateMsgsList(msg);
                Util.toast("Mensagem enviada");
            }

            @Override
            public void failure(RetrofitError error) {
                Util.toast("Erro ao enviar mensagem de chat, verifique sua conexão");
                View view = ChatAct.this.getCurrentFocus();
                if (view != null) {
                    InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                }
                try {
                    Log.e("SendMessages", error.getMessage());
                } catch (Exception e) {
                    Log.e("SendMessages", e.getMessage());
                }
            }
        });
    }


    @Subscribe
    public void updateMsgsList(ChatMessageReceived msg) {

        Log.i("GetMessages", "Updatando");

        chatMsgsList.add(msg);

//        msg.save();

        ChatMsgsAdapter adapter = (ChatMsgsAdapter) chatMsgs_rv.getAdapter();
        adapter.notifyItemInserted(chatMsgsList.size() - 1);

        chatMsgs_rv.scrollToPosition(chatMsgsList.size() - 1);

        swipeRefreshLayout.post(new Runnable() {
            @Override
            public void run() {
                swipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    public void updateMsgsList() {

        chatMsgsAdapter = new ChatMsgsAdapter(chatMsgsList, color);
        chatMsgs_rv.setAdapter(chatMsgsAdapter);
        chatMsgs_rv.setLayoutManager(new LinearLayoutManager(context));
        if (!chatMsgsList.isEmpty())
            chatMsgs_rv.scrollToPosition(chatMsgsList.size() - 1);

        swipeRefreshLayout.setRefreshing(false);

    }

    @Subscribe
    public void updateMsgsListWithServer(final String rideId) {

//        chatMsgsList = ChatMessageReceived.find(ChatMessageReceived.class, "ride_id = ?", rideId);
//        Collections.sort(chatMsgsList, new ChatMsgComparator());
//
//        final int listMsgSizeBefore = chatMsgsList.size();
//        Log.i("GetMessages", "Msg List Size Before: " + listMsgSizeBefore);
//
//        String since;
//        if(chatMsgsList.size() != 0){
//            since = chatMsgsList.get(chatMsgsList.size() - 1).getTime();
//        } else {
//            since = null;
//        }
//
//        Log.i("GetMessages", "message since: " + chatMsgsList.get(chatMsgsList.size() - 1).getMessage());
//        Log.i("GetMessages", "since: " + since);
//
//        /************************************************************/
//        App.getChatService().requestChatMsgs(rideId, since, new Callback<ModelReceivedFromChat>() {
//            @Override
//            public void success(ModelReceivedFromChat chatMessagesReceived, Response response) {
//                Log.i("GetMessages", "Entered in msglastwithserver");
//
//                Log.i("GetMessages", "Sulcefully Retrieved Chat Messages on Chat Act");
//                if (chatMessagesReceived != null) {
//                    Log.i("GetMessages", "messages are not null");
//
//                    List<ChatMessageReceivedFromJson> listMessages = chatMessagesReceived.getMessages();
//                    for (int mensagesNum = 0; mensagesNum < listMessages.size(); mensagesNum++) {
//                        ChatMessageReceived cmr = new ChatMessageReceived(listMessages.get(mensagesNum).getUser().getName(),
//                                String.valueOf(listMessages.get(mensagesNum).getUser().getId()),
//                                listMessages.get(mensagesNum).getMessage(),
//                                String.valueOf(listMessages.get(mensagesNum).getMessageId()),
//                                listMessages.get(mensagesNum).getTime());
//
//                        Log.i("GetMessages", cmr.getMessage());
//                        Log.i("GetMessages", cmr.getTime());
//
//
////                        chatMsgsList.add(cmr);
////                        cmr.save();
////                        App.getBus().post(cmr);
//                        updateMsgsList(chatMsgsList.get(chatMsgsList.size() - 1));
//                    }
//
//                    int listMsgSizeAfter = chatMsgsList.size();
//                    Log.i("GetMessages", "Msg List Size after: " + listMsgSizeAfter);
//
//                    for (int counter = listMsgSizeBefore; counter < listMsgSizeAfter; counter++){
//                        ChatMessageReceived cmr = chatMsgsList.get(counter);
//                        cmr.save();
//                    }
//                }
//            }
//
//            @Override
//            public void failure(RetrofitError error) {
//                Util.toast("Erro ao Recuperar mensagem de chat");
//                try {
//                    Log.e("GetMessages", error.getMessage());
//                } catch (Exception e) {
//                    Log.e("GetMessages", e.getMessage());
//                }
//            }
//        });
//        /************************************************************/

        String since = null;
        if(chatMsgsList.size() != 0){
            boolean lastMessageisMine = true;
            int counter = chatMsgsList.size() - 1;
            while (lastMessageisMine && counter >= 0) {
                if(!chatMsgsList.get(counter).getSenderId().equals(String.valueOf(App.getUser().getDbId()))) {
                    since = chatMsgsList.get(counter).getTime();
                    lastMessageisMine = false;
                }
                counter--;
            }
        }

        /************************************************************/
        App.getChatService().requestChatMsgs(rideId, since, new Callback<ModelReceivedFromChat>() {
            @Override
            public void success(ModelReceivedFromChat chatMessagesReceived, Response response) {

                if (chatMessagesReceived != null && chatMessagesReceived.getMessages().size() != 0){

                    List<ChatMessageReceivedFromJson> listMessages = chatMessagesReceived.getMessages();
                    for (int mensagesNum = 0; mensagesNum < listMessages.size(); mensagesNum++) {
                        ChatMessageReceived cmr = new ChatMessageReceived(listMessages.get(mensagesNum).getUser().getName(),
                                String.valueOf(listMessages.get(mensagesNum).getUser().getId()),
                                listMessages.get(mensagesNum).getMessage(),
                                rideId,
                                listMessages.get(mensagesNum).getTime());
                        cmr.setId(Long.parseLong(listMessages.get(mensagesNum).getMessageId()));
//                        chatMsgsList.add(cmr);

                        if (!messageAlrealdyExist(Long.parseLong(listMessages.get(mensagesNum).getMessageId()))) {
                            cmr.save();
                            updateMsgsList(cmr);
                        }
                    }
//                    new NewChatMsgIndicator(Integer.valueOf(rideId)).save();
//                    chatMsgsAdapter = new ChatMsgsAdapter(chatMsgsList, color);
//                    chatMsgs_rv.setAdapter(chatMsgsAdapter);
//                    chatMsgs_rv.setLayoutManager(new LinearLayoutManager(context));

//                    if (!chatMsgsList.isEmpty())
//                        chatMsgs_rv.scrollToPosition(chatMsgsList.size() - 1);

                }

                swipeRefreshLayout.post(new Runnable() {
                    @Override
                    public void run() {
                        swipeRefreshLayout.setRefreshing(false);
                    }
                });
            }

            @Override
            public void failure(RetrofitError error) {
                Util.toast("Erro ao Recuperar mensagem de chat");
                try {
                    Log.e("GetMessages", error.getMessage());
                } catch (Exception e) {
                    Log.e("GetMessages", e.getMessage());
                }
                swipeRefreshLayout.post(new Runnable() {
                    @Override
                    public void run() {
                        swipeRefreshLayout.setRefreshing(false);
                    }
                });            }
        });

        /************************************************************/

        swipeRefreshLayout.post(new Runnable() {
            @Override
            public void run() {
                swipeRefreshLayout.setRefreshing(false);
            }
        });

    }

    private boolean messageAlrealdyExist(long messageId) {
        boolean messageAlreadyExist = false;
        int counter = chatMsgsList.size() - 1;
        while (!messageAlreadyExist && counter >= 0){
            if (chatMsgsList.get(counter).getId() == messageId)
                messageAlreadyExist = true;
            counter--;
        }
        return messageAlreadyExist;
    }


    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        NewChatMsgIndicator.deleteAll(NewChatMsgIndicator.class, "db_id = ?", rideId);
    }

    @Subscribe
    public void rideEndedEvent(RideEndedEvent rideEndedEvent) {
        Log.i("rideEndedEvent", "chatact" + rideEndedEvent.getRideId());

        if (rideId.equals(rideEndedEvent.getRideId())) {
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        App.getBus().unregister(this);
        NewChatMsgIndicator.deleteAll(NewChatMsgIndicator.class, "db_id = ?", rideId);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            finish();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPref.setChatActIsForeground(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        SharedPref.setChatActIsForeground(false);
    }

    private String getSenderNameFromNotification(String message) {
        return message.split(":")[0];
    }

    private String getMessageFromNotification(String message) {
        return message.split(":")[1];
    }

    private String getTimeFromNotification(Long timeInMillis) {
        return Util.formatTime(String.valueOf(timeInMillis));
    }
}
