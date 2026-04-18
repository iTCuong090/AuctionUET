import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.Map;

public class test_gson2 {
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
        
        try {
            String dataStr = res.data.toString();
            System.out.println("dataStr: " + dataStr);
            try {
                JsonObject dataObj = JsonParser.parseString(dataStr).getAsJsonObject();
                System.out.println("Parsed object token: " + dataObj.get("token"));
            } catch(Exception e) {
                System.out.println("Exception parsing: " + e.toString());
            }
            UserDTO loggedInUser = gson.fromJson(dataStr, UserDTO.class);
            System.out.println("Role: " + loggedInUser.role);
        } catch(Exception e) {
            System.out.println("Outer Exception: " + e.toString());
        }
    }
}
