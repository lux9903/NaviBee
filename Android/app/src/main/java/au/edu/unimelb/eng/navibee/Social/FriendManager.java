package au.edu.unimelb.eng.navibee.social;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableResult;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import au.edu.unimelb.eng.navibee.NaviBeeApplication;

public class FriendManager {

    public final static String BROADCAST_FRIEND_UPDATED = "broadcast.friend.updated";

    public static class ContactPerson {
        private String url;
        private String name;
        private String uid;
        private int unreadMessage;
        private String lastMessageTime;
        private String lastMessage;
        private Boolean hasLastMessage;


        public ContactPerson(String name, String url, String uid) {
            this.url = url;
            this.name = name;
            this.uid = uid;
            this.unreadMessage = 0;
            Date date = new Date(2018, 12 ,12, 12, 12, 12);
            setLastMessageTime(date);
            hasLastMessage = false;
        }

        public String getUrl() {
            return this.url;
        }
        public String getUid(){
            return this.uid;
        }

        public String getName() {
            return this.name;
        }
        public int getUnreadMessage(){
            return this.unreadMessage;
        }
        public void setLastMessage(String message){
            if (message.length() > 13){
                String reduced = message.substring(0, 13);
                reduced = reduced + "...";
                lastMessage = reduced;
            }
            else{
                lastMessage = message;
            }
            hasLastMessage = true;
        }
        public String getLastMessage(){
            return this.lastMessage;
        }
        public void noMessage(){
            hasLastMessage = false;
        }

        public Boolean hasMessage() {
            return hasLastMessage;
        }

        public String getLastMessageTime (){
            return this.lastMessageTime;
        }

        public void setUnreadMessage(int i){
            this.unreadMessage = i;
        }

        public void setLastMessageTime(Date time){
            DateFormat df = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
            this.lastMessageTime = df.format(time);
        }
        public void setUrl(String url){
            this.url = url;
        }
        public void setName(String name){
            this.name = name;
        }
    }


    private static FriendManager instance = null;

    public static FriendManager getInstance() {
        return instance;
    }

    public static void init() {
        instance = new FriendManager();
    }

    private static final String TAG = "friend";

    private String uid;
    private FirebaseFirestore db;

    private Map<String, Object> friendInfo = new HashMap<>();


    public FriendManager() {
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        db = FirebaseFirestore.getInstance();

        db.collection("users").whereEqualTo("contacts."+uid, true)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot snapshots,
                                        @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.w(TAG, "listen:error", e);
                            return;
                        }

                        for (DocumentChange dc : snapshots.getDocumentChanges()) {
                            switch (dc.getType()) {
                                case ADDED:
                                    friendInfo.put(dc.getDocument().getId(), dc.getDocument().getData());
                                    break;
                                case MODIFIED:
                                    friendInfo.put(dc.getDocument().getId(), dc.getDocument().getData());
                                    break;
                                case REMOVED:
                                    friendInfo.remove(dc.getDocument().getId());
                                    break;
                            }
                        }

                        Intent intent = new Intent(BROADCAST_FRIEND_UPDATED);
                        NaviBeeApplication.getInstance().sendBroadcast(intent);
                    }
                });
    }

    public void fetchContactPersonList(ArrayList<ContactPerson> list) {
        list.clear();
        for (Map.Entry<String, Object> entry: friendInfo.entrySet()) {
            Map <String, Object> value = (Map <String, Object>) entry.getValue();
            ContactPerson cp = new ContactPerson((String)value.get("name"), (String)value.get("photoURL"), entry.getKey());
            list.add(cp);
        }
    }

    public void addFriend(String targetUid) {
        FirebaseFunctions mFunctions = FirebaseFunctions.getInstance();
        Map<String, Object> data = new HashMap<>();
        data.put("targetUid", targetUid);

        mFunctions
            .getHttpsCallable("addFriend")
            .call(data)
            .continueWith(new Continuation<HttpsCallableResult, String>() {
                @Override
                public String then(@NonNull Task<HttpsCallableResult> task) throws Exception {
                    // This continuation runs on either success or failure, but if the task
                    // has failed then getResult() will throw an Exception which will be
                    // propagated down.
//                    String result = (String) task.getResult().getData();
//                    return result;
                    return "";
                }
            });



    }


}

