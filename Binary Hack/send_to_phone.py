import time
import pyautogui
import pyperclip
import requests
import sys

# Replace with your phone's actual local IP address and chosen port
PHONE_URL = "http://10.183.113.33:8080/set-clipboard"

def main():
    time.sleep(0.2) 
   # pyautogui.hotkey('ctrl', 'c')
   # time.sleep(0.2)
    copied_text = pyperclip.paste()
    if not copied_text:
        sys.exit()

    try:
        # We use a short timeout so the invisible script doesn't hang forever if the phone is offline
        requests.post(PHONE_URL, json={"text": copied_text}, timeout=4)
    except requests.exceptions.RequestException:
        # You could add logging to a text file here later if you need to debug connection issues.
        pass

if __name__ == "__main__":
    main()
