import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.Map;

public class test_gson {
    public static class Response {
        public Object data;
    }
    public static class UserDTO {
        public String id, username, role;
    }
    public static void main(String[] args) {
        Gson gson = new Gson();
        String json = "{\"data\": {\"token\": \"123\", \"user\": {\"id\": \"1\", \"username\": \"abc\", \"role\": \"SELLER\"}}}";
        Response res = gson.fromJson(json, Response.class);
        
        System.out.println("res.getData() class: " + res.data.getClass().getName());
        String dataStr = gson.toJson(res.data); // Let's simulate gson.toJson instead of toString() to see if user changed it
        System.out.println("dataStr: " + dataStr);
        String dataStr2 = res.data.toString();
        System.out.println("using toString: " + dataStr2);
    }
}
