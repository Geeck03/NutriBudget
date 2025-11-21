# ml_model.py
import sys
import pandas as pd
from sklearn.linear_model import LinearRegression
import json

def main():
    # Read JSON input from stdin
    input_data = json.loads(sys.stdin.read())

    # Example: {"X": [[1], [2], [3]], "y": [2, 4, 6]}
    X = pd.DataFrame(input_data['X'])
    y = input_data['y']

    model = LinearRegression()
    model.fit(X, y)

    prediction = model.predict([[4]])  # Just an example
    result = {"prediction": prediction[0]}

    print(json.dumps(result))

if __name__ == "__main__":
    main()
