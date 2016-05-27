package com.algolia.instantsearch.ui;

import android.databinding.DataBindingUtil;
import android.databinding.ViewDataBinding;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.algolia.instantsearch.AlgoliaHelper;
import com.algolia.instantsearch.ImageLoadTask;
import com.algolia.instantsearch.RenderingHelper;
import com.algolia.instantsearch.model.Errors;
import com.algolia.instantsearch.views.AlgoliaAttributeView;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HitsAdapter extends RecyclerView.Adapter<HitsAdapter.ViewHolder> {

    private List<JSONObject> hits = new ArrayList<>();

    public HitsAdapter() {
        this(new ArrayList<JSONObject>());
    }

    public HitsAdapter(List<JSONObject> dataSet) {
        hits = dataSet;
    }

    @Override
    public ViewHolder onCreateViewHolder(final ViewGroup parent, int viewType) {
        ViewDataBinding binding = DataBindingUtil.inflate(
                LayoutInflater.from(parent.getContext()), AlgoliaHelper.getItemLayoutId(), parent, false);
        return new ViewHolder(binding.getRoot());
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.resetIfEmpty();

        // For every view we have, if we can handle its type let's use the hit's attribute
        for (Map.Entry<View, String> entry : holder.viewMap.entrySet()) {
            final View view = entry.getKey();
            final String attributeName = entry.getValue();
            final JSONObject hit = hits.get(position);
            final String attributeValue = hit.optString(attributeName);

            if (view instanceof AlgoliaAttributeView) {
                ((AlgoliaAttributeView) view).onUpdateView(hit);
            } else if (view instanceof EditText) {
                ((EditText) view).setHint(attributeValue);
            } else if (view instanceof TextView) {
                final TextView textView = (TextView) view;
                if (RenderingHelper.shouldHighlight(attributeName)) {
                    final int highlightColor = RenderingHelper.getHighlightColor(attributeName);
                    textView.setText(Highlighter.renderHighlightColor(hit, attributeName, highlightColor, view.getContext()));
                } else {
                    textView.setText(attributeValue);
                }
            } else if (view instanceof ImageView) {
                final ImageView imageView = (ImageView) view;
                new ImageLoadTask(imageView).execute(attributeValue);
            } else {
                throw new RuntimeException(Errors.ADAPTER_UNKNOWN_VIEW.replace("{className}", view.getClass().getCanonicalName()));
            }
        }
    }

    @Override
    public int getItemCount() {
        return hits.size();
    }

    public void clear() {
        hits.clear();
    }

    public void add(JSONObject result) {
        hits.add(result);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final Map<View, String> viewMap = new HashMap<>();

        public ViewHolder(View itemView) {
            super(itemView);
            initViewHolder(itemView);
        }

        /**
         * This method helps avoid a race condition: as the ViewHolder can be created before
         * our data-bindings are called, we need to check when binding it and eventually
         * initialize its views.
         */
        private void resetIfEmpty() {
            if (viewMap.size() == 0) {
                initViewHolder(itemView);
            }
        }

        /**
         * Initializes the ViewHolder, populating its viewMap
         *
         * @param itemView the rootView containing the holder's views
         */
        private void initViewHolder(View itemView) {
            // Store every annotated view with its attribute name
            for (Map.Entry<Integer, String> entry : AlgoliaHelper.getEntrySet()) {
                final String attributeName = entry.getValue();
                final View view = itemView.findViewById(entry.getKey());
                viewMap.put(view, attributeName);
            }
        }
    }
}