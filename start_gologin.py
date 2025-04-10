#!/usr/bin/env python3
import sys
from gologin import GoLogin

# Przyjmowanie argumentów (token i profile_id)
token = sys.argv[1]
profile_id = sys.argv[2]

# Uruchomienie profilu
gl = GoLogin({
    'token': token,
    'profile_id': profile_id,
})

try:
    # Uruchom przeglądarkę i pobierz adres debuggera
    debugger_address = gl.start()

    # Wypisz adres z wyraźnym oznaczeniem (dla łatwego parsowania przez Javę)
    print("DEBUGGER_ADDRESS:" + debugger_address)

    # Zakończ z kodem 0 (sukces)
    sys.exit(0)
except Exception as e:
    # W przypadku błędu, wypisz go na stderr i zakończ z kodem 1
    print(str(e), file=sys.stderr)
    sys.exit(1)
