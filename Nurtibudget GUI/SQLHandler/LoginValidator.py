# LoginValidator.py

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


def verify_login(username: str, password: str) -> str:
    """Verify username/password against DB.

    Returns one of: 'OK', 'NO_USER', 'INVALID_PASSWORD', 'ERROR:...'
    """
    try:
        conn = _connect()
        try:
            cur = conn.cursor()
            # case-insensitive username match
            cur.execute("SELECT password_hash FROM Users WHERE LOWER(username)=LOWER(%s)", (username.strip(),))
            row = cur.fetchone()
            cur.close()

            #FIXME: Does not display "Invalid username or password"
            if row is None:
                return "Invalid username or password."

            stored_hash = row[0]
            incoming_hash = hash_password(password)

            if incoming_hash == stored_hash:
                return "OK"
            else:
                return "Invalid username or password."
            
        finally:
            conn.close()
    except Error as e:
        return f"ERROR: {e}"


if __name__ == '__main__':
    # CLI modes:
    # 1) python LoginValidator.py login <username> <password>
    # 2) python LoginValidator.py login   (then provide username and password on stdin, each on its own line)
    if len(sys.argv) >= 2:
        cmd = sys.argv[1].lower()
    else:
        print("ERROR: usage: LoginValidator.py login <username> <password> or provide credentials on stdin")
        sys.exit(2)

    username = None
    password = None
    if len(sys.argv) >= 4:
        username = sys.argv[2]
        password = sys.argv[3]
    else:
        # read username and password from stdin (two lines)
        data = sys.stdin.read().splitlines()
        if len(data) >= 2:
            username = data[0].strip()
            password = data[1].strip()

    if username is None or password is None:
        print("ERROR: missing credentials")
        sys.exit(2)

    if cmd == 'login':
        print(verify_login(username, password))
    else:
        print("ERROR: unknown command")