package slowhackaton.com.findmetouchmecatchme;

/**
 * Created by Mateusz Bereziński on 2015-04-24.
 */
public class RowBean {

    public String userName;
    public String id;

    public RowBean(){

    }

    public RowBean(String userName, String id) {
       this.userName = userName;
       this.id = id;
    }

    /*
    public RowBean(String Username, String name) {


        this.UserName = Username;
        this.name = name;
    }*/
    @Override
    public boolean equals(Object object)
    {
        boolean isEqual= false;

        if (object != null && object instanceof RowBean)
        {
            isEqual = (this.id == ((RowBean) object).id);
        }

        return isEqual;
    }

    @Override
    public int hashCode() {
        return Integer.parseInt(this.id);
    }


}