package pages;

import org.json.JSONObject;

import java.util.UUID;

public class CustomIngredient {
    public String id;
    public String name;
    public String serving_label;
    public double price_per_serving;
    public double calories_per_serving;
    public String unit;
    public String notes;

    public CustomIngredient() {
        this.id = UUID.randomUUID().toString();
        this.name = "";
        this.serving_label = "";
        this.price_per_serving = 0.0;
        this.calories_per_serving = 0.0;
        this.unit = "";
        this.notes = "";
    }

    public JSONObject toJson() {
        JSONObject o = new JSONObject();
        o.put("id", id != null ? id : "");
        o.put("name", name != null ? name : "");
        o.put("serving_label", serving_label != null ? serving_label : "");
        o.put("price_per_serving", price_per_serving);
        o.put("calories_per_serving", calories_per_serving);
        o.put("unit", unit != null ? unit : "");
        o.put("notes", notes != null ? notes : "");
        return o;
    }

    public static CustomIngredient fromJson(JSONObject o) {
        CustomIngredient c = new CustomIngredient();
        c.id = o.optString("id", c.id);
        c.name = o.optString("name", "");
        c.serving_label = o.optString("serving_label", "");
        c.price_per_serving = o.optDouble("price_per_serving", 0.0);
        c.calories_per_serving = o.optDouble("calories_per_serving", 0.0);
        c.unit = o.optString("unit", "");
        c.notes = o.optString("notes", "");
        return c;
    }

    @Override
    public String toString() {
        return name != null ? name : "(custom)";
    }
}