import mysql.connector
from mysql.connector import Error
from typing import Any, List, Tuple, Optional, Dict
from dataclasses import asdict
from ingredient import Ingredient

class MySQLHandler:
    """
    A database handler for interacting with a MySQL database.

    Handles connection management, query execution, and domain-specific
    insert operations for Ingredients and Recipes as defined in the NBDB schema.

    Tables supported:
    - Users
    - Restrictions
    - UserRestrictions
    - Ingredients
    - Recipes
    - RecipeIngredients

    Future updates need to be made for possible FavoriteRecipes and CreatedRecipes table.

    NOTE: As a ingregrity measure, MySQL cursor does not reset an AUTO_INCREMENTED value if a value from the table is deleted.
    For example, if you were to run the given test main method twice, then the ingredient table would look like this:
    1 Oats ...
    2 Milk ...
    3 Eggs ...
    4 Bread ...
    
    Then deleting these records will not reset the cursor, so if you run the test main method again,
    the ingredient_ID will start at 5 instead of 1.
    """

    # Initialization and Connection Management
    def __init__(self, host: str, user: str, password: str, database: str):
        """
        Initialize the MySQL database handler.
        Parameters:
            host (str): The hostname or IP address of the MySQL server.
            user (str): The username for authentication.
            password (str): The user's password for authentication
            database (str): The name of the database to connect to
        """
        self.host = host
        self.user = user
        self.password = password
        self.database = database
        self.connection = None

    def connect(self):
        """Establish a connection to the MySQL database"""
        try:
            if not self.connection or not getattr(self.connection, "is_connected", lambda: False)():
                self.connection = mysql.connector.connect(
                    host = self.host,
                    user = self.user,
                    password = self.password,
                    database = self.database
                )
                print("[INFO] MySQL connection established.")
        except Error as e:
            print(f"[ERROR] Could not connect to MySQL:  {e}")
            self.connection = None

    def close(self):
        """Close the MySQL database connection"""
        if self.connection and self.connection.is_connected():
            self.connection.close()
            print("[INFO] MySQL connection closed.")

    # Core Query Execution Methods (INSERT, UPDATE, DELETE, and SELECT database info manually)
    def execute_query(self, query: str, params: Optional[Tuple[Any, ...]] = None) -> None:
        """
        Execute a SQL query that modifies data (INSERT, UPDATE, DELETE)

        Parameters:
            query (str): The full SQL query to execute.
            params (tuple): Optional paramters to safely insert into the query.

        Example Usage:
            db.execute_query("INSERT INTO users (username, email) VALUES (%s, %s)", ("HopeTaylor3", "hopetaylor@example.com"))
        """
        self.connect()
        cursor = None

        # Double-check active connection before continuing
        if not self.connection or not getattr(self.connection, "is_connected", lambda: False)():
            print("[ERROR] No active database connection.")
            return

        try:
            cursor = self.connection.cursor()
            cursor.execute(query, params or ())
            self.connection.commit()
            print(f"[SUCCESS] Query executed: {query}")
        except Error as e:
            print(f"[ERROR] Query failed: {e}")
            self.connection.rollback()
        except Exception as ex:
            print(f"[ERROR] Unexpected failure during query: {ex}")
            self.connection.rollback()
        finally:
            if cursor:
                cursor.close()

    def insert_value(self, table: str, data: Dict[str, Any]) -> Optional[int]:
        """
        Insert a new record into any table dynamically.

        Parameters:
            table (str): The table of the database to perfrom the insertion
            data (Dict[str, Any]): A dictionary of column-value pairs where
                - Keys are the column names of the table
                - Values are the data to insert into that column

        Example Usage:
            db.insert_value("Ingredients", {"ingredient_name": "Oats", "calories": 389})

        Returns:
            int or None: The auto-generated primary key ID if successful, or None if failed
        """
        self.connect()
        cursor = None

        # Double-check active connection before continuing
        if not self.connection or not getattr(self.connection, "is_connected", lambda: False)():
            print("[ERROR] No active database connection.")
            return

        try:
            cursor = self.connection.cursor()
            fields = ", ".join(data.keys())
            placeholders = ", ".join(["%s"] * len(data))
            values = tuple(data.values())

            query = f"INSERT INTO {table} ({fields}) VALUES ({placeholders})"
            print(f"[DEBUG] Executing INSERT: {query} with {values}")

            cursor.execute(query, values)
            self.connection.commit()
            new_id = cursor.lastrowid
            print(f"[SUCCESS] Inserted into '{table}' with ID {new_id}.")
            return new_id
        except Error as e:
            print(f"[ERROR] Failed to insert into '{table}': {e}")
            self.connection.rollback()
            return None
        except Exception as ex:
            print(f"[ERROR] Unexpected failure during query: {ex}")
            self.connection.rollback()
            return None
        finally:
            if cursor:
                cursor.close()
    
    def delete_value(self, table: str, where: Dict[str, any]) -> int:
        """
        Delete one or more records from a table using a WHERE clause.
        The WHERE clause is required in order to prevent full table deletion.

        Parameters:
            table (str): The table of the database to perfrom the deletion
            where (Dict[str, Any]): A dictionary specifying he condition(s) for deletion where
                - Keys are the column names
                - Values are the values to match in order to restrict the DELETE command

        Example Usage:
            db.delete_value("Ingredients", {"ingredient_ID": 5})

        Returns:
            int: The number of rows deleted from which table.
        """
        self.connect()
        cursor = None

        # Double-check active connection before continuing
        if not self.connection or not getattr(self.connection, "is_connected", lambda: False)():
            print("[ERROR] No active database connection.")
            return

        try:
            cursor = self.connection.cursor()

            if not where:
                print("[ERROR] DELETE operation requires a WHERE condition to prevent full-table deletion.")
                return 0
            
            # Build WHERE clause dynamically
            conditions = " AND ".join([f"{col} = %s" for col in where.keys()])
            values = tuple(where.values())

            query = f"DELETE FROM {table} WHERE {conditions}"
            print(f"[DEBUG] Executing DELETE: {query} with {values}.")

            cursor.execute(query, values)
            self.connection.commit()

            affected = cursor.rowcount
            print(f"[SUCCESS] Deleted {affected} records from '{table}'.")
            return affected
        
        except Error as e:
            print(f"[ERROR] Failed to delete from '{table}': {e}")
            self.connection.rollback()
            return 0
        
        except Exception as ex:
            print(f"[ERROR] Unexpected failure during query: {ex}")
            self.connection.rollback()
            return 0
        
        finally:
            if cursor:
                cursor.close()

    def update_value(self, table: str, updates: Dict[str, Any], where: Dict[str, Any]) -> int:
        """
        Update one or more records in a specified table using a WHERE clause.

        Parameters:
            table (str): The name of the table to update.
            updates (dict[str, Any]): A dictionary of column-value pairs representing the new values to set where
                - Keys are the column names
                - Values are what the coulmns are being updated to
            Example:
                {"calories": 120, "protein": 8.0}
            will generate:
                SET calories = 120, protein = 8.0
 
            where (dict[str, Any]): A dictionary specifying the condition(s) for which rows to update.
            Example:
                {"ingredient_name": "Milk"}
            will generate:
                WHERE ingredient_name = 'Milk'

        Example Usage:
            rows = db.update_value(
                "Ingredients",
                {"calories": 120, "protein": 8.0},
                {"ingredient_name": "Milk"}
            )

        Returns:
            int: The number of rows updated (0 if none were modified or on error)
        """
        self.connect()
        cursor = None

        # Double-check active connection before continuing
        if not self.connection or not getattr(self.connection, "is_connected", lambda: False)():
            print("[ERROR] No active database connection.")
            return

        try:
            cursor = self.connection.cursor()

            if not updates:
                print(f"[ERROR] UPDATE operation requires at least one field to modify.")
                return 0
            if not where:
                print(f"[ERROR] UPDATE operation requires a WHERE condition to prevent full-table overwriting.")
                return 0
            
            # Build SET and WHERE clauses dynamically
            set_clause = ", ".join([f"{col} = %s" for col in updates.keys()])
            where_clause = " AND ".join([f"{col} = %s" for col in where.keys()])

            values = tuple(updates.values()) + tuple(where.values())
            query = f"UPDATE {table} SET {set_clause} WHERE {where_clause}"

            print(f"[DEBUG] Executing UPDATE: {query} with {values}")

            cursor.execute(query, values)
            self.connection.commit()

            affected = cursor.rowcount
            print(f"[SUCCESS] Updated {affected} record(s) in '{table}'.")
            return affected
        except Error as e:
            print(f"[ERROR] Failed to update '{table}': {e}")
            self.connection.rollback()
            return 0
        except Exception as ex:
            print(f"[ERROR] Unexpected failure during query: {ex}")
            self.connection.rollback()
            return 0
        finally:
            if cursor:
                cursor.close()

    def fetch_all(self, query: str, params: Optional[Tuple[Any, ...]] = None) -> List[Dict[str, Any]]:
        """
        Execute a SELECT query and return all results as a list of dictionaries.

        Parameters:
            query (str): The full SQL SELECT query to execute
            params (tuple): Optional parameters for query substitution

        Example Usage:
            users = db.fetch_all("SELECT * FROM users")
            for user in users:
                print(user)

        Returns:
            List[Dict[str, Any]]: List of results as dictionaries keyed by column name
        """
        self.connect()
        cursor = None

        # Double-check active connection before continuing
        if not self.connection or not getattr(self.connection, "is_connected", lambda: False)():
            print("[ERROR] No active database connection.")
            return

        try:
            cursor = self.connection.cursor(dictionary=True)
            cursor.execute(query, params or ())
            results = cursor.fetchall()
            print(f"[INFO] Fetched {len(results)} rows.")
            return results
        except Error as e:
            print(f"[ERROR] Query failed: {e}")
            self.connection.rollback()
            return []
        except Exception as ex:
            print(f"[ERROR] Unexpected failure during query: {ex}")
            self.connection.rollback
            return []
        finally:
            if cursor:
                cursor.close()

    def fetch_one(self, query: str, params: Optional[Tuple[Any, ...]] = None) -> Optional[Dict[str, Any]]:
        """
        Execute a SELECT query and return one result.

        Parameters:
            query (str): The full SQL SELECT query to execute
            params (tuple): Optional parameters for query substitution

        Example Usage:
            single_user = db.fetch_one("SELECT * FROM users WHERE username = %s", ("HopeTaylor",))
            if single_user:
                print(f"User found: {single_user['email']}")

        Returns:
            dict or None: Single result row as a dictionary, or None if the user is not found.
        """
        self.connect()
        cursor = None

        # Double-check active connection before continuing
        if not self.connection or not getattr(self.connection, "is_connected", lambda: False)():
            print("[ERROR] No active database connection.")
            return

        try:
            cursor = self.connection.cursor(dictionary=True)
            cursor.execute(query, params or ())
            result = cursor.fetchone()
            print("[INFO] Fetched one row." if result else "[INFO] No result found.")
            return result
        except Error as e:
            print(f"[ERROR] Query failed: {e}")
            self.connection.rollback()
            return None
        except Exception as ex:
            print(f"[ERROR] Unexpected faliure during query: {ex}")
            self.connection.rollback()
            return None
        finally:
            if cursor:
                cursor.close()
    
    '''
    The following methods use a helper method along with the
    fetch_one() method to get specific fields of the recipe table.

    Parameters:
        recipe_id (int): The id of the recipe to pull from

    Example Usage of Three Methods:
    recipe_id = 1

    num_ing = db.get_num_ingredients(recipe_id)
    cost_per_serv = db.get_cost_per_serving(recipe_id)
    grade = db.get_nutrition_grade(recipe_id)
    '''

    def get_num_ingredients(self, recipe_id: int) -> Optional[int]:
        return self.get_recipe_helper(recipe_id, "num_ingredients")
    
    def get_ingredient_cost_sum(self, recipe_id: int) -> Optional[float]:
        return self.get_recipe_helper(recipe_id, "ingredient_cost_sum")
    
    def get_cost_cook(self, recipe_id: int) -> Optional[float]:
        return self.get_recipe_helper(recipe_id, "cost_cook")
    
    def get_cost_per_serving(self, recipe_id: int) -> Optional[float]:
        return self.get_recipe_helper(recipe_id, "cost_per_serving")
    
    def get_cart_cost(self, recipe_id: int) -> Optional[float]:
        return self.get_recipe_helper(recipe_id, "cart_cost")
    
    def get_nutrition_grade(self, recipe_id: int) -> Optional[str]:
        return self.get_recipe_helper(recipe_id, "nutrition_grade")

# Ingredient / Recipe Helpers

    def insert_ingredients(self, ingredient: Dict[str, Any]) -> Optional[int]:
        """
        Insert a single new ingredient record into the Ingredients table.
        Expects keys matching column names.
        Expected dictionary keys:
            ingredient_name, serving_size_g, promo, promo_per_unit_estimate,
            national_price, regular_per_unit_estimate, calories, protein,
            carbs, fats, fiber, sugars, vitamin_a, vitamin_c, vitamin_d,
            calcium, iron, potassium, sodium, magnesium, cholesterol,
            saturated_fat, trans_fat

        Only required field: ingredient_name

        Parameters:
            ingredient (dict): Key-value pairs matching Ingredients table columns.

        Example Usage:
            oats_id = db.insert_ingredient({
                "ingredient_name": "Oats",
                "calories": 389,
                "protein": 16.9,
                "carbs": 66.3,
                "fats": 6.9
            })

        Returns:
            int or None: Auto-generated ingredient_ID if successful, None otherwise
        """
        self.connect()
        cursor = None
        ingredient_name = ingredient.get("ingredient_name")

        # Double-check active connection before continuing
        if not self.connection or not getattr(self.connection, "is_connected", lambda: False)():
            print("[ERROR] No active database connection.")
            return

        if not ingredient_name:
            print("[ERROR] Ingredient must include an 'ingredient_name' field.")
            return None

        try:
            cursor = self.connection.cursor(dictionary=True)

            # Check if an ingredient already exists (case-insensitive)
            cursor.execute (
                "SELECT ingredient_ID FROM Ingredients WHERE LOWER(ingredient_name) = LOWER(%s)",
                (ingredient_name,)
            )
            existing = cursor.fetchone()
            if existing:
                print(f"[WARNING] Ingredient '{ingredient_name}' already exists with ID {existing['ingredient_ID']}.")
                return existing["ingredient_ID"]
            
            # If not found, insert new record
            fields = ", ".join(ingredient.keys())
            placeholders = ", ".join(["%s"] * len(ingredient))
            values = tuple(ingredient.values())

            query = f"INSERT INTO Ingredients({fields}) VALUES ({placeholders})"
            cursor.execute(query, values)
            self.connection.commit()
            ingredient_id = cursor.lastrowid
            print(f"[SUCCESS] Inserted ingredient ID {ingredient_id}")
            return ingredient_id
        
        except Error as e:
            print(f"[ERROR] Failed to insert ingredient: {e}")
            self.connection.rollback()
            return None
        
        except Exception as ex:
            print(f"[ERROR] Unexpected failure during query: {ex}")
            self.connection.rollback()
            return None
        
        finally:
            if cursor:
                cursor.close()

    def insert_recipe(self, recipe: Dict[str, Any], ingredients: List[Dict[str, Any]]) -> Optional[int]:
        """
        Insert a recipe and its ingredient list.

        Parameters:
            recipe (dict): Must contain 'recipe_name' and optionally 'instructions'.
            ingredients (list[dict]): List of ingredient mappings.
                Each ingredient entry should have 'ingredient_ID' (int) and 'quantity' (float).

        Example Usage:
            recipe_id = db.insert_recipe({
                "recipe_name": "Overnight Oats",
                "instructions": "Mix oats and milk. Refrigerate overnight."},
                [{"ingredient_ID": oats_id, "quantity": 50.0}]
            )

        Returns:
            int or None: Auto-generated recipe_ID if successful, None otherwise
        """
        self.connect()
        cursor = None
        recipe_name = recipe.get("recipe_name")

        # Double-check active connection before continuing
        if not self.connection or not getattr(self.connection, "is_connected", lambda: False)():
            print("[ERROR] No active database connection.")
            return

        if not recipe_name:
            print("[ERROR] Recipe must include a 'recipe_name' field.")
            return None

        cursor = None
        try:
            cursor = self.connection.cursor(dictionary=True)

            # Check if a recipe with the same name already exists (case-insenstive)
            cursor.execute(
                "SELECT recipe_ID FROM Recipes WHERE LOWER(recipe_name) = LOWER(%s)",
                (recipe_name,)
            )
            existing = cursor.fetchone()

            if existing:
                existing_id = existing["recipe_ID"] if isinstance(existing, dict) else existing[0]
                print(f"[WARNING] Recipe '{recipe_name}' already exists with ID {existing_id}.")
                return existing_id

            # Insert recipe into database
            cursor.execute(
                "INSERT INTO Recipes (recipe_name, instructions) VALUES (%s, %s)",
                (recipe.get("recipe_name"), recipe.get("instructions", "")) # Default empty string if instructions are not provided
            )
            recipe_id = cursor.lastrowid

            # Link ingredients to recipe
            for ing in ingredients:
                cursor.execute(
                    "INSERT INTO RecipeIngredients (recipe_ID, ingredient_ID, quantity) VALUES (%s, %s, %s)",
                    (recipe_id, ing["ingredient_ID"], ing["quantity"])
                )
            
            self.connection.commit()
            print(f"[SUCCESS] Inserted recipe ID {recipe_id} with {len(ingredients)} ingredients.")
            return recipe_id
        
        except Error as e:
            print(f"[ERROR] Failed to insert recipe: {e}")
            self.connection.rollback()
            return None
        
        except Exception as ex:
            print(f"[ERROR] Unexpected failure during query: {ex}")
            self.connection.rollback()
            return None
        
        finally:
            if cursor:
                cursor.close()
    
    def insert_ingredient_object(self, ingredient: Ingredient) -> Optional[int]:
        """
        Insert an ingredient dataclass object into the Ingredients table.
        Converts the Ingredient dataclass into a dictionary and automatically
        maps field names to the corresponding SQL column.

        Parameters:
            ingredient (Ingredient): The Ingredient object to insert based on Aramaea's code.

        Returns:
            int or None: The auto_generated ingredient_ID if successful, None otherwise
        """
        self.connect()
        cursor = None

        # Double-check active connection before continuing
        if not self.connection or not getattr(self.connection, "is_connected", lambda: False)():
            print("[ERROR] No active database connection.")
            return

        try:
            data = asdict(ingredient)

            # Remove fields that aren't part of the Ingredients table
            nutrients = data.pop("nutrients", None)

            # Build the dictionary that matches the ingredients table
            ingredient_data = {
                # Basic Data
                "ingredient_name": data.get("name"),
                "serving_size_g": self._safe_float(data.get("serving_size", 100.0)),
                "promo": self._safe_float(data.get("local_promo")),
                "promo_per_unit_estimate": self._safe_float(data.get("local_promo_per_unit_estimate")),
                "national_price": self._safe_float(data.get("national_regular", 0.0)),
                "regular_per_unit_estimate": self._safe_float(data.get("national_regular_per_unit_estimate", 0.0)),

                # Macronutrients
                "calories": self._safe_float(self._get_nutrient(nutrients, "Calories")),
                "protein": self._safe_float(self._get_nutrient(nutrients, "Protein")),
                "carbs": self._safe_float(self._get_nutrient(nutrients, "Carbohydrate")),
                "fats": self._safe_float(self._get_nutrient(nutrients, "Total Fat")),
                "fiber": self._safe_float(self._get_nutrient(nutrients, "Fiber")),
                "sugars": self._safe_float(self._get_nutrient(nutrients, "Sugar")),

                # Micronutrients
                "vitamin_a": self._safe_float(self._get_nutrient(nutrients, "Vitamin A")),
                "vitamin_c": self._safe_float(self._get_nutrient(nutrients, "Vitamin C")),
                "vitamin_d": self._safe_float(self._get_nutrient(nutrients, "Vitamin D")),
                "calcium": self._safe_float(self._get_nutrient(nutrients, "Calcium")),
                "iron": self._safe_float(self._get_nutrient(nutrients, "Iron")),
                "potassium": self._safe_float(self._get_nutrient(nutrients, "Potassium")),
                "sodium": self._safe_float(self._get_nutrient(nutrients, "Sodium")),
                "magnesium": self._safe_float(self._get_nutrient(nutrients, "Magnesium")),
                "cholesterol": self._safe_float(self._get_nutrient(nutrients, "Cholesterol")),
                "saturated_fat": self._safe_float(self._get_nutrient(nutrients, "Saturated Fat")),
                "trans_fat": self._safe_float(self._get_nutrient(nutrients, "Trans Fat"))
            }
            

            # Remove any None or empty entries
            clean_data = {k: v for k, v in ingredient_data.items() if v not in (None, "", [])}

            if not clean_data.get("ingredient_name"):
                print("[ERROR] Ingredient object missing required 'name' field.")
                return None
            
            new_id = self.insert_value("Ingredients", clean_data)
            return new_id
        
        except Exception as e:
            print(f"[ERROR] Failed to insert Ingredient object: {e}")
            self.connection.rollback()
            return None
        finally:
            if cursor:
                cursor.close()
    
    # Helper: Safely convert to float
    def _safe_float(self, value):
        if value in (None, "", [], {}):
            return 0.0
        if isinstance(value, int):
            value = value 
            return value
        if isinstance(value, str) and value.startswith("$"):
            value = value.replace("$", "")
            return float(value)
        try:
            return float(value)
        except (TypeError, ValueError):
            print(f"[ERROR] Failed to safely convert value to float.")
            return 0.0
        
    # Helper: Extract a nutrient value by displayName
    def _get_nutrient(self, ingredient_data, nutrient_name):
        for n in ingredient_data or []:
            if n.get("displayName", "").lower() == nutrient_name.lower():
                return n.get("quantity", 0.0)
        return 0.0
    
    # Helper: Call fetch_one() with other paramters
    def get_recipe_helper(self, recipe_id: int, column: str) -> Optional[Any]:
        """
        Internal helper to fetch a single column from the Recipes table.
        """
        row = self.fetch_one(
            f"SELECT {column} FROM Recipes WHERE recipe_ID = %s",
            (recipe_id,)
        )
        return row[column] if row is not None else None