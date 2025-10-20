from nutrient_ranges import NUTRIENT_RANGES

def score_nutrient_balance(value, rdi, ul, max_score=5):
    if value < 0:
        return -max_score

    if rdi <= value <= ul:
        return max_score

    if value < rdi:
        percent = value / rdi
        if percent >= 0.8:
            return 0
        elif percent >= 0.6:
            return -1
        elif percent >= 0.4:
            return -2
        elif percent >= 0.2:
            return -3
        else:
            return -5

    else:  # value > UL
        percent_excess = (value - ul) / ul
        if percent_excess < 0.1:
            return 0
        elif percent_excess < 0.25:
            return -1
        elif percent_excess < 0.5:
            return -3
        else:
            return -5

def grade_from_score(score):
    if score >= 200:
        return "A+"
    elif score >= 150:
        return "A"
    elif score >= 100:
        return "B"
    elif score >= 50:
        return "C"
    elif score >= 0:
        return "D"
    else:
        return "E"

def calculate_comprehensive_score(ingredient):
    total_score = 0
    details = {}

    for nutrient, (rdi, ul) in NUTRIENT_RANGES.items():
        value = getattr(ingredient, nutrient, 0.0)
        score = score_nutrient_balance(value, rdi, ul)
        total_score += score
        details[nutrient] = {"value": value, "score": score}

    grade = grade_from_score(total_score)

    return {
        "score": total_score,
        "grade": grade,
        "details": details
    }
