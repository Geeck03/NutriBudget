from dataclasses import dataclass
import sys
import mysql.connector
from mysql.connector import Error

from PasswordHash import hash_password


DB_USER = "442819"
DB_HOST = "mysql-nutribudget.alwaysdata.net"
DB_PORT = 3306
DB_NAME = "nutribudget_database"
DB_PASSWORD = "b3ntMouse51"


def _connect():
    dsn = dict(
        host=DB_HOST,
        port=DB_PORT,
        user=DB_USER,
        password=DB_PASSWORD,
        database=DB_NAME,
    )
    return mysql.connector.connect(**dsn)


def ensure_users_table(conn):
    ddl = """
    CREATE TABLE IF NOT EXISTS Users (
        id INT AUTO_INCREMENT PRIMARY KEY,
        username VARCHAR(50) UNIQUE NOT NULL,
        password_hash VARCHAR(255) NOT NULL,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    ) ENGINE=InnoDB;
    """
    cursor = conn.cursor()
    cursor.execute(ddl)
    conn.commit()
    cursor.close()


def register_user(username: str, password: str) -> str:
    """Register a user.

    Returns: 'CREATED', 'EXISTS', or 'ERROR:...'
    """
    try:
        conn = _connect()
        try:
            ensure_users_table(conn)
            cur = conn.cursor()
            # case-insensitive existence check
            cur.execute("SELECT username FROM Users WHERE LOWER(username)=LOWER(%s)", (username.strip(),))
            row = cur.fetchone()
            if row is not None:
                cur.close()
                return "EXISTS"

            pwd_hash = hash_password(password)
            cur.execute("INSERT INTO Users (username, password_hash) VALUES (%s, %s)", (username.strip(), pwd_hash))
            conn.commit()
            cur.close()
            return "CREATED"
        finally:
            conn.close()
    except Error as e:
        return f"ERROR: {e}"


if __name__ == '__main__':
    # CLI modes:
    # 1) python UserRegistration.py register <username> <password>
    # 2) python UserRegistration.py register   (then provide username and password on stdin, each on its own line)
    if len(sys.argv) >= 2:
        cmd = sys.argv[1].lower()
    else:
        print("ERROR: usage: UserRegistration.py register <username> <password> or provide credentials on stdin")
        sys.exit(2)

    username = None
    password = None
    if len(sys.argv) >= 4:
        username = sys.argv[2]
        password = sys.argv[3]
    else:
        data = sys.stdin.read().splitlines()
        if len(data) >= 2:
            username = data[0].strip()
            password = data[1].strip()

    if username is None or password is None:
        print("ERROR: missing credentials")
        sys.exit(2)

    if cmd == 'register':
        print(register_user(username, password))

    else:
        print("ERROR: unknown command")
