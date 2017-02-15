package com.elementary.tasks.voice;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.util.Log;

import com.backdoor.simpleai.Action;
import com.backdoor.simpleai.ActionType;
import com.backdoor.simpleai.Model;
import com.elementary.tasks.R;
import com.elementary.tasks.birthdays.AddBirthdayActivity;
import com.elementary.tasks.core.ThemedActivity;
import com.elementary.tasks.core.controller.EventControl;
import com.elementary.tasks.core.controller.EventControlImpl;
import com.elementary.tasks.core.dialogs.VolumeDialog;
import com.elementary.tasks.core.utils.Language;
import com.elementary.tasks.core.utils.LogUtil;
import com.elementary.tasks.core.utils.Module;
import com.elementary.tasks.core.utils.Permissions;
import com.elementary.tasks.core.utils.Prefs;
import com.elementary.tasks.core.utils.Recognize;
import com.elementary.tasks.databinding.ActivityConversationBinding;
import com.elementary.tasks.groups.GroupItem;
import com.elementary.tasks.notes.NoteItem;
import com.elementary.tasks.reminder.AddReminderActivity;
import com.elementary.tasks.reminder.models.Reminder;

import java.util.List;
import java.util.Locale;

/**
 * Copyright 2017 Nazar Suhovich
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

public class ConversationActivity extends ThemedActivity {

    private static final String TAG = "ConversationActivity";
    private static final int AUDIO_CODE = 255000;
    private static final int CHECK_CODE = 1651;

    private SpeechRecognizer speech = null;
    private ActivityConversationBinding binding;

    private ConversationAdapter mAdapter;
    private Recognize recognize;
    private TextToSpeech tts;
    private boolean isTtsReady;
    private AskAction mAskAction;

    private TextToSpeech.OnInitListener mTextToSpeechListener = new TextToSpeech.OnInitListener() {
        @Override
        public void onInit(int status) {
            if (status == TextToSpeech.SUCCESS) {
                int result = tts.setLanguage(Locale.ENGLISH);
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    LogUtil.d(TAG, "This Language is not supported");
                } else {
                    isTtsReady = true;
                    addResponse("Hi, how can I help you?");
                }
            } else {
                LogUtil.d(TAG, "Initialization Failed!");
            }
        }
    };
    private RecognitionListener mRecognitionListener = new RecognitionListener() {
        @Override
        public void onReadyForSpeech(Bundle bundle) {
            LogUtil.d(TAG, "onReadyForSpeech: ");
        }

        @Override
        public void onBeginningOfSpeech() {
            LogUtil.d(TAG, "onBeginningOfSpeech: ");
        }

        @Override
        public void onRmsChanged(float v) {
            v = v * 2000;
            double db = 0;
            if (v > 1) {
                db = 20 * Math.log10(v);
            }
            binding.recordingView.setVolume((float) db);
        }

        @Override
        public void onBufferReceived(byte[] bytes) {
            LogUtil.d(TAG, "onBufferReceived: ");
        }

        @Override
        public void onEndOfSpeech() {
            LogUtil.d(TAG, "onEndOfSpeech: ");
        }

        @Override
        public void onError(int i) {
            LogUtil.d(TAG, "onError: " + i);
            showErrorMessage(i);
        }

        @Override
        public void onResults(Bundle bundle) {
            binding.recordingView.loading();
            if (bundle == null) {
                showSilentMessage();
                return;
            }
            parseResults(bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION));
        }

        @Override
        public void onPartialResults(Bundle bundle) {
            LogUtil.d(TAG, "onPartialResults: ");
        }

        @Override
        public void onEvent(int i, Bundle bundle) {
            LogUtil.d(TAG, "onEvent: ");
        }
    };
    private ConversationAdapter.InsertCallback mInsertCallback = new ConversationAdapter.InsertCallback() {
        @Override
        public void onItemAdded() {
            binding.conversationList.scrollToPosition(0);
        }
    };

    private void showErrorMessage(int i) {
        stopView();
        playTts(getErrorText(i));
    }

    private void showSilentMessage() {
        stopView();
        playTts("Did you say something?");
    }

    private void parseResults(List<String> list) {
        LogUtil.d(TAG, "parseResults: " + list);
        if (list == null || list.isEmpty()) {
            showSilentMessage();
            return;
        }
        Model model = null;
        String suggestion = null;
        for (String s : list) {
            suggestion = s;
            model = recognize.findSuggestion(s);
            if (model != null) break;
        }
        if (model != null) {
            performResult(model, suggestion);
        } else {
            stopView();
            mAdapter.addReply(new Reply(Reply.REPLY, list.get(0)));
            addResponse("Can not recognize your command");
        }
    }

    private void performAnswer(Model answer) {
        stopView();
        if (mAskAction != null) {
            if (answer.getAction() == Action.YES) {
                mAskAction.onYes();
            } else if (answer.getAction() == Action.NO) {
                mAskAction.onNo();
            }
        }
    }

    private void stopView() {
        releaseSpeech();
        binding.recordingView.stop();
    }

    private void addObjectResponse(Reply reply) {
        stopView();
        mAdapter.addReply(reply);
    }

    private void performResult(Model model, String s) {
        mAdapter.addReply(new Reply(Reply.REPLY, s.toLowerCase()));
        LogUtil.d(TAG, "performResult: " + model);
        ActionType actionType = model.getType();
        if (actionType == ActionType.REMINDER) {
            reminderAction(model);
        } else if (actionType == ActionType.NOTE) {
            noteAction(model);
        } else if (actionType == ActionType.ACTION) {
            Action action = model.getAction();
            if (action == Action.BIRTHDAY) {
                stopView();
                startActivity(new Intent(this, AddBirthdayActivity.class));
            } else if (action == Action.REMINDER) {
                stopView();
                startActivity(new Intent(this, AddReminderActivity.class));
            } else if (action == Action.VOLUME) {
                stopView();
                startActivity(new Intent(this, VolumeDialog.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT));
            } else if (action == Action.TRASH) {
                clearTrash();
            } else if (action == Action.DISABLE) {
                disableReminders();
            }
        } else if (actionType == ActionType.GROUP) {
            groupAction(model);
        } else if (actionType == ActionType.ANSWER) {
            performAnswer(model);
        }
    }

    private void groupAction(Model model) {
        stopView();
        addResponse("Group created");
        GroupItem item = recognize.createGroup(model);
        addObjectResponse(new Reply(Reply.GROUP, item));
        new Handler().postDelayed(() -> askGroupAction(item), 1000);
    }

    private void noteAction(Model model) {
        stopView();
        addResponse("Note created");
        NoteItem item = recognize.createNote(model.getSummary());
        addObjectResponse(new Reply(Reply.NOTE, item));
        new Handler().postDelayed(() -> askNoteAction(item), 1000);
    }

    private void reminderAction(Model model) {
        stopView();
        addResponse("Reminder created");
        Reminder reminder = recognize.createReminder(model);
        addObjectResponse(new Reply(Reply.REMINDER, reminder));
        new Handler().postDelayed(() -> askReminderAction(reminder), 1000);
    }

    private void askGroupAction(GroupItem groupItem) {
        addResponse("Would you like to save it?");
        mAskAction = new AskAction() {
            @Override
            public void onYes() {
                recognize.saveGroup(groupItem, false);
                addResponse("Group saved");
                mAskAction = null;
            }

            @Override
            public void onNo() {
                addResponse("Group canceled");
                mAskAction = null;
            }
        };
        new Handler().postDelayed(this::micClick, 1500);
    }

    private void askReminderAction(Reminder reminder) {
        addResponse("Would you like to save it?");
        mAskAction = new AskAction() {
            @Override
            public void onYes() {
                EventControl control = EventControlImpl.getController(ConversationActivity.this, reminder);
                control.start();
                addResponse("Reminder saved");
                mAskAction = null;
            }

            @Override
            public void onNo() {
                addResponse("Reminder canceled");
                mAskAction = null;
            }
        };
        new Handler().postDelayed(this::micClick, 1500);
    }

    private void askNoteAction(NoteItem noteItem) {
        addResponse("Would you like to save it?");
        mAskAction = new AskAction() {
            @Override
            public void onYes() {
                recognize.saveNote(noteItem, false);
                addResponse("Note saved");
                mAskAction = null;
            }

            @Override
            public void onNo() {
                addResponse("Note canceled");
                mAskAction = null;
            }
        };
        new Handler().postDelayed(this::micClick, 1500);
    }

    private void addResponse(String message) {
        mAdapter.addReply(new Reply(Reply.RESPONSE, message));
        playTts(message);
    }

    private void disableReminders() {
        recognize.disableAllReminders(false);
        stopView();
        addResponse("All reminders were disabled");
    }

    private void clearTrash() {
        recognize.emptyTrash(false, () -> {
            stopView();
            addResponse("Trash was cleared");
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_conversation);
        recognize = new Recognize(this);
        initList();
        binding.recordingView.setOnClickListener(view -> micClick());
        checkTts();
    }

    private void playTts(String text) {
        if (!isTtsReady) return;
        if (Module.isLollipop()) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        } else {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
        }
    }

    private void checkTts() {
        Intent checkTTSIntent = new Intent();
        checkTTSIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        try {
            startActivityForResult(checkTTSIntent, CHECK_CODE);
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void initList() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setReverseLayout(true);
        binding.conversationList.setLayoutManager(layoutManager);
        mAdapter = new ConversationAdapter(this);
        mAdapter.setInsertListener(mInsertCallback);
        binding.conversationList.setAdapter(mAdapter);
    }

    private void initRecognizer() {
        Intent recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Language.getLanguage(Prefs.getInstance(this).getVoiceLocale()));
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, this.getPackageName());
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);
        speech = SpeechRecognizer.createSpeechRecognizer(this);
        speech.setRecognitionListener(mRecognitionListener);
        speech.startListening(recognizerIntent);
    }

    private void micClick() {
        if (binding.recordingView.isWorking()) {
            speech.stopListening();
            stopView();
            return;
        }
        if (!Permissions.checkPermission(this, Permissions.RECORD_AUDIO)) {
            Permissions.requestPermission(this, AUDIO_CODE, Permissions.RECORD_AUDIO);
            return;
        }
        binding.recordingView.start();
        initRecognizer();
    }

    private String getErrorText(int errorCode) {
        String message;
        switch (errorCode) {
            case SpeechRecognizer.ERROR_AUDIO:
            case SpeechRecognizer.ERROR_CLIENT:
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                message = "Something went wrong";
                break;
            case SpeechRecognizer.ERROR_SERVER:
            case SpeechRecognizer.ERROR_NETWORK:
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                message = "Network error";
                break;
            case SpeechRecognizer.ERROR_NO_MATCH:
                message = "No match";
                break;
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                message = "No speech input";
                break;
            default:
                message = "Didn't understand, please try again.";
                break;
        }
        return message;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseSpeech();
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
    }

    private void releaseSpeech() {
        if (speech != null) {
            speech.cancel();
            speech.destroy();
            speech = null;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CHECK_CODE) {
            if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
                tts = new TextToSpeech(this, mTextToSpeechListener);
            } else {
                showInstallTtsDialog();
            }
        }
    }

    private void showInstallTtsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.would_you_like_to_install_tts);
        builder.setPositiveButton(R.string.install, (dialogInterface, i) -> {
            dialogInterface.dismiss();
            installTts();
        });
        builder.setNegativeButton(R.string.cancel, (dialogInterface, i) -> dialogInterface.dismiss());
        builder.create().show();
    }

    private void installTts() {
        Intent installTTSIntent = new Intent();
        installTTSIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
        try {
            startActivity(installTTSIntent);
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case AUDIO_CODE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    micClick();
                }
                break;
        }
    }

    private interface AskAction {
        void onYes();
        void onNo();
    }
}
