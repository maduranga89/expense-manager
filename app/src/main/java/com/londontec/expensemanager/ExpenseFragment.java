package com.londontec.expensemanager;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.londontec.expensemanager.model.Data;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.Date;

/**
 *
 */
public class ExpenseFragment extends Fragment {

    FirebaseRecyclerAdapter adapter;
    //Firebase database
    private FirebaseAuth mAuth;
    private DatabaseReference mExpenseDatabase;
    // Recyclear view..
    private RecyclerView recyclerView;

    private TextView expenseTotalText;

    private EditText editAmount;
    private EditText editType;
    private EditText editNote;

    private Button btnUpdate;
    private Button btnDelete;

    private String type;
    private String note;
    private int amount;

    private String postKey;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View myView = inflater.inflate(R.layout.fragment_expense, container, false);
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser mUser = mAuth.getCurrentUser();
        String uid = mUser.getUid();
        mExpenseDatabase = FirebaseDatabase.getInstance().getReference().child("ExpenseData").child(uid);
        recyclerView = myView.findViewById(R.id.recycler_id_expense);
        expenseTotalText = myView.findViewById(R.id.expense_txt_result);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        layoutManager.setReverseLayout(true);
        layoutManager.setStackFromEnd(true);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(layoutManager);

        mExpenseDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int totalIncome = 0;
                for (DataSnapshot mysnapshot : snapshot.getChildren()) {
                    Data data = mysnapshot.getValue(Data.class);
                    totalIncome += data.getAmount();
                }
                expenseTotalText.setText(NumberFormat.getInstance().format(totalIncome));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        return myView;
    }

    @Override
    public void onStart() {
        super.onStart();
        FirebaseRecyclerOptions<Data> options =
                new FirebaseRecyclerOptions.Builder<Data>()
                        .setQuery(mExpenseDatabase, Data.class)
                        .build();

        adapter = new FirebaseRecyclerAdapter<Data, ExpenseFragment.MyViewHolder>(options) {
            @Override
            public ExpenseFragment.MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.expense_recycler_data, parent, false);

                return new ExpenseFragment.MyViewHolder(view);
            }

            @Override
            protected void onBindViewHolder(ExpenseFragment.MyViewHolder holder, int position, Data model) {
                holder.setDate(model.getDate());
                holder.setType(model.getType());
                holder.setNote(model.getNote());
                holder.setAmount(model.getAmount());

                holder.mView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        postKey = getRef(position).getKey();
                        type = model.getType();
                        note = model.getNote();
                        amount = model.getAmount();
                        updateDataItem();
                    }
                });
            }
        };
        recyclerView.setAdapter(adapter);
        adapter.startListening();
    }

    @Override
    public void onStop() {
        super.onStop();
        adapter.stopListening();
    }

    private void updateDataItem() {
        AlertDialog.Builder myDialog = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        View myView = inflater.inflate(R.layout.custom_layout_for_update_data, null);
        myDialog.setView(myView);
        AlertDialog dialog = myDialog.create();
        dialog.setCancelable(false);

        editAmount = myView.findViewById(R.id.amount_edt);
        editNote = myView.findViewById(R.id.note_edt);
        editType = myView.findViewById(R.id.type_edt);

        //Set Data
        editAmount.setText(String.valueOf(amount));
        editType.setText(String.valueOf(type));
        editNote.setText(String.valueOf(note));

        btnUpdate = myView.findViewById(R.id.btnUpdate);
        btnDelete = myView.findViewById(R.id.btnDelete);

        btnUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String type = editType.getText().toString().trim();
                String amount = editAmount.getText().toString().trim();
                String note = editNote.getText().toString().trim();


                if (TextUtils.isEmpty(type)) {
                    editType.setError("Required Field..");
                    return;
                }
                if (TextUtils.isEmpty(amount)) {
                    editAmount.setError("Required Field..");
                    return;
                }
                if (TextUtils.isEmpty(note)) {
                    editNote.setError("Required Field..");
                    return;
                }

                int amountInt = Integer.parseInt(amount);
                String id = mExpenseDatabase.push().getKey();
                String mDate = DateFormat.getDateInstance().format(new Date());

                Data data = new Data(amountInt, type, note, postKey, mDate);
                mExpenseDatabase.child(postKey).setValue(data);
                dialog.dismiss();
                Toast.makeText(getActivity(), "Data Added!", Toast.LENGTH_SHORT);
            }
        });

        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mExpenseDatabase.child(postKey).removeValue();
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private static class MyViewHolder extends RecyclerView.ViewHolder {

        View mView;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            mView = itemView;
        }

        private void setType(String type) {
            TextView mType = (TextView) mView.findViewById(R.id.type_txt_expense);
            if (type != null)
                mType.setText(type);
        }

        private void setNote(String note) {
            TextView mNote = mView.findViewById(R.id.note_txt_expense);
            mNote.setText(note);
        }

        private void setDate(String date) {
            TextView mDate = mView.findViewById(R.id.date_txt_expense);
            mDate.setText(date);
        }

        private void setAmount(int amount) {
            TextView mAmount = mView.findViewById(R.id.amount_txt_expense);
            mAmount.setText(NumberFormat.getInstance().format((amount)));
        }
    }
}
