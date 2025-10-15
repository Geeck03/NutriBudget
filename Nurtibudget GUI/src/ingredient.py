# Example usage: 

# Create a new ingredient
#banana = Ingredient(protein=1, carbs=27, fats=0, fiber=3)

# Access attributes
# print(banana.carbs)  # 27

# Print the whole object nicely
# print(banana)
# Output: Ingredient(promo=0, promo_per_unit_estimate=0, national_price=0, ...)
from dataclasses import asdict
from dataclasses import dataclass

@dataclass
class Ingredient:

    
    def describe(self) -> str:
        fields = asdict(self)
        return ", ".join(f"{key}={value}" for key, value in fields.items())
    
    # Price info for store
    local_regular: float = 0.0
    local_promo: float = 0.0
    local_promo_per_unit_estimate: float = 0.0
    local_regular_per_unit_estimate: float = 0.0

    #National average price info
    national_regular: float = 0.0
    national_promo: float = 0.0
    national_promo_per_unit_estimate: float = 0.0
    national_regular_per_unit_estimate: float = 0.0

    # Macronutrients
    calories: float = 0.0
    protein: float = 0.0
    carbs: float = 0.0
    fats: float = 0.0
    fiber: float = 0.0
    sugars: float = 0.0

    # Micronutrients
    vitamin_a: float = 0.0   # IU or µg
    vitamin_c: float = 0.0   # mg
    vitamin_d: float = 0.0   # IU or µg
    calcium: float = 0.0     # mg
    iron: float = 0.0        # mg
    potassium: float = 0.0   # mg
    sodium: float = 0.0      # mg
    magnesium: float = 0.0   # mg

    # Other nutrients
    cholesterol: float = 0.0 # mg
    saturated_fat: float = 0.0 # g
    trans_fat: float = 0.0     # g

    # Metadata
    name: str = ""
    serving_size_g: float = 100.0  # default per 100g