import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.Map;

public class test_gson3 {
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
            JsonObject dataObj = JsonParser.parseString(dataStr).getAsJsonObject();
            String exactToken = dataObj.get("token").getAsString();
            
            // Correct way
            UserDTO loggedInUser = gson.fromJson(dataObj.get("user"), UserDTO.class);
            System.out.println("Role corrected: " + loggedInUser.role);
        } catch(Exception e) {
            System.out.println("Exception: " + e.toString());
        }
    }
}
