
from difflib import SequenceMatcher
import sys
import json

recipes = [
   "Grilled Chicken Salad",
   "Spaghetti with Tomato Sauce",
   "Avocado Toast",
   "Chicken Stir Fry",
   "Vegan Buddha Bowl",
   "Beef Tacos",
   "Protein Pancakes",
   "Peanut Butter Smoothie",
   "Tuna Sandwich",
   "Egg Fried Rice",
   "Turkey Meatballs",
   "Lentil Soup",
   "Greek Yogurt Parfait",
   "Salmon with Quinoa",
   "Shrimp Alfredo Pasta",
   "Vegetable Curry",
   "Beef Burrito Bowl",
   "Tofu Scramble",
   "Oatmeal with Berries",
   "Caesar Salad Wrap",
   "Mushroom Risotto",
   "BBQ Pulled Pork Sandwich",
   "Spinach and Feta Omelette",
   "Vegan Chili",
   "Teriyaki Chicken Bowl",
   "Crispy Fish Tacos",
   "Pesto Pasta Salad",
   "Banana Protein Shake",
   "Quinoa and Black Bean Salad",
   "Eggplant Parmesan",
   "Sausage Breakfast Burrito",
   "Chicken Quesadilla",
   "Sweet Potato Fries",
   "Turkey Club Sandwich",
   "Vegetarian Lasagna",
   "Baked Salmon with Asparagus",
   "Coconut Rice and Beans",
   "Pancakes with Maple Syrup",
   "Shrimp Fried Rice",
   "Beef and Broccoli Stir Fry",
   "Avocado Chicken Wrap",
   "Berry Smoothie Bowl",
   "Vegan Mac and Cheese",
   "Taco Salad",
   "Buffalo Chicken Wings",
   "Mango Chicken Curry",
   "Steak and Potatoes",
   "Zucchini Noodle Pasta",
   "Tomato Basil Soup",
   "Ham and Cheese Omelette",
   "BBQ Chicken Pizza",
   "Cauliflower Rice Bowl",
   "Chicken Fajita Bowl",
   "Honey Garlic Shrimp",
   "Veggie Burger",
   "Bacon and Eggs",
   "Chickpea Salad",
   "Beef Stroganoff",
   "Vegan Lentil Curry",
   "Pulled BBQ Jackfruit Sandwich",
   "Cobb Salad",
   "Stuffed Bell Peppers",
   "Chicken Caesar Pasta",
   "Spicy Ramen Bowl",
   "Falafel Wrap",
   "Vegan Burrito Bowl",
   "Egg Salad Sandwich",
   "Garlic Butter Shrimp",
   "Kale and Quinoa Salad",
   "Beef Chili",
   "Teriyaki Tofu Bowl",
   "Grilled Cheese Sandwich",
   "Broccoli Cheddar Soup",
   "Chicken and Waffles",
   "Chocolate Protein Shake",
   "Vegetable Stir Fry",
   "Pork Fried Rice",
   "Chicken Noodle Soup",
   "Tofu Pad Thai",
   "Spicy Tuna Roll",
   "Vegan Pancakes",
   "Beef Fajitas",
   "Garlic Parmesan Wings",
   "Shrimp Tacos",
   "Chicken Alfredo",
   "Stuffed Zucchini Boats",
   "Salmon Sushi Bowl",
   "Breakfast Burrito",
   "Mushroom Stroganoff",
   "Cajun Chicken Pasta",
   "Vegetable Fried Rice",
   "Greek Chicken Bowl",
   "Vegan Tofu Curry",
   "Caprese Salad",
   "Banana Oat Muffins",
   "BBQ Ribs",
   "Pesto Chicken Sandwich",
   "Turkey Chili",
   "Vegan Power Bowl",
   "Lemon Garlic Salmon",
   "Veggie Stir Fry Noodles",
   "Honey Mustard Chicken",
   "Roasted Vegetable Wrap"
]


# Exact Search Function
def simple_search(query, recipes):
   query = query.lower()
   return [r for r in recipes if query in r.lower()]

def fuzzy_search(query, recipes, threshold=0.6):
   query = query.lower()
   results = []
   for r in recipes:
       ratio = SequenceMatcher(None, query, r.lower()).ratio()
       if ratio > threshold:
           results.append((r, round(ratio, 2)))
   return sorted(results, key=lambda x: x[1], reverse=True)

def smart_search(query, recipes):
   exact = simple_search(query, recipes)
   if exact:
       return exact
   fuzzy = fuzzy_search(query, recipes)
   return [r for r, _ in fuzzy]

def get_all_recipes():
   import json
   print(json.dumps(recipes))


if __name__ == "__main__":
   if len(sys.argv) == 1:
       print(json.dumps(recipes))
   else:
       query = sys.argv[1]
       results = simple_search(query)
       if not results:
           results = fuzzy_search(query)
       print(json.dumps(results))



