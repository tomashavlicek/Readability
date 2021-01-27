package cz.tomashavlicek.readability;

import android.content.Intent;
import android.icu.text.BreakIterator;
import android.icu.text.SimpleDateFormat;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionCloudTextRecognizerOptions;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    static final int REQUEST_IMAGE_GET = 1;
    static final int REQUEST_IMAGE_CAPTURE = 2;

    private EditText contentText;

    private String currentPhotoPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        contentText = findViewById(R.id.content_text);
        // Stack overflow magic to set ime action to a multiline edit text
        // https://stackoverflow.com/questions/2986387/multi-line-edittext-with-done-action-button/41022589#41022589
        contentText.setImeOptions(EditorInfo.IME_ACTION_DONE);
        contentText.setRawInputType(InputType.TYPE_CLASS_TEXT);
        contentText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    analyze(textView.getText().toString());
                    handled = true;
                }
                return handled;
            }
        });

        // Extract text from ACTION_PROCESS_TEXT intent
        CharSequence text = getIntent()
                .getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT);
        if (text != null) {
            contentText.setText(text);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.done:
                analyze(contentText.getText().toString());
                return true;
            case R.id.clear:
                contentText.getText().clear();
                return true;
            case R.id.photo:
                selectImage();
                return true;
            case R.id.camera:
                dispatchTakePictureIntent();
                return true;
            case R.id.help:
                startActivity(new Intent(this, HelpActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Uri photoUri = null;
        if (requestCode == REQUEST_IMAGE_GET && resultCode == RESULT_OK && data != null) {
            photoUri = data.getData();
        } else if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK && data != null) {
            photoUri = Uri.fromFile(new File(currentPhotoPath));
        }

        if (photoUri != null) {
            getTextFrom(photoUri);
        }
    }

    private void getTextFrom(Uri fullPhotoUri) {
        FirebaseVisionImage image;
        try {
            image = FirebaseVisionImage.fromFilePath(this, fullPhotoUri);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        FirebaseVisionCloudTextRecognizerOptions options = new FirebaseVisionCloudTextRecognizerOptions.Builder()
                .setLanguageHints(Collections.singletonList("en"))
                .build();
        FirebaseVisionTextRecognizer detector = FirebaseVision.getInstance()
                .getCloudTextRecognizer(options);
        detector.processImage(image)
                .addOnSuccessListener(new OnSuccessListener<FirebaseVisionText>() {
                    @Override
                    public void onSuccess(FirebaseVisionText firebaseVisionText) {
                        // Task completed successfully
                        contentText.setText(
                                firebaseVisionText.getText().replaceAll("\n", " "));
                    }
                })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                // Task failed with an exception
                                Snackbar.make(
                                        findViewById(android.R.id.content),
                                        R.string.text_recognize_fail,
                                        Snackbar.LENGTH_SHORT);
                            }
                        });
    }

    public void selectImage() {
        Intent selectImageIntent = new Intent(Intent.ACTION_GET_CONTENT);
        selectImageIntent.setType("image/*");
        if (selectImageIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(selectImageIntent, REQUEST_IMAGE_GET);
        }
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.android.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                .format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void analyze(String text) {
        ResultBottomSheet bottomSheet = new ResultBottomSheet(
                getColemanLiauIndex(text),
                getSentencesCount(text),
                getWordsCount(text),
                getLettersCount(text));
        bottomSheet.show(getSupportFragmentManager(), "TAG");
    }

    public double getColemanLiauIndex(String text) {
        int words = getWordsCount(text);
        float L = (getLettersCount(text) / (float) words) * 100;
        float S = (getSentencesCount(text) / (float) words) * 100;

        return ((0.0588 * L - 0.296 * S - 15.8) * 100.0) / 100.0;
    }

    public int getLettersCount(String text) {
        int count = 0;
        for (int i = 0, l = text.length(); i < l; i++) {
            if (Character.isLetterOrDigit(text.charAt(i))) {
                count++;
            }
        }
        return count;
    }

    public int getWordsCount(String text) {
        BreakIterator sentenceIterator = BreakIterator.getWordInstance(new Locale("en"));
        sentenceIterator.setText(text);
        int count = 0;

        int wordBoundaryIndex = sentenceIterator.first();
        int prevIndex = 0;
        while (wordBoundaryIndex != BreakIterator.DONE) {
            String word = text.substring(prevIndex, wordBoundaryIndex).toLowerCase();
            if (isWord(word)) {
                count++;
            }
            prevIndex = wordBoundaryIndex;
            wordBoundaryIndex = sentenceIterator.next();
        }
        return count;
    }

    private static boolean isWord(String word) {
        if (word.length() == 1) {
            return Character.isLetterOrDigit(word.charAt(0));
        }
        return !"".equals(word.trim());
    }

    public int getSentencesCount(String text) {
        BreakIterator sentenceIterator = BreakIterator.getSentenceInstance(new Locale("en@ss=standard"));
        sentenceIterator.setText(text);
        int count = 0;

        sentenceIterator.first();
        for (int end = sentenceIterator.next(); end != BreakIterator.DONE; end = sentenceIterator.next()) {
            count++;
        }
        return count;
    }
}
