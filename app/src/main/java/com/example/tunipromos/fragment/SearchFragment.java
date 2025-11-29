package com.example.tunipromos.fragment;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tunipromos.R;
import com.example.tunipromos.adapter.PromotionAdapter;
import com.example.tunipromos.model.Promotion;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class SearchFragment extends Fragment {

    private TextInputEditText searchEditText;
    private ChipGroup categoryChipGroup;
    private RecyclerView searchResultsRecyclerView;
    private TextView emptyStateTextView;
    private PromotionAdapter promotionAdapter;
    private List<Promotion> allPromotions;
    private List<Promotion> filteredList;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search, container, false);

        db = FirebaseFirestore.getInstance();
        searchEditText = view.findViewById(R.id.searchEditText);
        categoryChipGroup = view.findViewById(R.id.categoryChipGroup);
        searchResultsRecyclerView = view.findViewById(R.id.searchResultsRecyclerView);
        emptyStateTextView = view.findViewById(R.id.emptyStateTextView);

        allPromotions = new ArrayList<>();
        filteredList = new ArrayList<>();
        promotionAdapter = new PromotionAdapter(getContext(), filteredList);
        searchResultsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        searchResultsRecyclerView.setAdapter(promotionAdapter);

        setupSearch();
        setupFilters();
        loadAllPromotions();

        return view;
    }

    private void loadAllPromotions() {
        db.collection("promotions").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    allPromotions.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        allPromotions.add(document.toObject(Promotion.class));
                    }
                    filterPromotions();
                });
    }

    private void setupSearch() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterPromotions();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        searchEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                filterPromotions();
                return true;
            }
            return false;
        });
    }

    private void setupFilters() {
        categoryChipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            filterPromotions();
        });
    }

    private void filterPromotions() {
        String query = searchEditText.getText().toString().toLowerCase().trim();
        String selectedCategory = "All";

        int checkedChipId = categoryChipGroup.getCheckedChipId();
        if (checkedChipId != View.NO_ID) {
            Chip chip = categoryChipGroup.findViewById(checkedChipId);
            selectedCategory = chip.getText().toString();
        }

        filteredList.clear();
        for (Promotion p : allPromotions) {
            boolean matchesSearch = p.getTitle().toLowerCase().contains(query) ||
                    p.getDescription().toLowerCase().contains(query);
            boolean matchesCategory = selectedCategory.equals("All") ||
                    (p.getCategory() != null && p.getCategory().equalsIgnoreCase(selectedCategory));

            if (matchesSearch && matchesCategory) {
                filteredList.add(p);
            }
        }

        promotionAdapter.notifyDataSetChanged();

        if (filteredList.isEmpty()) {
            emptyStateTextView.setVisibility(View.VISIBLE);
            searchResultsRecyclerView.setVisibility(View.GONE);
        } else {
            emptyStateTextView.setVisibility(View.GONE);
            searchResultsRecyclerView.setVisibility(View.VISIBLE);
        }
    }
}
