package com.muhammedcavus.noteapp;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private LinearLayout rootLayout; // Programatik container
    private DBHelper dbHelper;

    // Görünüm tipi: true=List, false=Grid
    private boolean isListView = true;

    // Seçim modu için bir adaptör veya liste tutabiliriz
    private List<NoteModel> notesList = new ArrayList<>();
    private List<Long> selectedNotes = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Ana layout
        rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        setContentView(rootLayout);

        dbHelper = new DBHelper(this);

        // Üst menü görünümü kendi toolbar'ınızla veya ActionBar ile de yapabilirsiniz
        // Basit bir başlık ekleyelim
        TextView titleText = new TextView(this);
        titleText.setText("Not Defteri");
        titleText.setTextSize(24f);
        titleText.setGravity(Gravity.CENTER);
        titleText.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        rootLayout.addView(titleText);

        // "Hepsini Seç" butonu
        Button selectAllButton = new Button(this);
        selectAllButton.setText("Hepsini Seç");
        selectAllButton.setOnClickListener(v -> {
            selectedNotes.clear();
            for (NoteModel nm : notesList) {
                selectedNotes.add(nm.getId());
            }
            // Listeyi vs. güncelleyebilirsiniz
            Toast.makeText(MainActivity.this, "Tüm notlar seçildi", Toast.LENGTH_SHORT).show();
        });
        rootLayout.addView(selectAllButton);

        // Yeni not oluştur butonu
        Button newNoteButton = new Button(this);
        newNoteButton.setText("Yeni Not Oluştur");
        newNoteButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, NoteEditorActivity.class);
            startActivity(intent);
        });
        rootLayout.addView(newNoteButton);

        // Notları listele
        displayNotes();
    }

    // Notları veritabanından çekip görüntüleme
    private void displayNotes() {
        // Önce var olan görsel listeyi silelim (rootLayout'ta alt kısımlarda dinamik eklemiş olabiliriz)
        // başlık, butonlar gibi ilk 3 çocuğu korumak için removeViews() index kullanabiliriz
        if(rootLayout.getChildCount() > 3) {
            rootLayout.removeViews(3, rootLayout.getChildCount()-3);
        }

        loadNotesFromDB();

        if (notesList.isEmpty()) {
            TextView emptyView = new TextView(this);
            emptyView.setText("Henüz not yok...");
            emptyView.setGravity(Gravity.CENTER);
            emptyView.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ));
            rootLayout.addView(emptyView);
            return;
        }

        if (isListView) {
            // Liste görünümü
            for (NoteModel note : notesList) {
                rootLayout.addView(createNoteItemView(note));
            }
        } else {
            // Izgara görünümü
            // Basit bir TableLayout veya GridLayout programatik oluşturulabilir
            TableLayout tableLayout = new TableLayout(this);
            TableRow tableRow = null;
            int columns = 2;
            int index = 0;

            for (NoteModel note : notesList) {
                if (index % columns == 0) {
                    tableRow = new TableRow(this);
                    tableLayout.addView(tableRow);
                }
                if (tableRow != null) {
                    tableRow.addView(createNoteItemView(note));
                }
                index++;
            }

            rootLayout.addView(tableLayout);
        }
    }

    private View createNoteItemView(NoteModel note) {
        // Tek notu gösteren basit bir layout
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(16, 16, 16, 16);
        layout.setBackgroundColor(0xFFEFEFEF);

        // Basılı tutma ile sil butonu aktifleşmesi
        layout.setOnLongClickListener(v -> {
            showDeleteButton(note.getId());
            return true;
        });

        TextView titleView = new TextView(this);
        titleView.setText("Başlık: " + note.getTitle());
        titleView.setTextSize(18f);

        TextView contentView = new TextView(this);
        contentView.setText("Not: " + note.getContent());
        contentView.setTextSize(16f);

        TextView dateView = new TextView(this);
        dateView.setText("Tarih: " + note.getDate());
        dateView.setTextSize(14f);

        layout.addView(titleView);
        layout.addView(contentView);
        layout.addView(dateView);

        // Tıklama ile notu düzenleme
        layout.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, NoteEditorActivity.class);
            intent.putExtra("NOTE_ID", note.getId());
            startActivity(intent);
        });

        return layout;
    }

    // Alt kısımda sil butonu göstermek için basit bir yöntem
    private void showDeleteButton(long noteId) {
        // Alt kısımda buton göstermek isterseniz bir popup ya da snackbar kullanabilirsiniz
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Notu silmek istiyor musunuz?");
        builder.setPositiveButton("Sil", (dialog, which) -> {
            deleteNote(noteId);
            displayNotes(); // Listeyi yenile
        });
        builder.setNegativeButton("İptal", null);
        builder.show();
    }

    // Not silme
    private void deleteNote(long noteId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(DBHelper.TABLE_NOTES, DBHelper.COLUMN_ID + "=?", new String[]{String.valueOf(noteId)});
        db.close();
    }

    // DB'den notları çek
    private void loadNotesFromDB() {
        notesList.clear();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DBHelper.TABLE_NOTES, null, null, null,
                null, null, null);

        if (cursor != null) {
            while (cursor.moveToNext()) {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.COLUMN_ID));
                String title = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COLUMN_TITLE));
                String content = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COLUMN_CONTENT));
                String date = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COLUMN_DATE));
                notesList.add(new NoteModel(id, title, content, date));
            }
            cursor.close();
        }
        db.close();
    }

    // Üst menü (Arama, Sıralama, Ayarlar)
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 1, 0, "Arama");
        menu.add(0, 2, 1, "Sıralama");
        menu.add(0, 3, 2, "Ayarlar");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case 1:
                // Arama
                showSearchDialog();
                return true;
            case 2:
                // Sıralama
                showSortDialog();
                return true;
            case 3:
                // Ayarlar -> Görünümü değiştirme
                showSettingsDialog();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showSearchDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Not Ara");

        final EditText input = new EditText(this);
        builder.setView(input);

        builder.setPositiveButton("Ara", (dialog, which) -> {
            String query = input.getText().toString();
            searchNotes(query);
        });
        builder.setNegativeButton("İptal", null);
        builder.show();
    }

    // Basit arama (title veya content içinde)
    private void searchNotes(String query) {
        notesList.clear();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        // LIKE ifadesi ile arama
        Cursor cursor = db.query(DBHelper.TABLE_NOTES, null,
                DBHelper.COLUMN_TITLE + " LIKE ? OR " + DBHelper.COLUMN_CONTENT + " LIKE ?",
                new String[]{"%" + query + "%", "%" + query + "%"},
                null, null, null);

        if (cursor != null) {
            while (cursor.moveToNext()) {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.COLUMN_ID));
                String title = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COLUMN_TITLE));
                String content = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COLUMN_CONTENT));
                String date = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COLUMN_DATE));
                notesList.add(new NoteModel(id, title, content, date));
            }
            cursor.close();
        }
        db.close();
        displayNotes();
    }

    private void showSortDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Notları Sıralama");
        String[] sorts = {"Tarihe Göre", "Başlığa Göre"};
        builder.setItems(sorts, (dialog, which) -> {
            if (which == 0) {
                sortNotesByDate();
            } else {
                sortNotesByTitle();
            }
        });
        builder.show();
    }

    private void sortNotesByDate() {
        // DB üzerinde ORDER BY ile de yapabilirsiniz
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DBHelper.TABLE_NOTES, null, null, null, null, null,
                DBHelper.COLUMN_DATE + " DESC");
        notesList.clear();
        if (cursor != null) {
            while (cursor.moveToNext()) {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.COLUMN_ID));
                String title = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COLUMN_TITLE));
                String content = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COLUMN_CONTENT));
                String date = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COLUMN_DATE));
                notesList.add(new NoteModel(id, title, content, date));
            }
            cursor.close();
        }
        db.close();
        displayNotes();
    }

    private void sortNotesByTitle() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DBHelper.TABLE_NOTES, null, null, null, null, null,
                DBHelper.COLUMN_TITLE + " ASC");
        notesList.clear();
        if (cursor != null) {
            while (cursor.moveToNext()) {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.COLUMN_ID));
                String title = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COLUMN_TITLE));
                String content = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COLUMN_CONTENT));
                String date = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COLUMN_DATE));
                notesList.add(new NoteModel(id, title, content, date));
            }
            cursor.close();
        }
        db.close();
        displayNotes();
    }

    private void showSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Ayarlar");

        String[] options;
        if (isListView) {
            options = new String[] {"Izgara görünümü seç", "Genel ayarlar"};
        } else {
            options = new String[] {"Liste görünümü seç", "Genel ayarlar"};
        }

        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                // Görünüm modunu değiştir
                isListView = !isListView;
                displayNotes();
            } else {
                Toast.makeText(MainActivity.this, "Genel ayarlar tıklandı.", Toast.LENGTH_SHORT).show();
                // Gerçek ayarlar işlemlerinizi burada yapabilirsiniz
            }
        });
        builder.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        displayNotes();
    }
}