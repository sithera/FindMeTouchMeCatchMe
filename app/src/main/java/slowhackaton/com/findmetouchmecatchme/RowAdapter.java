package slowhackaton.com.findmetouchmecatchme;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import static java.lang.Thread.sleep;

/**
 * Created by Mateusz Bereziński on 2015-03-25.
 */
public class RowAdapter extends ArrayAdapter<RowBean> {

    Context context;
    int layoutResourceId;
    RowBean data[] = null;

    public RowAdapter(Context context, int layoutResourceId, RowBean[] data) {
        super(context, layoutResourceId, data);
        this.layoutResourceId = layoutResourceId;
        this.context = context;
        this.data = data;
        Log.d("adapter", "constructor called");
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        RowBeanHolder holder = null;
        final RowAdapter that = this;
        final RowBean object = data[position];

        Log.d("adapter", "is called");
        Log.d("adapter_row", object.id);

        if(row == null)
        {
            LayoutInflater inflater = ((Activity)context).getLayoutInflater();
            row = inflater.inflate(layoutResourceId, parent, false);

            holder = new RowBeanHolder();

            holder.UserName = (TextView)row.findViewById(R.id.friendName);

            ImageView messangerButton = (ImageView)row.findViewById(R.id.messanger);
            messangerButton.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View view) {
                    if(object.isClickable() == false)return;
                    ((BeaconFinder)that.context).startConversationWith(Long.parseLong(object.getGlobalId()));
                }
            });

            setPhoto(row,object);

            row.setTag(holder);
        }
        else
        {
            holder = (RowBeanHolder)row.getTag();
        }


        holder.UserName.setText(object.userName);
        Log.d("holder", object.userName);

        return row;
    }

    private void setPhoto(final View row,final RowBean data){

        Thread t = new Thread(new Runnable(){
            @Override
            public void run() {
                while(data.getGlobalId() == null){
                    try {
                        sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }


                final ImageView user_picture =(ImageView)row.findViewById(R.id.userPhoto);;

                URL img_value = null;

                try {
                    img_value = new URL("http://graph.facebook.com/"+ data.getGlobalId() +"/picture?type=small");
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }

                Log.d("setting photo (url)", img_value.toString());
                if(img_value == null) return;
                Bitmap mIcon1 = null;
                Log.d("photo url", "prepared");


                try {
                    mIcon1 = BitmapFactory.decodeStream(img_value.openConnection().getInputStream());
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if(mIcon1 == null) return;
                final Bitmap cheaterIcon = getResizedBitmap(mIcon1,20,20);
                Log.d("image","resized");
                user_picture.post(new Runnable() {
                    @Override
                    public void run() {
                        user_picture.setImageBitmap(cheaterIcon);
                    }
                });

            }
        });
        t.start();
    }



    public Bitmap getResizedBitmap(Bitmap bm, int newWidth, int newHeight) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // CREATE A MATRIX FOR THE MANIPULATION
        Matrix matrix = new Matrix();
        // RESIZE THE BIT MAP
        matrix.postScale(scaleWidth, scaleHeight);

        // "RECREATE" THE NEW BITMAP
        Bitmap resizedBitmap = Bitmap.createBitmap(bm, 0, 0, width, height, matrix, false);
        return resizedBitmap;
    }



    static class RowBeanHolder
    {

        TextView UserName;

    }


}