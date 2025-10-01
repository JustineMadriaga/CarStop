import time
from datetime import datetime, timedelta
import firebase_admin
from firebase_admin import credentials, db
import RPi.GPIO as GPIO

# --- Firebase Setup ---
cred = credentials.Certificate("carstop-b1c30-firebase-adminsdk-fbsvc-10b58e97bc.json")
firebase_admin.initialize_app(cred, {
    'databaseURL': 'https://carstop-b1c30-default-rtdb.europe-west1.firebasedatabase.app/'
})

# --- Parking Space Sensor Pins ---
spaces = [
    {"id": "space_1", "trig": 23, "echo": 24},
    {"id": "space_2", "trig": 17, "echo": 27},
    {"id": "space_3", "trig": 22, "echo": 5},
]

# --- GPIO Setup ---
GPIO.setmode(GPIO.BCM)
for space in spaces:
    GPIO.setup(space["trig"], GPIO.OUT)
    GPIO.setup(space["echo"], GPIO.IN)

# --- Distance Measurement with Timeout ---    
def measure_distance(trig, echo, timeout=0.05):
    GPIO.output(trig, True)
    time.sleep(0.00001)
    GPIO.output(trig, False)

    start_time = time.time()
    stop_time = time.time()

    # Wait for echo to go HIGH
    pulse_start = time.time()
    while GPIO.input(echo) == 0:
        start_time = time.time()
        if start_time - pulse_start > timeout:
            return 999.0

    # Wait for echo to go LOW
    pulse_end = time.time()
    while GPIO.input(echo) == 1:
        stop_time = time.time()
        if stop_time - pulse_end > timeout:
            return 999.0

    time_elapsed = stop_time - start_time
    distance = (time_elapsed * 34300) / 2
    return round(distance, 2)
	
				# --- Firebase Reservation Logic ---
def check_and_update_status(space_id, distance):
    ref = db.reference(f"parking_spaces/{space_id}")
    data = ref.get()
    now = datetime.utcnow()

    status = data.get("status", "Available") if data else "Available"
    reserved_at = data.get("reservedAt")
    reserved_for = data.get("reservedForHours")

    is_reserved = False
    if reserved_at and reserved_for:
        try:
            reserved_time = datetime.fromisoformat(reserved_at)
            expiry_time = reserved_time + timedelta(hours=int(reserved_for))
            if now < expiry_time:
                is_reserved = True
            else:
	                # Reservation expired
                ref.update({
                    "status": "Available",
                    "reservedBy": None,
                    "reservedAt": None,
                    "reservedForHours": None
                })
                status = "Available"
        except Exception as e:
            print(f"[ERROR] Could not parse reservation time: {e}")
            is_reserved = False

    # --- Set New Status Based on Distance and Reservation ---
    if distance < 10:
        new_status = "Occupied"
    else:
        new_status = "Reserved" if is_reserved else "Available"

    ref.update({
        "status": new_status,
        "distance": distance
    })
	
	print(f"{space_id}: {new_status} ({distance} cm)")

# --- Main Loop ---
try:
    print("Starting 3-sensor parking monitor... Press Ctrl+C to stop.")
    while True:
        for space in spaces:
            print(f"Checking {space['id']}...")
            dist = measure_distance(space["trig"], space["echo"])
            check_and_update_status(space["id"], dist)
        time.sleep(2)

except KeyboardInterrupt:
    print("Stopping monitor. Cleaning up GPIO.")
    GPIO.cleanup()