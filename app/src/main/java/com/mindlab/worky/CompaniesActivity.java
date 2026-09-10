package com.mindlab.worky;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mindlab.worky.data.PartnerCompanies;

import java.util.ArrayList;
import java.util.List;

public class CompaniesActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_companies);
        com.mindlab.worky.ui.WorkyNav.bindFromContent(this, getString(R.string.companies_title));

        RecyclerView recycler = findViewById(R.id.recyclerCompanies);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        PartnerAdapter adapter = new PartnerAdapter();
        recycler.setAdapter(adapter);
        adapter.submit(PartnerCompanies.all());
    }

    private static class PartnerAdapter extends RecyclerView.Adapter<PartnerAdapter.Holder> {
        private final List<PartnerCompanies.Partner> data = new ArrayList<>();

        void submit(List<PartnerCompanies.Partner> items) {
            data.clear();
            if (items != null) data.addAll(items);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_partner_company, parent, false);
            return new Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            PartnerCompanies.Partner p = data.get(position);
            holder.initials.setText(p.initials);
            holder.name.setText(p.name);
            holder.meta.setText(p.sector + " · " + p.location + " · " + p.size);
            holder.highlight.setText(p.highlight);
            holder.jobs.setText(p.jobsOpen + " vaga(s) abertas");
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        static class Holder extends RecyclerView.ViewHolder {
            final TextView initials;
            final TextView name;
            final TextView meta;
            final TextView highlight;
            final TextView jobs;

            Holder(@NonNull View itemView) {
                super(itemView);
                initials = itemView.findViewById(R.id.textPartnerInitials);
                name = itemView.findViewById(R.id.textPartnerName);
                meta = itemView.findViewById(R.id.textPartnerMeta);
                highlight = itemView.findViewById(R.id.textPartnerHighlight);
                jobs = itemView.findViewById(R.id.textPartnerJobs);
            }
        }
    }
}
