from dataclasses import asdict
from dataclasses import dataclass
from dataclasses import dataclass, field
from typing import List

from ingredient import Ingredient 
from ingredient import nutrientInfo

'''
    Scale the ingredient object given based on the number given such as 2/3, 3, or 0.5 
    Then return the ingredient object that was modified 
'''

def scaleIngredient(ing: Ingredient, scale: int) -> Ingredient: 
     
     # Scales quanity and percentDailyIntake based on the scale 
     for n in ing.nutrients: 
         n.quantity = n.quantity * scale if n.quantity else 0 
         n.percentDailyIntake = n.percentDailyIntake * scale if n.percentDailyIntake else 0 
         
     return ing