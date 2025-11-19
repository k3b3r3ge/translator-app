package com.trans.translator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.ml.common.modeldownload.FirebaseModelDownloadConditions;
import com.google.firebase.ml.naturallanguage.FirebaseNaturalLanguage;
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslateLanguage;
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslator;
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslatorOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.BackgroundColorSpan;
import android.text.style.StyleSpan;
import android.graphics.Color;
import android.graphics.Typeface;

public class MainActivity extends AppCompatActivity {

    private Spinner fromSpinner, toSpinner;
    private TextInputEditText sourceEdt;
    private ImageView micIV;
    private MaterialButton translateBtn;
    private TextView translatedTV;

    private static final int LANGUAGE_CODE_RUKIGA = -1;

    String[] fromLanguages = {"From", "English", "French", "Arabic", "Swahili", "Korean", "Runyakole/Rukiga"};
    String[] toLanguages = {"To", "English", "French", "Arabic", "Swahili", "Korean", "Runyakole/Rukiga"};

    private static final int REQUEST_PERMISSION_CODE = 1;
    int languageCode, fromLanguageCode, toLanguageCode = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Rukiga_Runyakore_Translator.preload(getApplicationContext());
        fromSpinner = findViewById(R.id.idFromSpinner);
        toSpinner = findViewById(R.id.idToSpinner);
        sourceEdt = findViewById(R.id.idEdtSource);
        micIV = findViewById(R.id.idIVMic);
        translateBtn = findViewById(R.id.idBtnTranslate);
        translatedTV = findViewById(R.id.idTVTranslatedTV);
// From spinner
        fromSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
                fromLanguageCode = getLanguageCode(fromLanguages[position]);

            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        ArrayAdapter fromAdapter = new ArrayAdapter(this, R.layout.spiner_item, fromLanguages);
        fromAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        fromSpinner.setAdapter(fromAdapter);

        //To Spinner
        toSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
                toLanguageCode = getLanguageCode(toLanguages[position]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        ArrayAdapter toAdapter = new ArrayAdapter(this, R.layout.spiner_item, toLanguages);
        toAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        toSpinner.setAdapter(toAdapter);

        //Adding listener to the button
        translateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                translatedTV.setText(" ");
                if (sourceEdt.getText().toString().isEmpty()) {
                    Toast.makeText(MainActivity.this, "Enter Text to translate", Toast.LENGTH_SHORT).show();
                } else if (fromLanguageCode == 0) {
                    Toast.makeText(MainActivity.this, "Please select source language", Toast.LENGTH_SHORT).show();
                } else if (toLanguageCode == 0) {
                    Toast.makeText(MainActivity.this, "Please select language to translate to", Toast.LENGTH_SHORT).show();
                }
                else {
                    if (fromLanguageCode == FirebaseTranslateLanguage.EN && toLanguageCode == LANGUAGE_CODE_RUKIGA) {
                        List<Rukiga_Runyakore_Translator.DictionaryEntry> results = Rukiga_Runyakore_Translator.translateEnglishToRukiga(getApplicationContext(), sourceEdt.getText().toString());
                        if (results.isEmpty()) {
                            translatedTV.setText("Translation not found");
                        } else {
                            SpannableStringBuilder builder = new SpannableStringBuilder();
                            String query = sourceEdt.getText().toString().trim().toLowerCase(Locale.ROOT);

                            for (int i = 0; i < results.size(); i++) {
                                Rukiga_Runyakore_Translator.DictionaryEntry entry = results.get(i);

                                // Append Headword (Bold)
                                int start = builder.length();
                                builder.append(entry.displayHeadword);
                                builder.setSpan(new StyleSpan(Typeface.BOLD), start, builder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                                builder.append(" — ");

                                // Append Definition with Highlighting
                                String definition = entry.definition;
                                int defStart = builder.length();
                                builder.append(definition);

                                // Highlight matches in definition
                                String lowerDef = definition.toLowerCase(Locale.ROOT);
                                int searchIndex = lowerDef.indexOf(query);
                                while (searchIndex != -1) {
                                    int matchStart = defStart + searchIndex;
                                    int matchEnd = matchStart + query.length();
                                    builder.setSpan(new BackgroundColorSpan(Color.YELLOW), matchStart, matchEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                                    searchIndex = lowerDef.indexOf(query, searchIndex + 1);
                                }

                                if (i < results.size() - 1) {
                                    builder.append("\n\n");
                                }
                            }
                            translatedTV.setText(builder);
                        }
                    } else if (fromLanguageCode == LANGUAGE_CODE_RUKIGA && toLanguageCode == FirebaseTranslateLanguage.EN) {
                        String result = Rukiga_Runyakore_Translator.translateRukigaToEnglish(getApplicationContext(), sourceEdt.getText().toString());
                        translatedTV.setText(result);
                    } else if (fromLanguageCode == LANGUAGE_CODE_RUKIGA || toLanguageCode == LANGUAGE_CODE_RUKIGA) {
                        Toast.makeText(MainActivity.this, "Only English ↔ Runyakole/Rukiga translations are currently supported.", Toast.LENGTH_LONG).show();
                    } else {
                        translateText(fromLanguageCode, toLanguageCode, sourceEdt.getText().toString());
                    }
                }

            }
        });

        micIV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, Locale.getDefault());
                i.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to convert into text");
                try {
                    startActivityForResult(i, REQUEST_PERMISSION_CODE);
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
//@Override
//public void onActivityResult(int requestCode, int resultCode, @Nullable @org.jetbrains.annotations.Nullable)


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_PERMISSION_CODE){
            if(resultCode==RESULT_OK && data !=null){
                ArrayList <String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                sourceEdt.setText(result.get(0));
            }
        }
    }

    public void translateText(int fromLanguageCode, int toLanguageCode, String source){
        translatedTV.setText("Downloading Model..");
        FirebaseTranslatorOptions options =  new FirebaseTranslatorOptions.Builder()
                .setSourceLanguage(fromLanguageCode)
                .setTargetLanguage(toLanguageCode)
                .build();
        FirebaseTranslator translator = FirebaseNaturalLanguage.getInstance().getTranslator(options);

        FirebaseModelDownloadConditions conditions = new FirebaseModelDownloadConditions.Builder().build();

        translator.downloadModelIfNeeded(conditions).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void unused) {
                translatedTV.setText("Translating ...");
                translator.translate(source).addOnSuccessListener(new OnSuccessListener<String>() {
                    @Override
                    public void onSuccess(String s) {
                        translatedTV.setText(s);
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(MainActivity.this, "Failed to Translate" + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(MainActivity.this, "Failed to download language model" + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    public int getLanguageCode (String language){
        int languagecode = 0;
        switch (language){
            case "English":
                languagecode = FirebaseTranslateLanguage.EN;
                break;
            case "French":
                languagecode = FirebaseTranslateLanguage.FR;
                break;
            case "Arabic":
                languagecode = FirebaseTranslateLanguage.AR;
                break;
            case "Swahili":
                languagecode = FirebaseTranslateLanguage.SW;
                break;
            case "Korean":
                languagecode = FirebaseTranslateLanguage.KO;
                break;
            case "Runyakole/Rukiga":
                languagecode = LANGUAGE_CODE_RUKIGA; // Our special code for our custom translator
                break;
            default:
                languagecode = 0;
        }
        return languagecode;

    }



}
