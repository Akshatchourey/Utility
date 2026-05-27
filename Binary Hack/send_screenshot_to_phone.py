import pyautogui
import requests
import io
import sys

# Replace with your phone's actual local IP address and chosen port
PHONE_URL = "http://10.183.113.33:8080/set-image"

def main():
    try:
        # Capture the entire screen
        screenshot = pyautogui.screenshot()

        # Save to an in-memory buffer as PNG (no temp file on disk)
        buffer = io.BytesIO()
        screenshot.save(buffer, format="PNG")
        buffer.seek(0)

        # Send the image as a multipart file upload
        files = {"file": ("screenshot.png", buffer, "image/png")}
        response = requests.post(PHONE_URL, files=files, timeout=10)
        response.raise_for_status()

        data = response.json()
        print(f"Screenshot sent successfully: {data.get('filename', 'unknown')}")

    except requests.exceptions.RequestException as e:
        print(f"Failed to send screenshot: {e}")
        sys.exit(1)
    except Exception as e:
        print(f"Error capturing screenshot: {e}")
        sys.exit(1)

if __name__ == "__main__":
    main()
