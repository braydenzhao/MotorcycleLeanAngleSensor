# Lean Angle Sensor

Standalone Android Studio project for displaying motorcycle lean angle using a phone's IMU.

## What it does

- Displays live lean angle on a semicircular ±60° gauge.
- Uses the phone's game rotation-vector sensor, falling back to the normal rotation-vector sensor.
- Calibrates the upright position by averaging samples for one second.
- Smooths the live angle to reduce vibration flicker.
- Records maximum left and right lean during a ride.
- Saves the last completed ride locally on the phone.

## Phone mounting assumption

The first version assumes:

- The phone screen faces upward.
- The phone's top edge points toward the front of the motorcycle.
- The phone is held rigidly with no movement in the mount.

Under this arrangement, positive roll is treated as a right lean. If the displayed side is reversed, the sign can be inverted in `LeanEstimator.kt`.

## Running it

1. Open the `LeanAngleSensor` folder in Android Studio.
2. Allow Gradle to sync and install any requested Android SDK components.
3. Connect an Android phone with USB debugging enabled.
4. Install and run the `app` configuration.
5. With the motorcycle stationary and upright, tap **Calibrate Upright**.
6. Tap **Start Ride** to begin updating the left/right maximums.
7. Tap **Stop Ride** to save the maximums for the last ride.

The project intentionally has no location, internet, camera, or storage permissions. Ride history is currently limited to the last saved ride; a later version can store a full ride list or export CSV data.

## Important limitation

This is an indication tool, not a certified lean-angle instrument. Acceleration, braking, bumps, tire slip, and movement in the mount can affect the reading. Always secure the phone and configure the app before riding.
