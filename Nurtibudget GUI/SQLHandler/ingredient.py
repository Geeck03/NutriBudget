from dataclasses import asdict
from dataclasses import dataclass
from dataclasses import dataclass, field
from typing import List


'''
Ingredient Data Class
Each Ingredient object will represent a single product from Kroger API
Each nutrient will be stored as a nutrientInfo object in a list within the Ingredient object
'''

@dataclass 
class nutrientInfo:   
    displayName: str = ""
    code: str = ""
    description: str = ""
    quantity: float = 0.0
    percentDailyIntake: float = 0.0
    unitOfMeasure: str = ""
    
    def describe(self) -> str:
        return f"{self.displayName}: Quantity: {self.quantity} {self.unitOfMeasure} ({self.percentDailyIntake}% DV)"

@dataclass
class Ingredient:    
    def describe(self) -> str:
       lines = [
            "Ingredient Description:",
            self.name,
            self.product_ID,
            f"Serving Size : {self.serving_size} {self.serving_size_unit}",
            f"Servings per Container: {self.servings_per_container}",
            #f"Serving Size Description: ({self.serving_size_description})",
            f"----------------------------------------",
            f"Price Information:",
            f"Regular price {self.local_regular}",
            f"Regular per unit estimate {self.local_regular_per_unit_estimate}",
            f"Local promo {self.local_promo}",  # fix 'ocal_promo' typo
            f"Local per unit estimate {self.local_promo_per_unit_estimate}",
            f"National regular price {self.national_regular}",
            f"National promo price {self.national_promo}",
            f"National promo per unit estimate {self.national_promo_per_unit_estimate}",
            f"National regular per unit estimate {self.national_regular_per_unit_estimate}",
            f"----------------------------------------",
       ]
       for n in self.nutrients:
           lines.append(n.describe() if self.nutrients else "No nutrients available")
       lines.append("------------------------------------------------------------------------")
       return "\n".join(lines)
    
    
    # Metadata
    product_ID: str = ""
    name: str = ""
    
    # Seving size information
    serving_size: float = 0 # default per 100g
    #serving_size_description: str = ""
    serving_size_unit: str = ""
    servings_per_container: float = 0.0
    
    nutrients: List[nutrientInfo] = field(default_factory=list) #Empty List of nutrientInfo objects
    
    # Price info for store
    local_regular: str = ""
    local_promo: str = ""
    local_promo_per_unit_estimate: str = ""
    local_regular_per_unit_estimate: str = ""
    
    # Can implement price per serving later
    # Price per serving is: servings_per_container / local_regular 

    #National average price info
    national_regular: float = 0.0
    national_promo: float = 0.0
    national_promo_per_unit_estimate: float = 0.0
    national_regular_per_unit_estimate: float = 0.0

    
    '''
    Nutrient fields are below. The acutal nutrient data will be stored in the nutrients list above.
    Below are field for examples 
    '''

    '''
    # Macronutrients
    calories: float = 0.0
    protein: float = 0.0
    carbs: float = 0.0
    fats: float = 0.0
    fiber: float = 0.0
    sugars: float = 0.0


    # Vitamins
    vitamin_a: float = 0.0       # µg RAE
    vitmain_a_unitOfMeasure  = ""  # Measurement unit for vitamin A    
    vitamin_b1: float = 0.0      # Thiamin (mg)
    vitamin_b1_unitOfMeasure  = ""  # Measurement unit for vitamin B1
    vitamin_b2: float = 0.0      # Riboflavin (mg)
    vitamin_b2_unitOfMeasure  = ""  # Measurement unit for vitamin B2
    vitamin_b3: float = 0.0      # Niacin (mg)
    vitamin_b3_unitOfMeasure  = ""  # Measurement unit for vitamin B3
    vitamin_b5: float = 0.0      # Pantothenic acid (mg)
    vitamin_b5_unitOfMeasure  = ""  # Measurement unit for vitamin B5
    vitamin_b6: float = 0.0      # Pyridoxine (mg)
    vitamin_b6_unitOfMeasure  = ""  # Measurement unit for vitamin B6
    vitamin_b7: float = 0.0      # Biotin (µg)
    vitamin_b7_unitOfMeasure  = ""  # Measurement unit for vitamin B7
    vitamin_b9: float = 0.0      # Folate (µg)
    vitamin_b9_unitOfMeasure  = ""  # Measurement unit for vitamin B9
    vitamin_b12: float = 0.0     # Cobalamin (µg)
    vitamin_b12_unitOfMeasure  = ""  # Measurement unit for vitamin B12
    vitamin_c: float = 0.0       # mg
    vitamin_c_unitOfMeasure  = ""  # Measurement unit for vitamin C
    vitamin_d: float = 0.0       # µg
    vitamind_d_unitOfMeasure  = ""  # Measurement unit for vitamin D
    vitamin_e: float = 0.0       # mg
    vitamine_e_unitOfMeasure  = ""  # Measurement unit for vitamin E
    vitamin_k: float = 0.0       # µg
    vitamin_k_unitOfMeasure  = ""  # Measurement unit for vitamin K

    # Minerals (macro + trace)
    calcium: float = 0.0         # mg
    calcium_unitOfMeasure  = ""  # Measurement unit for calcium
    phosphorus: float = 0.0      # mg
    phosphorus_unitOfMeasure  = ""  # Measurement unit for phosphorus
    magnesium: float = 0.0       # mg
    magnesium_unitOfMeasure  = ""  # Measurement unit for magnesium
    sodium: float = 0.0          # mg
    sodium_unitOfMeasure  = ""  # Measurement unit for sodium
    potassium: float = 0.0       # mg
    potassium_unitOfMeasure  = ""  # Measurement unit for potassium
    chloride: float = 0.0        # mg
    chloride_unitOfMeasure  = ""  # Measurement unit for chloride
    sulfur: float = 0.0          # mg
    sulfur_unitOfMeasure  = ""  # Measurement unit for sulfur

    iron: float = 0.0            # mg
    iron_unitOfMeasure  = ""  # Measurement unit for iron
    zinc: float = 0.0            # mg
    zinc_unitOfMeasure  = ""  # Measurement unit for zinc
    copper: float = 0.0          # mg
    copper_unitOfMeasure  = ""  # Measurement unit for copper
    manganese: float = 0.0       # mg
    manganese_unitOfMeasure  = ""  # Measurement unit for manganese
    iodine: float = 0.0          # µg
    iodine_unitOfMeasure  = ""  # Measurement unit for iodine
    selenium: float = 0.0        # µg
    selenium_unitOfMeasure  = ""  # Measurement unit for selenium
    molybdenum: float = 0.0      # µg
    chromium: float = 0.0        # µg
    fluoride: float = 0.0        # µg
    cobalt: float = 0.0          # µg (as part of B12)

    # Other nutrients
    cholesterol: float = 0.0 # mg
    saturated_fat: float = 0.0 # g
    trans_fat: float = 0.0     # g
    '''

    