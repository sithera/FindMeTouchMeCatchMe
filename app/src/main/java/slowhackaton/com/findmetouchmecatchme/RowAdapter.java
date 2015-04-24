package slowhackaton.com.findmetouchmecatchme;
import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

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
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        RowBeanHolder holder = null;

        if(row == null)
        {
            LayoutInflater inflater = ((Activity)context).getLayoutInflater();
            row = inflater.inflate(layoutResourceId, parent, false);

            holder = new RowBeanHolder();

            holder.UserName = (TextView)row.findViewById(R.id.friendName);


            row.setTag(holder);
        }
        else
        {
            holder = (RowBeanHolder)row.getTag();
        }

        RowBean object = data[position];
        holder.UserName.setText(object.UserName);



        return row;
    }

    static class RowBeanHolder
    {

        TextView UserName;

    }
}