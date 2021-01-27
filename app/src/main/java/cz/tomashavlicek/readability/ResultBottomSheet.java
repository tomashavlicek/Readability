package cz.tomashavlicek.readability;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class ResultBottomSheet extends BottomSheetDialogFragment {

    private double colemanLiauIndex;
    private int sentences, words, characters;

    public ResultBottomSheet(double colemanLiauIndex, int sentences, int words, int characters) {
        this.colemanLiauIndex = colemanLiauIndex;
        this.sentences = sentences;
        this.words = words;
        this.characters = characters;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_result, container, false);

        TextView comprehensionText = rootView.findViewById(R.id.comprehension_text);
        TextView colemanLiauIndexText = rootView.findViewById(R.id.index_text);
        TextView sentencesText = rootView.findViewById(R.id.sentences_text);
        TextView wordsText = rootView.findViewById(R.id.words_text);
        TextView charactersText = rootView.findViewById(R.id.characters_text);

        comprehensionText.setText(getComprehension(colemanLiauIndex));
        colemanLiauIndexText.setText(
                getString(
                        R.string.index_prefix,
                        String.format(
                                getResources().getConfiguration().getLocales().get(0),
                                "%.2f",
                                colemanLiauIndex)));
        sentencesText.setText(getString(R.string.sentences_prefix, sentences));
        wordsText.setText(getString(R.string.words_prefix, words));
        charactersText.setText(getString(R.string.characters_prefix, characters));

        return rootView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // Opt-in the same animation for dismiss as is used for the drag-to-dismiss
        // (https://medium.com/over-engineering/hands-on-with-material-components-for-android-bottom-sheet-970c5f0f1840)
        ((BottomSheetDialog) requireDialog()).setDismissWithAnimation(true);
    }

    private String getComprehension(double colemanLiauIndex) {
        if (colemanLiauIndex >= 17) {
            return getString(R.string.extremely_difficult);
        } else if (colemanLiauIndex >= 13) {
            return getString(R.string.difficult);
        } else if (colemanLiauIndex >= 11) {
            return getString(R.string.fairly_difficult);
        } else if (colemanLiauIndex >= 8) {
            return getString(R.string.conversational);
        } else if (colemanLiauIndex >= 7) {
            return getString(R.string.fairly_easy);
        } else if (colemanLiauIndex >= 6) {
            return getString(R.string.easy);
        } else {
            return getString(R.string.very_easy);
        }
    }
}
