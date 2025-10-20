# Example usage: 

# Create a new ingredient
#banana = Ingredient(protein=1, carbs=27, fats=0, fiber=3)

# Access attributes
# print(banana.carbs)  # 27

# Print the whole object nicely
# print(banana)
# Output: Ingredient(promo=0, promo_per_unit_estimate=0, national_price=0, ...)
from dataclasses import dataclass, asdict

@dataclass
class Ingredient:
    # Pricing
    local_regular: float = 0.0
    local_promo: float = 0.0
    local_promo_per_unit_estimate: float = 0.0
    local_regular_per_unit_estimate: float = 0.0
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

    saturated_fat: float = 0.0
    trans_fat: float = 0.0
    cholesterol: float = 0.0

    # Vitamins
    vitamin_a: float = 0.0
    vitamin_b1: float = 0.0
    vitamin_b2: float = 0.0
    vitamin_b3: float = 0.0
    vitamin_b5: float = 0.0
    vitamin_b6: float = 0.0
    vitamin_b7: float = 0.0
    vitamin_b9: float = 0.0
    vitamin_b12: float = 0.0
    vitamin_c: float = 0.0
    vitamin_d: float = 0.0
    vitamin_e: float = 0.0
    vitamin_k: float = 0.0

    # Minerals
    calcium: float = 0.0
    phosphorus: float = 0.0
    magnesium: float = 0.0
    sodium: float = 0.0
    potassium: float = 0.0
    chloride: float = 0.0
    sulfur: float = 0.0
    iron: float = 0.0
    zinc: float = 0.0
    copper: float = 0.0
    manganese: float = 0.0
    iodine: float = 0.0
    selenium: float = 0.0
    molybdenum: float = 0.0
    chromium: float = 0.0
    fluoride: float = 0.0
    cobalt: float = 0.0

    # Metadata
    name: str = ""
    serving_size_g: float = 100.0

    def describe(self) -> str:
        fields = asdict(self)
        return ", ".join(f"{key}={value}" for key, value in fields.items())
