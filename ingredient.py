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


    # Vitamins
    vitamin_a: float = 0.0       # µg RAE
    vitamin_b1: float = 0.0      # Thiamin (mg)
    vitamin_b2: float = 0.0      # Riboflavin (mg)
    vitamin_b3: float = 0.0      # Niacin (mg)
    vitamin_b5: float = 0.0      # Pantothenic acid (mg)
    vitamin_b6: float = 0.0      # Pyridoxine (mg)
    vitamin_b7: float = 0.0      # Biotin (µg)
    vitamin_b9: float = 0.0      # Folate (µg)
    vitamin_b12: float = 0.0     # Cobalamin (µg)
    vitamin_c: float = 0.0       # mg
    vitamin_d: float = 0.0       # µg
    vitamin_e: float = 0.0       # mg
    vitamin_k: float = 0.0       # µg


    # Minerals (macro + trace)
    calcium: float = 0.0         # mg
    phosphorus: float = 0.0      # mg
    magnesium: float = 0.0       # mg
    sodium: float = 0.0          # mg
    potassium: float = 0.0       # mg
    chloride: float = 0.0        # mg
    sulfur: float = 0.0          # mg

    iron: float = 0.0            # mg
    zinc: float = 0.0            # mg
    copper: float = 0.0          # mg
    manganese: float = 0.0       # mg
    iodine: float = 0.0          # µg
    selenium: float = 0.0        # µg
    molybdenum: float = 0.0      # µg
    chromium: float = 0.0        # µg
    fluoride: float = 0.0        # µg
    cobalt: float = 0.0          # µg (as part of B12)

    # Other nutrients
    cholesterol: float = 0.0 # mg
    saturated_fat: float = 0.0 # g
    trans_fat: float = 0.0     # g

    # Metadata
    name: str = ""
    serving_size_g: float = 100.0  # default per 100g