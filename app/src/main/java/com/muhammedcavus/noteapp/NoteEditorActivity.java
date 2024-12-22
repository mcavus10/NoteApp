package com.muhammedcavus.noteapp;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class NoteEditorActivity extends AppCompatActivity {

    private EditText titleEditText;
    private EditText contentEditText;
    private DBHelper dbHelper;
    private long noteId = -1; // Yeni not için -1

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        dbHelper = new DBHelper(this);

        // Programatik layout
        LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        setContentView(rootLayout);

        // Üst kısımda geri butonu (<-). Tik butonu (✓) klavye açık olduğunda da görünsün diye menüye ekleyeceğiz
        // Basit bir yatay layout
        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);

        Button backButton = new Button(this);
        backButton.setText("<-");
        backButton.setOnClickListener(v -> {
            // Geri dönüldüğünde otomatik kaydetme
            saveNote();
            finish();
        });
        topBar.addView(backButton);

        // Boşluk eklemek için
        Space space = new Space(this);
        LinearLayout.LayoutParams spaceParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1
        );
        space.setLayoutParams(spaceParams);
        topBar.addView(space);

        // Tik butonu
        Button saveButton = new Button(this);
        saveButton.setText("✓");
        saveButton.setOnClickListener(v -> {
            saveNote();
            finish();
        });
        topBar.addView(saveButton);

        rootLayout.addView(topBar);

        // Başlık edit alanı
        titleEditText = new EditText(this);
        titleEditText.setHint("Başlık giriniz");
        titleEditText.setTextSize(20f);
        rootLayout.addView(titleEditText);

        // İçerik edit alanı
        contentEditText = new EditText(this);
        contentEditText.setHint("Yazmaya başla...");
        contentEditText.setMinLines(5);
        rootLayout.addView(contentEditText);

        // Not ID varsa yükle
        noteId = getIntent().getLongExtra("NOTE_ID", -1);
        if (noteId != -1) {
            loadNoteData(noteId);
        }
    }

    // Üç nokta menüsü: Paylaş, Sil
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 1, 0, "Paylaş");
        menu.add(0, 2, 1, "Sil");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch(item.getItemId()) {
            case 1:
                shareNote();
                return true;
            case 2:
                deleteCurrentNote();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void shareNote() {
        String title = titleEditText.getText().toString();
        String content = contentEditText.getText().toString();

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, title);
        shareIntent.putExtra(Intent.EXTRA_TEXT, content);
        startActivity(Intent.createChooser(shareIntent, "Notu Paylaş"));
    }

    private void deleteCurrentNote() {
        if (noteId == -1) {
            Toast.makeText(this, "Kaydedilmemiş not silindi.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Notu Sil");
        builder.setMessage("Bu notu silmek istediğinize emin misiniz?");
        builder.setPositiveButton("Evet", (dialog, which) -> {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            db.delete(DBHelper.TABLE_NOTES, DBHelper.COLUMN_ID + "=?",
                    new String[]{String.valueOf(noteId)});
            db.close();
            Toast.makeText(NoteEditorActivity.this, "Not silindi.", Toast.LENGTH_SHORT).show();
            finish();
        });
        builder.setNegativeButton("Hayır", null);
        builder.show();
    }

    // Notu yüklemek (varsa)
    private void loadNoteData(long id) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DBHelper.TABLE_NOTES, null,
                DBHelper.COLUMN_ID + "=?",
                new String[]{String.valueOf(id)},
                null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            String title = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COLUMN_TITLE));
            String content = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COLUMN_CONTENT));
            titleEditText.setText(title);
            contentEditText.setText(content);
            cursor.close();
        }
        db.close();
    }

    // Notu kaydetmek (varsa güncelle, yoksa ekle)
    private void saveNote() {
        String title = titleEditText.getText().toString().trim();
        String content = contentEditText.getText().toString().trim();
        String currentDate = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date());

        // Boşsa kaydetmeyebilirsiniz veya varsayılan bir metin kullanabilirsiniz
        if (title.isEmpty() && content.isEmpty()) {
            return; // Boş not kaydetmiyoruz veya kendinize göre işlem yapın
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(DBHelper.COLUMN_TITLE, title);
        values.put(DBHelper.COLUMN_CONTENT, content);
        values.put(DBHelper.COLUMN_DATE, currentDate);

        if (noteId == -1) {
            // Yeni not
            long insertId = db.insert(DBHelper.TABLE_NOTES, null, values);
            if (insertId != -1) {
                noteId = insertId;
            }
        } else {
            // Güncelle
            db.update(DBHelper.TABLE_NOTES, values, DBHelper.COLUMN_ID + "=?",
                    new String[]{String.valueOf(noteId)});
        }
        db.close();
    }
}