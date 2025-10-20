NUTRIENT_RANGES = {
    # format: (RDI, Upper Limit (UL))

    # Vitamins
    "vitamin_a": (900, 3000),
    "vitamin_b1": (1.2, 50),
    "vitamin_b2": (1.3, 50),
    "vitamin_b3": (16, 35),
    "vitamin_b5": (5, 1000),
    "vitamin_b6": (1.3, 100),
    "vitamin_b7": (30, 1000),
    "vitamin_b9": (400, 1000),
    "vitamin_b12": (2.4, 1000),
    "vitamin_c": (90, 2000),
    "vitamin_d": (20, 100),
    "vitamin_e": (15, 1000),
    "vitamin_k": (120, 1000),

    # Minerals
    "calcium": (1000, 2500),
    "phosphorus": (700, 4000),
    "magnesium": (400, 350),  # Note: UL applies to supplements, use carefully
    "sodium": (1500, 2300),
    "potassium": (4700, 5000),
    "chloride": (2300, 3600),
    "sulfur": (1000, 2000),
    "iron": (18, 45),
    "zinc": (11, 40),
    "copper": (0.9, 10),
    "manganese": (2.3, 11),
    "iodine": (150, 1100),
    "selenium": (55, 400),
    "molybdenum": (45, 2000),
    "chromium": (35, 1000),
    "fluoride": (4, 10),
    "cobalt": (5, 100),

    # Macronutrient bonuses
    "fiber": (25, 70),
    "protein": (50, 200),
}
