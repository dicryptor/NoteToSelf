package com.mkreator.notetoself;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.BitmapFactory;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    //animation stuff
    Animation mAnimFlash;
    Animation mAnimFadeIn;
    Animation mIdeaAnim;

    private NoteAdapter mNoteAdapter;
    private boolean mSound;
    private int mAnimOption;
    private boolean mNotification;
    private SharedPreferences mPrefs;

    int mIdBeep = -1;
    SoundPool mSp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // adding sound feature to app
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        mSp = new SoundPool.Builder()
                .setMaxStreams(5)
                .setAudioAttributes(audioAttributes)
                .build();

        try {
            AssetManager assetManager = this.getAssets();
            AssetFileDescriptor descriptor;
            // try and load sound file
            descriptor = assetManager.openFd("beep.ogg");
            mIdBeep = mSp.load(descriptor, 0);
        } catch (IOException e) {
            Log.e("error", "failed to load sound file");
        }

        mNoteAdapter = new NoteAdapter();

        ListView listNote = (ListView) findViewById(R.id.listView);

        listNote.setAdapter(mNoteAdapter);

        // enable long clicks
        listNote.setLongClickable(true);

        // to detect long clicks and call delete method
        listNote.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, final int i, long l) {

                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(MainActivity.this);
                dialogBuilder.setTitle("Delete");
                dialogBuilder.setMessage("Confirm selected note deletion?");
                dialogBuilder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int j) {
                        if(mNoteAdapter.getItem(i).isImportant()) {
                            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                            notificationManager.cancelAll();
                        }
                        mNoteAdapter.deleteNote(i);
                    }
                });
                dialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int j) {
                        dialogInterface.cancel();
                    }
                }).show();

                return true;
            }
        });

        // Handle clicks on the ListView
        listNote.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapter, View view, int whichItem, long id) {

                if(mSound) {
                    mSp.play(mIdBeep, 1, 1, 0, 0, 1);
                }
                Log.i("Sound= ", " " + mSound);
				/*
					Create  a temporary Note
					Which is a reference to the Note
					that has just been clicked
				*/
                Note tempNote = mNoteAdapter.getItem(whichItem);

                // Create a new dialog window
                DialogShowNote dialog = new DialogShowNote();

                // Send in a reference to the note to be shown
                dialog.sendNoteSelected(tempNote);

                // Show the dialog window with the note in it
                dialog.show(getFragmentManager(), "");

            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        mNoteAdapter.saveNotes();
    }

    @Override
    protected void onResume() {
        super.onResume();

        mPrefs = getSharedPreferences("Note To Self", MODE_PRIVATE);
        mSound = mPrefs.getBoolean("sound", true);
        mAnimOption = mPrefs.getInt("anim option", SettingsActivity.FAST);
        mNotification = mPrefs.getBoolean("notification", true);
        Log.i("mSound", "" + mSound);
        Log.i("mAnimOption", "" + mAnimOption);

        mAnimFlash = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.flash);
        mAnimFadeIn = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fade_in);
        mIdeaAnim = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.idea_anim);

        // set rate based on user prefs
        if(mAnimOption == SettingsActivity.FAST){
            mAnimFlash.setDuration(300);
            mIdeaAnim.setDuration(500);
        }else if(mAnimOption == SettingsActivity.SLOW){
            mAnimFlash.setDuration(1000);
            mIdeaAnim.setDuration(1000);
        }

        mNoteAdapter.notifyDataSetChanged();
    }

    public void createNewNote(Note n){

        mNoteAdapter.addNote(n);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {

            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }

        if (id == R.id.action_add) {
            DialogNewNote dialog = new DialogNewNote();
            dialog.show(getFragmentManager(), "");
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public class NoteAdapter extends BaseAdapter {

        private JSONSerializer mSerializer;
        List<Note> noteList = new ArrayList<Note>();

        public NoteAdapter() {
            mSerializer = new JSONSerializer("NoteToSelf.json",
                    MainActivity.this.getApplication());

            try {
                noteList = mSerializer.load();
            } catch (Exception e) {
                noteList = new ArrayList<Note>();
                Log.e("Error loading notes: ", "", e);
            }
        }

        public void saveNotes() {
            try {
                mSerializer.save(noteList);
            } catch (Exception e) {
                Log.e("Error saving notes", "", e);
            }
        }

        public void deleteNote(int n){
            noteList.remove(n);
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return noteList.size();
        }

        @Override
        public Note getItem(int whichItem) {
            // Returns the requested note
            return noteList.get(whichItem);
        }

        @Override
        public long getItemId(int whichItem) {
            // Method used internally
            return whichItem;
        }

        @Override
        public View getView(int whichItem, View view, ViewGroup viewGroup) {
			/*
				Prepare a list item to show data
				The list item is contained in the view parameter
				The position of the data in our ArrayList is contained
				in whichItem parameter
			*/
            // Has view been inflated already
            if(view == null){
                // No. So do so here
                LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = inflater.inflate(R.layout.listitem, viewGroup,false);
            }// End if

            // Grab a reference to all TextView and ImageView widgets
            TextView txtTitle = (TextView) view.findViewById(R.id.txtTitle);
            TextView txtDescription = (TextView) view.findViewById(R.id.txtDescription);
            TextView txtDate = (TextView) view.findViewById(R.id.txtType);
            ImageView ivPlaceholder = (ImageView) view.findViewById(R.id.imageViewPlaceholder) ;

            // Hide any ImageView widgets that are not relevant
            Note tempNote = noteList.get(whichItem);

            // To animate or not
            if(tempNote.isImportant() && mAnimOption != SettingsActivity.NONE){
                view.setAnimation(mAnimFlash);
            } else {
                view.setAnimation(mAnimFadeIn);
            }

            if (tempNote.isImportant()){
//                txtDate.setText("Important");
                if(mNotification) sendNotification();
                ivPlaceholder.setImageResource(R.drawable.ic_warning_black_24dp);
                ivPlaceholder.clearAnimation();
            } else if (tempNote.isTodo()){
//                txtDate.setText("Todo");
                ivPlaceholder.setImageResource(R.drawable.ic_check_box_outline_blank_black_24dp);
                ivPlaceholder.clearAnimation();
            } else if (tempNote.isIdea()){
//                txtDate.setText("Idea");
                ivPlaceholder.setImageResource(R.drawable.ic_wb_incandescent_black_24dp);
                if (mAnimOption != SettingsActivity.NONE) {ivPlaceholder.setAnimation(mIdeaAnim);}
            }

            // Add the text to the heading and description
            txtTitle.setText(tempNote.getTitle());
            txtDescription.setText(tempNote.getDescription());
            txtDate.setText(tempNote.getmDateTime());

            return view;
        }

        public void addNote(Note n){

            noteList.add(n);
            notifyDataSetChanged();

        }

    }

    public void sendNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setAutoCancel(true);
        builder.setSmallIcon(R.drawable.ic_stat_name);
        builder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));
        builder.setContentTitle("Note To Self");
        builder.setContentText("You have important tasks.");
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        builder.setContentIntent(pendingIntent);

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(1, builder.build());
    }

}