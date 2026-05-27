import time
import pyautogui
import pyperclip
import requests

# Replace with your phone's actual local IP address and chosen port
PHONE_URL = "http://10.183.113.33:8080/get-clipboard"

def main():
    time.sleep(0.2) 

    try:
        response = requests.get(PHONE_URL, timeout=3)
        response.raise_for_status()
        data = response.json()
        new_text = data.get("text", "")

        if new_text:
            pyperclip.copy(new_text)
            time.sleep(0.1)
            pyautogui.hotkey('ctrl', 'v')

    except requests.exceptions.RequestException:
        pass

if __name__ == "__main__":
    main()
