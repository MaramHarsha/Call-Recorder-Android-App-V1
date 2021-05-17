package com.harsha.callrecorder.adapter;

import android.Manifest;
import android.app.Dialog;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.provider.ContactsContract;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.harsha.callrecorder.R;
import com.harsha.callrecorder.activity.CallActivity;
import com.harsha.callrecorder.envr.AppEnvr;
import com.harsha.callrecorder.object.IncomingCallObject;
import com.harsha.callrecorder.util.ResourceUtil;

import java.io.File;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import io.realm.Realm;

public class IncomingCallRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements Filterable {
    private static final int VIEW_HEADER = 0, VIEW_ITEM = 1;

    private LayoutInflater mLayoutInflater = null;

    private List<IncomingCallObject> mIncomingCallObjectList, mIncomingCallObjectFilteredList;

    private boolean mReadContacts = false;

    public IncomingCallRecyclerViewAdapter(@NonNull Context context, @NonNull List<IncomingCallObject> incomingCallObjectList) {
        try {
            mLayoutInflater = LayoutInflater.from(context);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (mLayoutInflater == null) {
            try {
                mLayoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        mIncomingCallObjectList = mIncomingCallObjectFilteredList = incomingCallObjectList;
    }

    public IncomingCallRecyclerViewAdapter(@NonNull Context context, @NonNull List<IncomingCallObject> incomingCallObjectList, boolean readContacts) {
        this(context, incomingCallObjectList);

        mReadContacts = readContacts;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        /*return null;*/

        // ----

        RecyclerView.ViewHolder viewHolder;

        if (viewType == VIEW_HEADER) {
            View view = mLayoutInflater.inflate(R.layout.adapter_view_header, parent, false);

            viewHolder = new HeaderViewHolder(view);
        } else /*if (viewType == VIEW_ITEM)*/ {
            View view = mLayoutInflater.inflate(R.layout.adapter_incoming_item, parent, false);

            viewHolder = new ItemViewHolder(view);
        }

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        // holder.setIsRecyclable(false);

        // ----

        IncomingCallObject incomingCallObject = mIncomingCallObjectFilteredList.get(position);

        if (incomingCallObject != null) {
            boolean isFirstItem = position == 0, isLastItem = position == mIncomingCallObjectFilteredList.size() - 1;

            if (holder instanceof HeaderViewHolder) {
                HeaderViewHolder headerViewHolder = (HeaderViewHolder) holder;

                headerViewHolder.titleTextView.setText(incomingCallObject.getHeaderTitle());
            } else /*if (holder instanceof ItemViewHolder)*/ {
                ItemViewHolder itemViewHolder = (ItemViewHolder) holder;

                String correspondent = incomingCallObject.getPhoneNumber();

                if (correspondent != null && !correspondent.trim().isEmpty()) {
                    if (mReadContacts) {
                        try {
                            Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(correspondent));

                            Cursor cursor = holder.itemView.getContext().getContentResolver().query(uri, new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME, ContactsContract.PhoneLookup._ID}, null, null, null);

                            if (cursor != null) {
                                if (cursor.moveToFirst()) {
                                    String tempDisplayName = cursor.getString(cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME));

                                    if (tempDisplayName != null && !tempDisplayName.trim().isEmpty()) {
                                        incomingCallObject.setCorrespondentName(correspondent = tempDisplayName);
                                    }

                                    String id = cursor.getString(cursor.getColumnIndex(ContactsContract.PhoneLookup._ID));

                                    if (id != null && !id.trim().isEmpty()) {
                                        InputStream inputStream = null;
                                        try {
                                            inputStream = ContactsContract.Contacts.openContactPhotoInputStream(holder.itemView.getContext().getContentResolver(), ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, Long.valueOf(id)));
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }

                                        if (inputStream != null) {
                                            Bitmap bitmap = null;
                                            try {
                                                bitmap = BitmapFactory.decodeStream(inputStream);
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                            }

                                            if (bitmap != null) {
                                                itemViewHolder.imageView.setImageBitmap(ResourceUtil.getBitmapClippedCircle(bitmap));
                                            }
                                        }
                                    }
                                }

                                cursor.close();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    itemViewHolder.numberTextView.setText(correspondent);
                } else {
                    itemViewHolder.numberTextView.setText(correspondent = holder.itemView.getContext().getString(R.string.unknown_number));
                }

                String beginDateTime = null;

                if (!DateFormat.is24HourFormat(holder.itemView.getContext())) {
                    try {
                        beginDateTime = new SimpleDateFormat("hh:mm:ss a - E dd/MM/yy", Locale.getDefault()).format(new Date(incomingCallObject.getBeginTimestamp()));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    try {
                        beginDateTime = new SimpleDateFormat("HH:mm:ss - E dd/MM/yy", Locale.getDefault()).format(new Date(incomingCallObject.getBeginTimestamp()));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                itemViewHolder.beginDateTimeTextView.setText(beginDateTime != null ? beginDateTime : "Begin date & time: N/A");

                /*if (incomingCallObject.getIsLastInCategory() || isLastItem) {
                    itemViewHolder.hrView.setVisibility(View.GONE);
                }*/

                // ----

                String finalCorrespondent = correspondent;

                itemViewHolder.linearLayout.setOnClickListener(view -> {
                    openIncomingCall(holder.itemView.getContext(), incomingCallObject);
                });
                itemViewHolder.linearLayout.setOnLongClickListener(view -> showItemMenuDialog(holder.itemView.getContext(), incomingCallObject, finalCorrespondent));
                itemViewHolder.menuImageButton.setOnClickListener(view -> showItemMenuDialog(holder.itemView.getContext(), incomingCallObject, finalCorrespondent));
            }
        }
    }

    @Override
    public int getItemCount() {
        /*return 0;*/

        // ----

        return mIncomingCallObjectFilteredList != null ? mIncomingCallObjectFilteredList.size() : 0;
    }

    // ----

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        return mIncomingCallObjectFilteredList.get(position).getIsHeader() ? VIEW_HEADER : VIEW_ITEM;
    }

    // ----

    class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView;

        public HeaderViewHolder(@NonNull View itemView) {
            super(itemView);

            titleTextView = itemView.findViewById(R.id.adapter_view_header_title);
        }
    }

    class ItemViewHolder extends RecyclerView.ViewHolder {
        LinearLayout linearLayout;

        ImageView imageView;

        TextView numberTextView, beginDateTimeTextView;

        ImageButton menuImageButton;

        //View hrView;

        public ItemViewHolder(@NonNull View itemView) {
            super(itemView);

            linearLayout = itemView.findViewById(R.id.adapter_incoming_item_layout);

            imageView = itemView.findViewById(R.id.adapter_incoming_item_image_view);

            numberTextView = itemView.findViewById(R.id.adapter_incoming_item_number);
            beginDateTimeTextView = itemView.findViewById(R.id.adapter_incoming_item_begin_date_time);

            menuImageButton = itemView.findViewById(R.id.adapter_incoming_item_menu);

            //hrView = itemView.findViewById(R.id.adapter_incoming_hr_item_view);
        }
    }

    // ----

    @Override
    public Filter getFilter() {
        /*return null;*/

        // ----

        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence charSequence) {
                /*return null;*/

                // ----

                String query = charSequence != null ? charSequence.toString() : null;

                if (query != null && !query.trim().isEmpty()) {
                    List<IncomingCallObject> newIncomingCallObjectFilteredList = new ArrayList<>();

                    for (IncomingCallObject incomingCallObject : mIncomingCallObjectList) {
                        if (!incomingCallObject.getIsHeader()) {
                            String phoneNumber = incomingCallObject.getPhoneNumber();

                            if (phoneNumber != null && !phoneNumber.trim().isEmpty()) {
                                if (phoneNumber.toLowerCase(Locale.getDefault()).contains(query.toLowerCase(Locale.getDefault()))) {
                                    newIncomingCallObjectFilteredList.add(incomingCallObject);
                                }
                            }

                            if (mReadContacts) {
                                String correspondentName = incomingCallObject.getCorrespondentName();

                                if (correspondentName != null && !correspondentName.trim().isEmpty()) {
                                    if (correspondentName.toLowerCase(Locale.getDefault()).contains(query.toLowerCase(Locale.getDefault()))) {
                                        newIncomingCallObjectFilteredList.add(incomingCallObject);
                                    }
                                }
                            }
                        }
                    }

                    mIncomingCallObjectFilteredList = newIncomingCallObjectFilteredList;
                } else {
                    mIncomingCallObjectFilteredList = mIncomingCallObjectList;
                }

                FilterResults filterResults = new FilterResults();
                filterResults.values = mIncomingCallObjectFilteredList;
                filterResults.count = mIncomingCallObjectFilteredList.size();

                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
                mIncomingCallObjectFilteredList = (ArrayList<IncomingCallObject>) filterResults.values;

                notifyDataSetChanged();
            }
        };
    }

    // ----

    private void openIncomingCall(@NonNull Context context, @NonNull IncomingCallObject incomingCallObject) {
        Intent intent = new Intent(context, CallActivity.class);
        intent.putExtra(AppEnvr.INTENT_ACTION_INCOMING_CALL, true);
        intent.putExtra("mBeginTimestamp", incomingCallObject.getBeginTimestamp());
        intent.putExtra("mEndTimestamp", incomingCallObject.getEndTimestamp());

        if (incomingCallObject.getCorrespondentName() != null && !incomingCallObject.getCorrespondentName().trim().isEmpty()) {
            intent.putExtra("mCorrespondentName", incomingCallObject.getCorrespondentName());
        }

        try {
            context.startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean showItemMenuDialog(@NonNull Context context, @NonNull IncomingCallObject incomingCallObject, @NonNull String correspondent) {
        CharSequence[] menuItems = {"Open recording", "Make phone call", "Delete"};

        Drawable drawable = ResourceUtil.getDrawable(context, R.drawable.ic_baseline_call_received_24px);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            drawable.setTint(ResourceUtil.getColor(context, R.color.colorPrimary));
        } else {
            DrawableCompat.setTint(drawable, ResourceUtil.getColor(context, R.color.colorPrimary));
        }

        Dialog dialog = new AlertDialog.Builder(context)
                .setIcon(drawable)
                /*.setTitle(context.getString(R.string.incoming_call) + " " + context.getString(R.string.from).toLowerCase() + " " + correspondent)*/
                .setTitle(context.getString(R.string.incoming_call) + " - " + correspondent)
                .setItems(menuItems, (dialogInterface, which) -> {
                    dialogInterface.dismiss();

                    // ----

                    switch (which) {
                        case 0: // "Open recording"
                            openIncomingCall(context, incomingCallObject);

                            break;
                        case 1: // "Make phone call"
                            String phoneNumber = incomingCallObject.getPhoneNumber();

                            if (phoneNumber != null && !phoneNumber.trim().isEmpty()
                                    && ActivityCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                                context.startActivity(new Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", phoneNumber, null)));
                            } else {
                                new AlertDialog.Builder(context)
                                        .setTitle("Cannot make phone call")
                                        .setMessage("Making phone call to this correspondent is not possible.")
                                        .setNeutralButton(android.R.string.ok, (dialogInterface1, i) -> {
                                            dialogInterface1.dismiss();
                                        })
                                        .create().show();
                            }

                            break;
                        case 2: // "Delete"
                            new AlertDialog.Builder(context)
                                    .setTitle("Delete call recording")
                                    .setMessage("Are you sure you want to delete this call recording (and its audio file)? Data cannot be recovered.")
                                    .setPositiveButton(R.string.yes, (dialogInterface1, i) -> {
                                        dialogInterface1.dismiss();

                                        // ----

                                        Realm realm = null;
                                        try {
                                            realm = Realm.getDefaultInstance();
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }

                                        if (realm != null && !realm.isClosed()) {
                                            try {
                                                realm.beginTransaction();

                                                IncomingCallObject incomingCallObject1 = realm.where(IncomingCallObject.class)
                                                        .equalTo("mBeginTimestamp", incomingCallObject.getBeginTimestamp())
                                                        .equalTo("mEndTimestamp", incomingCallObject.getEndTimestamp())
                                                        .findFirst();

                                                if (incomingCallObject1 != null) {
                                                    File outputFile = null;
                                                    try {
                                                        outputFile = new File(incomingCallObject1.getOutputFile());
                                                    } catch (Exception e) {
                                                        e.printStackTrace();
                                                    }

                                                    if (outputFile != null) {
                                                        if (outputFile.exists() && outputFile.isFile()) {
                                                            try {
                                                                outputFile.delete();
                                                            } catch (Exception e) {
                                                                e.printStackTrace();
                                                            }
                                                        }
                                                    }

                                                    // ----

                                                    incomingCallObject1.deleteFromRealm();

                                                    realm.commitTransaction();

                                                    Toast.makeText(context, "Call recording is deleted", Toast.LENGTH_SHORT).show();
                                                } else {
                                                    realm.cancelTransaction();

                                                    Toast.makeText(context, "Call recording is not deleted", Toast.LENGTH_SHORT).show();
                                                }

                                                realm.close();
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                            }
                                        } else {
                                            Toast.makeText(context, "Call recording is not deleted", Toast.LENGTH_SHORT).show();
                                        }
                                    })
                                    .setNegativeButton(R.string.no, (dialogInterface1, i) -> dialogInterface1.dismiss())
                                    .create().show();

                            break;
                    }
                }).create();

        dialog.show();

        return dialog.isShowing();
    }
}
