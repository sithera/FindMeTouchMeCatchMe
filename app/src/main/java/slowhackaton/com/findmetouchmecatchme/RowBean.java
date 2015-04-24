package slowhackaton.com.findmetouchmecatchme;

/**
 * Created by Mateusz Bereziński on 2015-04-24.
 */
public class RowBean {


    public String UserName;
    public String name;

    public RowBean(){

    }

    //nie jestem pewny jak zadziała tutaj string, wiec testować trzeba
    public RowBean(String Username) {


       this.UserName = Username;
    }

    /*
    public RowBean(String Username, String name) {


        this.UserName = Username;
        this.name = name;
    }*/

}