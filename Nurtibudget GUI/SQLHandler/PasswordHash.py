# PasswordHash.py
import hashlib

def hash_password(password: str) -> str:
    """Return the SHA-256 hex digest for the given password.

    Raises ValueError for empty/None passwords.
    """
    if password is None or password == "":
        raise ValueError("Password cannot be blank")
    return hashlib.sha256(password.encode()).hexdigest()


def get_password_hash_for_user(conn, username):
    """Return stored password_hash for username, or None if not found."""
    sql = "SELECT password_hash FROM Users WHERE username = %s"
    cursor = conn.cursor()
    cursor.execute(sql, (username.strip(),))
    row = cursor.fetchone()
    cursor.close()

    if row is None:
        return None

    return row[0]